package com.cramer.service;

import com.cramer.dto.GradingStatusDTO;
import com.cramer.entity.CreditTransaction;
import com.cramer.entity.Section;
import com.cramer.entity.TestAttempt;
import com.cramer.entity.WritingSubmission;
import com.cramer.repository.SectionRepository;
import com.cramer.repository.WritingSubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Separate service for async grading operations.
 * This ensures @Async works correctly via Spring proxy.
 * 
 * Uses server-side DeepSeek API key for all grading operations.
 */
@Service
public class AsyncGradingService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncGradingService.class);

    // Cost of AI grading (ATTEMPT_AI) in Lúa when subscription limit is exceeded
    // Per spec: 20 Lúa per ATTEMPT_AI overage
    private static final int AI_GRADING_LUA_COST = 20;

    private final WritingSubmissionRepository writingSubmissionRepository;
    private final SectionRepository sectionRepository;
    private final LLMGradingService llmGradingService;
    private final SubscriptionService subscriptionService;
    private final CreditService creditService;

    // Server's DeepSeek API key (required)
    @Value("${DEEPSEEK_API_KEY:}")
    private String deepSeekApiKey;

    @Value("${DEEPSEEK_MODEL:deepseek-chat}")
    private String deepSeekModel;

    @Autowired
    public AsyncGradingService(WritingSubmissionRepository writingSubmissionRepository,
            SectionRepository sectionRepository,
            LLMGradingService llmGradingService,
            SubscriptionService subscriptionService,
            CreditService creditService) {
        this.writingSubmissionRepository = writingSubmissionRepository;
        this.sectionRepository = sectionRepository;
        this.llmGradingService = llmGradingService;
        this.subscriptionService = subscriptionService;
        this.creditService = creditService;
    }

    /**
     * Async method to grade submissions in background with PARALLEL execution.
     * Task 1 and Task 2 are graded simultaneously for faster results.
     * IMPORTANT: This must be called from another bean (not from within this class)
     * for the @Async proxy to work.
     */
    @Async
    public void gradeSubmissionsAsync(List<WritingSubmission> submissions, TestAttempt attempt, UUID userId) {
        logger.info("🚀 Starting PARALLEL ASYNC grading for {} submissions (thread: {})",
                submissions.size(), Thread.currentThread().getName());

        try {
            // Validate server API key is configured
            if (deepSeekApiKey == null || deepSeekApiKey.trim().isEmpty()) {
                logger.error("❌ DEEPSEEK_API_KEY not configured on server!");
                for (WritingSubmission submission : submissions) {
                    submission.setGradingStatus("FAILED");
                    Map<String, Object> errorFeedback = new HashMap<>();
                    errorFeedback.put("error", "Hệ thống chấm điểm AI đang bảo trì. Vui lòng thử lại sau.");
                    submission.setAiFeedback(errorFeedback);
                    writingSubmissionRepository.save(submission);
                }
                return;
            }

            // Check if user has AI grading available (subscription or Lúa)
            GradingStatusDTO gradingStatus = subscriptionService.checkAIGradingAllowed(userId);
            boolean usingLua = false;

            if (!Boolean.TRUE.equals(gradingStatus.getAllowed())) {
                // Not allowed via subscription, check if can pay with Lúa
                if (Boolean.TRUE.equals(gradingStatus.getCanUseExtraWithLua())
                        && gradingStatus.getLuaBalance() >= AI_GRADING_LUA_COST) {
                    logger.info("📊 User {} exceeded subscription limit, will use Lúa for grading", userId);
                    usingLua = true;
                } else {
                    logger.warn("❌ User {} has no AI grading available (limit: {}, used: {}, lua: {})",
                            userId, gradingStatus.getLimit(), gradingStatus.getUsed(), gradingStatus.getLuaBalance());
                    for (WritingSubmission submission : submissions) {
                        submission.setGradingStatus("FAILED");
                        Map<String, Object> errorFeedback = new HashMap<>();
                        errorFeedback.put("error",
                                "Bạn đã hết lượt chấm AI trong tháng. Vui lòng nâng cấp gói hoặc mua thêm Lúa để tiếp tục.");
                        submission.setAiFeedback(errorFeedback);
                        writingSubmissionRepository.save(submission);
                    }
                    return;
                }
            }

            logger.info("✅ Using server's DeepSeek API key for grading (model: {})", deepSeekModel);

            // Track if we need to deduct Lúa (set before grading loop)
            final boolean shouldUseLua = usingLua;

            // Get task prompts
            List<Section> sections = sectionRepository.findByExamSourceAndTestNumberAndSkill(
                    attempt.getExamSource(),
                    Integer.parseInt(attempt.getTestNumber()),
                    "writing");

            Map<Integer, Section> sectionMap = sections.stream()
                    .collect(Collectors.toMap(Section::getPartNumber, s -> s));

            // 🚀 NEW: Grade submissions in PARALLEL using CompletableFuture
            List<CompletableFuture<Boolean>> gradingTasks = new ArrayList<>();

            for (WritingSubmission submission : submissions) {
                CompletableFuture<Boolean> gradingTask = CompletableFuture.supplyAsync(() -> {
                    try {
                        logger.info("📝 Grading Task {} for attempt {} (thread: {})...",
                                submission.getTaskNumber(), attempt.getId(), Thread.currentThread().getName());

                        // Set status to GRADING
                        submission.setGradingStatus("GRADING");
                        writingSubmissionRepository.save(submission);

                        Section section = sectionMap.get(submission.getTaskNumber());
                        String taskPrompt = section != null ? section.getPassageText() : "";
                        String imageUrl = section != null ? section.getDisplayContentUrl() : null;
                        String imageDescription = section != null ? section.getImageDescription() : null;

                        llmGradingService.gradeSubmission(submission, taskPrompt, imageUrl, imageDescription,
                                deepSeekApiKey, deepSeekModel);
                        writingSubmissionRepository.save(submission);

                        logger.info("✅ Graded Task {} with band {}",
                                submission.getTaskNumber(), submission.getOverallBand());

                        return "COMPLETED".equals(submission.getGradingStatus());

                    } catch (Exception e) {
                        logger.error("❌ Failed to grade Task {}: {}", submission.getTaskNumber(), e.getMessage());
                        submission.setGradingStatus("FAILED");
                        Map<String, Object> errorFeedback = new HashMap<>();
                        errorFeedback.put("error", "Grading failed: " + e.getMessage());
                        submission.setAiFeedback(errorFeedback);
                        writingSubmissionRepository.save(submission);
                        return false;
                    }
                });

                gradingTasks.add(gradingTask);
            }

            // Wait for ALL tasks to complete
            CompletableFuture<Void> allTasks = CompletableFuture.allOf(
                    gradingTasks.toArray(new CompletableFuture[0]));

            try {
                // Wait for all grading tasks with timeout (20 minutes total)
                allTasks.get(20, java.util.concurrent.TimeUnit.MINUTES);
            } catch (Exception waitEx) {
                logger.error("❌ Timeout or error waiting for parallel grading: {}", waitEx.getMessage());
                // Mark any still-pending submissions as failed
                for (WritingSubmission submission : submissions) {
                    if ("GRADING".equals(submission.getGradingStatus())) {
                        submission.setGradingStatus("FAILED");
                        Map<String, Object> errorFeedback = new HashMap<>();
                        errorFeedback.put("error", "Grading timeout after 20 minutes");
                        submission.setAiFeedback(errorFeedback);
                        writingSubmissionRepository.save(submission);
                    }
                }
            }

            // Check if ANY task succeeded
            boolean hasSuccessfulGrading = gradingTasks.stream()
                    .map(task -> {
                        try {
                            return task.get();
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .anyMatch(success -> success);

            // 🚨 NEW LOGIC: If ANY task failed, mark ALL as failed and don't charge
            boolean anyFailed = submissions.stream()
                    .anyMatch(s -> "FAILED".equals(s.getGradingStatus()));

            if (anyFailed) {
                logger.warn("⚠️ At least one task failed - marking entire attempt as FAILED and skipping billing");
                // Mark ALL submissions as failed
                for (WritingSubmission submission : submissions) {
                    submission.setGradingStatus("FAILED");
                    if (submission.getAiFeedback() == null || !submission.getAiFeedback().containsKey("error")) {
                        Map<String, Object> errorFeedback = submission.getAiFeedback() != null
                                ? submission.getAiFeedback()
                                : new HashMap<>();
                        errorFeedback.put("error", "Grading failed for one or more tasks. Please try again.");
                        submission.setAiFeedback(errorFeedback);
                    }
                    writingSubmissionRepository.save(submission);
                }
                hasSuccessfulGrading = false; // Override to skip billing
            }

            // Track AI grading usage ONLY if ALL tasks succeeded
            if (hasSuccessfulGrading) {
                try {
                    if (shouldUseLua) {
                        // Deduct Lúa for AI grading
                        creditService.spendCredits(userId, AI_GRADING_LUA_COST,
                                CreditTransaction.Category.AI_GRADING,
                                "Chấm điểm AI bài viết - " + attempt.getExamSource() + " Test "
                                        + attempt.getTestNumber(),
                                "attempt_" + attempt.getId());
                        logger.info("💰 Deducted {} Lúa for AI grading from user {}", AI_GRADING_LUA_COST, userId);
                    } else {
                        // Increment subscription usage counter
                        subscriptionService.incrementAIGradingUsage(userId);
                        logger.info("📊 Incremented AI grading usage for user {}", userId);
                    }
                } catch (Exception usageEx) {
                    logger.error("⚠️ Failed to track AI grading usage for user {}: {}", userId, usageEx.getMessage());
                    // Don't fail the grading because of usage tracking error
                }
            } else {
                logger.warn("⚠️ Grading incomplete or failed - skipping billing for user {}", userId);
            }

            logger.info("🎉 Completed parallel async grading for attempt {}", attempt.getId());

        } catch (Exception e) {
            logger.error("💥 Error in async grading: {}", e.getMessage(), e);
        }
    }
}
