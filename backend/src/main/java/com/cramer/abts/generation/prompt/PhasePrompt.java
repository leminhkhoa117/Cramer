package com.cramer.abts.generation.prompt;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A composed prompt for one generation phase (SPEC-21 §3, SPEC-22 §4): system + user text and
 * the JSON schema the response must satisfy. Generators wrap this into an OpenRouter request with
 * the model/temperature/reasoning config.
 *
 * @param systemPrompt system role content
 * @param userPrompt   user role content
 * @param schemaName   schema name for {@code json_schema.name}
 * @param schema       JSON schema node
 */
public record PhasePrompt(String systemPrompt, String userPrompt, String schemaName, JsonNode schema) {
}
