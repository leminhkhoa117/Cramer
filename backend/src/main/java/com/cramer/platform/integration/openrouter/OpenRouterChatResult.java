package com.cramer.platform.integration.openrouter;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Result of an OpenRouter chat completion (streaming or not). Content is the parsed
 * assistant message (schema-constrained JSON); reasoning is the model's thinking text when
 * available (SPEC-21 §7); usage carries token/cost accounting (SPEC-21 §8).
 *
 * @param content          parsed JSON content (may be a text node if not JSON)
 * @param rawContent       the raw assistant message string
 * @param reasoning        reasoning text (null when the model did not reason)
 * @param promptTokens     prompt token count (null if unknown)
 * @param completionTokens completion token count (null if unknown)
 * @param totalTokens      total token count (null if unknown)
 * @param cost             upstream cost in USD (null if unknown)
 * @param model            the model slug that served the request
 */
public record OpenRouterChatResult(
        JsonNode content,
        String rawContent,
        String reasoning,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        Double cost,
        String model) {
}
