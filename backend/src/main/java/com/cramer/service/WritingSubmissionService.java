package com.cramer.service;

import com.cramer.dto.BillingResultDTO;
import com.cramer.dto.WritingReviewDTO;
import com.cramer.dto.WritingSubmissionDTO;
import com.cramer.entity.Section;
import com.cramer.entity.TestAttempt;
import com.cramer.entity.WritingSubmission;
import com.cramer.repository.SectionRepository;
import com.cramer.repository.TestAttemptRepository;
import com.cramer.repository.WritingSubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing Writing test submissions and grading.
 */
@Service
public class WritingSubmissionService {

    private static final Logger logger = LoggerFactory.getLogger(WritingSubmissionService.class);

    private final WritingSubmissionRepository writingSubmissionRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final SectionRepository sectionRepository;
    private final LLMGradingService llmGradingService;
    private final AsyncGradingService asyncGradingService;
    private final QuotaBillingService quotaBillingService;

    @Autowired
    public WritingSubmissionService(WritingSubmissionRepository writingSubmissionRepository,
            TestAttemptRepository testAttemptRepository,
            SectionRepository sectionRepository,
            LLMGradingService llmGradingService,
            AsyncGradingService asyncGradingService,
            QuotaBillingService quotaBillingService) {
        this.writingSubmissionRepository = writingSubmissionRepository;
        this.testAttemptRepository = testAttemptRepository;
        this.sectionRepository = sectionRepository;
        this.llmGradingService = llmGradingService;
        this.asyncGradingService = asyncGradingService;
        this.quotaBillingService = quotaBillingService;
    }

    /**
     * Save essay draft during test.
     */
    @Transactional
    public WritingSubmissionDTO saveDraft(Long attemptId, Integer taskNumber, String essayText, UUID userId) {
        TestAttempt attempt = testAttemptRepository.findById(Objects.requireNonNull(attemptId))
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));

        if (!attempt.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to attempt");
        }

        WritingSubmission submission = writingSubmissionRepository
                .findByAttemptIdAndTaskNumber(attemptId, taskNumber)
                .orElse(new WritingSubmission(attemptId, userId, taskNumber, essayText));

        submission.setEssayText(essayText);
        submission.setGradingStatus("PENDING");

        WritingSubmission saved = writingSubmissionRepository.save(submission);
        return toDTO(saved);
    }

    /**
     * Submit essays for grading (async background grading).
     * Also cleans up any stale IN_PROGRESS attempts for the same test.
     */
    @Transactional
    public Map<String, Object> submitForGrading(Long attemptId, Map<Integer, String> essays, UUID userId) {
        logger.info("Submitting writing essays for attempt {} by user {}", attemptId, userId);

        TestAttempt attempt = testAttemptRepository.findById(Objects.requireNonNull(attemptId))
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));

        if (!attempt.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to attempt");
        }

        // IMPORTANT: Cancel any other IN_PROGRESS attempts for the same test
        // This prevents "ghost" IN_PROGRESS attempts showing up in dashboard
        List<TestAttempt> otherInProgressAttempts = testAttemptRepository
                .findByUserIdAndExamSourceAndTestNumberAndSkillAndStatus(
                        userId, attempt.getExamSource(), attempt.getTestNumber(), attempt.getSkill(), "IN_PROGRESS");

        for (TestAttempt otherAttempt : otherInProgressAttempts) {
            if (!otherAttempt.getId().equals(attemptId)) {
                logger.info("Cancelling stale IN_PROGRESS attempt {} before submitting {}", otherAttempt.getId(),
                        attemptId);
                otherAttempt.setStatus("CANCELLED");
                testAttemptRepository.save(otherAttempt);
            }
        }

        // Update attempt status
        attempt.setStatus("COMPLETED");
        attempt.setCompletedAt(OffsetDateTime.now());
        testAttemptRepository.save(attempt);

        // Check Quota upfront
        BillingResultDTO quotaCheck = quotaBillingService.processAttemptBilling(userId, "WRITING", true);
        boolean isQuotaBlocked = !quotaCheck.isAllowed();

        // Save all submissions
        List<WritingSubmission> submissions = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : essays.entrySet()) {
            Integer taskNumber = entry.getKey();
            String essayText = entry.getValue();

            WritingSubmission submission = writingSubmissionRepository
                    .findByAttemptIdAndTaskNumber(attemptId, taskNumber)
                    .orElse(new WritingSubmission(attemptId, userId, taskNumber, essayText));

            submission.setEssayText(essayText);
            submission.setSubmittedAt(OffsetDateTime.now());

            if (isQuotaBlocked) {
                // If blocked, set status FAILED immediately and dont call async grader
                submission.setGradingStatus("FAILED");
                Map<String, Object> errorFeedback = new HashMap<>();
                errorFeedback.put("error", quotaCheck.getReason());
                submission.setAiFeedback(errorFeedback);
            } else {
                submission.setGradingStatus("PENDING");
            }

            submissions.add(writingSubmissionRepository.save(submission));
        }

        if (isQuotaBlocked) {
            logger.warn("User {} quota exceeded for writing submission. Saved as FAILED without async grading.",
                    userId);

            // Return result immediately without triggering async grading
            Map<String, Object> result = new HashMap<>();
            result.put("attemptId", attemptId);
            result.put("status", "COMPLETED"); // Mark attempt as completed so frontend stops polling
            result.put("submissionCount", submissions.size());
            result.put("message", "Submission saved but AI grading skipped due to quota limits.");
            return result;
        }

        // Start async grading AFTER transaction commits to ensure data is visible to
        // async thread
        // This prevents the async thread from reading uncommitted/stale data
        final List<WritingSubmission> submissionsToGrade = new ArrayList<>(submissions);
        final TestAttempt attemptToGrade = attempt;
        final UUID userIdForGrading = userId;

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    logger.info("Transaction committed. Starting async grading for attempt {}", attemptId);
                    asyncGradingService.gradeSubmissionsAsync(submissionsToGrade, attemptToGrade, userIdForGrading);
                }
            });
        } else {
            // Fallback: no active transaction synchronization (shouldn't happen in normal
            // flow)
            logger.warn("No active transaction synchronization. Starting async grading immediately for attempt {}",
                    attemptId);
            asyncGradingService.gradeSubmissionsAsync(submissionsToGrade, attemptToGrade, userIdForGrading);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("attemptId", attemptId);
        result.put("status", "GRADING_STARTED");
        result.put("submissionCount", submissions.size());
        result.put("message", "Essays submitted. Grading in progress...");

        return result;
    }

    /**
     * Get grading status for an attempt.
     */
    public Map<String, Object> getGradingStatus(Long attemptId, UUID userId) {
        TestAttempt attempt = testAttemptRepository.findById(Objects.requireNonNull(attemptId))
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found"));

        if (!attempt.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access");
        }

        List<WritingSubmission> submissions = writingSubmissionRepository.findByAttemptId(attemptId);

        boolean allCompleted = submissions.stream()
                .allMatch(s -> "COMPLETED".equals(s.getGradingStatus()) || "FAILED".equals(s.getGradingStatus()));

        boolean anyGrading = submissions.stream()
                .anyMatch(s -> "GRADING".equals(s.getGradingStatus()));

        boolean anyFailed = submissions.stream()
                .anyMatch(s -> "FAILED".equals(s.getGradingStatus()));

        Map<String, Object> status = new HashMap<>();
        status.put("attemptId", attemptId);
        status.put("totalTasks", submissions.size());
        status.put("completedTasks", submissions.stream()
                .filter(s -> "COMPLETED".equals(s.getGradingStatus())).count());
        status.put("failedTasks", submissions.stream()
                .filter(s -> "FAILED".equals(s.getGradingStatus())).count());

        // Add individual task statuses for granular progress tracking
        Map<Integer, String> taskStatuses = new HashMap<>();
        for (WritingSubmission sub : submissions) {
            taskStatuses.put(sub.getTaskNumber(), sub.getGradingStatus());
        }
        status.put("taskStatuses", taskStatuses);

        if (allCompleted && !anyFailed) {
            status.put("status", "COMPLETED");
        } else if (allCompleted && anyFailed) {
            status.put("status", "PARTIAL_FAILURE");
        } else if (anyGrading) {
            status.put("status", "GRADING");
        } else {
            status.put("status", "PENDING");
        }

        return status;
    }

    /**
     * Get full writing review with all grading details.
     */
    @Transactional(readOnly = true)
    public WritingReviewDTO getWritingReview(Long attemptId, UUID userId) {
        TestAttempt attempt = testAttemptRepository.findById(Objects.requireNonNull(attemptId))
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found"));

        if (!attempt.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access");
        }

        List<WritingSubmission> submissions = writingSubmissionRepository.findByAttemptId(attemptId);

        // Get task prompts
        List<Section> sections = sectionRepository.findByExamSourceAndTestNumberAndSkill(
                attempt.getExamSource(),
                Integer.parseInt(attempt.getTestNumber()),
                "writing");

        WritingReviewDTO review = new WritingReviewDTO();
        review.setAttemptId(attemptId);
        review.setExamSource(attempt.getExamSource());
        review.setTestNumber(attempt.getTestNumber());
        review.setSkill("writing");
        review.setStatus(attempt.getStatus());
        review.setCompletedAt(attempt.getCompletedAt());

        // Calculate duration
        if (attempt.getStartedAt() != null && attempt.getCompletedAt() != null) {
            Duration duration = Duration.between(attempt.getStartedAt(), attempt.getCompletedAt());
            review.setDuration(duration.getSeconds());
        }

        // Build task reviews
        List<WritingReviewDTO.WritingTaskReviewDTO> taskReviews = new ArrayList<>();
        BigDecimal totalBand = BigDecimal.ZERO;
        int gradedCount = 0;

        for (WritingSubmission sub : submissions) {
            WritingReviewDTO.WritingTaskReviewDTO taskReview = new WritingReviewDTO.WritingTaskReviewDTO();
            taskReview.setTaskNumber(sub.getTaskNumber());
            taskReview.setEssayText(sub.getEssayText());
            taskReview.setWordCount(sub.getWordCount());
            taskReview.setGradingStatus(sub.getGradingStatus());
            taskReview.setOverallBand(sub.getOverallBand());
            taskReview.setBandScores(sub.getBandScores());
            taskReview.setAiFeedback(sub.getAiFeedback());
            taskReview.setSubmittedAt(sub.getSubmittedAt());
            taskReview.setGradedAt(sub.getGradedAt());
            taskReviews.add(taskReview);

            if (sub.getOverallBand() != null) {
                totalBand = totalBand.add(sub.getOverallBand());
                gradedCount++;
            }
        }
        review.setTasks(taskReviews);

        // Calculate overall band (IELTS writing is average of both tasks, with Task 2
        // counting more)
        if (gradedCount > 0) {
            // IELTS Writing: Task 1 = 1/3, Task 2 = 2/3
            BigDecimal overallBand = calculateWeightedOverallBand(submissions);
            review.setOverallBand(overallBand);
        }

        // Build prompts
        List<WritingReviewDTO.WritingTaskPromptDTO> prompts = new ArrayList<>();
        for (Section section : sections) {
            WritingReviewDTO.WritingTaskPromptDTO prompt = new WritingReviewDTO.WritingTaskPromptDTO();
            prompt.setTaskNumber(section.getPartNumber());
            prompt.setPromptText(section.getPassageText());
            prompt.setImageUrl(section.getDisplayContentUrl());
            prompts.add(prompt);
        }
        review.setPrompts(prompts);

        return review;
    }

    /**
     * Calculate weighted overall band score for IELTS Writing.
     * Task 1 = 1/3 weight, Task 2 = 2/3 weight
     */
    private BigDecimal calculateWeightedOverallBand(List<WritingSubmission> submissions) {
        BigDecimal task1Band = null;
        BigDecimal task2Band = null;

        for (WritingSubmission sub : submissions) {
            if (sub.getOverallBand() != null) {
                if (sub.getTaskNumber() == 1) {
                    task1Band = sub.getOverallBand();
                } else if (sub.getTaskNumber() == 2) {
                    task2Band = sub.getOverallBand();
                }
            }
        }

        if (task1Band != null && task2Band != null) {
            // Weighted average: (Task1 * 1 + Task2 * 2) / 3
            BigDecimal weighted = task1Band.add(task2Band.multiply(BigDecimal.valueOf(2)))
                    .divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
            // Round to nearest 0.5
            double rounded = Math.round(weighted.doubleValue() * 2) / 2.0;
            return BigDecimal.valueOf(rounded).setScale(1, RoundingMode.HALF_UP);
        } else if (task1Band != null) {
            return task1Band;
        } else if (task2Band != null) {
            return task2Band;
        }

        return null;
    }

    /**
     * Get submissions for an attempt.
     */
    public List<WritingSubmissionDTO> getSubmissions(Long attemptId, UUID userId) {
        TestAttempt attempt = testAttemptRepository.findById(Objects.requireNonNull(attemptId))
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found"));

        if (!attempt.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access");
        }

        return writingSubmissionRepository.findByAttemptId(attemptId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Delete submissions for an attempt.
     */
    @Transactional
    public void deleteSubmissions(Long attemptId) {
        writingSubmissionRepository.deleteByAttemptId(attemptId);
    }

    /**
     * Validate DeepSeek API key.
     */
    public boolean validateApiKey(String apiKey) {
        return llmGradingService.validateApiKey(apiKey);
    }

    /**
     * Re-grade a completed writing attempt.
     * This resets the grading status and triggers async grading again.
     */
    @Transactional
    public Map<String, Object> regradeAttempt(Long attemptId, UUID userId) {
        logger.info("Re-grading writing attempt {} for user {}", attemptId, userId);

        TestAttempt attempt = testAttemptRepository.findById(Objects.requireNonNull(attemptId))
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));

        if (!attempt.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to attempt");
        }

        if (!"COMPLETED".equals(attempt.getStatus())) {
            throw new IllegalArgumentException("Cannot re-grade an incomplete attempt");
        }

        List<WritingSubmission> submissions = writingSubmissionRepository.findByAttemptId(attemptId);
        if (submissions.isEmpty()) {
            throw new IllegalArgumentException("No submissions found for attempt");
        }

        // Check Quota upfront for RE-GRADING
        BillingResultDTO quotaCheck = quotaBillingService.processAttemptBilling(userId, "WRITING", true);
        boolean isQuotaBlocked = !quotaCheck.isAllowed();

        // Reset grading status for all submissions
        for (WritingSubmission sub : submissions) {
            if (isQuotaBlocked) {
                // Blocked
                sub.setGradingStatus("FAILED");
                Map<String, Object> errorFeedback = new HashMap<>();
                errorFeedback.put("error", quotaCheck.getReason());
                sub.setAiFeedback(errorFeedback);
            } else {
                // Allowed
                sub.setGradingStatus("PENDING");
            }

            sub.setOverallBand(null);
            sub.setBandScores(null);
            // sub.setAiFeedback(null); // Don't clear if blocked, we just set it!
            if (!isQuotaBlocked)
                sub.setAiFeedback(null); // Clear only if allowed
            sub.setGradedAt(null);
        }

        // Batch save all submissions
        List<WritingSubmission> savedSubmissions = writingSubmissionRepository.saveAll(submissions);
        // Force flush to ensure database is updated before async call
        writingSubmissionRepository.flush();

        if (isQuotaBlocked) {
            logger.warn("User {} quota exceeded for writing re-grading. Marked as FAILED.", userId);

            Map<String, Object> result = new HashMap<>();
            result.put("attemptId", attemptId);
            result.put("status", "COMPLETED"); // Mark as completed (failed)
            result.put("submissionCount", submissions.size());
            result.put("message", "Re-grading blocked due to quota limits.");
            return result;
        }

        // Trigger async grading AFTER transaction commits to ensure data is visible to
        // async thread
        final List<WritingSubmission> submissionsToRegrade = new ArrayList<>(savedSubmissions);
        final TestAttempt attemptToRegrade = attempt;
        final UUID userIdForRegrading = userId;
        final Long attemptIdForLog = attemptId;

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    logger.info("Transaction committed. Starting async re-grading for attempt {}", attemptIdForLog);
                    asyncGradingService.gradeSubmissionsAsync(submissionsToRegrade, attemptToRegrade,
                            userIdForRegrading);
                }
            });
        } else {
            // Fallback: no active transaction synchronization (shouldn't happen in normal
            // flow)
            logger.warn("No active transaction synchronization. Starting async re-grading immediately for attempt {}",
                    attemptIdForLog);
            asyncGradingService.gradeSubmissionsAsync(submissionsToRegrade, attemptToRegrade, userIdForRegrading);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("attemptId", attemptId);
        result.put("status", "REGRADING_STARTED");
        result.put("submissionCount", submissions.size());
        result.put("message", "Re-grading in progress...");

        return result;
    }

    private WritingSubmissionDTO toDTO(WritingSubmission submission) {
        WritingSubmissionDTO dto = new WritingSubmissionDTO();
        dto.setId(submission.getId());
        dto.setAttemptId(submission.getAttemptId());
        dto.setTaskNumber(submission.getTaskNumber());
        dto.setEssayText(submission.getEssayText());
        dto.setWordCount(submission.getWordCount());
        dto.setGradingStatus(submission.getGradingStatus());
        dto.setOverallBand(submission.getOverallBand());
        dto.setBandScores(submission.getBandScores());
        dto.setAiFeedback(submission.getAiFeedback());
        dto.setSubmittedAt(submission.getSubmittedAt());
        dto.setGradedAt(submission.getGradedAt());
        return dto;
    }
}
