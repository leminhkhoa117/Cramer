package com.cramer.abts.service;

import com.cramer.platform.common.json.Json;
import com.cramer.platform.integration.openrouter.OpenRouterClient;
import com.cramer.platform.integration.openrouter.OpenRouterException;
import com.cramer.platform.integration.openrouter.OpenRouterProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * OpenRouter model catalog (SPEC-24 §1, C7). Fetches {@code /models}, caches for 5 minutes,
 * enriches each entry with capability descriptors, and falls back to a curated list when the live
 * catalog is unavailable. The configured default model is validated at startup; an invalid default
 * logs a warning and relies on the resolver's fallback (no fatal startup failure).
 */
@Service
public class ModelCatalogService {

    private static final Logger log = LoggerFactory.getLogger(ModelCatalogService.class);
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;
    private static final List<String> FALLBACK_MODELS = List.of(
            "deepseek/deepseek-v4-flash", "deepseek/deepseek-chat", "deepseek/deepseek-r1",
            "openai/gpt-5", "anthropic/claude-sonnet-4", "google/gemini-2.5-flash",
            "meta-llama/llama-3.1-70b-instruct");

    private final OpenRouterClient client;
    private final ModelCapabilityRegistry capabilities;
    private final OpenRouterProperties props;

    private volatile ArrayNode cache;
    private volatile long cachedAt;

    public ModelCatalogService(OpenRouterClient client, ModelCapabilityRegistry capabilities,
                               OpenRouterProperties props) {
        this.client = client;
        this.capabilities = capabilities;
        this.props = props;
    }

    /** Enriched model list; cached 5 minutes, curated fallback on upstream failure. */
    public ArrayNode listModels() {
        long now = System.currentTimeMillis();
        ArrayNode current = cache;
        if (current != null && (now - cachedAt) < CACHE_TTL_MS) {
            return current;
        }
        try {
            JsonNode raw = client.listModels();
            ArrayNode enriched = Json.mapper().createArrayNode();
            if (raw.isArray()) {
                for (JsonNode model : raw) {
                    enriched.add(capabilities.describe(model));
                }
            }
            if (enriched.isEmpty()) {
                enriched = curatedFallback();
            }
            cache = enriched;
            cachedAt = now;
            return enriched;
        } catch (OpenRouterException e) {
            log.warn("OpenRouter /models unavailable ({}); using curated fallback", e.error());
            ArrayNode fallback = curatedFallback();
            cache = fallback;
            cachedAt = now;
            return fallback;
        }
    }

    /** Capability descriptor for one model id (from the catalog, else synthesized). */
    public ObjectNode capability(String modelId) {
        for (JsonNode model : listModels()) {
            if (model.path("id").asText("").equalsIgnoreCase(modelId)) {
                return (ObjectNode) model;
            }
        }
        return capabilities.describe(Json.mapper().createObjectNode().put("id", modelId));
    }

    private ArrayNode curatedFallback() {
        ArrayNode arr = Json.mapper().createArrayNode();
        for (String id : FALLBACK_MODELS) {
            arr.add(capabilities.describe(Json.mapper().createObjectNode().put("id", id).put("name", id)));
        }
        return arr;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateDefaultModel() {
        if (!client.isConfigured()) {
            log.info("ABTS: OpenRouter key not configured; model catalog validation skipped");
            return;
        }
        String defaultModel = props.resolvedDefaultModel();
        try {
            boolean known = false;
            for (JsonNode model : listModels()) {
                if (model.path("id").asText("").equalsIgnoreCase(defaultModel)) {
                    known = true;
                    break;
                }
            }
            if (!known) {
                log.warn("ABTS default generation model '{}' not found in live catalog; "
                        + "the resolver fallback chain will apply at request time", defaultModel);
            }
        } catch (RuntimeException e) {
            log.warn("ABTS default-model validation skipped: {}", e.getMessage());
        }
    }
}
