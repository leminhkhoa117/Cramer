package com.cramer.abts.generation;

import com.cramer.abts.domain.StreamEvent;

/**
 * Sink for {@link StreamEvent}s produced during generation (SPEC-21 §5). The streaming path wires
 * this to the SSE emitter; the synchronous path uses a no-op.
 */
@FunctionalInterface
public interface StreamEmitter {

    void emit(StreamEvent event);

    StreamEmitter NOOP = event -> { };
}
