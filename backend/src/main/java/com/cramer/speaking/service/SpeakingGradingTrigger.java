package com.cramer.speaking.service;

/**
 * Seam for enqueuing asynchronous grading after a session completes (SPEC-14 §6). The concrete
 * grading worker/dispatcher (OpenRouter + audio prep + retry) implements this; the session
 * service depends only on this interface, so completion logic is testable without the worker.
 */
public interface SpeakingGradingTrigger {

    /** Enqueue grading for a completed session (claims {@code completed → grading}). */
    void enqueue(long sessionId);
}
