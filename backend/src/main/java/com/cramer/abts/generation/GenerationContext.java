package com.cramer.abts.generation;

import com.cramer.abts.domain.StreamEvent;
import com.cramer.abts.domain.TokenUsage;
import com.cramer.abts.generation.prompt.PhasePrompt;
import com.cramer.abts.web.dto.ModelConfig;
import com.cramer.platform.integration.openrouter.OpenRouterChatRequest;
import com.cramer.platform.integration.openrouter.OpenRouterChatResult;
import com.cramer.platform.integration.openrouter.OpenRouterClient;
import com.cramer.platform.integration.openrouter.OpenRouterStreamListener;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Per-generation runtime state and phase runner (SPEC-21 §3, §6, §7). Carries the model config,
 * a phase cache (so a passing upstream phase is never re-billed across retries), token/reasoning
 * accumulators, the optional SSE emitter, and a cooperative cancellation flag. Not a Spring bean;
 * built per request by the generation service.
 */
public class GenerationContext {

    private final OpenRouterClient client;
    private final String model;
    private final ModelConfig modelConfig;
    private final JsonNode reasoningPayload;
    private final boolean streaming;
    private final StreamEmitter emitter;
    private final BooleanSupplier cancelled;
    private final String language;
    private final String customInstructions;

    private final Map<String, JsonNode> phaseCache = new HashMap<>();
    private final StringBuilder reasoning = new StringBuilder();
    private TokenUsage usage = TokenUsage.ZERO;

    public GenerationContext(OpenRouterClient client, String model, ModelConfig modelConfig,
                             JsonNode reasoningPayload, boolean streaming,
                             StreamEmitter emitter, BooleanSupplier cancelled,
                             String language, String customInstructions) {
        this.client = client;
        this.model = model;
        this.modelConfig = modelConfig;
        this.reasoningPayload = reasoningPayload;
        this.streaming = streaming;
        this.emitter = emitter == null ? StreamEmitter.NOOP : emitter;
        this.cancelled = cancelled == null ? () -> false : cancelled;
        this.language = language;
        this.customInstructions = customInstructions;
    }

    /**
     * Run one generation phase. Cacheable phases (passage/transcript/stems/task/sample) return the
     * cached result on retry; the validated phase (questions/answers/band) is never cached so a
     * failed validation re-generates it.
     */
    public JsonNode runPhase(String phaseKey, PhasePrompt prompt, int partNumber, boolean cacheable) {
        checkCancelled();
        String cacheId = partNumber + ":" + phaseKey;
        if (cacheable) {
            JsonNode cached = phaseCache.get(cacheId);
            if (cached != null) {
                return cached;
            }
        }
        emitter.emit(StreamEvent.promptBuilt("Prompt ready: " + prompt.schemaName(), partNumber));

        OpenRouterChatRequest request = new OpenRouterChatRequest(
                model, prompt.systemPrompt(), prompt.userPrompt(), prompt.schemaName(), prompt.schema(),
                modelConfig.resolvedTemperature(), modelConfig.resolvedMaxTokens(), reasoningPayload,
                false, modelConfig.cacheEnabled());

        OpenRouterChatResult result;
        if (streaming) {
            OpenRouterStreamListener listener = new OpenRouterStreamListener() {
                @Override
                public void onContentDelta(String delta) {
                    emitter.emit(StreamEvent.aiChunk(delta, partNumber));
                }

                @Override
                public void onReasoningDelta(String delta) {
                    reasoning.append(delta);
                    emitter.emit(StreamEvent.aiThinking(delta, partNumber));
                }
            };
            result = client.streamChat(request, listener, cancelled);
        } else {
            result = client.chat(request);
            if (result.reasoning() != null) {
                reasoning.append(result.reasoning());
            }
        }
        usage = usage.plus(TokenUsage.of(result.promptTokens(), result.completionTokens(),
                result.totalTokens(), result.cost()));

        JsonNode content = result.content();
        if (cacheable) {
            phaseCache.put(cacheId, content);
        }
        return content;
    }

    public void checkCancelled() {
        if (cancelled.getAsBoolean()) {
            throw new GenerationCancelledException("Generation cancelled");
        }
    }

    /** Pre-seed a phase result so its generation is skipped (used by regenerate-questions). */
    public void seedPhase(int partNumber, String phaseKey, JsonNode content) {
        phaseCache.put(partNumber + ":" + phaseKey, content);
    }

    public String model() {
        return model;
    }

    public String language() {
        return language;
    }

    public String customInstructions() {
        return customInstructions;
    }

    public void emit(StreamEvent event) {
        emitter.emit(event);
    }

    public TokenUsage usage() {
        return usage;
    }

    public String reasoningText() {
        return reasoning.length() == 0 ? null : reasoning.toString();
    }
}
