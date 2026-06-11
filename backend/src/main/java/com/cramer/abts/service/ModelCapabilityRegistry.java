package com.cramer.abts.service;

import com.cramer.abts.web.dto.ModelConfig;
import com.cramer.platform.common.json.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Maps a model slug to the correct OpenRouter reasoning payload (SPEC-24 §1, SPEC-21 §7).
 * Requests that enable reasoning on a non-reasoning model degrade gracefully (returns
 * {@code null} — no reasoning payload — and content is still generated).
 */
@Component
public class ModelCapabilityRegistry {

    /** Slug substrings that indicate reasoning capability. */
    private static final List<String> REASONING_SLUGS = List.of(
            "o1", "o3", "o4", "deepseek-r", "deepseek-reasoner", "deepseek-v4", "reasoning", "reasoner",
            "thinking", "qwq", "gpt-5", "claude-opus-4", "claude-sonnet-4", "claude-3.7", "grok-4", "gemini-2.5");

    public boolean supportsReasoning(String modelSlug) {
        if (modelSlug == null) {
            return false;
        }
        String s = modelSlug.toLowerCase(Locale.ROOT);
        return REASONING_SLUGS.stream().anyMatch(s::contains);
    }

    /**
     * Build the OpenRouter {@code reasoning} payload for a request, or {@code null} when reasoning
     * is disabled or the model does not support it (graceful degradation).
     */
    public JsonNode reasoningPayload(String modelSlug, ModelConfig config) {
        if (config == null || !config.reasoningEnabled() || !supportsReasoning(modelSlug)) {
            return null;
        }
        ObjectNode reasoning = Json.mapper().createObjectNode();
        if (config.reasoningBudget() != null && config.reasoningBudget() > 0) {
            reasoning.put("max_tokens", config.reasoningBudget());
        } else {
            String effort = (config.reasoningEffort() == null || config.reasoningEffort().isBlank())
                    ? "medium" : config.reasoningEffort().trim().toLowerCase(Locale.ROOT);
            if (!List.of("high", "medium", "low").contains(effort)) {
                effort = "medium";
            }
            reasoning.put("effort", effort);
        }
        return reasoning;
    }

    /**
     * Enrich a raw OpenRouter model node with capability descriptors (SPEC-24 §1) for the catalog.
     */
    public ObjectNode describe(JsonNode modelNode) {
        ObjectNode out = Json.mapper().createObjectNode();
        String id = modelNode.path("id").asText("");
        out.put("id", id);
        out.put("name", modelNode.path("name").asText(id));
        out.put("contextLength", modelNode.path("context_length").asInt(0));
        out.put("supportsReasoning", supportsReasoning(id));
        JsonNode modality = modelNode.path("architecture").path("modality");
        out.put("modality", modality.isMissingNode() ? "text" : modality.asText("text"));
        JsonNode pricing = modelNode.path("pricing");
        if (!pricing.isMissingNode()) {
            out.set("pricing", pricing);
        }
        return out;
    }
}
