package com.cramer.service.abts;

import com.cramer.dto.abts.GeneratedContentDTO;
import com.cramer.dto.abts.GenerationRequestDTO;
import com.cramer.dto.abts.GenerationResponseDTO;
import com.cramer.dto.abts.GenerationResponseDTO.GenerationMetadataDTO;
import com.cramer.dto.abts.StreamEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;

final class AbtsSkillStreamingRunner {

    private static final Logger logger = LoggerFactory.getLogger(AbtsSkillStreamingRunner.class);
    private static final int MAX_RETRIES = 3;

    private final OpenRouterClient openRouterClient;
    private final PromptBuilderService promptBuilderService;
    private final JsonValidatorService jsonValidatorService;
    private final AbtsGenerationSupport generationSupport;
    private final AbtsGenerationRunner generationRunner;
    private final AbtsMultiPartStreamingRunner multiPartStreamingRunner;

    AbtsSkillStreamingRunner(
            OpenRouterClient openRouterClient,
            PromptBuilderService promptBuilderService,
            JsonValidatorService jsonValidatorService,
            AbtsGenerationSupport generationSupport,
            AbtsGenerationRunner generationRunner,
            AbtsMultiPartStreamingRunner multiPartStreamingRunner) {
        this.openRouterClient = openRouterClient;
        this.promptBuilderService = promptBuilderService;
        this.jsonValidatorService = jsonValidatorService;
        this.generationSupport = generationSupport;
        this.generationRunner = generationRunner;
        this.multiPartStreamingRunner = multiPartStreamingRunner;
    }

    void generateWithStream(GenerationRequestDTO request, SseEmitter emitter,
            java.util.concurrent.atomic.AtomicBoolean cancelled) throws IOException {
        switch (request.getSkill()) {
            case READING -> generateReadingWithStream(request, emitter, cancelled);
            case LISTENING -> generateListeningWithStream(request, emitter, cancelled);
            case WRITING -> generateWritingWithStream(request, emitter, cancelled);
            case SPEAKING -> {
                sendEvent(emitter, StreamEventDTO.failed("Speaking generation is not yet implemented"));
                emitter.complete();
            }
        }
    }

    private void generateReadingWithStream(GenerationRequestDTO request, SseEmitter emitter,
            java.util.concurrent.atomic.AtomicBoolean cancelled) throws IOException {
        logger.info("Starting Reading generation (streaming, 2-phase delegate) for topic: {}", request.getTopic());

        Integer partNumber = request.getPartNumber();
        if (partNumber == null) {
            partNumber = 1;
        }

        try {
            // FIX 13: started() is already emitted by ABTSService.generateWithStream; FIX 12: signal prompt ready.
            sendEvent(emitter, StreamEventDTO.promptBuilt());
            GenerationResponseDTO response = multiPartStreamingRunner.generateReadingForPart(
                    request, emitter, cancelled, partNumber, 1, 0, 100);

            if (response == null) {
                sendEvent(emitter, StreamEventDTO.aborted());
            } else if (response.getStatus() == GenerationResponseDTO.GenerationStatus.FAILED) {
                String msg = (response.getErrors() != null && !response.getErrors().isEmpty())
                        ? String.join("; ", response.getErrors())
                        : "Reading generation failed";
                sendEvent(emitter, StreamEventDTO.failed(msg));
            } else {
                sendEvent(emitter, StreamEventDTO.completed(response));
                logger.info("Reading generation (streaming, delegate) successful");
            }
        } catch (Exception e) {
            logger.error("Reading generation (streaming, delegate) failed: {}", e.getMessage(), e);
            sendEvent(emitter, StreamEventDTO.failed(e.getMessage()));
        } finally {
            emitter.complete();
        }
    }

    private void generateListeningWithStream(GenerationRequestDTO request, SseEmitter emitter,
            java.util.concurrent.atomic.AtomicBoolean cancelled) throws IOException {
        logger.info("Starting Listening generation (streaming, 3-phase delegate) for topic: {}", request.getTopic());

        Integer partNumber = request.getPartNumber();
        if (partNumber == null) {
            partNumber = 1;
        }

        try {
            // FIX 13: started() is already emitted by ABTSService.generateWithStream; FIX 12: signal prompt ready.
            sendEvent(emitter, StreamEventDTO.promptBuilt());
            GenerationResponseDTO response = multiPartStreamingRunner.generateListeningForPart(
                    request, emitter, cancelled, partNumber, 1, 0, 100);

            if (response == null) {
                sendEvent(emitter, StreamEventDTO.aborted());
            } else if (response.getStatus() == GenerationResponseDTO.GenerationStatus.FAILED) {
                String msg = (response.getErrors() != null && !response.getErrors().isEmpty())
                        ? String.join("; ", response.getErrors())
                        : "Listening generation failed";
                sendEvent(emitter, StreamEventDTO.failed(msg));
            } else {
                sendEvent(emitter, StreamEventDTO.completed(response));
                logger.info("Listening generation (streaming, delegate) successful");
            }
        } catch (Exception e) {
            logger.error("Listening generation (streaming, delegate) failed: {}", e.getMessage(), e);
            sendEvent(emitter, StreamEventDTO.failed(e.getMessage()));
        } finally {
            emitter.complete();
        }
    }

    private void generateWritingWithStream(GenerationRequestDTO request, SseEmitter emitter,
            java.util.concurrent.atomic.AtomicBoolean cancelled) throws IOException {
        logger.info("Starting Writing generation (streaming, 3-phase delegate) for topic: {}", request.getTopic());

        Integer partNumber = request.getPartNumber();
        if (partNumber == null) {
            partNumber = 1;
        }

        try {
            // FIX 13: started() is already emitted by ABTSService.generateWithStream; FIX 12: signal prompt ready.
            sendEvent(emitter, StreamEventDTO.promptBuilt());
            GenerationResponseDTO response = multiPartStreamingRunner.generateWritingForPart(
                    request, emitter, cancelled, partNumber, 1, 0, 100);

            if (response == null) {
                sendEvent(emitter, StreamEventDTO.aborted());
            } else if (response.getStatus() == GenerationResponseDTO.GenerationStatus.FAILED) {
                String msg = (response.getErrors() != null && !response.getErrors().isEmpty())
                        ? String.join("; ", response.getErrors())
                        : "Writing generation failed";
                sendEvent(emitter, StreamEventDTO.failed(msg));
            } else {
                sendEvent(emitter, StreamEventDTO.completed(response));
                logger.info("Writing generation (streaming, delegate) successful");
            }
        } catch (Exception e) {
            logger.error("Writing generation (streaming, delegate) failed: {}", e.getMessage(), e);
            sendEvent(emitter, StreamEventDTO.failed(e.getMessage()));
        } finally {
            emitter.complete();
        }
    }

    private void sendEvent(SseEmitter emitter, StreamEventDTO event) {
        sendEvent(emitter, event, null);
    }

    /**
     * FIX 6: cancelled-aware SSE send mirroring {@code AbtsMultiPartStreamingRunner}. When the
     * client connection is gone (IOException) or the emitter is already completed
     * (IllegalStateException), flip {@code cancelled} so any still-running generation work
     * stops promptly instead of burning further AI calls against a dead stream.
     */
    private void sendEvent(SseEmitter emitter, StreamEventDTO event,
            java.util.concurrent.atomic.AtomicBoolean cancelled) {
        try {
            emitter.send(SseEmitter.event()
                    .name(Objects.requireNonNull(event.getType().name().toLowerCase()))
                    .data(event));
        } catch (IOException e) {
            logger.debug("Failed to send SSE event (connection closed): {}", e.getMessage());
            if (cancelled != null) {
                cancelled.set(true);
            }
        } catch (IllegalStateException e) {
            logger.debug("Emitter already completed, skipping event: {}", event.getType());
            if (cancelled != null) {
                cancelled.set(true);
            }
        }
    }
}