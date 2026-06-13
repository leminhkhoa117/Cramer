package com.cramer.writing.service;

import com.cramer.assessment.service.AttemptWriteBackPort;
import com.cramer.assessment.service.AttemptWriteBackPort.AttemptContext;
import com.cramer.billing.service.UsageBillingPort;
import com.cramer.catalog.service.ContentLookupPort;
import com.cramer.catalog.service.ReviewSection;
import com.cramer.platform.common.ielts.Skill;
import com.cramer.platform.error.OperationNotAllowedException;
import com.cramer.platform.error.RateLimitExceededException;
import com.cramer.platform.ratelimit.RateLimiter;
import com.cramer.writing.domain.WritingStatus;
import com.cramer.writing.domain.WritingSubmission;
import com.cramer.writing.repository.WritingSubmissionRepository;
import com.cramer.writing.web.dto.WritingReviewView;
import com.cramer.writing.web.dto.WritingStatusResponse;
import com.cramer.writing.web.dto.WritingTaskReview;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Writing submission lifecycle (SPEC-13 §3/§5/§6). Attempt completion is delegated to the
 * assessment {@link AttemptWriteBackPort} (writing never writes {@code test_attempts}). AI
 * grading is dispatched <strong>after commit</strong> and billed only on success (SPEC-13 §4).
 */
@Service
public class WritingSubmissionService {

    private final WritingSubmissionRepository submissions;
    private final WritingGradingDispatcher dispatcher;
    private final WritingBandCalculator calculator;
    private final ContentLookupPort content;
    private final AttemptWriteBackPort attemptWriteBack;
    private final RateLimiter rateLimiter;

    public WritingSubmissionService(WritingSubmissionRepository submissions,
                                    WritingGradingDispatcher dispatcher,
                                    WritingBandCalculator calculator,
                                    ContentLookupPort content,
                                    AttemptWriteBackPort attemptWriteBack,
                                    RateLimiter rateLimiter) {
        this.submissions = submissions;
        this.dispatcher = dispatcher;
        this.calculator = calculator;
        this.content = content;
        this.attemptWriteBack = attemptWriteBack;
        this.rateLimiter = rateLimiter;
    }

    /** Save/update a draft essay (SPEC-13 §3). Ownership enforced via the parent attempt. */
    @Transactional
    public void saveDraft(long attemptId, int taskNumber, String essayText, UUID userId) {
        attemptWriteBack.requireOwnedContext(attemptId, userId); // 403/404 guard
        WritingSubmission s = submissions.findByAttemptIdOrderByTaskNumberAsc(attemptId).stream()
                .filter(x -> x.getTaskNumber() == taskNumber)
                .findFirst()
                .orElseGet(WritingSubmission::new);
        s.setAttemptId(attemptId);
        s.setUserId(userId);
        s.setTaskNumber(taskNumber);
        s.setEssayText(essayText);
        s.setWordCount(calculator.countWords(essayText));
        s.setGradingStatus(WritingStatus.PENDING);
        submissions.save(s);
    }

    /**
     * Submit 1–2 essays (SPEC-13 §3): rate-limit, complete the attempt (+cancel siblings), save
     * submissions PENDING, then dispatch async grading after commit. Billing happens at grading
     * time (after success), not here.
     */
    @Transactional
    public WritingStatusResponse submit(long attemptId, Map<Integer, String> essays, UUID userId) {
        if (!rateLimiter.tryConsume(userId, RateLimiter.GRADING)) {
            throw new RateLimitExceededException("Too many grading requests; please wait a moment");
        }
        AttemptContext ctx = attemptWriteBack.completeForGrading(attemptId, userId);

        submissions.deleteByAttemptId(attemptId);
        for (Map.Entry<Integer, String> e : essays.entrySet()) {
            WritingSubmission s = new WritingSubmission();
            s.setAttemptId(attemptId);
            s.setUserId(userId);
            s.setTaskNumber(e.getKey());
            s.setEssayText(e.getValue());
            s.setWordCount(calculator.countWords(e.getValue()));
            s.setGradingStatus(WritingStatus.PENDING);
            submissions.save(s);
        }

        dispatchAfterCommit(attemptId, userId, ctx.examSource(), ctx.testNumber());
        return status(attemptId, userId);
    }

    /** Re-grade a completed attempt's existing submissions (SPEC-13 §6). */
    @Transactional
    public WritingStatusResponse regrade(long attemptId, UUID userId) {
        if (!rateLimiter.tryConsume(userId, RateLimiter.GRADING)) {
            throw new RateLimitExceededException("Too many grading requests; please wait a moment");
        }
        AttemptContext ctx = attemptWriteBack.requireOwnedContext(attemptId, userId);
        List<WritingSubmission> existing = submissions.findByAttemptIdOrderByTaskNumberAsc(attemptId);
        if (existing.isEmpty()) {
            throw new OperationNotAllowedException("No submissions to regrade for attempt " + attemptId);
        }
        for (WritingSubmission s : existing) {
            s.setGradingStatus(WritingStatus.PENDING);
            s.setOverallBand(null);
            s.setBandScores(null);
            s.setAiFeedback(null);
            submissions.save(s);
        }
        dispatchAfterCommit(attemptId, userId, ctx.examSource(), ctx.testNumber());
        return status(attemptId, userId);
    }

    /** Aggregate grading status (SPEC-13 §5). */
    @Transactional(readOnly = true)
    public WritingStatusResponse status(long attemptId, UUID userId) {
        attemptWriteBack.requireOwnedContext(attemptId, userId);
        List<WritingSubmission> tasks = submissions.findByAttemptIdOrderByTaskNumberAsc(attemptId);
        int completed = (int) tasks.stream().filter(t -> t.getGradingStatus() == WritingStatus.COMPLETED).count();
        int failed = (int) tasks.stream().filter(t -> t.getGradingStatus() == WritingStatus.FAILED).count();
        boolean allTerminal = tasks.stream().allMatch(t ->
                t.getGradingStatus() == WritingStatus.COMPLETED || t.getGradingStatus() == WritingStatus.FAILED);
        boolean anyGrading = tasks.stream().anyMatch(t -> t.getGradingStatus() == WritingStatus.GRADING);

        String overall;
        if (tasks.isEmpty()) {
            overall = "PENDING";
        } else if (allTerminal && failed == 0) {
            overall = "COMPLETED";
        } else if (allTerminal) {
            overall = "PARTIAL_FAILURE";
        } else if (anyGrading) {
            overall = "GRADING";
        } else {
            overall = "PENDING";
        }

        List<WritingStatusResponse.TaskStatus> taskStatuses = tasks.stream()
                .map(t -> new WritingStatusResponse.TaskStatus(t.getTaskNumber(), t.getGradingStatus().name()))
                .toList();
        return new WritingStatusResponse(attemptId, overall, tasks.size(), completed, failed, taskStatuses);
    }

    /** Full review with weighted overall band and task prompts (SPEC-13 §5). */
    @Transactional(readOnly = true)
    public WritingReviewView review(long attemptId, UUID userId) {
        AttemptContext ctx = attemptWriteBack.requireOwnedContext(attemptId, userId);
        List<WritingSubmission> tasks = submissions.findByAttemptIdOrderByTaskNumberAsc(attemptId);
        Map<Integer, ReviewSection> prompts = loadPrompts(ctx.examSource(), ctx.testNumber());

        Double task1 = bandOf(tasks, 1);
        Double task2 = bandOf(tasks, 2);
        Double weighted = calculator.weightedOverall(task1, task2);

        List<WritingTaskReview> taskReviews = tasks.stream().map(t -> {
            ReviewSection p = prompts.get(t.getTaskNumber());
            return new WritingTaskReview(
                    t.getTaskNumber(),
                    t.getGradingStatus().name(),
                    t.getEssayText(),
                    t.getWordCount(),
                    t.getOverallBand(),
                    t.getBandScores(),
                    t.getAiFeedback(),
                    p == null ? null : p.passageText(),
                    p == null ? null : p.displayContentUrl(),
                    t.getGradedAt());
        }).toList();

        return new WritingReviewView(attemptId, weighted, null, taskReviews);
    }

    @Transactional(readOnly = true)
    public List<WritingTaskReview> rawSubmissions(long attemptId, UUID userId) {
        attemptWriteBack.requireOwnedContext(attemptId, userId);
        return submissions.findByAttemptIdOrderByTaskNumberAsc(attemptId).stream()
                .map(t -> new WritingTaskReview(t.getTaskNumber(), t.getGradingStatus().name(), t.getEssayText(),
                        t.getWordCount(), t.getOverallBand(), t.getBandScores(), t.getAiFeedback(), null, null,
                        t.getGradedAt()))
                .toList();
    }

    private Double bandOf(List<WritingSubmission> tasks, int taskNumber) {
        return tasks.stream()
                .filter(t -> t.getTaskNumber() == taskNumber && t.getOverallBand() != null)
                .map(t -> t.getOverallBand().doubleValue())
                .findFirst()
                .orElse(null);
    }

    private Map<Integer, ReviewSection> loadPrompts(String examSource, String testNumber) {
        Map<Integer, ReviewSection> map = new HashMap<>();
        int tn;
        try {
            tn = Integer.parseInt(testNumber.trim());
        } catch (NumberFormatException e) {
            return map;
        }
        for (ReviewSection s : content.reviewContent(examSource, tn, Skill.WRITING)) {
            map.put(s.partNumber(), s);
        }
        return map;
    }

    /** Dispatch grading only after the surrounding transaction commits (SPEC-04 §5). */
    private void dispatchAfterCommit(long attemptId, UUID userId, String examSource, String testNumber) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatcher.gradeAttempt(attemptId, userId, examSource, testNumber);
                }
            });
        } else {
            dispatcher.gradeAttempt(attemptId, userId, examSource, testNumber);
        }
    }
}
