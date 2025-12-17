package com.cramer.controller;

import com.cramer.config.RateLimitConfig;
import com.cramer.dto.WritingReviewDTO;
import com.cramer.dto.WritingSubmissionDTO;
import com.cramer.dto.WritingSubmitDTO;
import com.cramer.exception.RateLimitExceededException;
import com.cramer.service.WritingSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for Writing test submissions and grading.
 */
@RestController
@RequestMapping("/api/writing")
@Tag(name = "Writing API", description = "API for IELTS Writing test submissions and AI grading")
public class WritingController {

    private static final Logger logger = LoggerFactory.getLogger(WritingController.class);

    private final WritingSubmissionService writingSubmissionService;
    private final RateLimitConfig rateLimitConfig;

    @Autowired
    public WritingController(WritingSubmissionService writingSubmissionService, 
                            RateLimitConfig rateLimitConfig) {
        this.writingSubmissionService = writingSubmissionService;
        this.rateLimitConfig = rateLimitConfig;
    }

    /**
     * Save essay draft during test.
     */
    @PostMapping("/draft/{attemptId}")
    @Operation(summary = "Save essay draft", description = "Save an essay draft during the test")
    public ResponseEntity<WritingSubmissionDTO> saveDraft(
            @PathVariable Long attemptId,
            @RequestParam Integer taskNumber,
            @RequestBody String essayText,
            Authentication authentication) {
        
        logger.info("📥 POST /api/writing/draft/{} - taskNumber={}", attemptId, taskNumber);
        
        UUID userId = UUID.fromString(authentication.getName());
        WritingSubmissionDTO saved = writingSubmissionService.saveDraft(attemptId, taskNumber, essayText, userId);
        
        logger.info("✅ Draft saved for attempt {} task {}", attemptId, taskNumber);
        return ResponseEntity.ok(saved);
    }

    /**
     * Submit essays for grading.
     */
    @PostMapping("/submit/{attemptId}")
    @Operation(summary = "Submit essays for grading", description = "Submit essays and start background AI grading")
    public ResponseEntity<Map<String, Object>> submitForGrading(
            @PathVariable Long attemptId,
            @Valid @RequestBody WritingSubmitDTO submitDTO,
            Authentication authentication) {
        
        logger.info("📥 POST /api/writing/submit/{} - essays count: {}", 
                   attemptId, submitDTO.getEssays() != null ? submitDTO.getEssays().size() : 0);
        
        UUID userId = UUID.fromString(authentication.getName());
        
        // Rate limit check for grading
        if (!rateLimitConfig.tryConsume(userId.toString(), "grading")) {
            throw new RateLimitExceededException("Grading rate limit exceeded. Max 5 requests per minute.");
        }
        
        Map<String, Object> result = writingSubmissionService.submitForGrading(
            attemptId, submitDTO.getEssays(), userId);
        
        logger.info("✅ Essays submitted for grading, attempt {}", attemptId);
        return ResponseEntity.ok(result);
    }

    /**
     * Get grading status for an attempt.
     */
    @GetMapping("/status/{attemptId}")
    @Operation(summary = "Get grading status", description = "Check the grading status of submitted essays")
    public ResponseEntity<Map<String, Object>> getGradingStatus(
            @PathVariable Long attemptId,
            Authentication authentication) {
        
        logger.info("📥 GET /api/writing/status/{}", attemptId);
        
        UUID userId = UUID.fromString(authentication.getName());
        Map<String, Object> status = writingSubmissionService.getGradingStatus(attemptId, userId);
        
        return ResponseEntity.ok(status);
    }

    /**
     * Get full writing review with grading results.
     */
    @GetMapping("/review/{attemptId}")
    @Operation(summary = "Get writing review", description = "Get full review with AI grading results and feedback")
    public ResponseEntity<WritingReviewDTO> getWritingReview(
            @PathVariable Long attemptId,
            Authentication authentication) {
        
        logger.info("📥 GET /api/writing/review/{}", attemptId);
        
        UUID userId = UUID.fromString(authentication.getName());
        WritingReviewDTO review = writingSubmissionService.getWritingReview(attemptId, userId);
        
        logger.info("✅ Writing review fetched for attempt {}", attemptId);
        return ResponseEntity.ok(review);
    }

    /**
     * Get submissions for an attempt.
     */
    @GetMapping("/submissions/{attemptId}")
    @Operation(summary = "Get submissions", description = "Get all writing submissions for an attempt")
    public ResponseEntity<List<WritingSubmissionDTO>> getSubmissions(
            @PathVariable Long attemptId,
            Authentication authentication) {
        
        logger.info("📥 GET /api/writing/submissions/{}", attemptId);
        
        UUID userId = UUID.fromString(authentication.getName());
        List<WritingSubmissionDTO> submissions = writingSubmissionService.getSubmissions(attemptId, userId);
        
        return ResponseEntity.ok(submissions);
    }

    /**
     * Validate Gemini API key.
     */
    @PostMapping("/validate-api-key")
    @Operation(summary = "Validate API key", description = "Validate a Gemini API key")
    public ResponseEntity<Map<String, Object>> validateApiKey(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        logger.info("📥 POST /api/writing/validate-api-key");
        
        String apiKey = request.get("apiKey");
        boolean isValid = writingSubmissionService.validateApiKey(apiKey);
        
        Map<String, Object> result = Map.of(
            "valid", isValid,
            "message", isValid ? "API key is valid" : "API key is invalid or expired"
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * Re-grade a writing attempt.
     */
    @PostMapping("/regrade/{attemptId}")
    @Operation(summary = "Re-grade attempt", description = "Re-grade a completed writing attempt with AI")
    public ResponseEntity<Map<String, Object>> regradeAttempt(
            @PathVariable Long attemptId,
            Authentication authentication) {
        
        logger.info("📥 POST /api/writing/regrade/{}", attemptId);
        
        UUID userId = UUID.fromString(authentication.getName());
        
        // Rate limit check for grading
        if (!rateLimitConfig.tryConsume(userId.toString(), "grading")) {
            throw new RateLimitExceededException("Grading rate limit exceeded. Max 5 requests per minute.");
        }
        
        Map<String, Object> result = writingSubmissionService.regradeAttempt(attemptId, userId);
        
        logger.info("✅ Re-grading started for attempt {}", attemptId);
        return ResponseEntity.ok(result);
    }
}
