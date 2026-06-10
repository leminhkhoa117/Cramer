package com.cramer.controller.admin;

import com.cramer.config.OpenRouterConfig;
import com.cramer.dto.abts.GenerationRequestDTO;
import com.cramer.dto.abts.GenerationResponseDTO;
import com.cramer.dto.abts.RefinementApplyRequestDTO;
import com.cramer.dto.abts.RefinementApplyResponseDTO;
import com.cramer.dto.abts.RefinementRequestDTO;
import com.cramer.dto.abts.SaveContentRequestDTO;
import com.cramer.dto.abts.SaveContentResponseDTO;
import com.cramer.dto.abts.StreamEventDTO;
import com.cramer.service.abts.ABTSService;
import com.cramer.service.abts.JsonHunkApplier;
import com.cramer.service.abts.RefinementService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

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
    private final RefinementService refinementService;
    private final JsonHunkApplier jsonHunkApplier;
    private final Executor abtsStreamingExecutor;
    private final OpenRouterConfig openRouterConfig;

    public ABTSController(ABTSService abtsService, RefinementService refinementService,
            JsonHunkApplier jsonHunkApplier,
            @Qualifier("abtsStreamingExecutor") Executor abtsStreamingExecutor,
            OpenRouterConfig openRouterConfig) {
        this.abtsService = abtsService;
        this.refinementService = refinementService;
        this.jsonHunkApplier = jsonHunkApplier;
        this.abtsStreamingExecutor = abtsStreamingExecutor;
        this.openRouterConfig = openRouterConfig;
    }

    /**
     * FIX 1: Dispatch a streaming generation task onto the bounded ABTS pool.
     *
     * <p>The pool now uses {@code AbortPolicy}, so a saturated pool throws
     * {@link RejectedExecutionException} synchronously on this (request) thread.
     * We translate that into a clean SSE {@code error} event and complete the
     * emitter, instead of letting the exception bubble up as an opaque 500 with
     * a dangling connection.
     */
    private void dispatchStreamingTask(SseEmitter emitter, Runnable task,
            java.util.concurrent.atomic.AtomicBoolean cancelled, String label) {
        try {
            CompletableFuture.runAsync(task, abtsStreamingExecutor);
        } catch (RejectedExecutionException rex) {
            logger.warn("ABTS streaming pool saturated, rejecting {} request: {}", label, rex.getMessage());
            cancelled.set(true);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(Objects.requireNonNull(
                                StreamEventDTO.failed("Server at capacity, please retry shortly"))));
            } catch (IOException | IllegalStateException sendEx) {
                logger.debug("Could not notify {} client of capacity rejection: {}", label, sendEx.getMessage());
            }
            emitter.complete();
        }
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

        SseEmitter emitter = new SseEmitter(openRouterConfig.getEmitterTimeoutMs());

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
        // FIX 1: register lifecycle callbacks BEFORE dispatching so an immediate
        // timeout/disconnect cannot race ahead of an unregistered handler.
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

        dispatchStreamingTask(emitter, task, cancelled, "Reading");

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

        SseEmitter emitter = new SseEmitter(openRouterConfig.getEmitterTimeoutMs());

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
        // FIX 1: register lifecycle callbacks BEFORE dispatching.
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

        dispatchStreamingTask(emitter, task, cancelled, "Listening");

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

        SseEmitter emitter = new SseEmitter(openRouterConfig.getEmitterTimeoutMs());

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
        // FIX 1: register lifecycle callbacks BEFORE dispatching.
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

        dispatchStreamingTask(emitter, task, cancelled, "Writing");

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
     * Get available AI models for ABTS, each augmented with an inferred
     * reasoning capability descriptor.
     */
    @GetMapping("/models")
    public ResponseEntity<List<Map<String, Object>>> getAvailableModels() {
        return ResponseEntity.ok(abtsService.getAvailableModelsWithCapabilities());
    }

    /**
     * Get the reasoning capability descriptor for a single model id.
     * Uses a terminal wildcard so slugs containing slashes
     * (e.g. {@code deepseek/deepseek-v4-flash}) are captured intact.
     */
    @Operation(summary = "Get capability descriptor for a specific model")
    @GetMapping("/models/capabilities/{*id}")
    public ResponseEntity<Map<String, Object>> getModelCapabilities(@PathVariable("id") String id) {
        String modelId = id != null && id.startsWith("/") ? id.substring(1) : id;
        return ResponseEntity.ok(abtsService.getModelCapabilities(modelId));
    }

    /**
     * Get ABTS configuration status.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(abtsService.getStatus());
    }

    // ==================== REFINEMENT ENDPOINT (AGENT 2) ====================

    /**
     * Refine generated content by fixing user-selected validation issues.
     * Uses Agent 2 (Refinement Agent) to make targeted fixes with streaming output.
     */
    @PostMapping("/refine/stream")
    public SseEmitter refineContentStream(
            @Valid @RequestBody RefinementRequestDTO request,
            @RequestHeader("X-User-Id") String adminUserId) {

        logger.info("ABTS refinement requested by admin: {} for {} issues",
                adminUserId, request.getSelectedIssueIds() != null ? request.getSelectedIssueIds().size() : 0);

        SseEmitter emitter = new SseEmitter(openRouterConfig.getEmitterTimeoutMs());

        // Cancellation flag
        final java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean(
                false);

        // Run refinement in background thread
        Runnable task = new DelegatingSecurityContextRunnable(() -> {
            try {
                refinementService.refineWithStream(request, emitter, cancelled);
            } catch (Exception e) {
                if (cancelled.get()) {
                    logger.info("Refinement was cancelled by user");
                    return;
                }
                if (e instanceof IOException || (e.getCause() != null && e.getCause() instanceof IOException)) {
                    logger.info("SSE Refinement connection closed by client: {}", e.getMessage());
                    return;
                }
                logger.error("Streaming refinement failed: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Objects.requireNonNull(StreamEventDTO.failed(e.getMessage()))));
                    emitter.complete();
                } catch (IOException ex) {
                    logger.debug("Cannot send error to refinement client (already disconnected): {}", ex.getMessage());
                } catch (IllegalStateException ex) {
                    logger.debug("Refinement emitter already completed: {}", ex.getMessage());
                }
            }
        });
        // FIX 1: register lifecycle callbacks BEFORE dispatching.
        emitter.onTimeout(() -> {
            logger.warn("SSE refinement connection timed out");
            cancelled.set(true);
            emitter.complete();
        });

        emitter.onCompletion(() -> {
            cancelled.set(true);
        });

        emitter.onError((ex) -> {
            logger.info("SSE refinement connection error (client disconnected): {}", ex.getMessage());
            cancelled.set(true);
        });

        dispatchStreamingTask(emitter, task, cancelled, "Refinement");

        return emitter;
    }

    /**
     * Partial-apply endpoint for the refinement diff-approval flow.
     *
     * <p>Given the original JSON, the full set of refinement hunks, and the
     * subset of hunk ids the admin accepted in the Issue Rail, apply only the
     * accepted hunks and return the resulting JSON. This is a synchronous,
     * non-streaming operation (no AI call).</p>
     *
     * @since 2026 - ABTS refinement loop (PART D)
     */
    @Operation(summary = "Apply user-accepted refinement hunks",
            description = "Synchronously applies only the admin-accepted refinement hunks to the original JSON and returns the merged result; no AI call is made.")
    @PostMapping("/refine/apply")
    public ResponseEntity<RefinementApplyResponseDTO> applyRefinementHunks(
            @org.springframework.validation.annotation.Validated @jakarta.validation.Valid @RequestBody RefinementApplyRequestDTO request,
            @RequestHeader("X-User-Id") String adminUserId) {

        int hunkCount = request.getHunks() != null ? request.getHunks().size() : 0;
        int acceptedCount = request.getAcceptedHunkIds() != null ? request.getAcceptedHunkIds().size() : 0;
        logger.info("ABTS refine/apply requested by admin: {} ({} accepted of {} hunks)",
                adminUserId, acceptedCount, hunkCount);

        try {
            JsonHunkApplier.ApplyResult result = jsonHunkApplier.apply(
                    request.getOriginalJson(), request.getHunks(), request.getAcceptedHunkIds());

            // FIX 9: map per-hunk skip reasons onto the response DTO.
            List<RefinementApplyResponseDTO.SkippedHunkDTO> skipped = result.skippedHunks.stream()
                    .map(s -> RefinementApplyResponseDTO.SkippedHunkDTO.builder()
                            .id(s.id)
                            .reason(s.reason)
                            .build())
                    .collect(java.util.stream.Collectors.toList());

            // FIX 13: INFO-log the OUTCOME (counts + skipped ids/reasons) for
            // observability. The originalJson / patchedJson are intentionally NOT
            // logged (can be large + may contain content under review).
            logger.info("ABTS refine/apply result for admin {}: accepted={} applied={} rejected={} skipped={} reasons={}",
                    adminUserId,
                    request.getAcceptedHunkIds(),
                    result.appliedCount,
                    result.rejectedCount,
                    result.skippedHunkIds,
                    skipped);

            return ResponseEntity.ok(RefinementApplyResponseDTO.builder()
                    .patchedJson(result.patchedJson)
                    .appliedCount(result.appliedCount)
                    .rejectedCount(result.rejectedCount)
                    .skippedHunkIds(result.skippedHunkIds)
                    .skippedHunks(skipped)
                    .success(true)
                    .build());
        } catch (IllegalArgumentException e) {
            // FIX 5: a malformed request BODY (originalJson null/blank/unparseable)
            // is a genuine client error -> 400. Per-hunk application failures are
            // NOT exceptions; they are captured as skippedHunks above and still
            // return 200 + success=true so the UI can show partial results.
            logger.warn("refine/apply bad request body: {}", e.getMessage());
            return ResponseEntity.badRequest().body(RefinementApplyResponseDTO.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build());
        } catch (Exception e) {
            logger.error("refine/apply failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(RefinementApplyResponseDTO.builder()
                    .success(false)
                    .errorMessage("Failed to apply hunks: " + e.getMessage())
                    .build());
        }
    }
}
