package com.cramer.abts.domain;

/**
 * SSE event types (SPEC-21 §5). The {@code type} field inside the payload is authoritative;
 * the client drives its UI from {@code data.type}.
 */
public enum StreamEventType {
    STARTED,
    PROMPT_BUILT,
    AI_THINKING,
    AI_CHUNK,
    PROGRESS,
    RETRY,
    COMPLETED,
    FAILED,
    ABORTED,
    REFINEMENT_COMPLETED
}
