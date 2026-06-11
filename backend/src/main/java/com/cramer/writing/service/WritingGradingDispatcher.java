package com.cramer.writing.service;

import com.cramer.billing.service.UsageBillingPort;
import com.cramer.catalog.service.ContentLookupPort;
import com.cramer.catalog.service.ReviewSection;
import com.cramer.platform.common.ielts.Skill;
import com.cramer.writing.config.WritingAsyncConfig;
import com.cramer.writing.domain.WritingStatus;
import com.cramer.writing.domain.WritingSubmission;
import com.cramer.writing.repository.WritingSubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous writing grading (SPEC-13 §4). Dispatched <strong>after the submit transaction
 * commits</strong>. Grades tasks in parallel; if any task fails, all are marked {@code FAILED}
 * and <strong>no billing occurs</strong>. If all succeed, AI grading is charged once via
 * {@link UsageBillingPort} (charge-after-success). A post-grade billing failure never hides a
 * successful grade — results stay visible and the issue is logged for reconciliation.
 */
@Service
public class WritingGradingDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WritingGradingDispatcher.class);

    private final WritingSubmissionRepository submissions;
    private final WritingGradingService grading;
    private final ContentLookupPort content;
    private final UsageBillingPort billing;

    public WritingGradingDispatcher(WritingSubmissionRepository submissions,
                                    WritingGradingService grading,
                                    ContentLookupPort content,
                                    UsageBillingPort billing) {
        this.submissions = submissions;
        this.grading = grading;
        this.content = content;
        this.billing = billing;
    }

    /**
     * Grade all submissions for an attempt asynchronously (SPEC-13 §4). Runs on the module
     * executor; each phase uses its own transaction (this method is not itself transactional).
     */
    @Async(WritingAsyncConfig.EXECUTOR)
    public void gradeAttempt(long attemptId, UUID userId, String examSource, String testNumber) {
        if (!grading.isAvailable()) {
            log.warn("DeepSeek not configured; marking writing attempt {} FAILED", attemptId);
            markAll(attemptId, WritingStatus.FAILED);
            return;
        }

        Map<Integer, TaskPrompt> prompts = loadPrompts(examSource, testNumber);
        List<WritingSubmission> tasks = submissions.findByAttemptIdOrderByTaskNumberAsc(attemptId);

        List<CompletableFuture<Boolean>> futures = tasks.stream()
                .map(s -> CompletableFuture.supplyAsync(() -> gradeOne(s.getId(), prompts)))
                .toList();
        boolean allOk = futures.stream().map(CompletableFuture::join).reduce(true, Boolean::logicalAnd);

        if (!allOk) {
            log.warn("At least one writing task failed for attempt {}; skipping billing", attemptId);
            markAll(attemptId, WritingStatus.FAILED);
            return;
        }

        // All tasks graded — charge once, after success.
        try {
            billing.chargeAiGrading(userId, "attempt_" + attemptId);
        } catch (Exception e) {
            // Never hide a successful grade behind a billing error.
            log.error("BILLING_RECONCILIATION_REQUIRED: writing attempt {} graded but charge failed: {}",
                    attemptId, e.getMessage());
        }
    }

    /** Grade a single submission in its own transaction; returns success. */
    @Transactional
    protected boolean gradeOne(long submissionId, Map<Integer, TaskPrompt> prompts) {
        WritingSubmission s = submissions.findById(submissionId).orElse(null);
        if (s == null) {
            return false;
        }
        s.setGradingStatus(WritingStatus.GRADING);
        submissions.save(s);
        try {
            TaskPrompt p = prompts.getOrDefault(s.getTaskNumber(), TaskPrompt.EMPTY);
            GradingOutcome outcome = grading.grade(s.getTaskNumber(), s.getEssayText(), p.promptText(), p.imageDescription(), null);
            s.setOverallBand(outcome.overallBand());
            s.setBandScores(outcome.bandScores());
            s.setAiFeedback(outcome.aiFeedback());
            s.setGradingStatus(WritingStatus.COMPLETED);
            s.setGradedAt(OffsetDateTime.now());
            submissions.save(s);
            return true;
        } catch (Exception e) {
            log.error("Writing grading failed for submission {}: {}", submissionId, e.getMessage());
            s.setGradingStatus(WritingStatus.FAILED);
            submissions.save(s);
            return false;
        }
    }

    @Transactional
    protected void markAll(long attemptId, WritingStatus status) {
        for (WritingSubmission s : submissions.findByAttemptIdOrderByTaskNumberAsc(attemptId)) {
            s.setGradingStatus(status);
            submissions.save(s);
        }
    }

    private Map<Integer, TaskPrompt> loadPrompts(String examSource, String testNumber) {
        Map<Integer, TaskPrompt> map = new HashMap<>();
        int tn = parseTestNumber(testNumber);
        for (ReviewSection section : content.reviewContent(examSource, tn, Skill.WRITING)) {
            // part_number → task number; passage_text → prompt; displayContentUrl → image
            map.put(section.partNumber(), new TaskPrompt(section.passageText(), section.imageDescription()));
        }
        return map;
    }

    private static int parseTestNumber(String testNumber) {
        try {
            return Integer.parseInt(testNumber.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** A writing task's prompt context (text + optional image description). */
    record TaskPrompt(String promptText, String imageDescription) {
        static final TaskPrompt EMPTY = new TaskPrompt(null, null);
    }
}
