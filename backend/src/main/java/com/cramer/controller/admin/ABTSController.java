package com.cramer.controller.admin;

import com.cramer.dto.abts.GenerationRequestDTO;
import com.cramer.dto.abts.GenerationResponseDTO;
import com.cramer.dto.abts.SaveContentRequestDTO;
import com.cramer.dto.abts.SaveContentResponseDTO;
import com.cramer.dto.abts.StreamEventDTO;
import com.cramer.service.abts.ABTSService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

        // Cancellation flag for stop generation feature
        final java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean(
                false);

        // Run generation in background thread
        Runnable task = new DelegatingSecurityContextRunnable(() -> {
            try {
                abtsService.generateWithStream(request, emitter, cancelled);
            } catch (Exception e) {
                if (cancelled.get()) {
                    logger.info("Generation was cancelled by user");
                    return;
                }
                // Check if this is a connection-related error (user disconnected)
                if (e instanceof IOException || (e.getCause() != null && e.getCause() instanceof IOException)) {
                    logger.info("SSE connection closed by client: {}", e.getMessage());
                    return;
                }
                logger.error("Streaming generation failed: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Objects.requireNonNull(StreamEventDTO.failed(e.getMessage()))));
                    emitter.complete();
                } catch (IOException ex) {
                    // Client already disconnected, just log and return silently
                    logger.debug("Cannot send error to client (already disconnected): {}", ex.getMessage());
                } catch (IllegalStateException ex) {
                    // Emitter already completed
                    logger.debug("Emitter already completed: {}", ex.getMessage());
                }
            }
        });
        CompletableFuture.runAsync(task);

        emitter.onTimeout(() -> {
            logger.warn("SSE connection timed out");
            cancelled.set(true);
            emitter.complete();
        });

        emitter.onCompletion(() -> {
            logger.info("SSE connection completed");
            cancelled.set(true);
        });

        emitter.onError((ex) -> {
            logger.info("SSE connection error (client disconnected): {}", ex.getMessage());
            cancelled.set(true);
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

        // Cancellation flag for stop generation feature
        final java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean(
                false);

        Runnable task = new DelegatingSecurityContextRunnable(() -> {
            try {
                abtsService.generateWithStream(request, emitter, cancelled);
            } catch (Exception e) {
                if (cancelled.get()) {
                    logger.info("Listening generation was cancelled by user");
                    return;
                }
                // Check if this is a connection-related error (user disconnected)
                if (e instanceof IOException || (e.getCause() != null && e.getCause() instanceof IOException)) {
                    logger.info("SSE Listening connection closed by client: {}", e.getMessage());
                    return;
                }
                logger.error("Streaming Listening generation failed: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Objects.requireNonNull(StreamEventDTO.failed(e.getMessage()))));
                    emitter.complete();
                } catch (IOException ex) {
                    logger.debug("Cannot send error to Listening client (already disconnected): {}", ex.getMessage());
                } catch (IllegalStateException ex) {
                    logger.debug("Listening emitter already completed: {}", ex.getMessage());
                }
            }
        });
        CompletableFuture.runAsync(task);

        emitter.onTimeout(() -> {
            logger.warn("SSE connection timed out for Listening");
            cancelled.set(true);
            emitter.complete();
        });

        emitter.onCompletion(() -> {
            cancelled.set(true);
        });

        emitter.onError((ex) -> {
            logger.info("SSE Listening connection error (client disconnected): {}", ex.getMessage());
            cancelled.set(true);
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

        // Cancellation flag for stop generation feature
        final java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean(
                false);

        Runnable task = new DelegatingSecurityContextRunnable(() -> {
            try {
                abtsService.generateWithStream(request, emitter, cancelled);
            } catch (Exception e) {
                if (cancelled.get()) {
                    logger.info("Writing generation was cancelled by user");
                    return;
                }
                // Check if this is a connection-related error (user disconnected)
                if (e instanceof IOException || (e.getCause() != null && e.getCause() instanceof IOException)) {
                    logger.info("SSE Writing connection closed by client: {}", e.getMessage());
                    return;
                }
                logger.error("Streaming Writing generation failed: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Objects.requireNonNull(StreamEventDTO.failed(e.getMessage()))));
                    emitter.complete();
                } catch (IOException ex) {
                    logger.debug("Cannot send error to Writing client (already disconnected): {}", ex.getMessage());
                } catch (IllegalStateException ex) {
                    logger.debug("Writing emitter already completed: {}", ex.getMessage());
                }
            }
        });
        CompletableFuture.runAsync(task);

        emitter.onTimeout(() -> {
            logger.warn("SSE connection timed out for Writing");
            cancelled.set(true);
            emitter.complete();
        });

        emitter.onCompletion(() -> {
            cancelled.set(true);
        });

        emitter.onError((ex) -> {
            logger.info("SSE Writing connection error (client disconnected): {}", ex.getMessage());
            cancelled.set(true);
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

        boolean requiresPassage = request.getSkill() == null
                || request.getSkill() == GenerationRequestDTO.SkillType.READING
                || request.getSkill() == GenerationRequestDTO.SkillType.LISTENING;

        if (requiresPassage
                && (request.getExistingPassageText() == null || request.getExistingPassageText().isEmpty())) {
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

    // ==================== SAVE ENDPOINT ====================

    /**
     * Save generated content to the database.
     * Creates a new section and all associated questions.
     */
    @PostMapping("/save")
    public ResponseEntity<SaveContentResponseDTO> saveContent(
            @Valid @RequestBody SaveContentRequestDTO request,
            @RequestHeader("X-User-Id") String adminUserId) {

        logger.info("ABTS save content requested by admin: {} for skill: {}",
                adminUserId, request.getSkill());

        try {
            SaveContentResponseDTO response = abtsService.saveContent(request, adminUserId);

            if (response.isSuccess()) {
                logger.info("ABTS content saved successfully: sectionId={}, questions={}",
                        response.getSectionId(), response.getQuestionsCreated());
            } else {
                logger.warn("ABTS content save failed: {}", response.getMessage());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("ABTS save failed: {}", e.getMessage(), e);
            return ResponseEntity.ok(SaveContentResponseDTO.error(e.getMessage()));
        }
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
