package com.cramer.service.abts;

import com.cramer.dto.abts.GenerationRequestDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Vendor-aware reasoning capability registry.
 *
 * <p>Different OpenRouter model families expose "reasoning"/"thinking" through
 * <em>different top-level request schemas</em>. The legacy code always wrapped a
 * flat {@code {"effort": "high"}} object under a single {@code "reasoning"} key,
 * which silently no-ops (or errors) for Anthropic, Google Gemini and DeepSeek
 * reasoning models. This registry infers a model's reasoning "knob" from its slug
 * and produces the correct top-level payload to spread into the request body.</p>
 *
 * <p>Stateless and side-effect free; safe to share as a singleton or instantiate
 * directly.</p>
 *
 * @since 2026 - ABTS PART C (Model picker overhaul)
 */
final class ModelCapabilityRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ModelCapabilityRegistry.class);

    /** FIX 7: only WARN once when an out-of-range effort is coerced to "medium". */
    private final AtomicBoolean invalidEffortWarned = new AtomicBoolean(false);

    /**
     * The shape of the reasoning control a given model family exposes.
     */
    enum ReasoningKnob {
        /** OpenAI o-series / GPT-5: {@code reasoning.effort = low|medium|high}. */
        EFFORT_LOW_MED_HIGH,
        /** Anthropic Claude: {@code thinking.type=enabled, thinking.budget_tokens}. */
        ANTHROPIC_BUDGET,
        /** Google Gemini: {@code thinking_config.thinking_budget}. */
        GEMINI_BUDGET,
        /** DeepSeek R1 / V4: {@code reasoning.enabled + reasoning.max_tokens}. */
        DEEPSEEK_TOGGLE,
        /** Qwen "thinking" variants: {@code reasoning.enabled} (schema TBD). */
        QWEN_THINKING,
        /** Z.AI GLM "thinking": {@code reasoning.enabled} (schema TBD). */
        GLM_THINKING,
        /** Moonshot Kimi: no exposed reasoning knob. */
        KIMI_NONE,
        /** No reasoning support / not a reasoning model. */
        NONE
    }

    /**
     * Immutable descriptor of a model's reasoning + I/O capabilities.
     */
    record ModelCapability(
            String id,
            String displayName,
            String vendor,
            ReasoningKnob knob,
            int defaultMaxTokens,
            int contextLength,
            boolean supportsJsonSchema,
            boolean supportsStreaming,
            List<String> validEfforts,
            Integer minThinkingBudget,
            Integer maxThinkingBudget) {
    }

    /**
     * Infer a model's capability descriptor purely from its OpenRouter slug.
     *
     * @param modelId raw model slug, e.g. {@code "anthropic/claude-sonnet-4.5"}
     * @return a non-null {@link ModelCapability}; falls back to {@link ReasoningKnob#NONE}
     */
    ModelCapability inferFromModelId(String modelId) {
        String raw = modelId == null ? "" : modelId.trim();
        String id = raw.toLowerCase(Locale.ROOT);
        String vendor = extractVendor(id);
        String displayName = prettyName(raw);

        // Anthropic Claude -> token-budget extended thinking
        if (id.startsWith("anthropic/") || id.contains("claude")) {
            return new ModelCapability(raw, displayName, "anthropic",
                    ReasoningKnob.ANTHROPIC_BUDGET, 16384, 200000, true, true,
                    List.of(), 1024, 65536);
        }
        // Google Gemini -> thinking_budget
        if (id.startsWith("google/") || id.contains("gemini")) {
            return new ModelCapability(raw, displayName, "google",
                    ReasoningKnob.GEMINI_BUDGET, 16384, 1048576, true, true,
                    List.of(), 0, 24576);
        }
        // DeepSeek reasoning families (R1, V4) -> enable toggle + max_tokens
        if (id.startsWith("deepseek/") && (id.contains("r1") || id.contains("v4"))) {
            return new ModelCapability(raw, displayName, "deepseek",
                    ReasoningKnob.DEEPSEEK_TOGGLE, 16384, 65536, true, true,
                    List.of(), null, null);
        }
        // DeepSeek non-reasoning families (V3, chat) -> no reasoning
        if (id.startsWith("deepseek/") && (id.contains("v3") || id.contains("chat"))) {
            return new ModelCapability(raw, displayName, "deepseek",
                    ReasoningKnob.NONE, 8192, 65536, true, true,
                    List.of(), null, null);
        }
        // Qwen "thinking" variants
        if (id.contains("qwen") && id.contains("thinking")) {
            return new ModelCapability(raw, displayName, "qwen",
                    ReasoningKnob.QWEN_THINKING, 16384, 131072, true, true,
                    List.of(), null, null);
        }
        // Z.AI GLM
        if (id.startsWith("z-ai/") || id.contains("glm")) {
            return new ModelCapability(raw, displayName, "z-ai",
                    ReasoningKnob.GLM_THINKING, 16384, 131072, true, true,
                    List.of(), null, null);
        }
        // Moonshot Kimi -> no exposed reasoning knob
        if (id.startsWith("moonshotai/") || id.contains("kimi")) {
            return new ModelCapability(raw, displayName, "moonshotai",
                    ReasoningKnob.KIMI_NONE, 8192, 131072, true, true,
                    List.of(), null, null);
        }
        // OpenAI reasoning models -> effort low/medium/high
        // FIX 18: anchor the o1/o3/gpt-5 substring checks to the "openai/" vendor prefix so
        // future third-party slugs like "someorg/turbo-o1x" are not misclassified as OpenAI.
        if (id.startsWith("openai/")) {
            return new ModelCapability(raw, displayName, "openai",
                    ReasoningKnob.EFFORT_LOW_MED_HIGH, 16384, 128000, true, true,
                    List.of("low", "medium", "high"), null, null);
        }
        // Default -> conservative, no reasoning
        return new ModelCapability(raw, displayName, vendor.isEmpty() ? "unknown" : vendor,
                ReasoningKnob.NONE, 8192, 0, true, true,
                List.of(), null, null);
    }

    /**
     * Build the vendor-correct, top-level reasoning payload for a request.
     *
     * <p>The returned map already contains the correct top-level key(s)
     * (e.g. {@code "reasoning"}, {@code "thinking"} or {@code "thinking_config"})
     * and is intended to be spread into the request body via {@code body.putAll(...)}.</p>
     *
     * @return possibly-empty (never null) top-level payload map
     */
    Map<String, Object> buildReasoningPayload(String modelId, GenerationRequestDTO request) {
        if (request == null || !Boolean.TRUE.equals(request.getEnableReasoning())) {
            return Map.of();
        }

        ModelCapability cap = inferFromModelId(modelId);
        switch (cap.knob()) {
            case EFFORT_LOW_MED_HIGH: {
                String effort = (request.getReasoningEffort() != null && !request.getReasoningEffort().isBlank())
                        ? request.getReasoningEffort()
                        : "medium";
                // FIX 7: never forward an effort the model does not advertise — OpenRouter
                // returns a 422 for unknown effort levels. Coerce to "medium" and WARN once.
                List<String> valid = cap.validEfforts();
                if (valid != null && !valid.isEmpty()) {
                    final String requested = effort;
                    boolean ok = valid.stream().anyMatch(e -> e.equalsIgnoreCase(requested));
                    if (!ok) {
                        if (invalidEffortWarned.compareAndSet(false, true)) {
                            logger.warn("Reasoning effort '{}' is not valid for model '{}' (allowed: {}); "
                                    + "falling back to 'medium'", requested, modelId, valid);
                        }
                        effort = "medium";
                    }
                }
                return Map.of("reasoning", Map.of("effort", effort));
            }
            case ANTHROPIC_BUDGET:
                // FIX 1: OpenRouter does NOT honor the Anthropic-native
                // {"thinking":{"type":"enabled","budget_tokens":N}} shape — extended thinking
                // would be silently dropped while we still pay for assumed reasoning tokens.
                // Use the OpenRouter-normalized {"reasoning":{"max_tokens":N}} instead.
                return Map.of("reasoning", Map.of(
                        "max_tokens", resolveBudget(request, cap, 4096)));
            case GEMINI_BUDGET:
                // FIX 2: same as Anthropic — {"thinking_config":{"thinking_budget":N}} is the
                // Google GenAI native shape, not an OpenRouter parameter, so it is silently
                // dropped. Use {"reasoning":{"max_tokens":N}} (budget still clamped to [0,24576]).
                return Map.of("reasoning", Map.of(
                        "max_tokens", resolveBudget(request, cap, 4096)));
            case DEEPSEEK_TOGGLE:
                // FIX 17: when max_tokens is present, OpenRouter implies enabled=true, so the
                // explicit enabled flag is redundant.
                return Map.of("reasoning", Map.of(
                        "max_tokens", resolveBudget(request, cap, 4096)));
            case QWEN_THINKING:
            case GLM_THINKING:
                // TODO verify against current OpenRouter docs for Qwen/GLM thinking schema.
                return Map.of("reasoning", Map.of("enabled", true));
            case KIMI_NONE:
            case NONE:
            default:
                return Map.of();
        }
    }

    /**
     * Map an effort level (or explicit absence) to a thinking-token budget.
     */
    int resolveBudget(GenerationRequestDTO request, int defaultBudget) {
        String effort = request != null ? request.getReasoningEffort() : null;
        if (effort == null) {
            return defaultBudget;
        }
        switch (effort.toLowerCase(Locale.ROOT)) {
            case "low":
                return 2048;
            case "medium":
                return 4096;
            case "high":
                return 16384;
            default:
                return defaultBudget;
        }
    }

    /**
     * Resolve a thinking-token budget, preferring an explicit {@code reasoningBudget}
     * when supplied. An explicit budget is clamped to the model's supported
     * {@code [minThinkingBudget, maxThinkingBudget]} range; otherwise the
     * effort-derived budget is used.
     */
    int resolveBudget(GenerationRequestDTO request, ModelCapability cap, int defaultBudget) {
        Integer explicit = request != null ? request.getReasoningBudget() : null;
        if (explicit != null) {
            int value = explicit;
            if (cap != null && cap.minThinkingBudget() != null) {
                value = Math.max(value, cap.minThinkingBudget());
            }
            if (cap != null && cap.maxThinkingBudget() != null) {
                value = Math.min(value, cap.maxThinkingBudget());
            }
            return value;
        }
        return resolveBudget(request, defaultBudget);
    }

    /**
     * Serialize a capability into a stable, frontend-friendly descriptor map.
     */
    Map<String, Object> toClientDescriptor(ModelCapability cap) {
        Map<String, Object> descriptor = new LinkedHashMap<>();
        descriptor.put("id", cap.id());
        descriptor.put("displayName", cap.displayName());
        descriptor.put("vendor", cap.vendor());
        descriptor.put("knobType", cap.knob().name());
        descriptor.put("validEfforts", cap.validEfforts());

        Map<String, Object> budgetRange = new LinkedHashMap<>();
        budgetRange.put("min", cap.minThinkingBudget());
        budgetRange.put("max", cap.maxThinkingBudget());
        descriptor.put("budgetRange", budgetRange);

        descriptor.put("defaultMaxTokens", cap.defaultMaxTokens());
        descriptor.put("contextLength", cap.contextLength());
        descriptor.put("supportsJsonSchema", cap.supportsJsonSchema());
        descriptor.put("supportsStreaming", cap.supportsStreaming());
        return descriptor;
    }

    private static String extractVendor(String lowerId) {
        int slash = lowerId.indexOf('/');
        return slash > 0 ? lowerId.substring(0, slash) : "";
    }

    private static String prettyName(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return "Unknown Model";
        }
        int slash = rawId.indexOf('/');
        return slash >= 0 && slash + 1 < rawId.length() ? rawId.substring(slash + 1) : rawId;
    }
}
