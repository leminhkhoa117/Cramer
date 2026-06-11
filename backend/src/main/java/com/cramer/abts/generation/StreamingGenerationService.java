package com.cramer.abts.generation;

import com.cramer.abts.config.AbtsProperties;
import com.cramer.abts.domain.GenerationResult;
import com.cramer.abts.domain.GenerationStatus;
import com.cramer.abts.domain.StreamEvent;
import com.cramer.abts.web.dto.GenerationRequest;
import com.cramer.platform.common.ielts.Skill;
import com.cramer.platform.common.json.Json;
import com.cramer.platform.integration.openrouter.OpenRouterClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Streaming generation over SSE (SPEC-21 §2.2, §5, §6, SPEC-25 §1.1). Accepts the HTTP connection
 * first, runs generation on the bounded executor, and delivers all lifecycle events (incl. capacity
 * rejection / failure / completion / abort) as {@link StreamEvent}s. Client disconnect flips a
 * cancellation flag so upstream token usage stops.
 */
@Service
public class StreamingGenerationService {

    private static final Logger log = LoggerFactory.getLogger(StreamingGenerationService.class);

    private final GenerationService generationService;
    private final RefinementService refinementService;
    private final OpenRouterClient openRouter;
    private final ThreadPoolExecutor executor;
    private final long emitterTimeoutMs;

    public StreamingGenerationService(GenerationService generationService,
                                      RefinementService refinementService,
                                      OpenRouterClient openRouter,
                                      ThreadPoolExecutor abtsStreamingExecutor,
                                      AbtsProperties props) {
        this.generationService = generationService;
        this.refinementService = refinementService;
        this.openRouter = openRouter;
        this.executor = abtsStreamingExecutor;
        this.emitterTimeoutMs = props.streaming().emitterTimeoutMs();
    }

    /** Open an SSE stream for a generation request. */
    public SseEmitter stream(Skill skill, GenerationRequest request) {
        SseEmitter sse = new SseEmitter(emitterTimeoutMs);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        sse.onCompletion(() -> cancelled.set(true));
        sse.onError(e -> cancelled.set(true));
        sse.onTimeout(() -> {
            cancelled.set(true);
            sse.complete();
        });
        StreamEmitter emitter = sseSink(sse, cancelled);

        submit(sse, emitter, cancelled, () -> {
            emitter.emit(StreamEvent.started("Generation started"));
            if (!openRouter.isConfigured()) {
                emitter.emit(StreamEvent.failed("OpenRouter API key is not configured", "AUTH_FAILED"));
                return;
            }
            GenerationResult result = generationService.generate(skill, request, emitter, cancelled::get, true);
            if (result.status() == GenerationStatus.FAILED) {
                emitter.emit(StreamEvent.failed("Generation failed", result.errorCode()));
            } else {
                emitter.emit(StreamEvent.completed(Json.mapper().valueToTree(result)));
            }
        });
        return sse;
    }

    /** Open an SSE stream for a refinement request (emits {@code REFINEMENT_COMPLETED}). */
    public SseEmitter streamRefinement(com.cramer.abts.web.dto.RefinementRequest request) {
        SseEmitter sse = new SseEmitter(emitterTimeoutMs);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        sse.onCompletion(() -> cancelled.set(true));
        sse.onError(e -> cancelled.set(true));
        sse.onTimeout(() -> {
            cancelled.set(true);
            sse.complete();
        });
        StreamEmitter emitter = sseSink(sse, cancelled);

        submit(sse, emitter, cancelled, () -> {
            emitter.emit(StreamEvent.started("Refinement started"));
            if (!openRouter.isConfigured()) {
                emitter.emit(StreamEvent.failed("OpenRouter API key is not configured", "AUTH_FAILED"));
                return;
            }
            var hunks = refinementService.refine(request, emitter, cancelled::get);
            emitter.emit(StreamEvent.refinementCompleted(Json.mapper().valueToTree(hunks)));
        });
        return sse;
    }

    // ---------------------------------------------------------------- internals

    private void submit(SseEmitter sse, StreamEmitter emitter, AtomicBoolean cancelled, Runnable work) {
        try {
            executor.execute(() -> {
                try {
                    work.run();
                } catch (GenerationCancelledException ce) {
                    emitter.emit(StreamEvent.aborted("Cancelled"));
                } catch (IllegalArgumentException bad) {
                    emitter.emit(StreamEvent.failed(bad.getMessage(), "BAD_REQUEST"));
                } catch (Exception e) {
                    log.warn("ABTS stream task failed: {}", e.getMessage());
                    emitter.emit(StreamEvent.failed("Unexpected error", "UPSTREAM_ERROR"));
                } finally {
                    safeComplete(sse);
                }
            });
        } catch (RejectedExecutionException rex) {
            emitter.emit(StreamEvent.aborted("Generation capacity reached; please retry shortly"));
            safeComplete(sse);
        }
    }

    private StreamEmitter sseSink(SseEmitter sse, AtomicBoolean cancelled) {
        return event -> {
            if (cancelled.get()) {
                return;
            }
            try {
                sse.send(SseEmitter.event().name(event.type()).data(event, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException io) {
                cancelled.set(true);
            }
        };
    }

    private void safeComplete(SseEmitter sse) {
        try {
            sse.complete();
        } catch (RuntimeException ignored) {
            // already completed
        }
    }
}
