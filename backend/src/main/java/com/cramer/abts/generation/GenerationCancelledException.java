package com.cramer.abts.generation;

/**
 * Signals cooperative cancellation of an in-flight generation (SPEC-21 §6) — e.g. the SSE client
 * disconnected or the bounded queue was saturated. The streaming service translates this into an
 * {@code ABORTED} event.
 */
public class GenerationCancelledException extends RuntimeException {

    public GenerationCancelledException(String message) {
        super(message);
    }
}
