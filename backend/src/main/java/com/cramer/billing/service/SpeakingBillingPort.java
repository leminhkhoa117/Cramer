package com.cramer.billing.service;

import java.util.UUID;

/**
 * Published billing contract for Speaking sessions (SPEC-04 §4, SPEC-15 §6). Check on create,
 * deduct once on complete, refund idempotently on grading failure/watchdog. Default cost 15 Lúa.
 */
public interface SpeakingBillingPort {

    /** @return true if the user can afford the session cost (no deduction). */
    boolean canAfford(UUID userId, int luaCost);

    /** Deduct the session cost once (category {@code SPEAKING_SESSION}); idempotent by session. */
    void deduct(UUID userId, long sessionId, int luaCost);

    /** Refund the session cost (category {@code SPEAKING_REFUND}); idempotent by session. */
    void refund(UUID userId, long sessionId, int luaCost);
}
