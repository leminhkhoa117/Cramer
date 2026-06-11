package com.cramer.platform.integration.openrouter;

/**
 * Callback for OpenRouter SSE streaming deltas (SPEC-21 §5). Implementations forward deltas to
 * the ABTS stream as {@code AI_CHUNK} / {@code AI_THINKING} events. Default methods make each
 * callback optional.
 */
public interface OpenRouterStreamListener {

    /** A content token/delta arrived. */
    default void onContentDelta(String delta) {
    }

    /** A reasoning token/delta arrived (reasoning-capable models only). */
    default void onReasoningDelta(String delta) {
    }
}
