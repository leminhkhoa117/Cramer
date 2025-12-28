package com.cramer.service.abts;

import com.cramer.config.OpenRouterConfig;
import com.cramer.dto.abts.GeneratedContentDTO;
import com.cramer.dto.abts.GenerationRequestDTO;
import com.cramer.dto.abts.GenerationResponseDTO;
import com.cramer.dto.abts.GenerationResponseDTO.GenerationMetadataDTO;
import com.cramer.dto.abts.GenerationResponseDTO.ValidationResultDTO;
import com.cramer.dto.abts.SaveContentRequestDTO;
import com.cramer.dto.abts.SaveContentResponseDTO;
import com.cramer.dto.abts.StreamEventDTO;
import com.cramer.entity.Hashtag;
import com.cramer.entity.IeltsTest;
import com.cramer.entity.Section;
import com.cramer.entity.TestSet;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.repository.HashtagRepository;
import com.cramer.repository.IeltsTestRepository;
import com.cramer.repository.SectionRepository;
import com.cramer.repository.TestSetRepository;
import com.cramer.service.HashtagService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ABTS Service - Core orchestration for AI-Based Test Generation.
 * 
 * Coordinates the generation pipeline:
 * 1. Build prompts using PromptBuilderService
 * 2. Call AI via OpenRouterClient
 * 3. Validate output with JsonValidatorService
 * 4. Transform to internal format with ContentTransformerService
 * 
 * Implements 3-retry fail-hard strategy for JSON errors.
 * 
 * @since 2025-12-20 - ABTS v2.0
 */
@Service
public class ABTSService {

    private static final Logger logger = LoggerFactory.getLogger(ABTSService.class);
    private static final int MAX_RETRIES = 3;

    private final OpenRouterConfig config;
    private final OpenRouterClient openRouterClient;
    private final PromptBuilderService promptBuilderService;
    private final JsonValidatorService jsonValidatorService;
    private final JdbcTemplate jdbcTemplate;

    // New dependencies for test hierarchy support
    private final TestSetRepository testSetRepository;
    private final IeltsTestRepository ieltsTestRepository;
    private final HashtagRepository hashtagRepository;
    private final HashtagService hashtagService;
    private final SectionRepository sectionRepository;
    private final ObjectMapper objectMapper;

    public ABTSService(
            OpenRouterConfig config,
            OpenRouterClient openRouterClient,
            PromptBuilderService promptBuilderService,
            JsonValidatorService jsonValidatorService,
            JdbcTemplate jdbcTemplate,
            TestSetRepository testSetRepository,
            IeltsTestRepository ieltsTestRepository,
            HashtagRepository hashtagRepository,
            HashtagService hashtagService,
            SectionRepository sectionRepository) {
        this.config = config;
        this.openRouterClient = openRouterClient;
        this.promptBuilderService = promptBuilderService;
        this.jsonValidatorService = jsonValidatorService;
        this.jdbcTemplate = jdbcTemplate;
        this.testSetRepository = testSetRepository;
        this.ieltsTestRepository = ieltsTestRepository;
        this.hashtagRepository = hashtagRepository;
        this.hashtagService = hashtagService;
        this.sectionRepository = sectionRepository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Main generation entry point.
     * Routes to skill-specific generation methods.
     */
    public GenerationResponseDTO generate(GenerationRequestDTO request) {
        long startTime = System.currentTimeMillis();

        try {
            // Validate API key availability
            if (!config.hasApiKey()) {
                return GenerationResponseDTO.error(
                        "AUTH_FAILED",
                        "OpenRouter API key not configured. Set OPENROUTER_API_KEY environment variable.",
                        false);
            }

            // Route to skill-specific generation
            GenerationResponseDTO response = switch (request.getSkill()) {
                case READING -> generateReading(request);
                case LISTENING -> generateListening(request);
                case WRITING -> generateWriting(request);
                case SPEAKING -> GenerationResponseDTO.error(
                        "NOT_IMPLEMENTED",
                        "Speaking generation is not yet implemented.",
                        false);
            };

            // Add timing metadata
            double generationTime = (System.currentTimeMillis() - startTime) / 1000.0;
            if (response.getMetadata() != null) {
                response.getMetadata().setGenerationTimeSeconds(generationTime);
                response.getMetadata()
                        .setGeneratedAt(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }

            return response;

        } catch (OpenRouterClient.OpenRouterException e) {
            logger.error("OpenRouter API error during generation: {}", e.getMessage());
            return GenerationResponseDTO.error(e.getErrorCode(), e.getMessage(), e.isRetryable());
        } catch (Exception e) {
            logger.error("ABTS generation failed: {}", e.getMessage(), e);
            return GenerationResponseDTO.error("GENERATION_FAILED", e.getMessage(), false);
        }
    }

    /**
     * Generate content with streaming progress updates.
     * Sends SSE events to the emitter during the generation process.
     * Supports cancellation via the provided AtomicBoolean flag.
     */
    public void generateWithStream(GenerationRequestDTO request, SseEmitter emitter,
            java.util.concurrent.atomic.AtomicBoolean cancelled) throws IOException {
        try {
            // Check cancellation at start
            if (cancelled != null && cancelled.get()) {
                sendEvent(emitter, StreamEventDTO.aborted());
                emitter.complete();
                return;
            }

            // Send started event
            sendEvent(emitter, StreamEventDTO.started());

            // Validate API key availability
            if (!config.hasApiKey()) {
                sendEvent(emitter, StreamEventDTO.failed("OpenRouter API key not configured"));
                emitter.complete();
                return;
            }

            // Check cancellation before starting generation
            if (cancelled != null && cancelled.get()) {
                sendEvent(emitter, StreamEventDTO.aborted());
                emitter.complete();
                return;
            }

            // Route to skill-specific streaming generation
            switch (request.getSkill()) {
                case READING -> generateReadingWithStream(request, emitter, cancelled);
                case LISTENING -> generateListeningWithStream(request, emitter, cancelled);
                case WRITING -> generateWritingWithStream(request, emitter, cancelled);
                case SPEAKING -> {
                    sendEvent(emitter, StreamEventDTO.failed("Speaking generation is not yet implemented"));
                    emitter.complete();
                }
            }

        } catch (OpenRouterClient.OpenRouterException e) {
            if (cancelled != null && cancelled.get()) {
                logger.info("Generation was cancelled by user");
                return;
            }
            logger.error("OpenRouter API error during streaming generation: {}", e.getMessage());
            sendEvent(emitter, StreamEventDTO.failed(e.getMessage()));
            emitter.complete();
        } catch (Exception e) {
            if (cancelled != null && cancelled.get()) {
                logger.info("Generation was cancelled by user");
                return;
            }
            logger.error("Streaming generation failed: {}", e.getMessage(), e);
            sendEvent(emitter, StreamEventDTO.failed(e.getMessage()));
            emitter.complete();
        }
    }

    /**
     * Backward-compatible overload for generateWithStream
     */
    public void generateWithStream(GenerationRequestDTO request, SseEmitter emitter) throws IOException {
        generateWithStream(request, emitter, null);
    }

    /**
     * Generate Reading content with streaming updates.
     */
    private void generateReadingWithStream(GenerationRequestDTO request, SseEmitter emitter,
            java.util.concurrent.atomic.AtomicBoolean cancelled) throws IOException {
        logger.info("Starting Reading generation (streaming) for topic: {}", request.getTopic());

        int attempts = 0;
        String lastError = null;

        while (attempts < MAX_RETRIES) {
            attempts++;

            if (attempts > 1) {
                sendEvent(emitter, StreamEventDTO.retry(attempts, MAX_RETRIES, lastError));
            }

            try {
                // 1. Build prompts
                sendEvent(emitter, StreamEventDTO.promptBuilt());
                String systemPrompt = promptBuilderService.buildReadingSystemPrompt();
                String userPrompt = promptBuilderService.buildReadingPrompt(request);
                Map<String, Object> jsonSchema = promptBuilderService.getReadingJsonSchema();

                // 2. Determine model to use
                String model = resolveModel(request, false);
                logger.info("Requested model: {}", model);

                // 3. Send AI calling event with model name
                sendEvent(emitter, StreamEventDTO.progress(20, "Calling AI model: " + model));

                // 4. Build reasoning config
                Map<String, Object> reasoningConfig = new HashMap<>();
                if (Boolean.TRUE.equals(request.getEnableReasoning())) {
                    reasoningConfig.put("effort", request.getReasoningEffort() != null
                            ? request.getReasoningEffort()
                            : "high");
                }

                // 5. Use TRUE SSE STREAMING to receive tokens as they arrive
                final OpenRouterClient.OpenRouterResponse[] responseHolder = new OpenRouterClient.OpenRouterResponse[1];
                final Exception[] errorHolder = new Exception[1];
                final Object lock = new Object();
                final boolean[] completed = { false };

                Integer maxTokens = resolveMaxTokens(request, 16384);

                openRouterClient.callChatCompletionStreaming(
                        model,
                        systemPrompt,
                        userPrompt,
                        jsonSchema,
                        reasoningConfig,
                        request.getTemperature(),
                        maxTokens,
                        new OpenRouterClient.StreamCallback() {
                            @Override
                            public void onReasoningChunk(String reasoningDelta) {
                                // Forward reasoning tokens to frontend in real-time
                                sendEvent(emitter, StreamEventDTO.aiThinking(reasoningDelta));
                            }

                            @Override
                            public void onContentChunk(String contentDelta) {
                                // Forward content tokens (JSON content, less useful to display)
                                sendEvent(emitter, StreamEventDTO.aiChunk(contentDelta));
                            }

                            @Override
                            public void onProgress(int percent, String message) {
                                sendEvent(emitter, StreamEventDTO.progress(percent, message));
                            }

                            @Override
                            public void onComplete(OpenRouterClient.OpenRouterResponse response) {
                                synchronized (lock) {
                                    responseHolder[0] = response;
                                    completed[0] = true;
                                    lock.notify();
                                }
                            }

                            @Override
                            public void onError(String error) {
                                synchronized (lock) {
                                    errorHolder[0] = new RuntimeException(error);
                                    completed[0] = true;
                                    lock.notify();
                                }
                            }
                        },
                        cancelled);

                // Wait for streaming to complete (blocking)
                synchronized (lock) {
                    while (!completed[0]) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted while waiting for streaming", e);
                        }
                    }
                }

                // Check for errors
                if (errorHolder[0] != null) {
                    if (shouldFallbackToNonStreaming(errorHolder[0])) {
                        sendEvent(emitter, StreamEventDTO.progress(25,
                                "Streaming JSON schema unsupported. Switching to non-streaming."));
                        GenerationResponseDTO fallback = generateReading(request);
                        sendEvent(emitter, StreamEventDTO.completed(fallback));
                        emitter.complete();
                        return;
                    }
                    throw errorHolder[0];
                }

                OpenRouterClient.OpenRouterResponse aiResponse = responseHolder[0];

                // 6. Send AI completed event
                sendEvent(emitter, StreamEventDTO.aiCompleted(aiResponse.getDurationMs()));

                // 7. Validate response
                sendEvent(emitter, StreamEventDTO.validating());
                JsonValidatorService.ValidationResult validationResult = jsonValidatorService
                        .validateReadingContent(aiResponse.getContent(), request);

                if (!validationResult.isValid()) {
                    lastError = String.join("; ", validationResult.getAllErrors());
                    sendEvent(emitter, StreamEventDTO.validationResult(false, validationResult.getAllErrors()));
                    logger.warn("Validation failed on attempt {}: {}", attempts, lastError);

                    // RELAXED VALIDATION: Only retry on FATAL Schema errors (invalid JSON
                    // structure)
                    // Content/Business errors (word count, etc.) are treated as warnings ->
                    // PARTIAL_SUCCESS
                    if (!validationResult.getSchemaErrors().isEmpty()) {
                        if (attempts < MAX_RETRIES) {
                            continue; // Retry only for broken JSON
                        }
                    }
                    // Else: PROCEED with warnings
                }

                // 8. Send validation success
                sendEvent(emitter, StreamEventDTO.validationResult(true, validationResult.getWarnings()));

                // 9. Parse content
                GeneratedContentDTO content = jsonValidatorService.parseGeneratedContent(aiResponse.getContent());

                // 10. Build success response
                GenerationResponseDTO response = new GenerationResponseDTO();
                response.setStatus(GenerationResponseDTO.GenerationStatus.SUCCESS);
                response.setContent(content);
                response.setReasoning(aiResponse.getReasoning());
                response.setValidation(buildValidationDto(validationResult));

                // Add warnings (include validation errors if invalid)
                List<String> allWarnings = new ArrayList<>(validationResult.getWarnings());
                if (!validationResult.isValid()) {
                    allWarnings.addAll(validationResult.getAllErrors());
                }
                if (!allWarnings.isEmpty()) {
                    response.setWarnings(allWarnings);
                    response.setStatus(GenerationResponseDTO.GenerationStatus.PARTIAL_SUCCESS);
                }

                // Build metadata
                GenerationMetadataDTO metadata = buildMetadata(request, aiResponse, content);
                response.setMetadata(metadata);

                // 11. Send completed event with full response
                sendEvent(emitter, StreamEventDTO.completed(response));
                emitter.complete();

                logger.info("Reading generation (streaming) successful on attempt {}", attempts);
                return;

            } catch (OpenRouterClient.OpenRouterException e) {
                lastError = e.getMessage();
                logger.error("OpenRouter error on attempt {}: {}", attempts, lastError);
                sendEvent(emitter, StreamEventDTO.progress(20, "Error: " + lastError));

                if (shouldFallbackToNonStreaming(e)) {
                    sendEvent(emitter, StreamEventDTO.progress(25,
                            "Streaming JSON schema unsupported. Switching to non-streaming."));
                    GenerationResponseDTO fallback = generateListening(request);
                    sendEvent(emitter, StreamEventDTO.completed(fallback));
                    emitter.complete();
                    return;
                }

                if (!e.isRetryable()) {
                    sendEvent(emitter, StreamEventDTO.failed(e.getMessage()));
                    emitter.complete();
                    return;
                }
            } catch (Exception e) {
                lastError = e.getMessage();
                logger.error("Exception on attempt {}: {}", attempts, lastError, e);
            }
        }

        // All retries exhausted
        sendEvent(emitter, StreamEventDTO.failed("Generation failed after " + MAX_RETRIES + " attempts: " + lastError));
        emitter.complete();
    }

    /**
     * Generate Listening content with streaming updates.
     */
    private void generateListeningWithStream(GenerationRequestDTO request, SseEmitter emitter,
            java.util.concurrent.atomic.AtomicBoolean cancelled) throws IOException {
        logger.info("Starting Listening generation (streaming) for topic: {}", request.getTopic());

        int attempts = 0;
        String lastError = null;

        while (attempts < MAX_RETRIES) {
            attempts++;

            if (attempts > 1) {
                sendEvent(emitter, StreamEventDTO.retry(attempts, MAX_RETRIES, lastError));
            }

            try {
                // Check cancellation
                if (cancelled != null && cancelled.get()) {
                    throw new IOException("Generation cancelled by user");
                }

                // 1. Build prompts
                sendEvent(emitter, StreamEventDTO.promptBuilt());
                String systemPrompt = promptBuilderService.buildListeningSystemPrompt();
                String userPrompt = promptBuilderService.buildListeningPrompt(request);
                Map<String, Object> jsonSchema = promptBuilderService.getListeningJsonSchema();

                // 2. Determine model
                String model = resolveModel(request, false);
                logger.info("Listening generation using model: {}", model);

                // 3. Send AI calling event
                sendEvent(emitter, StreamEventDTO.progress(20, "Calling AI model: " + model));

                // 4. Build reasoning config
                Map<String, Object> reasoningConfig = new HashMap<>();
                if (Boolean.TRUE.equals(request.getEnableReasoning())) {
                    reasoningConfig.put("effort", request.getReasoningEffort() != null
                            ? request.getReasoningEffort()
                            : "high");
                }

                // 5. Call OpenRouter API with progress updates
                // 5. Use TRUE SSE STREAMING to receive tokens as they arrive
                Integer maxTokens = resolveMaxTokens(request, 16384);

                // We need to capture the response from the streaming client
                // Using array wrappers to allow modification from inner class/lambda if needed
                // (though StreamResult returns it)
                // Actually callChatCompletionStreaming returns the full response object after
                // stream ends
                final OpenRouterClient.OpenRouterResponse[] responseHolder = new OpenRouterClient.OpenRouterResponse[1];
                final Exception[] errorHolder = new Exception[1];
                final Object lock = new Object();
                final boolean[] streamCompleted = { false };

                openRouterClient.callChatCompletionStreaming(
                        model,
                        systemPrompt,
                        userPrompt,
                        jsonSchema,
                        reasoningConfig,
                        request.getTemperature(),
                        maxTokens,
                        new OpenRouterClient.StreamCallback() {
                            @Override
                            public void onReasoningChunk(String reasoningDelta) {
                                sendEvent(emitter, StreamEventDTO.aiThinking(reasoningDelta));
                            }

                            @Override
                            public void onContentChunk(String contentDelta) {
                                sendEvent(emitter, StreamEventDTO.aiChunk(contentDelta));
                            }

                            @Override
                            public void onProgress(int percent, String message) {
                                sendEvent(emitter, StreamEventDTO.progress(percent, message));
                            }

                            @Override
                            public void onComplete(OpenRouterClient.OpenRouterResponse response) {
                                synchronized (lock) {
                                    responseHolder[0] = response;
                                    streamCompleted[0] = true;
                                    lock.notifyAll();
                                }
                            }

                            @Override
                            public void onError(String error) {
                                synchronized (lock) {
                                    errorHolder[0] = new java.io.IOException(error);
                                    streamCompleted[0] = true;
                                    lock.notifyAll();
                                }
                            }
                        },
                        cancelled);

                // Wait for completion
                synchronized (lock) {
                    while (!streamCompleted[0]) {
                        try {
                            lock.wait(500);
                            if (cancelled != null && cancelled.get()) {
                                throw new java.io.IOException("Generation cancelled by user");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new java.io.IOException("Generation interrupted", e);
                        }
                    }
                }

                if (errorHolder[0] != null) {
                    throw errorHolder[0];
                }

                OpenRouterClient.OpenRouterResponse aiResponse = responseHolder[0];

                // 6. Send AI completed event
                sendEvent(emitter, StreamEventDTO.aiCompleted(aiResponse.getDurationMs()));

                // 7. Validate response
                sendEvent(emitter, StreamEventDTO.validating());
                JsonValidatorService.ValidationResult validationResult = jsonValidatorService
                        .validateListeningContent(aiResponse.getContent(), request);

                if (!validationResult.isValid()) {
                    lastError = String.join("; ", validationResult.getAllErrors());
                    sendEvent(emitter, StreamEventDTO.validationResult(false, validationResult.getAllErrors()));
                    logger.warn("Listening validation failed on attempt {}: {}", attempts, lastError);

                    // RELAXED VALIDATION: Only retry on FATAL Schema errors
                    if (!validationResult.getSchemaErrors().isEmpty()) {
                        if (attempts < MAX_RETRIES) {
                            continue;
                        }
                    }
                    // Else: PROCEED with warnings
                }

                // 8. Send validation success
                sendEvent(emitter, StreamEventDTO.validationResult(true, validationResult.getWarnings()));

                // 9. Parse content
                GeneratedContentDTO content = jsonValidatorService.parseGeneratedContent(aiResponse.getContent());

                // 10. Build response
                GenerationResponseDTO response = new GenerationResponseDTO();
                response.setStatus(validationResult.isValid()
                        ? GenerationResponseDTO.GenerationStatus.SUCCESS
                        : GenerationResponseDTO.GenerationStatus.PARTIAL_SUCCESS);
                response.setContent(content);
                response.setReasoning(aiResponse.getReasoning());
                response.setValidation(buildValidationDto(validationResult));

                List<String> allWarnings = new ArrayList<>(validationResult.getWarnings());
                if (!validationResult.isValid()) {
                    allWarnings.addAll(validationResult.getAllErrors());
                }
                if (!allWarnings.isEmpty()) {
                    response.setWarnings(allWarnings);
                }

                response.setMetadata(buildMetadata(request, aiResponse, content));

                // 11. Send completed event
                sendEvent(emitter, StreamEventDTO.completed(response));
                emitter.complete();

                logger.info("Listening generation (streaming) successful on attempt {}", attempts);
                return;

            } catch (OpenRouterClient.OpenRouterException e) {
                lastError = e.getMessage();
                logger.error("OpenRouter error on attempt {}: {}", attempts, lastError);
                sendEvent(emitter, StreamEventDTO.progress(20, "Error: " + lastError));

                if (shouldFallbackToNonStreaming(e)) {
                    sendEvent(emitter, StreamEventDTO.progress(25,
                            "Streaming JSON schema unsupported. Switching to non-streaming."));
                    GenerationResponseDTO fallback = generateWriting(request);
                    sendEvent(emitter, StreamEventDTO.completed(fallback));
                    emitter.complete();
                    return;
                }

                if (!e.isRetryable()) {
                    sendEvent(emitter, StreamEventDTO.failed(e.getMessage()));
                    emitter.complete();
                    return;
                }
            } catch (Exception e) {
                lastError = e.getMessage();
                logger.error("Exception on attempt {}: {}", attempts, lastError, e);
            }
        }

        // All retries exhausted
        sendEvent(emitter,
                StreamEventDTO.failed("Listening generation failed after " + MAX_RETRIES + " attempts: " + lastError));
        emitter.complete();
    }

    /**
     * Generate Writing content with streaming updates.
     */
    private void generateWritingWithStream(GenerationRequestDTO request, SseEmitter emitter,
            java.util.concurrent.atomic.AtomicBoolean cancelled) throws IOException {
        logger.info("Starting Writing generation (streaming) for topic: {}", request.getTopic());

        int attempts = 0;
        String lastError = null;

        while (attempts < MAX_RETRIES) {
            attempts++;

            if (attempts > 1) {
                sendEvent(emitter, StreamEventDTO.retry(attempts, MAX_RETRIES, lastError));
            }

            try {
                // Check cancellation
                if (cancelled != null && cancelled.get()) {
                    throw new IOException("Generation cancelled by user");
                }

                // 1. Build prompts
                sendEvent(emitter, StreamEventDTO.promptBuilt());
                String systemPrompt = promptBuilderService.buildWritingSystemPrompt();
                String userPrompt = promptBuilderService.buildWritingPrompt(request);
                Map<String, Object> jsonSchema = promptBuilderService.getWritingJsonSchema();

                // 2. Determine model
                String model = resolveModel(request, false);
                logger.info("Writing generation using model: {}", model);

                // 3. Send AI calling event
                sendEvent(emitter, StreamEventDTO.progress(20, "Calling AI model: " + model));

                // 4. Build reasoning config
                Map<String, Object> reasoningConfig = new HashMap<>();
                if (Boolean.TRUE.equals(request.getEnableReasoning())) {
                    reasoningConfig.put("effort", request.getReasoningEffort() != null
                            ? request.getReasoningEffort()
                            : "high");
                }

                // 5. Call OpenRouter API with progress updates
                OpenRouterClient.OpenRouterResponse aiResponse = callWithProgressUpdates(
                        emitter, model, systemPrompt, userPrompt, jsonSchema,
                        reasoningConfig, request.getTemperature(), resolveMaxTokens(request, 16384),
                        cancelled);

                // 6. Send AI completed event
                sendEvent(emitter, StreamEventDTO.aiCompleted(aiResponse.getDurationMs()));

                // 7. Validate response
                sendEvent(emitter, StreamEventDTO.validating());
                JsonValidatorService.ValidationResult validationResult = jsonValidatorService
                        .validateWritingContent(aiResponse.getContent(), request);

                if (!validationResult.isValid()) {
                    lastError = String.join("; ", validationResult.getAllErrors());
                    sendEvent(emitter, StreamEventDTO.validationResult(false, validationResult.getAllErrors()));
                    logger.warn("Writing validation failed on attempt {}: {}", attempts, lastError);

                    // RELAXED VALIDATION: Only retry on FATAL Schema errors
                    if (!validationResult.getSchemaErrors().isEmpty()) {
                        if (attempts < MAX_RETRIES) {
                            continue;
                        }
                    }
                    // Else: PROCEED
                }

                // 8. Send validation success
                sendEvent(emitter, StreamEventDTO.validationResult(true, validationResult.getWarnings()));

                // 9. Parse content
                GeneratedContentDTO content = jsonValidatorService.parseGeneratedContent(aiResponse.getContent());

                // 10. Build response
                GenerationResponseDTO response = new GenerationResponseDTO();
                response.setStatus(validationResult.isValid()
                        ? GenerationResponseDTO.GenerationStatus.SUCCESS
                        : GenerationResponseDTO.GenerationStatus.PARTIAL_SUCCESS);
                response.setContent(content);
                response.setReasoning(aiResponse.getReasoning());
                response.setValidation(buildValidationDto(validationResult));

                List<String> allWarnings = new ArrayList<>(validationResult.getWarnings());
                if (!validationResult.isValid()) {
                    allWarnings.addAll(validationResult.getAllErrors());
                }
                if (!allWarnings.isEmpty()) {
                    response.setWarnings(allWarnings);
                }

                response.setMetadata(buildMetadata(request, aiResponse, content));

                // 11. Send completed event
                sendEvent(emitter, StreamEventDTO.completed(response));
                emitter.complete();

                logger.info("Writing generation (streaming) successful on attempt {}", attempts);
                return;

            } catch (OpenRouterClient.OpenRouterException e) {
                lastError = e.getMessage();
                logger.error("OpenRouter error on attempt {}: {}", attempts, lastError);
                sendEvent(emitter, StreamEventDTO.progress(20, "Error: " + lastError));

                if (!e.isRetryable()) {
                    sendEvent(emitter, StreamEventDTO.failed(e.getMessage()));
                    emitter.complete();
                    return;
                }
            } catch (Exception e) {
                lastError = e.getMessage();
                logger.error("Exception on attempt {}: {}", attempts, lastError, e);
            }
        }

        // All retries exhausted
        sendEvent(emitter,
                StreamEventDTO.failed("Writing generation failed after " + MAX_RETRIES + " attempts: " + lastError));
        emitter.complete();
    }

    /**
     * Helper method to send SSE events.
     * Gracefully handles connection closure (when client aborts).
     */
    private void sendEvent(SseEmitter emitter, StreamEventDTO event) {
        try {
            emitter.send(SseEmitter.event()
                    .name(event.getType().name().toLowerCase())
                    .data(event));
        } catch (IOException e) {
            logger.debug("Failed to send SSE event (connection closed): {}", e.getMessage());
        } catch (IllegalStateException e) {
            // This happens when the emitter is already completed (client disconnected)
            logger.debug("Emitter already completed, skipping event: {}", event.getType());
        }
    }

    /**
     * Call OpenRouter API with periodic progress updates.
     * Sends AI_THINKING events every 5 seconds while waiting for the blocking API
     * call to complete.
     * This creates a more engaging user experience during long AI generation times.
     */
    private OpenRouterClient.OpenRouterResponse callWithProgressUpdates(
            SseEmitter emitter,
            String model,
            String systemPrompt,
            String userPrompt,
            Map<String, Object> jsonSchema,
            Map<String, Object> reasoningConfig,
            Double temperature,
            Integer maxTokens,
            java.util.concurrent.atomic.AtomicBoolean cancelled) throws Exception {

        // Track progress (starts at 20%, ends at 80% when AI completes)
        final int[] progressCounter = { 20 };
        final boolean[] completed = { false };

        // Start a thread to send periodic thinking events
        Thread progressThread = new Thread(() -> {
            try {
                int thinkingSeconds = 0;
                while (!completed[0] && progressCounter[0] < 80) {
                    Thread.sleep(5000); // Wait 5 seconds between updates
                    if (completed[0])
                        break;

                    thinkingSeconds += 5;
                    // Increment progress by ~10% every 5 seconds, max 80%
                    progressCounter[0] = Math.min(80, 20 + (thinkingSeconds / 5) * 10);

                    String message = String.format("AI is thinking... (%ds)", thinkingSeconds);
                    sendEvent(emitter, StreamEventDTO.progress(progressCounter[0], message));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        progressThread.setDaemon(true);
        progressThread.start();

        try {
            // Make the blocking API call
            OpenRouterClient.OpenRouterResponse response = openRouterClient.callChatCompletion(
                    model,
                    systemPrompt,
                    userPrompt,
                    jsonSchema,
                    null, // No fallback models
                    reasoningConfig,
                    temperature,
                    maxTokens);

            completed[0] = true;
            progressThread.interrupt();
            return response;

        } catch (Exception e) {
            completed[0] = true;
            progressThread.interrupt();
            throw e;
        }
    }

    /**
     * Generate Reading content (passage + questions).
     */
    private GenerationResponseDTO generateReading(GenerationRequestDTO request) {
        logger.info("Starting Reading generation for topic: {}", request.getTopic());

        int attempts = 0;
        String lastError = null;

        while (attempts < MAX_RETRIES) {
            attempts++;
            logger.info("Reading generation attempt {}/{}", attempts, MAX_RETRIES);

            try {
                // 1. Build prompts
                String systemPrompt = promptBuilderService.buildReadingSystemPrompt();
                String userPrompt = promptBuilderService.buildReadingPrompt(request);
                Map<String, Object> jsonSchema = promptBuilderService.getReadingJsonSchema();

                // 2. Determine model to use
                String model = resolveModel(request, false);

                // 3. Build reasoning config
                Map<String, Object> reasoningConfig = new HashMap<>();
                if (Boolean.TRUE.equals(request.getEnableReasoning())) {
                    reasoningConfig.put("effort", request.getReasoningEffort() != null
                            ? request.getReasoningEffort()
                            : "high");
                }

                // 4. Call OpenRouter API with optional web search and context caching
                boolean enableWebSearch = Boolean.TRUE.equals(request.getEnableWebSearch())
                        && (request.getFacts() == null || request.getFacts().isEmpty());
                boolean enableCaching = Boolean.TRUE.equals(request.getEnableContextCaching());

                OpenRouterClient.OpenRouterResponse aiResponse = openRouterClient.callChatCompletionWithFeatures(
                        model,
                        systemPrompt,
                        userPrompt,
                        jsonSchema,
                        List.of("anthropic/claude-3.5-sonnet", "deepseek/deepseek-chat"),
                        reasoningConfig,
                        request.getTemperature(),
                        resolveMaxTokens(request, 16384),
                        enableWebSearch,
                        enableCaching);

                // 5. Validate response
                JsonValidatorService.ValidationResult validationResult = jsonValidatorService
                        .validateReadingContent(aiResponse.getContent(), request);

                if (!validationResult.isValid()) {
                    lastError = String.join("; ", validationResult.getAllErrors());
                    logger.warn("Validation failed on attempt {}: {}", attempts, lastError);

                    if (attempts < MAX_RETRIES) {
                        continue; // Retry
                    }

                    // Failed after all retries -> FORCE ACCEPT WITH WARNINGS (Soft Fail)
                    // This matches streaming behavior and prevents user frustration.
                    logger.warn("Max retries reached. Forcing acceptance of invalid content as PARTIAL_SUCCESS.");
                    // Don't return error - continue to parse and return with warnings
                }

                // 6. Parse content
                GeneratedContentDTO content = jsonValidatorService.parseGeneratedContent(aiResponse.getContent());

                // 7. Build success response
                GenerationResponseDTO response = new GenerationResponseDTO();
                response.setStatus(GenerationResponseDTO.GenerationStatus.SUCCESS);
                response.setContent(content);
                response.setReasoning(aiResponse.getReasoning());

                // Add validation result
                response.setValidation(buildValidationDto(validationResult));

                // Add warnings if any (including validation errors as warnings for soft-fail)
                List<String> allWarnings = new ArrayList<>(validationResult.getWarnings());
                if (!validationResult.isValid()) {
                    allWarnings.addAll(validationResult.getAllErrors());
                    response.setStatus(GenerationResponseDTO.GenerationStatus.PARTIAL_SUCCESS);
                } else if (!validationResult.getWarnings().isEmpty()) {
                    response.setStatus(GenerationResponseDTO.GenerationStatus.PARTIAL_SUCCESS);
                }
                if (!allWarnings.isEmpty()) {
                    response.setWarnings(allWarnings);
                }

                // Build metadata
                GenerationMetadataDTO metadata = buildMetadata(request, aiResponse, content);
                response.setMetadata(metadata);

                logger.info("Reading generation successful on attempt {}", attempts);
                return response;

            } catch (OpenRouterClient.OpenRouterException e) {
                lastError = e.getMessage();
                logger.error("OpenRouter error on attempt {}: {}", attempts, lastError);

                if (!e.isRetryable()) {
                    return buildErrorResponse(e.getErrorCode(), e.getMessage(), attempts, false);
                }
            } catch (Exception e) {
                lastError = e.getMessage();
                logger.error("Error on attempt {}: {}", attempts, lastError, e);
            }
        }

        // All retries exhausted with no parseable content
        return buildErrorResponse("MAX_RETRIES_EXCEEDED",
                "Failed after " + MAX_RETRIES + " attempts. Last error: " + lastError,
                attempts, false);
    }

    /**
     * Generate Listening content (transcript + questions).
     */
    private GenerationResponseDTO generateListening(GenerationRequestDTO request) {
        logger.info("Starting Listening generation for topic: {}", request.getTopic());

        int attempts = 0;
        String lastError = null;

        while (attempts < MAX_RETRIES) {
            attempts++;

            try {
                // Build prompts
                String systemPrompt = promptBuilderService.buildListeningSystemPrompt();
                String userPrompt = promptBuilderService.buildListeningPrompt(request);
                Map<String, Object> jsonSchema = promptBuilderService.getListeningJsonSchema();

                String model = resolveModel(request, false);

                Map<String, Object> reasoningConfig = new HashMap<>();
                if (Boolean.TRUE.equals(request.getEnableReasoning())) {
                    reasoningConfig.put("effort", request.getReasoningEffort() != null
                            ? request.getReasoningEffort()
                            : "high");
                }

                // Call API with optional web search and context caching
                boolean enableWebSearch = Boolean.TRUE.equals(request.getEnableWebSearch())
                        && (request.getFacts() == null || request.getFacts().isEmpty());
                boolean enableCaching = Boolean.TRUE.equals(request.getEnableContextCaching());

                OpenRouterClient.OpenRouterResponse aiResponse = openRouterClient.callChatCompletionWithFeatures(
                        model,
                        systemPrompt,
                        userPrompt,
                        jsonSchema,
                        List.of("anthropic/claude-3.5-sonnet", "deepseek/deepseek-chat"),
                        reasoningConfig,
                        request.getTemperature(),
                        resolveMaxTokens(request, 16384),
                        enableWebSearch,
                        enableCaching);

                // Validate
                JsonValidatorService.ValidationResult validationResult = jsonValidatorService
                        .validateListeningContent(aiResponse.getContent(), request);

                if (!validationResult.isValid()) {
                    lastError = String.join("; ", validationResult.getAllErrors());
                    if (attempts < MAX_RETRIES)
                        continue;
                    // SOFT-FAIL: Force accept with warnings instead of failing
                    logger.warn("Max retries reached for Listening. Forcing PARTIAL_SUCCESS.");
                }

                // Parse and build response
                GeneratedContentDTO content = jsonValidatorService.parseGeneratedContent(aiResponse.getContent());

                GenerationResponseDTO response = new GenerationResponseDTO();
                response.setStatus(validationResult.isValid()
                        ? GenerationResponseDTO.GenerationStatus.SUCCESS
                        : GenerationResponseDTO.GenerationStatus.PARTIAL_SUCCESS);
                response.setContent(content);
                response.setReasoning(aiResponse.getReasoning());
                response.setMetadata(buildMetadata(request, aiResponse, content));
                response.setValidation(buildValidationDto(validationResult));

                List<String> allWarnings = new ArrayList<>(validationResult.getWarnings());
                if (!validationResult.isValid()) {
                    allWarnings.addAll(validationResult.getAllErrors());
                }
                if (!allWarnings.isEmpty()) {
                    response.setWarnings(allWarnings);
                }

                return response;

            } catch (OpenRouterClient.OpenRouterException e) {
                lastError = e.getMessage();
                if (!e.isRetryable()) {
                    return buildErrorResponse(e.getErrorCode(), e.getMessage(), attempts, false);
                }
            } catch (Exception e) {
                lastError = e.getMessage();
            }
        }

        return buildErrorResponse("MAX_RETRIES_EXCEEDED", lastError, attempts, false);
    }

    /**
     * Generate Writing content (chart data / essay prompt).
     */
    private GenerationResponseDTO generateWriting(GenerationRequestDTO request) {
        logger.info("Starting Writing generation for topic: {}", request.getTopic());

        int attempts = 0;
        String lastError = null;

        while (attempts < MAX_RETRIES) {
            attempts++;

            try {
                // Build prompts
                String systemPrompt = promptBuilderService.buildWritingSystemPrompt();
                String userPrompt = promptBuilderService.buildWritingPrompt(request);
                Map<String, Object> jsonSchema = promptBuilderService.getWritingJsonSchema();

                String model = resolveModel(request, false);

                Map<String, Object> reasoningConfig = new HashMap<>();
                if (Boolean.TRUE.equals(request.getEnableReasoning())) {
                    reasoningConfig.put("effort", request.getReasoningEffort() != null
                            ? request.getReasoningEffort()
                            : "high");
                }

                // Call API with optional web search and context caching
                boolean enableWebSearch = Boolean.TRUE.equals(request.getEnableWebSearch())
                        && (request.getFacts() == null || request.getFacts().isEmpty());
                boolean enableCaching = Boolean.TRUE.equals(request.getEnableContextCaching());

                OpenRouterClient.OpenRouterResponse aiResponse = openRouterClient.callChatCompletionWithFeatures(
                        model,
                        systemPrompt,
                        userPrompt,
                        jsonSchema,
                        List.of("anthropic/claude-3.5-sonnet", "deepseek/deepseek-chat"),
                        reasoningConfig,
                        request.getTemperature(),
                        resolveMaxTokens(request, 16384),
                        enableWebSearch,
                        enableCaching);

                // Validate
                JsonValidatorService.ValidationResult validationResult = jsonValidatorService
                        .validateWritingContent(aiResponse.getContent(), request);

                if (!validationResult.isValid()) {
                    lastError = String.join("; ", validationResult.getAllErrors());
                    if (attempts < MAX_RETRIES)
                        continue;
                    // SOFT-FAIL: Force accept with warnings instead of failing
                    logger.warn("Max retries reached for Writing. Forcing PARTIAL_SUCCESS.");
                }

                // Parse and build response
                GeneratedContentDTO content = jsonValidatorService.parseGeneratedContent(aiResponse.getContent());

                GenerationResponseDTO response = new GenerationResponseDTO();
                response.setStatus(validationResult.isValid()
                        ? GenerationResponseDTO.GenerationStatus.SUCCESS
                        : GenerationResponseDTO.GenerationStatus.PARTIAL_SUCCESS);
                response.setContent(content);
                response.setReasoning(aiResponse.getReasoning());
                response.setMetadata(buildMetadata(request, aiResponse, content));
                response.setValidation(buildValidationDto(validationResult));

                List<String> allWarnings = new ArrayList<>(validationResult.getWarnings());
                if (!validationResult.isValid()) {
                    allWarnings.addAll(validationResult.getAllErrors());
                }
                if (!allWarnings.isEmpty()) {
                    response.setWarnings(allWarnings);
                }

                return response;

            } catch (OpenRouterClient.OpenRouterException e) {
                lastError = e.getMessage();
                if (!e.isRetryable()) {
                    return buildErrorResponse(e.getErrorCode(), e.getMessage(), attempts, false);
                }
            } catch (Exception e) {
                lastError = e.getMessage();
            }
        }

        return buildErrorResponse("MAX_RETRIES_EXCEEDED", lastError, attempts, false);
    }

    /**
     * Regenerate specific questions while keeping existing passage.
     */
    public GenerationResponseDTO regenerateQuestions(GenerationRequestDTO request) {
        logger.info("Starting question regeneration for {} question(s)",
                request.getQuestionsToRegenerate() != null ? request.getQuestionsToRegenerate().size() : "all");

        try {
            GenerationRequestDTO.SkillType skill = request.getSkill() != null
                    ? request.getSkill()
                    : GenerationRequestDTO.SkillType.READING;

            return switch (skill) {
                case READING -> regenerateReadingQuestions(request);
                case LISTENING -> regenerateListeningQuestions(request);
                case WRITING -> generateWriting(request);
                case SPEAKING -> GenerationResponseDTO.error(
                        "NOT_IMPLEMENTED",
                        "Speaking regeneration is not yet implemented.",
                        false);
            };

        } catch (OpenRouterClient.OpenRouterException e) {
            logger.error("Question regeneration failed: {}", e.getMessage());
            return buildErrorResponse(e.getErrorCode(), e.getMessage(), 1, false);
        } catch (Exception e) {
            logger.error("Question regeneration failed: {}", e.getMessage());
            return GenerationResponseDTO.error("REGENERATION_FAILED", e.getMessage(), true);
        }
    }

    private GenerationResponseDTO regenerateReadingQuestions(GenerationRequestDTO request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("## TASK: Regenerate IELTS Questions for Existing Passage\n\n");
        prompt.append("### Existing Passage\n");
        prompt.append(request.getExistingPassageText()).append("\n\n");
        prompt.append("You MUST include the original passage in the output without any changes.\n");

        if (request.getQuestionsToRegenerate() != null && !request.getQuestionsToRegenerate().isEmpty()) {
            prompt.append("### Questions to Regenerate\n");
            prompt.append("Regenerate ONLY questions: ");
            prompt.append(request.getQuestionsToRegenerate().stream()
                    .map(String::valueOf)
                    .reduce((a, b) -> a + ", " + b).orElse(""));
            prompt.append("\n");
            prompt.append("Return ONLY the specified questions in the questions array.\n\n");
        }

        String model = resolveModel(request, true);
        Map<String, Object> jsonSchema = promptBuilderService.getReadingJsonSchema();

        OpenRouterClient.OpenRouterResponse aiResponse = openRouterClient.callChatCompletion(
                model,
                promptBuilderService.buildReadingSystemPrompt(),
                prompt.toString(),
                jsonSchema,
                List.of("anthropic/claude-3.5-sonnet", "deepseek/deepseek-chat"),
                Map.of("effort", "high"),
                request.getTemperature(),
                resolveMaxTokens(request, 8192));

        GeneratedContentDTO content;
        try {
            content = jsonValidatorService.parseGeneratedContent(aiResponse.getContent());
        } catch (Exception e) {
            logger.error("Failed to parse regenerated Reading content: {}", e.getMessage());
            return GenerationResponseDTO.error("PARSE_ERROR", e.getMessage(), false);
        }

        GenerationResponseDTO response = new GenerationResponseDTO();
        response.setStatus(GenerationResponseDTO.GenerationStatus.SUCCESS);
        response.setContent(content);
        response.setReasoning(aiResponse.getReasoning());
        response.setMetadata(buildMetadata(request, aiResponse, content));

        return response;
    }

    private GenerationResponseDTO regenerateListeningQuestions(GenerationRequestDTO request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("## TASK: Regenerate IELTS Listening Questions for Existing Transcript\n\n");
        prompt.append("### Existing Transcript (DO NOT MODIFY)\n");
        prompt.append(request.getExistingPassageText()).append("\n\n");
        prompt.append("You MUST include the original transcript in the output without any changes.\n");

        if (request.getQuestionsToRegenerate() != null && !request.getQuestionsToRegenerate().isEmpty()) {
            prompt.append("### Questions to Regenerate\n");
            prompt.append("Regenerate ONLY questions: ");
            prompt.append(request.getQuestionsToRegenerate().stream()
                    .map(String::valueOf)
                    .reduce((a, b) -> a + ", " + b).orElse(""));
            prompt.append("\n");
            prompt.append("Return ONLY the specified questions in the questions array.\n\n");
        }

        String model = resolveModel(request, true);
        Map<String, Object> jsonSchema = promptBuilderService.getListeningJsonSchema();

        OpenRouterClient.OpenRouterResponse aiResponse = openRouterClient.callChatCompletion(
                model,
                promptBuilderService.buildListeningSystemPrompt(),
                prompt.toString(),
                jsonSchema,
                List.of("anthropic/claude-3.5-sonnet", "deepseek/deepseek-chat"),
                Map.of("effort", "high"),
                request.getTemperature(),
                resolveMaxTokens(request, 8192));

        GeneratedContentDTO content;
        try {
            content = jsonValidatorService.parseGeneratedContent(aiResponse.getContent());
        } catch (Exception e) {
            logger.error("Failed to parse regenerated Listening content: {}", e.getMessage());
            return GenerationResponseDTO.error("PARSE_ERROR", e.getMessage(), false);
        }

        GenerationResponseDTO response = new GenerationResponseDTO();
        response.setStatus(GenerationResponseDTO.GenerationStatus.SUCCESS);
        response.setContent(content);
        response.setReasoning(aiResponse.getReasoning());
        response.setMetadata(buildMetadata(request, aiResponse, content));

        return response;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Build error response with metadata.
     */
    private GenerationResponseDTO buildErrorResponse(
            String errorCode, String message, int attempts, boolean retryable) {

        GenerationResponseDTO response = new GenerationResponseDTO();
        response.setStatus(GenerationResponseDTO.GenerationStatus.FAILED);
        response.setErrors(List.of(errorCode + ": " + message));

        GenerationMetadataDTO metadata = new GenerationMetadataDTO();
        metadata.setFailedAttempts(attempts);
        metadata.setLastError(message);
        metadata.setRetryable(retryable);
        response.setMetadata(metadata);

        return response;
    }

    /**
     * Build success metadata.
     */
    private GenerationMetadataDTO buildMetadata(
            GenerationRequestDTO request,
            OpenRouterClient.OpenRouterResponse aiResponse,
            GeneratedContentDTO content) {

        GenerationMetadataDTO metadata = new GenerationMetadataDTO();

        metadata.setTopic(request.getTopic());
        metadata.setDifficulty(request.getDifficulty().getDisplayName());
        metadata.setBandRange(request.getDifficulty().getBandRange());
        metadata.setModelUsed(aiResponse.getModelUsed());
        metadata.setPromptTokens(aiResponse.getPromptTokens());
        metadata.setCompletionTokens(aiResponse.getCompletionTokens());
        metadata.setReasoningTokens(aiResponse.getReasoningTokens());

        // Calculate word count and question count from content
        if (content != null && content.getSection() != null) {
            metadata.setWordCount(content.getSection().getWordCount());
        }
        if (content != null && content.getQuestions() != null) {
            metadata.setQuestionCount(content.getQuestions().size());
        }

        // Estimate cost (rough estimates based on OpenRouter pricing)
        double estimatedCost = estimateCost(aiResponse);
        metadata.setEstimatedCostUsd(estimatedCost);

        return metadata;
    }

    private ValidationResultDTO buildValidationDto(JsonValidatorService.ValidationResult validationResult) {
        ValidationResultDTO validationDTO = new ValidationResultDTO();
        validationDTO.setValid(validationResult.isValid());
        validationDTO.setSchemaErrors(validationResult.getSchemaErrors());
        validationDTO.setContentErrors(validationResult.getContentErrors());
        validationDTO.setBusinessRuleErrors(validationResult.getBusinessRuleErrors());
        return validationDTO;
    }

    /**
     * Estimate API cost based on tokens used.
     */
    private double estimateCost(OpenRouterClient.OpenRouterResponse response) {
        // Rough estimates: DeepSeek R1 is ~$0.55/M input, $2.19/M output
        double inputCost = (response.getPromptTokens() != null ? response.getPromptTokens() : 0) * 0.00000055;
        double outputCost = (response.getCompletionTokens() != null ? response.getCompletionTokens() : 0) * 0.00000219;
        double reasoningCost = (response.getReasoningTokens() != null ? response.getReasoningTokens() : 0) * 0.00000219;

        return Math.round((inputCost + outputCost + reasoningCost) * 10000.0) / 10000.0;
    }

    private String resolveModel(GenerationRequestDTO request, boolean forRegeneration) {
        String base = request.getModel() != null
                ? request.getModel()
                : (forRegeneration ? config.getRegenerationModel() : config.getGenerationModel());

        if (request.getModelVariant() != null && !request.getModelVariant().isBlank()) {
            return base + request.getModelVariant();
        }

        return base;
    }

    private Integer resolveMaxTokens(GenerationRequestDTO request, int defaultValue) {
        Integer requested = request.getMaxTokens();
        if (requested != null && requested > 0) {
            return requested;
        }
        return defaultValue;
    }

    private boolean shouldFallbackToNonStreaming(Exception e) {
        String message = e != null && e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return message.contains("response_format")
                || message.contains("json_schema")
                || message.contains("stream")
                || message.contains("streaming")
                || message.contains("not supported")
                || message.contains("unsupported");
    }

    /**
     * Validate content against JSON schemas and business rules.
     */
    public Map<String, Object> validateContent(Map<String, Object> content) {
        Map<String, Object> result = new HashMap<>();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.valueToTree(content);

            GenerationRequestDTO request = new GenerationRequestDTO();
            request.setDifficulty(GenerationRequestDTO.DifficultyLevel.INTERMEDIATE);
            request.setTestType(GenerationRequestDTO.TestType.ACADEMIC);

            if (root.has("explanation_language")) {
                String lang = root.get("explanation_language").asText();
                request.setExplanationLanguage("EN".equalsIgnoreCase(lang)
                        ? GenerationRequestDTO.ExplanationLanguage.EN
                        : GenerationRequestDTO.ExplanationLanguage.VI);
            }

            JsonValidatorService.ValidationResult validationResult;

            if (root.has("task_prompt")) {
                request.setSkill(GenerationRequestDTO.SkillType.WRITING);
                request.setPartNumber(detectWritingPart(root));
                validationResult = jsonValidatorService.validateWritingContent(root.toString(), request);
            } else if (root.has("transcript")) {
                request.setSkill(GenerationRequestDTO.SkillType.LISTENING);
                request.setPartNumber(detectListeningPart(root));
                validationResult = jsonValidatorService.validateListeningContent(root.toString(), request);
            } else {
                request.setSkill(GenerationRequestDTO.SkillType.READING);
                validationResult = jsonValidatorService.validateReadingContent(root.toString(), request);
            }

            result.put("valid", validationResult.isValid());
            result.put("schemaErrors", validationResult.getSchemaErrors());
            result.put("contentErrors", validationResult.getContentErrors());
            result.put("businessRuleErrors", validationResult.getBusinessRuleErrors());
            result.put("warnings", validationResult.getWarnings());

        } catch (Exception e) {
            result.put("valid", false);
            result.put("schemaErrors", List.of("Invalid content: " + e.getMessage()));
            result.put("contentErrors", List.of());
            result.put("businessRuleErrors", List.of());
            result.put("warnings", List.of());
        }

        return result;
    }

    private Integer detectListeningPart(JsonNode root) {
        if (root.has("part_number")) {
            return root.get("part_number").asInt();
        }
        if (root.has("partNumber")) {
            return root.get("partNumber").asInt();
        }
        JsonNode questions = root.get("questions");
        if (questions != null && questions.isArray() && !questions.isEmpty()) {
            int min = Integer.MAX_VALUE;
            for (JsonNode q : questions) {
                if (q.has("question_number")) {
                    min = Math.min(min, q.get("question_number").asInt());
                }
            }
            if (min != Integer.MAX_VALUE) {
                int part = ((min - 1) / 10) + 1;
                return Math.max(1, Math.min(4, part));
            }
        }
        return 1;
    }

    private Integer detectWritingPart(JsonNode root) {
        if (root.has("task_type")) {
            String taskType = root.get("task_type").asText().toUpperCase();
            if (taskType.startsWith("TASK_2")) {
                return 2;
            }
        }
        return 1;
    }

    /**
     * Get topic template categories from database.
     * Falls back to hardcoded values if database query fails.
     */
    public List<Map<String, Object>> getTemplateCategories() {
        try {
            String sql = """
                    SELECT DISTINCT
                        category as id,
                        category_label as name,
                        category_label as name_vi,
                        category_icon as emoji,
                        COUNT(*) as template_count
                    FROM public.abts_templates
                    WHERE is_active = true
                    GROUP BY category, category_label, category_icon
                    ORDER BY category
                    """;

            List<Map<String, Object>> categories = jdbcTemplate.queryForList(sql);

            if (categories != null && !categories.isEmpty()) {
                logger.info("Fetched {} template categories from database", categories.size());
                return categories;
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch template categories from database, using fallback: {}", e.getMessage());
        }

        // Fallback to hardcoded categories
        return List.of(
                Map.of("id", "environment", "emoji", "🌍", "name", "Environment", "name_vi", "Môi trường"),
                Map.of("id", "technology", "emoji", "💻", "name", "Technology", "name_vi", "Công nghệ"),
                Map.of("id", "education", "emoji", "📚", "name", "Education", "name_vi", "Giáo dục"),
                Map.of("id", "health", "emoji", "🏥", "name", "Health & Medicine", "name_vi", "Y tế"),
                Map.of("id", "society", "emoji", "👥", "name", "Society", "name_vi", "Xã hội"),
                Map.of("id", "business", "emoji", "💼", "name", "Business & Economy", "name_vi", "Kinh doanh"),
                Map.of("id", "science", "emoji", "🔬", "name", "Science", "name_vi", "Khoa học"),
                Map.of("id", "history", "emoji", "🏛️", "name", "History", "name_vi", "Lịch sử"),
                Map.of("id", "arts", "emoji", "🎨", "name", "Arts & Culture", "name_vi", "Nghệ thuật"),
                Map.of("id", "travel", "emoji", "✈️", "name", "Travel & Tourism", "name_vi", "Du lịch"));
    }

    /**
     * Get topic templates for a category from database.
     * Falls back to hardcoded sample if database query fails.
     */
    public List<Map<String, Object>> getTemplatesByCategory(String categoryId) {
        try {
            String sql = """
                    SELECT
                        id::text as id,
                        topic as name,
                        description,
                        hashtags,
                        facts,
                        skill,
                        difficulty,
                        test_type as "testType",
                        suggested_question_types as "suggestedQuestionTypes",
                        is_featured as "isFeatured",
                        use_count as "useCount"
                    FROM public.abts_templates
                    WHERE category = ? AND is_active = true
                    ORDER BY is_featured DESC, use_count DESC, topic
                    """;

            List<Map<String, Object>> templates = jdbcTemplate.queryForList(sql, categoryId);

            if (templates != null && !templates.isEmpty()) {
                logger.info("Fetched {} templates for category '{}' from database", templates.size(), categoryId);
                return templates;
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch templates for category '{}' from database: {}", categoryId, e.getMessage());
        }

        // Fallback to sample template
        return List.of(
                Map.of(
                        "id", categoryId + "_template_1",
                        "name", "Sample Template 1",
                        "hashtags", List.of("sample", categoryId),
                        "facts", List.of(
                                "This is a sample fact for the template.",
                                "Templates will be populated with more content.")));
    }

    /**
     * Increment use count for a template.
     */
    public void incrementTemplateUseCount(String templateId) {
        try {
            String sql = """
                    UPDATE public.abts_templates
                    SET use_count = use_count + 1, last_used_at = NOW()
                    WHERE id = ?::uuid
                    """;
            jdbcTemplate.update(sql, templateId);
            logger.debug("Incremented use count for template: {}", templateId);
        } catch (Exception e) {
            logger.warn("Failed to increment template use count: {}", e.getMessage());
        }
    }

    // Model cache (thread-safe)
    private volatile List<Map<String, Object>> cachedModels = null;
    private final AtomicLong cachedModelsTimestamp = new AtomicLong(0);
    private static final long MODEL_CACHE_DURATION_MS = 5 * 60 * 1000; // 5 minutes

    /**
     * Get available AI models for ABTS.
     * Fetches from OpenRouter API with caching.
     */
    public List<Map<String, Object>> getAvailableModels() {
        long now = System.currentTimeMillis();

        // Return cache if valid
        if (cachedModels != null && (now - cachedModelsTimestamp.get()) < MODEL_CACHE_DURATION_MS) {
            logger.debug("Returning cached models ({} models)", cachedModels.size());
            return cachedModels;
        }

        // Fetch from OpenRouter
        logger.info("Fetching models from OpenRouter API...");
        List<Map<String, Object>> models = openRouterClient.fetchAvailableModels();

        if (models != null && !models.isEmpty()) {
            cachedModels = models;
            cachedModelsTimestamp.set(now);
            logger.info("Cached {} models from OpenRouter", models.size());
            return models;
        }

        // Fallback to hardcoded if API fails
        logger.warn("OpenRouter API failed, using fallback models");
        return getFallbackModels();
    }

    /**
     * Fallback models if OpenRouter API is unavailable.
     */
    private List<Map<String, Object>> getFallbackModels() {
        return List.of(
                Map.of(
                        "id", "google/gemini-2.0-flash-001",
                        "name", "Gemini 2.0 Flash",
                        "description", "Fast and reliable for content generation",
                        "context_length", 1048576,
                        "pricing", Map.of("prompt", "0", "completion", "0")),
                Map.of(
                        "id", "deepseek/deepseek-chat",
                        "name", "DeepSeek Chat",
                        "description", "Best value model for quick regeneration",
                        "context_length", 65536,
                        "pricing", Map.of("prompt", "0.00000014", "completion", "0.00000028")),
                Map.of(
                        "id", "anthropic/claude-3.5-sonnet",
                        "name", "Claude 3.5 Sonnet",
                        "description", "High-quality for complex generation",
                        "context_length", 200000,
                        "pricing", Map.of("prompt", "0.000003", "completion", "0.000015")),
                Map.of(
                        "id", "openai/gpt-4o",
                        "name", "GPT-4o",
                        "description", "OpenAI latest multimodal model",
                        "context_length", 128000,
                        "pricing", Map.of("prompt", "0.0000025", "completion", "0.00001")),
                Map.of(
                        "id", "meta-llama/llama-3.1-70b-instruct:free",
                        "name", "Llama 3.1 70B (Free)",
                        "description", "Free tier for testing",
                        "context_length", 131072,
                        "pricing", Map.of("prompt", "0", "completion", "0")));
    }

    /**
     * Get ABTS status and configuration.
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("apiKeyConfigured", config.hasApiKey());
        status.put("baseUrl", config.getBaseUrl());
        status.put("defaultGenerationModel", config.getGenerationModel());
        status.put("defaultRegenerationModel", config.getRegenerationModel());
        status.put("streamingEnabled", config.isStreamingEnabled());
        status.put("timeoutMs", config.getTimeoutMs());
        status.put("version", "2.0.0-beta");
        status.put("phase", "Phase 4 - Reading/Listening/Writing Generation");

        return status;
    }

    // ==================== SAVE CONTENT ====================

    /**
     * Save AI-generated content to the database using the new test hierarchy.
     * Creates TestSet, IeltsTest, Section, and Questions as needed.
     * 
     * @param request     The save request containing exam metadata and generated
     *                    content
     * @param adminUserId The admin user ID for audit logging
     * @return SaveContentResponseDTO with results including new IDs
     * 
     * @since 2025-12-26 - Phase 3.5/6: Updated for test hierarchy
     */
    @Transactional
    public SaveContentResponseDTO saveContent(SaveContentRequestDTO request, String adminUserId) {
        logger.info("Saving ABTS content: skill={}, partNumber={}, setCode={}, testId={}",
                request.getSkill(), request.getPartNumber(), request.getSetCode(), request.getTestId());

        try {
            GeneratedContentDTO content = request.getContent();
            if (content == null) {
                return SaveContentResponseDTO.error("Content is null");
            }

            // Parse admin user ID to UUID if possible
            UUID createdBy = null;
            if (adminUserId != null && !adminUserId.isBlank()) {
                try {
                    createdBy = UUID.fromString(adminUserId);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid admin user ID format: {}", adminUserId);
                }
            }

            // 1. Resolve or create TestSet
            TestSet testSet = resolveTestSet(request, createdBy);
            logger.info("Using TestSet: id={}, code={}", testSet.getId(), testSet.getCode());

            // 2. Resolve or create IeltsTest
            IeltsTest ieltsTest = resolveTest(request, testSet, createdBy);
            logger.info("Using IeltsTest: id={}, testNumber={}", ieltsTest.getId(), ieltsTest.getTestNumber());

            // 3. Associate hashtags with the test
            if (request.getHashtagCodes() != null && !request.getHashtagCodes().isEmpty()) {
                Set<Hashtag> hashtags = hashtagService.findOrCreateByCodes(request.getHashtagCodes());
                ieltsTest.setHashtags(hashtags);
                hashtagService.incrementUseCounts(hashtags);
                logger.info("Associated {} hashtags with test", hashtags.size());
            } else if (request.getHashtagIds() != null && !request.getHashtagIds().isEmpty()) {
                Set<Hashtag> hashtags = new HashSet<>(hashtagRepository.findAllById(request.getHashtagIds()));
                ieltsTest.setHashtags(hashtags);
                hashtagService.incrementUseCounts(hashtags);
                logger.info("Associated {} hashtags (by ID) with test", hashtags.size());
            }

            // 4. Store generation metadata
            if (request.getGenerationConfig() != null) {
                Map<String, Object> metadata = buildGenerationMetadata(request);
                try {
                    JsonNode metadataJson = objectMapper.valueToTree(metadata);
                    ieltsTest.setGenerationMetadata(metadataJson);
                } catch (Exception e) {
                    logger.warn("Failed to set generation metadata: {}", e.getMessage());
                }
            }
            ieltsTest.setIsAiGenerated(true);

            // 5. Save the test (cascades updates)
            ieltsTest = ieltsTestRepository.save(ieltsTest);

            // 6. Check if section already exists for this test + skill + part
            String skillLower = request.getSkill().toLowerCase();
            Optional<Section> existingSection = sectionRepository.findByIeltsTestIdAndSkillAndPartNumber(
                    ieltsTest.getId(), skillLower, request.getPartNumber());

            Section section;
            boolean isUpdate = existingSection.isPresent();

            if (isUpdate) {
                // Update existing section
                section = existingSection.get();
                logger.info("Updating existing section ID: {} for test ID: {}", section.getId(), ieltsTest.getId());

                // Update content
                if (content.getSection() != null) {
                    GeneratedContentDTO.GeneratedSectionDTO sectionData = content.getSection();
                    section.setPassageText(sectionData.getPassageText());
                    if (sectionData.getSectionLayout() != null) {
                        section.setSectionLayout(sectionData.getSectionLayout());
                    }
                }

                // Delete old questions before inserting new ones
                jdbcTemplate.update("DELETE FROM questions WHERE section_id = ?", section.getId());
            } else {
                // Create new section
                section = createSection(request, content, ieltsTest);
            }

            section = sectionRepository.save(section);
            logger.info("{} section with ID: {}, linked to test ID: {}",
                    isUpdate ? "Updated" : "Created", section.getId(), ieltsTest.getId());

            // 7. Insert questions using JDBC for performance
            int questionsCreated = createQuestions(content, section, ieltsTest);
            logger.info("Created {} questions for section {}", questionsCreated, section.getId());

            // 8. Return success response with new hierarchy IDs
            SaveContentResponseDTO response = SaveContentResponseDTO.success(
                    section.getId(),
                    ieltsTest.getId(),
                    testSet.getId(),
                    testSet.getCode(),
                    ieltsTest.getTestNumber(),
                    request.getSkill().toLowerCase(),
                    request.getPartNumber(),
                    questionsCreated);

            // Add warnings if question count mismatch
            if (content.getQuestions() != null && questionsCreated < content.getQuestions().size()) {
                response.setWarnings(List.of(
                        String.format("Only %d of %d questions were saved",
                                questionsCreated, content.getQuestions().size())));
            }

            return response;

        } catch (Exception e) {
            logger.error("Failed to save ABTS content: {}", e.getMessage(), e);
            return SaveContentResponseDTO.error("Database error: " + e.getMessage());
        }
    }

    /**
     * Resolve or create a TestSet based on request parameters.
     */
    private TestSet resolveTestSet(SaveContentRequestDTO request, UUID createdBy) {
        // If setId provided, use it
        if (request.getSetId() != null) {
            return testSetRepository.findById(request.getSetId())
                    .orElseThrow(() -> new ResourceNotFoundException("TestSet", "id", request.getSetId()));
        }

        // If setCode provided, find or create
        String setCode = request.getSetCode();
        if (setCode == null || setCode.isEmpty()) {
            // Default to AI-generated set
            setCode = "ai_generated";
        }

        // Try to find existing
        Optional<TestSet> existing = testSetRepository.findByCode(setCode);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new TestSet - use explicit setNameVi if provided, otherwise format
        // from code
        String displayName = request.getSetNameVi();
        if (displayName == null || displayName.isBlank()) {
            displayName = formatCodeToName(setCode);
        }

        TestSet newSet = TestSet.builder()
                .code(setCode)
                .nameVi(displayName)
                .nameEn("Test Set: " + displayName)
                .sourceType("ai_generated")
                .isPublished(false)
                .isSystem(false)
                .displayOrder(testSetRepository.findMaxDisplayOrder() + 1)
                .createdBy(createdBy)
                .build();

        return testSetRepository.save(newSet);
    }

    /**
     * Resolve or create an IeltsTest based on request parameters.
     */
    private IeltsTest resolveTest(SaveContentRequestDTO request, TestSet testSet, UUID createdBy) {
        // If testId provided, use it
        if (request.getTestId() != null) {
            return ieltsTestRepository.findById(request.getTestId())
                    .orElseThrow(() -> new ResourceNotFoundException("IeltsTest", "id", request.getTestId()));
        }

        // Determine test number
        Integer testNumber = null;
        if (request.getTestNumber() != null && !request.getTestNumber().isBlank()) {
            try {
                testNumber = Integer.parseInt(request.getTestNumber());
            } catch (NumberFormatException e) {
                // Will auto-generate below
            }
        }

        if (testNumber == null) {
            // Auto-generate: get next available test number for this set
            Integer maxTestNumber = ieltsTestRepository.findMaxTestNumberByTestSetId(testSet.getId());
            testNumber = (maxTestNumber != null ? maxTestNumber : 0) + 1;
        }

        // Check if test exists for this set + number
        Optional<IeltsTest> existingTest = ieltsTestRepository.findByTestSetIdAndTestNumber(testSet.getId(),
                testNumber);
        if (existingTest.isPresent()) {
            return existingTest.get();
        }

        // Determine difficulty - prefer explicit request difficulty, then content
        // metadata, then default
        String difficulty = "INTERMEDIATE";
        if (request.getDifficulty() != null && !request.getDifficulty().isBlank()) {
            difficulty = request.getDifficulty();
        } else {
            GeneratedContentDTO content = request.getContent();
            if (content != null && content.getMetadata() != null && content.getMetadata().getDifficulty() != null) {
                difficulty = content.getMetadata().getDifficulty();
            }
        }

        // Create new test
        // Create new test
        String topic = request.getTopic() != null ? request.getTopic() : "AI Generated Test";

        String nameVi = request.getTestNameVi();
        if (nameVi == null || nameVi.isBlank()) {
            nameVi = "AI Test " + testNumber + (topic != null ? " - " + topic : "");
        }

        String nameEn = request.getTestNameEn();
        if (nameEn == null || nameEn.isBlank()) {
            nameEn = "AI Generated Test " + testNumber;
        }

        IeltsTest newTest = IeltsTest.builder()
                .testSet(testSet)
                .testNumber(testNumber)
                .nameVi(nameVi)
                .nameEn(nameEn)
                .difficulty(difficulty)
                .isPublished(false)
                .isAiGenerated(true)
                .createdBy(createdBy)
                .build();

        return ieltsTestRepository.save(newTest);
    }

    /**
     * Create a Section entity linked to the IeltsTest.
     */
    private Section createSection(SaveContentRequestDTO request, GeneratedContentDTO content, IeltsTest ieltsTest) {
        Section section = new Section();

        // NEW: Set the test FK
        section.setIeltsTest(ieltsTest);

        // Backward compatibility: still set these for existing queries
        section.setExamSource(ieltsTest.getTestSet().getCode());
        section.setTestNumber(ieltsTest.getTestNumber());

        section.setSkill(request.getSkill().toLowerCase());
        section.setPartNumber(request.getPartNumber());
        section.setStatus("DRAFT");

        // Set content from generated data
        if (content.getSection() != null) {
            GeneratedContentDTO.GeneratedSectionDTO sectionData = content.getSection();
            section.setPassageText(sectionData.getPassageText());

            // Handle section_layout for Listening tests
            if (sectionData.getSectionLayout() != null) {
                section.setSectionLayout(sectionData.getSectionLayout());
            }
        }

        // Handle figure_description for maps/diagrams
        if (content.getFigureDescription() != null) {
            try {
                section.setImageDescription(objectMapper.writeValueAsString(content.getFigureDescription()));
            } catch (Exception e) {
                logger.warn("Failed to serialize figure_description: {}", e.getMessage());
            }
        }

        return section;
    }

    /**
     * Create questions for the section using JDBC for performance.
     */
    private int createQuestions(GeneratedContentDTO content, Section section, IeltsTest ieltsTest) {
        if (content.getQuestions() == null || content.getQuestions().isEmpty()) {
            return 0;
        }

        String insertQuestionSql = """
                INSERT INTO questions (section_id, question_number, question_uid, question_type,
                                      question_content, correct_answer, explanation, word_limit, image_url)
                VALUES (?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?)
                """;

        String skillCode = section.getSkill().substring(0, 1); // r, l, w
        int questionsCreated = 0;

        for (GeneratedContentDTO.GeneratedQuestionDTO q : content.getQuestions()) {
            try {
                // Generate question UID
                String questionUid = String.format("%s-t%d-%s-q%d",
                        ieltsTest.getTestSet().getCode().toLowerCase(),
                        ieltsTest.getTestNumber(),
                        skillCode,
                        q.getQuestionNumber());

                // Serialize question_content to JSON
                String questionContentJson = null;
                if (q.getQuestionContent() != null) {
                    questionContentJson = objectMapper.writeValueAsString(q.getQuestionContent());
                }

                // Serialize correct_answer to JSON array
                String correctAnswerJson = null;
                if (q.getCorrectAnswer() != null) {
                    correctAnswerJson = objectMapper.writeValueAsString(q.getCorrectAnswer());
                }

                jdbcTemplate.update(
                        insertQuestionSql,
                        section.getId(),
                        q.getQuestionNumber(),
                        questionUid,
                        q.getQuestionType(),
                        questionContentJson,
                        correctAnswerJson,
                        q.getExplanation(),
                        q.getWordLimit(),
                        q.getImageUrl());

                questionsCreated++;
            } catch (Exception e) {
                logger.error("Failed to insert question {}: {}", q.getQuestionNumber(), e.getMessage());
            }
        }

        return questionsCreated;
    }

    /**
     * Build generation metadata for reproducibility.
     */
    private Map<String, Object> buildGenerationMetadata(SaveContentRequestDTO request) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("generated_by", "ABTS");
        metadata.put("version", "2.0.0");
        metadata.put("generated_at", Instant.now().toString());

        // Store the generation inputs for reproducibility
        if (request.getGenerationConfig() != null) {
            metadata.put("generation_config", request.getGenerationConfig());
        }

        // Store topic and hashtags
        if (request.getTopic() != null) {
            metadata.put("topic", request.getTopic());
        }
        if (request.getHashtagCodes() != null) {
            metadata.put("hashtags", request.getHashtagCodes());
        }

        return metadata;
    }

    /**
     * Format a code to a readable name.
     * e.g., "ai_generated" -> "Ai Generated"
     */
    private String formatCodeToName(String code) {
        if (code == null || code.isEmpty()) {
            return code;
        }
        return Arrays.stream(code.split("[-_]"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .reduce((a, b) -> a + " " + b)
                .orElse(code);
    }
}
