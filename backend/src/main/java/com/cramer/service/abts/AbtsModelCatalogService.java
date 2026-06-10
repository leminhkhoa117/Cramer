package com.cramer.service.abts;

import com.cramer.config.OpenRouterConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

final class AbtsModelCatalogService {

    private static final Logger logger = LoggerFactory.getLogger(AbtsModelCatalogService.class);
    private static final long MODEL_CACHE_DURATION_MS = 5 * 60 * 1000;

    private final OpenRouterConfig config;
    private final OpenRouterClient openRouterClient;
    private final ModelCapabilityRegistry capabilityRegistry;
    private final AtomicLong cachedModelsTimestamp = new AtomicLong(0);

    private volatile List<Map<String, Object>> cachedModels = null;
    // FIX 15: tracks whether the most recent successful catalog load came from the live
    // OpenRouter API (true) or fell back to the hardcoded list (false). Startup
    // validation must NOT fail fast when the live catalog was never available.
    private volatile boolean lastCatalogWasLive = false;

    AbtsModelCatalogService(OpenRouterConfig config, OpenRouterClient openRouterClient,
            ModelCapabilityRegistry capabilityRegistry) {
        this.config = config;
        this.openRouterClient = openRouterClient;
        this.capabilityRegistry = capabilityRegistry;
    }

    List<Map<String, Object>> getAvailableModels() {
        long now = System.currentTimeMillis();

        if (cachedModels != null && (now - cachedModelsTimestamp.get()) < MODEL_CACHE_DURATION_MS) {
            logger.debug("Returning cached models ({} models)", cachedModels.size());
            return cachedModels;
        }

        logger.info("Fetching models from OpenRouter API...");
        List<Map<String, Object>> models = openRouterClient.fetchAvailableModels();

        if (models != null && !models.isEmpty()) {
            cachedModels = models;
            cachedModelsTimestamp.set(now);
            lastCatalogWasLive = true;
            logger.info("Cached {} models from OpenRouter", models.size());
            return models;
        }

        logger.warn("OpenRouter API failed, using fallback models");
        lastCatalogWasLive = false;
        return getFallbackModels();
    }

    /**
     * Same as {@link #getAvailableModels()} but augments each model with a
     * {@code "capabilities"} descriptor inferred from its slug.
     */
    List<Map<String, Object>> getAvailableModelsWithCapabilities() {
        List<Map<String, Object>> models = getAvailableModels();
        List<Map<String, Object>> enriched = new ArrayList<>(models.size());
        for (Map<String, Object> model : models) {
            Map<String, Object> copy = new LinkedHashMap<>(model);
            Object id = model.get("id");
            String modelId = id != null ? id.toString() : "";
            copy.put("capabilities",
                    capabilityRegistry.toClientDescriptor(capabilityRegistry.inferFromModelId(modelId)));
            enriched.add(copy);
        }
        return enriched;
    }

    /**
     * Resolve the reasoning capability descriptor for a single model id.
     *
     * <p>Best-effort: attempts a fuzzy {@link #resolveSlug(String)} against the live
     * catalog first; if that fails (no API key / unknown slug) it still returns a
     * descriptor inferred directly from the raw id.</p>
     */
    Map<String, Object> getModelCapabilities(String modelId) {
        String resolved;
        try {
            resolved = resolveSlug(modelId);
        } catch (RuntimeException ex) {
            // FIX 8: do not silently swallow resolution failures — surface a WARN so an
            // operator can see that the slug fell back to a slug-inferred descriptor.
            logger.warn("Model slug '{}' could not be resolved against catalog; using inferred descriptor",
                    modelId);
            resolved = modelId;
        }
        return capabilityRegistry.toClientDescriptor(capabilityRegistry.inferFromModelId(resolved));
    }

    /**
     * Fuzzy-resolve a user-supplied model reference to a concrete catalog slug.
     *
     * <p>Resolution order: exact match, case-insensitive exact, contains-all-tokens,
     * then substring. Throws {@link IllegalArgumentException} (listing up to 5 candidate
     * ids) when nothing matches.</p>
     */
    String resolveSlug(String reference) {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("Model reference must not be blank");
        }
        String ref = reference.trim();
        List<Map<String, Object>> models = getAvailableModels();
        List<String> ids = new ArrayList<>();
        for (Map<String, Object> model : models) {
            Object id = model.get("id");
            if (id != null) {
                ids.add(id.toString());
            }
        }

        // 1. Exact match
        for (String id : ids) {
            if (id.equals(ref)) {
                return id;
            }
        }
        // 2. Case-insensitive exact
        for (String id : ids) {
            if (id.equalsIgnoreCase(ref)) {
                return id;
            }
        }
        // 3. Contains all tokens
        String[] tokens = ref.toLowerCase(Locale.ROOT).split("[\\s/:_-]+");
        for (String id : ids) {
            String lower = id.toLowerCase(Locale.ROOT);
            boolean all = true;
            for (String token : tokens) {
                if (!token.isBlank() && !lower.contains(token)) {
                    all = false;
                    break;
                }
            }
            if (all) {
                return id;
            }
        }
        // 4. Substring
        String lowerRef = ref.toLowerCase(Locale.ROOT);
        for (String id : ids) {
            if (id.toLowerCase(Locale.ROOT).contains(lowerRef)) {
                return id;
            }
        }

        List<String> candidates = ids.size() > 5 ? ids.subList(0, 5) : ids;
        throw new IllegalArgumentException(
                "No model matched '" + ref + "'. Sample available ids: " + candidates);
    }

    /**
     * Validate the configured default models against the live OpenRouter catalog at
     * startup. Recovers the generation model via a curated fallback chain when the
     * configured slug is unresolvable; throws {@link IllegalStateException} only if no
     * fallback resolves. The regeneration model is best-effort (never fatal).
     *
     * <p>No-ops when no API key is configured (test/dev startup safety).</p>
     */
    void validateConfiguredDefaults() {
        if (!config.hasApiKey()) {
            logger.warn("OpenRouter API key not configured - skipping model default validation");
            return;
        }

        String generationModel = config.getGenerationModel();
        try {
            String resolved = resolveSlug(generationModel);
            logger.info("Validated default generation model: {} -> {}", generationModel, resolved);
        } catch (RuntimeException ex) {
            logger.warn("Configured generation model '{}' not found in catalog: {}",
                    generationModel, ex.getMessage());
            // FIX 15: if the live catalog was never available (fetch failed -> fallback list),
            // we cannot trust a "not found" result. Degrade to a WARN and keep the configured
            // slug instead of fatally aborting startup on a transient network/API outage.
            if (!lastCatalogWasLive) {
                logger.warn("Live OpenRouter catalog unavailable; skipping fatal validation and "
                        + "keeping configured generation model '{}'", generationModel);
            } else {
                String recovered = recoverViaFallback(List.of(
                        "deepseek/deepseek-v4-flash",
                        "deepseek/deepseek-v3.2-exp",
                        "deepseek/deepseek-chat-v3.1",
                        "deepseek/deepseek-chat"));
                if (recovered == null) {
                    throw new IllegalStateException(
                            "Configured generation model '" + generationModel
                                    + "' is invalid and no fallback model could be resolved against OpenRouter.");
                }
                logger.warn("Recovered generation model -> {}", recovered);
                config.setGenerationModel(recovered);
            }
        }
        // FIX 16: always record the effective generation model after validation/recovery.
        logger.info("ABTS generation model effectively '{}'", config.getGenerationModel());

        String regenerationModel = config.getRegenerationModel();
        try {
            String resolved = resolveSlug(regenerationModel);
            logger.info("Validated default regeneration model: {} -> {}", regenerationModel, resolved);
        } catch (RuntimeException ex) {
            logger.warn("Configured regeneration model '{}' not found in catalog (best-effort, ignoring): {}",
                    regenerationModel, ex.getMessage());
            String recovered = recoverViaFallback(List.of(
                    "deepseek/deepseek-chat-v3.1",
                    "deepseek/deepseek-chat"));
            if (recovered != null) {
                logger.warn("Recovered regeneration model -> {}", recovered);
                config.setRegenerationModel(recovered);
            }
        }
    }

    private String recoverViaFallback(List<String> chain) {
        for (String candidate : chain) {
            try {
                return resolveSlug(candidate);
            } catch (RuntimeException ignored) {
                // try next candidate
            }
        }
        return null;
    }

    Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("apiKeyConfigured", config.hasApiKey());
        status.put("baseUrl", config.getBaseUrl());
        status.put("defaultGenerationModel", config.getGenerationModel());
        status.put("defaultRegenerationModel", config.getRegenerationModel());
        status.put("streamingEnabled", config.isStreamingEnabled());
        status.put("timeoutMs", config.getTimeoutMs());
        // FIX 11: surface the server-side refinement-loop cap so the frontend
        // stops hardcoding "5" for the round limit / disabled "Refine again".
        status.put("maxRefinementRounds", config.getMaxRefinementRounds());
        status.put("version", "2.0.0-beta");
        status.put("phase", "Phase 4 - Reading/Listening/Writing Generation");

        return status;
    }

    private List<Map<String, Object>> getFallbackModels() {
        return List.of(
                Map.of(
                        "id", "deepseek/deepseek-v4-flash",
                        "name", "DeepSeek V4 Flash",
                        "description", "Fast, low-cost default for content generation",
                        "context_length", 65536,
                        "pricing", Map.of("prompt", "0.00000014", "completion", "0.00000028")),
                Map.of(
                        "id", "deepseek/deepseek-v4",
                        "name", "DeepSeek V4",
                        "description", "Higher-quality DeepSeek generation model",
                        "context_length", 65536,
                        "pricing", Map.of("prompt", "0.00000027", "completion", "0.0000011")),
                Map.of(
                        "id", "deepseek/deepseek-r1",
                        "name", "DeepSeek R1",
                        "description", "DeepSeek reasoning model (toggle reasoning)",
                        "context_length", 65536,
                        "pricing", Map.of("prompt", "0.00000055", "completion", "0.00000219")),
                Map.of(
                        "id", "z-ai/glm-4.6",
                        "name", "GLM 4.6",
                        "description", "Z.AI GLM with thinking support",
                        "context_length", 131072,
                        "pricing", Map.of("prompt", "0.0000006", "completion", "0.0000022")),
                Map.of(
                        "id", "qwen/qwen3-235b-a22b-thinking",
                        "name", "Qwen3 235B Thinking",
                        "description", "Qwen reasoning model with thinking mode",
                        "context_length", 131072,
                        "pricing", Map.of("prompt", "0.00000013", "completion", "0.0000006")),
                Map.of(
                        "id", "moonshotai/kimi-k2",
                        "name", "Kimi K2",
                        "description", "Moonshot Kimi long-context model",
                        "context_length", 131072,
                        "pricing", Map.of("prompt", "0.0000006", "completion", "0.0000025")),
                Map.of(
                        "id", "google/gemini-2.5-flash",
                        "name", "Gemini 2.5 Flash",
                        "description", "Fast Google model with thinking budget",
                        "context_length", 1048576,
                        "pricing", Map.of("prompt", "0.0000003", "completion", "0.0000025")),
                Map.of(
                        "id", "anthropic/claude-sonnet-4.5",
                        "name", "Claude Sonnet 4.5",
                        "description", "High-quality Anthropic model with extended thinking",
                        "context_length", 200000,
                        "pricing", Map.of("prompt", "0.000003", "completion", "0.000015")),
                Map.of(
                        "id", "openai/gpt-5-mini",
                        "name", "GPT-5 Mini",
                        "description", "OpenAI reasoning model with effort control",
                        "context_length", 128000,
                        "pricing", Map.of("prompt", "0.00000025", "completion", "0.000002")));
    }
}