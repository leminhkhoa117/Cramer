package com.cramer.abts.web.dto;

/**
 * Service status (SPEC-25 §1 {@code GET /status}): whether the OpenRouter key is configured,
 * the default model, streaming timeouts, the refinement round cap, and an API version tag.
 */
public record StatusResponse(
        boolean keyConfigured,
        String defaultModel,
        int emitterTimeoutMs,
        int partTimeoutMs,
        int maxRefinementRounds,
        String version) {
}
