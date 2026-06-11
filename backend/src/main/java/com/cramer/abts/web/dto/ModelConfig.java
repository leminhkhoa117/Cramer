package com.cramer.abts.web.dto;

/**
 * Model + reasoning config (SPEC-21 §1, §7). All fields optional; resolvers apply defaults.
 *
 * @param model           model slug (null → service default)
 * @param temperature     sampling temperature (null → 0.7)
 * @param maxTokens       max output tokens (null/≤0 → omitted)
 * @param enableReasoning request reasoning (ignored by non-reasoning models)
 * @param reasoningEffort {@code high}|{@code medium}|{@code low}
 * @param reasoningBudget reasoning token budget (effort alternative)
 * @param contextCache    enable provider context caching
 */
public record ModelConfig(
        String model,
        Double temperature,
        Integer maxTokens,
        Boolean enableReasoning,
        String reasoningEffort,
        Integer reasoningBudget,
        Boolean contextCache) {

    public double resolvedTemperature() {
        return temperature == null ? 0.7 : temperature;
    }

    public int resolvedMaxTokens() {
        return maxTokens == null ? 0 : maxTokens;
    }

    public boolean reasoningEnabled() {
        return Boolean.TRUE.equals(enableReasoning);
    }

    public boolean cacheEnabled() {
        return Boolean.TRUE.equals(contextCache);
    }
}
