package com.cramer.controller;

import com.cramer.dto.AnswerSubmissionDTO;
import com.cramer.dto.SaveProgressDTO;
import com.cramer.dto.TestResultDTO;
import com.cramer.dto.TestReviewDTO;
import com.cramer.dto.UserAnswerDTO;
import com.cramer.entity.TestAttempt;
import com.cramer.service.TestAttemptService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/test-attempts")
@Tag(name = "Test Attempts API", description = "API for starting and submitting test attempts")
public class TestAttemptController {

    private final TestAttemptService testAttemptService;

    @Autowired
    public TestAttemptController(TestAttemptService testAttemptService) {
        this.testAttemptService = testAttemptService;
    }

    @PostMapping("/start")
    public ResponseEntity<TestAttempt> startOrGetAttempt(
            @RequestParam String source,
            @RequestParam String test,
            @RequestParam String skill,
            Authentication authentication) {

        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestAttemptController.class);
        logger.info("📥 POST /api/test-attempts/start - source={}, test={}, skill={}", source, test, skill);
        
        try {
            if (authentication == null || authentication.getName() == null) {
                logger.error("❌ No authentication provided");
                throw new IllegalArgumentException("Authentication required");
            }
            
            UUID userId = UUID.fromString(authentication.getName());
            logger.info("🔐 User authenticated: userId={}", userId);
            
            TestAttempt attempt = testAttemptService.startOrGetAttempt(source, test, skill, userId);
            logger.info("✅ Test attempt returned: attemptId={}", attempt.getId());
            return ResponseEntity.ok(attempt);
        } catch (IllegalArgumentException e) {
            logger.error("❌ Invalid parameters: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("❌ Error starting test attempt", e);
            throw e;
        }
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<TestResultDTO> submitAttempt(@PathVariable Long id, @RequestBody AnswerSubmissionDTO submissionDTO, Authentication authentication) {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestAttemptController.class);
        logger.info("📥 POST /api/test-attempts/{}/submit - Received request", id);
        
        try {
            if (authentication == null || authentication.getName() == null) {
                logger.error("❌ No authentication provided for submit");
                throw new IllegalArgumentException("Authentication required");
            }
            
            String userIdStr = authentication.getName();
            UUID userId = UUID.fromString(userIdStr);
            logger.info("🔐 User authenticated: userId={}, answersCount={}", userId, 
                        submissionDTO != null && submissionDTO.getAnswers() != null ? submissionDTO.getAnswers().size() : 0);
            
            TestResultDTO result = testAttemptService.submitAttempt(id, submissionDTO.getAnswers(), userId);
            logger.info("✅ Test submitted successfully: score={}/{}", result.getScore(), result.getTotalQuestions());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error submitting test attempt: attemptId={}, error={}", id, e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/{id}/progress")
    public ResponseEntity<Void> saveProgress(
            @PathVariable Long id,
            @RequestBody SaveProgressDTO saveProgressDTO,
            Authentication authentication) {
        
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestAttemptController.class);
        logger.info("📥 POST /api/test-attempts/{}/progress - attemptId={}, timeLeft={}, currentPart={}, answersCount={}", 
                    id, saveProgressDTO.getTimeLeft(), saveProgressDTO.getCurrentPart(), 
                    saveProgressDTO.getAnswers() != null ? saveProgressDTO.getAnswers().size() : 0);

        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Authentication required");
        }

        UUID userId = UUID.fromString(authentication.getName());
        testAttemptService.saveProgress(id, saveProgressDTO, userId);
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelAttempt(@PathVariable Long id, Authentication authentication) {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestAttemptController.class);
        logger.info("📥 POST /api/test-attempts/{}/cancel - Received request", id);

        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Authentication required");
        }

        UUID userId = UUID.fromString(authentication.getName());
        testAttemptService.cancelAttempt(id, userId);
        
        logger.info("✅ Test attempt cancelled successfully: attemptId={}", id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<Void> resumeAttempt(@PathVariable Long id, Authentication authentication) {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestAttemptController.class);
        logger.info("📥 POST /api/test-attempts/{}/resume - Received request", id);

        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Authentication required");
        }

        UUID userId = UUID.fromString(authentication.getName());
        testAttemptService.resumeAttempt(id, userId);
        
        logger.info("✅ Test attempt marked for resume successfully: attemptId={}", id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/answers")
    public ResponseEntity<List<UserAnswerDTO>> getAttemptAnswers(@PathVariable Long id, Authentication authentication) {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestAttemptController.class);
        logger.info("📥 GET /api/test-attempts/{}/answers - Received request", id);

        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Authentication required");
        }

        UUID userId = UUID.fromString(authentication.getName());
        List<UserAnswerDTO> answers = testAttemptService.getAnswersForAttempt(id, userId);
        
        logger.info("✅ Successfully fetched {} answers for attemptId={}", answers.size(), id);
        return ResponseEntity.ok(answers);
    }

    @GetMapping("/{id}/review")
    public ResponseEntity<TestReviewDTO> getTestReview(@PathVariable Long id, Authentication authentication) {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestAttemptController.class);
        logger.info("📥 GET /api/test-attempts/{}/review - Received request", id);

        if (authentication == null || authentication.getName() == null) {
            logger.error("❌ No authentication provided for review");
            throw new IllegalArgumentException("Authentication required");
        }

        UUID userId = UUID.fromString(authentication.getName());
        TestReviewDTO reviewDTO = testAttemptService.getTestReview(id, userId);
        
        logger.info("✅ Successfully fetched test review: attemptId={}", id);
        return ResponseEntity.ok(reviewDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttempt(@PathVariable Long id, Authentication authentication) {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestAttemptController.class);
        logger.info("📥 DELETE /api/test-attempts/{} - Received request", id);

        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Authentication required");
        }

        UUID userId = UUID.fromString(authentication.getName());
        testAttemptService.deleteAttempt(id, userId);
        
        logger.info("✅ Successfully deleted test attempt: attemptId={}", id);
        return ResponseEntity.noContent().build();
    }
}
