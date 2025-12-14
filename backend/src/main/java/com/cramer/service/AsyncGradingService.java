package com.cramer.service;

import com.cramer.dto.GradingStatusDTO;
import com.cramer.entity.CreditTransaction;
import com.cramer.entity.Profile;
import com.cramer.entity.Section;
import com.cramer.entity.TestAttempt;
import com.cramer.entity.WritingSubmission;
import com.cramer.repository.ProfileRepository;
import com.cramer.repository.SectionRepository;
import com.cramer.repository.WritingSubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Separate service for async grading operations.
 * This ensures @Async works correctly via Spring proxy.
 */
@Service
public class AsyncGradingService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncGradingService.class);
    
    // Cost of AI grading in Lúa when subscription limit is exceeded
    private static final int AI_GRADING_LUA_COST = 10;

    private final WritingSubmissionRepository writingSubmissionRepository;
    private final SectionRepository sectionRepository;
    private final ProfileRepository profileRepository;
    private final LLMGradingService llmGradingService;
    private final SubscriptionService subscriptionService;
    private final CreditService creditService;

    @Autowired
    public AsyncGradingService(WritingSubmissionRepository writingSubmissionRepository,
                               SectionRepository sectionRepository,
                               ProfileRepository profileRepository,
                               LLMGradingService llmGradingService,
                               SubscriptionService subscriptionService,
                               CreditService creditService) {
        this.writingSubmissionRepository = writingSubmissionRepository;
        this.sectionRepository = sectionRepository;
        this.profileRepository = profileRepository;
        this.llmGradingService = llmGradingService;
        this.subscriptionService = subscriptionService;
        this.creditService = creditService;
    }

    /**
     * Async method to grade submissions in background.
     * IMPORTANT: This must be called from another bean (not from within this class)
     * for the @Async proxy to work.
     */
    @Async
    public void gradeSubmissionsAsync(List<WritingSubmission> submissions, TestAttempt attempt, UUID userId) {
        logger.info("🚀 Starting ASYNC grading for {} submissions (thread: {})", 
                   submissions.size(), Thread.currentThread().getName());
        
        try {
            // Check if user has AI grading available (subscription or Lúa)
            GradingStatusDTO gradingStatus = subscriptionService.checkAIGradingAllowed(userId);
            boolean usingLua = false;
            
            if (!Boolean.TRUE.equals(gradingStatus.getAllowed())) {
                // Not allowed via subscription, check if can pay with Lúa
                if (Boolean.TRUE.equals(gradingStatus.getCanUseExtraWithLua()) && gradingStatus.getLuaBalance() >= AI_GRADING_LUA_COST) {
                    logger.info("📊 User {} exceeded subscription limit, will use Lúa for grading", userId);
                    usingLua = true;
                } else {
                    logger.warn("❌ User {} has no AI grading available (limit: {}, used: {}, lua: {})",
                            userId, gradingStatus.getLimit(), gradingStatus.getUsed(), gradingStatus.getLuaBalance());
                    for (WritingSubmission submission : submissions) {
                        submission.setGradingStatus("FAILED");
                        Map<String, Object> errorFeedback = new HashMap<>();
                        errorFeedback.put("error", "Bạn đã hết lượt chấm AI trong tháng. Vui lòng nâng cấp gói hoặc mua thêm Lúa để tiếp tục.");
                        submission.setAiFeedback(errorFeedback);
                        writingSubmissionRepository.save(submission);
                    }
                    return;
                }
            }
            
            // Get user's API key
            Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));
            
            String apiKey = profile.getLlmApiKey();
            String model = profile.getLlmModel();
            if (apiKey == null || apiKey.trim().isEmpty()) {
                logger.warn("❌ No DeepSeek API key found for user {}", userId);
                for (WritingSubmission submission : submissions) {
                    submission.setGradingStatus("FAILED");
                    Map<String, Object> errorFeedback = new HashMap<>();
                    errorFeedback.put("error", "Vui lòng thêm DeepSeek API key trong phần Cài đặt Hồ sơ để sử dụng tính năng chấm điểm AI.");
                    submission.setAiFeedback(errorFeedback);
                    writingSubmissionRepository.save(submission);
                }
                return;
            }
            
            logger.info("✅ Found API key for user, starting grading...");
            
            // Track if we need to deduct Lúa (set before grading loop)
            final boolean shouldUseLua = usingLua;
            
            // Get task prompts
            List<Section> sections = sectionRepository.findByExamSourceAndTestNumberAndSkill(
                attempt.getExamSource(),
                Integer.parseInt(attempt.getTestNumber()),
                "writing"
            );
            
            Map<Integer, Section> sectionMap = sections.stream()
                .collect(Collectors.toMap(Section::getPartNumber, s -> s));
            
            // Grade each submission
            boolean hasSuccessfulGrading = false;
            for (WritingSubmission submission : submissions) {
                try {
                    logger.info("📝 Grading Task {} for attempt {}...", 
                               submission.getTaskNumber(), attempt.getId());
                    
                    Section section = sectionMap.get(submission.getTaskNumber());
                    String taskPrompt = section != null ? section.getPassageText() : "";
                    String imageUrl = section != null ? section.getDisplayContentUrl() : null;
                    String imageDescription = section != null ? section.getImageDescription() : null;

                    llmGradingService.gradeSubmission(submission, taskPrompt, imageUrl, imageDescription, apiKey, model);
                    writingSubmissionRepository.save(submission);
                    
                    logger.info("✅ Graded submission {} with band {}", 
                               submission.getId(), submission.getOverallBand());
                    
                    // Mark that we have at least one successful grading
                    if ("COMPLETED".equals(submission.getGradingStatus())) {
                        hasSuccessfulGrading = true;
                    }
                    
                } catch (Exception e) {
                    logger.error("❌ Failed to grade submission {}: {}", submission.getId(), e.getMessage());
                    submission.setGradingStatus("FAILED");
                    Map<String, Object> errorFeedback = new HashMap<>();
                    errorFeedback.put("error", "Grading failed: " + e.getMessage());
                    submission.setAiFeedback(errorFeedback);
                    writingSubmissionRepository.save(submission);
                }
            }
            
            // Track AI grading usage ONLY if at least one grading was successful
            // This ensures users are not charged for failed gradings
            if (hasSuccessfulGrading) {
                try {
                    if (shouldUseLua) {
                        // Deduct Lúa for AI grading
                        creditService.spendCredits(userId, AI_GRADING_LUA_COST, 
                                CreditTransaction.Category.AI_GRADING,
                                "Chấm điểm AI bài viết - " + attempt.getExamSource() + " Test " + attempt.getTestNumber(),
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
                logger.warn("⚠️ No successful gradings - skipping billing for user {}", userId);
            }
            
            logger.info("🎉 Completed async grading for attempt {}", attempt.getId());
            
        } catch (Exception e) {
            logger.error("💥 Error in async grading: {}", e.getMessage(), e);
        }
    }
}
