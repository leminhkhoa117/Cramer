package com.cramer.controller.admin;

import com.cramer.dto.abts.GenerationRequestDTO;
import com.cramer.dto.abts.GenerationResponseDTO;
import com.cramer.dto.abts.StreamEventDTO;
import com.cramer.service.abts.ABTSService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * ABTS Controller - AI-Based Test Generation System endpoints.
 * 
 * Provides REST API for generating IELTS test content using AI.
 * All endpoints require admin authentication (AdminAuthFilter).
 * 
 * @since 2025-12-20 - ABTS v2.0
 */
@RestController
@RequestMapping("/api/admin/abts")
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:5173" })
public class ABTSController {

    private static final Logger logger = LoggerFactory.getLogger(ABTSController.class);

    private final ABTSService abtsService;

    public ABTSController(ABTSService abtsService) {
        this.abtsService = abtsService;
    }

    // ==================== GENERATION ENDPOINTS ====================

    /**
     * Generate Reading content (passage + questions).
     */
    @PostMapping("/generate/reading")
    public ResponseEntity<GenerationResponseDTO> generateReading(
            @Valid @RequestBody GenerationRequestDTO request,
            @RequestHeader("X-User-Id") String adminUserId) {

        logger.info("ABTS Reading generation requested by admin: {}", adminUserId);

        request.setSkill(GenerationRequestDTO.SkillType.READING);
        GenerationResponseDTO response = abtsService.generate(request);

        logger.info("ABTS Reading generation completed with status: {}", response.getStatus());
        return ResponseEntity.ok(response);
    }

    /**
     * Generate Reading content with streaming progress updates (SSE).
     */
    @PostMapping("/generate/reading/stream")
    public SseEmitter generateReadingStream(
            @Valid @RequestBody GenerationRequestDTO request,
            @RequestHeader("X-User-Id") String adminUserId) {

        logger.info("ABTS Reading generation (streaming) requested by admin: {}", adminUserId);

        SseEmitter emitter = new SseEmitter(300000L); // 5 min timeout

        request.setSkill(GenerationRequestDTO.SkillType.READING);

        // Run generation in background thread
        CompletableFuture.runAsync(() -> {
            try {
                abtsService.generateWithStream(request, emitter);
            } catch (Exception e) {
                logger.error("Streaming generation failed: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(StreamEventDTO.failed(e.getMessage())));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        emitter.onTimeout(() -> {
            logger.warn("SSE connection timed out");
            emitter.complete();
        });

        emitter.onCompletion(() -> {
            logger.info("SSE connection completed");
        });

        return emitter;
    }

    /**
     * Generate Listening content (transcript + questions).
     */
    @PostMapping("/generate/listening")
    public ResponseEntity<GenerationResponseDTO> generateListening(
            @Valid @RequestBody GenerationRequestDTO request,
            @RequestHeader("X-User-Id") String adminUserId) {

        logger.info("ABTS Listening generation requested by admin: {}", adminUserId);

        request.setSkill(GenerationRequestDTO.SkillType.LISTENING);
        GenerationResponseDTO response = abtsService.generate(request);

        logger.info("ABTS Listening generation completed with status: {}", response.getStatus());
        return ResponseEntity.ok(response);
    }

    /**
     * Generate Listening content with streaming progress updates (SSE).
     */
    @PostMapping("/generate/listening/stream")
    public SseEmitter generateListeningStream(
            @Valid @RequestBody GenerationRequestDTO request,
            @RequestHeader("X-User-Id") String adminUserId) {

        logger.info("ABTS Listening generation (streaming) requested by admin: {}", adminUserId);

        SseEmitter emitter = new SseEmitter(300000L); // 5 min timeout

        request.setSkill(GenerationRequestDTO.SkillType.LISTENING);

        CompletableFuture.runAsync(() -> {
            try {
                abtsService.generateWithStream(request, emitter);
            } catch (Exception e) {
                logger.error("Streaming Listening generation failed: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(StreamEventDTO.failed(e.getMessage())));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        emitter.onTimeout(() -> {
            logger.warn("SSE connection timed out for Listening");
            emitter.complete();
        });

        return emitter;
    }

    /**
     * Generate Writing content (Task 1 chart/Task 2 essay prompt).
     */
    @PostMapping("/generate/writing")
    public ResponseEntity<GenerationResponseDTO> generateWriting(
            @Valid @RequestBody GenerationRequestDTO request,
            @RequestHeader("X-User-Id") String adminUserId) {

        logger.info("ABTS Writing generation requested by admin: {}", adminUserId);

        request.setSkill(GenerationRequestDTO.SkillType.WRITING);
        GenerationResponseDTO response = abtsService.generate(request);

        logger.info("ABTS Writing generation completed with status: {}", response.getStatus());
        return ResponseEntity.ok(response);
    }

    /**
     * Generate Writing content with streaming progress updates (SSE).
     */
    @PostMapping("/generate/writing/stream")
    public SseEmitter generateWritingStream(
            @Valid @RequestBody GenerationRequestDTO request,
            @RequestHeader("X-User-Id") String adminUserId) {

        logger.info("ABTS Writing generation (streaming) requested by admin: {}", adminUserId);

        SseEmitter emitter = new SseEmitter(300000L); // 5 min timeout

        request.setSkill(GenerationRequestDTO.SkillType.WRITING);

        CompletableFuture.runAsync(() -> {
            try {
                abtsService.generateWithStream(request, emitter);
            } catch (Exception e) {
                logger.error("Streaming Writing generation failed: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(StreamEventDTO.failed(e.getMessage())));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        emitter.onTimeout(() -> {
            logger.warn("SSE connection timed out for Writing");
            emitter.complete();
        });

        return emitter;
    }

    /**
     * Regenerate specific questions (keeping existing passage).
     */
    @PostMapping("/generate/questions")
    public ResponseEntity<GenerationResponseDTO> regenerateQuestions(
            @Valid @RequestBody GenerationRequestDTO request,
            @RequestHeader("X-User-Id") String adminUserId) {

        logger.info("ABTS question regeneration requested by admin: {}", adminUserId);

        if (request.getExistingPassageText() == null || request.getExistingPassageText().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    GenerationResponseDTO.error("MISSING_PASSAGE",
                            "Existing passage text is required for question regeneration", false));
        }

        GenerationResponseDTO response = abtsService.regenerateQuestions(request);

        logger.info("ABTS question regeneration completed with status: {}", response.getStatus());
        return ResponseEntity.ok(response);
    }

    // ==================== VALIDATION ENDPOINT ====================

    /**
     * Validate generated JSON content.
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateContent(
            @RequestBody Map<String, Object> content,
            @RequestHeader("X-User-Id") String adminUserId) {

        logger.info("ABTS content validation requested by admin: {}", adminUserId);

        Map<String, Object> validationResult = abtsService.validateContent(content);
        return ResponseEntity.ok(validationResult);
    }

    // ==================== TEMPLATE ENDPOINTS ====================

    /**
     * Get all topic template categories.
     */
    @GetMapping("/templates")
    public ResponseEntity<List<Map<String, Object>>> getTemplateCategories() {
        return ResponseEntity.ok(abtsService.getTemplateCategories());
    }

    /**
     * Get topic templates for a specific category.
     */
    @GetMapping("/templates/{categoryId}")
    public ResponseEntity<List<Map<String, Object>>> getTemplatesByCategory(
            @PathVariable String categoryId) {
        return ResponseEntity.ok(abtsService.getTemplatesByCategory(categoryId));
    }

    // ==================== MODEL/CONFIG ENDPOINTS ====================

    /**
     * Get available AI models for ABTS.
     */
    @GetMapping("/models")
    public ResponseEntity<List<Map<String, Object>>> getAvailableModels() {
        return ResponseEntity.ok(abtsService.getAvailableModels());
    }

    /**
     * Get ABTS configuration status.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(abtsService.getStatus());
    }
}
