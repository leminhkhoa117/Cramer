package com.cramer.platform.integration.openrouter;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * An OpenRouter chat-completion request (SPEC-22 §1, SPEC-24 §2). A {@code jsonSchema} (when
 * present) is sent as a {@code response_format: json_schema, strict: true} constraint; a
 * {@code reasoning} payload (built by the model-capability registry, SPEC-24 §1) is attached
 * verbatim when the model supports it.
 *
 * @param model        model slug (required)
 * @param systemPrompt system role content
 * @param userPrompt   user role content
 * @param schemaName   schema name (for {@code json_schema.name}); ignored when schema is null
 * @param jsonSchema   JSON-schema node; null → free-form / json_object
 * @param temperature  sampling temperature
 * @param maxTokens    max output tokens (≤0 → omitted)
 * @param reasoning    OpenRouter {@code reasoning} payload; null → none
 * @param webSearch    enable web-search plugin (SPEC-24 §2)
 * @param contextCache hint to enable provider context caching
 */
public record OpenRouterChatRequest(
        String model,
        String systemPrompt,
        String userPrompt,
        String schemaName,
        JsonNode jsonSchema,
        double temperature,
        int maxTokens,
        JsonNode reasoning,
        boolean webSearch,
        boolean contextCache) {

    public static OpenRouterChatRequest jsonSchema(String model, String systemPrompt, String userPrompt,
                                                   String schemaName, JsonNode jsonSchema,
                                                   double temperature, int maxTokens, JsonNode reasoning) {
        return new OpenRouterChatRequest(model, systemPrompt, userPrompt, schemaName, jsonSchema,
                temperature, maxTokens, reasoning, false, false);
    }
}
