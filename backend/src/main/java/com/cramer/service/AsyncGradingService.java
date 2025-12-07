package com.cramer.service;

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

    private final WritingSubmissionRepository writingSubmissionRepository;
    private final SectionRepository sectionRepository;
    private final ProfileRepository profileRepository;
    private final GeminiGradingService geminiGradingService;

    @Autowired
    public AsyncGradingService(WritingSubmissionRepository writingSubmissionRepository,
                               SectionRepository sectionRepository,
                               ProfileRepository profileRepository,
                               GeminiGradingService geminiGradingService) {
        this.writingSubmissionRepository = writingSubmissionRepository;
        this.sectionRepository = sectionRepository;
        this.profileRepository = profileRepository;
        this.geminiGradingService = geminiGradingService;
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
            // Get user's API key
            Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));
            
            String apiKey = profile.getGeminiApiKey();
            String model = profile.getGeminiModel();
            if (apiKey == null || apiKey.trim().isEmpty()) {
                logger.warn("❌ No Gemini API key found for user {}", userId);
                for (WritingSubmission submission : submissions) {
                    submission.setGradingStatus("FAILED");
                    Map<String, Object> errorFeedback = new HashMap<>();
                    errorFeedback.put("error", "Vui lòng thêm Gemini API key trong phần Cài đặt Hồ sơ để sử dụng tính năng chấm điểm AI.");
                    submission.setAiFeedback(errorFeedback);
                    writingSubmissionRepository.save(submission);
                }
                return;
            }
            
            logger.info("✅ Found API key for user, starting grading...");
            
            // Get task prompts
            List<Section> sections = sectionRepository.findByExamSourceAndTestNumberAndSkill(
                attempt.getExamSource(),
                Integer.parseInt(attempt.getTestNumber()),
                "writing"
            );
            
            Map<Integer, Section> sectionMap = sections.stream()
                .collect(Collectors.toMap(Section::getPartNumber, s -> s));
            
            // Grade each submission
            for (WritingSubmission submission : submissions) {
                try {
                    logger.info("📝 Grading Task {} for attempt {}...", 
                               submission.getTaskNumber(), attempt.getId());
                    
                    Section section = sectionMap.get(submission.getTaskNumber());
                    String taskPrompt = section != null ? section.getPassageText() : "";
                    String imageUrl = section != null ? section.getDisplayContentUrl() : null;
                    
                    geminiGradingService.gradeSubmission(submission, taskPrompt, imageUrl, apiKey, model);
                    writingSubmissionRepository.save(submission);
                    
                    logger.info("✅ Graded submission {} with band {}", 
                               submission.getId(), submission.getOverallBand());
                    
                } catch (Exception e) {
                    logger.error("❌ Failed to grade submission {}: {}", submission.getId(), e.getMessage());
                    submission.setGradingStatus("FAILED");
                    Map<String, Object> errorFeedback = new HashMap<>();
                    errorFeedback.put("error", "Grading failed: " + e.getMessage());
                    submission.setAiFeedback(errorFeedback);
                    writingSubmissionRepository.save(submission);
                }
            }
            
            logger.info("🎉 Completed async grading for attempt {}", attempt.getId());
            
        } catch (Exception e) {
            logger.error("💥 Error in async grading: {}", e.getMessage(), e);
        }
    }
}
