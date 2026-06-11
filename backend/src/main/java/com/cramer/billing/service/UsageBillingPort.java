package com.cramer.billing.service;

import java.util.UUID;

/**
 * Published billing contract for AI usage that is charged <strong>after success</strong>
 * (SPEC-04 §4, SPEC-15 §6). Consumed by {@code writing} for essay grading.
 *
 * <p>The canonical AI-grading overage is <strong>20 Lúa</strong> (single source — no "10 Lúa"
 * variants). Charges are idempotent by {@code reference}; {@link #refund} reverses an actual
 * charge only (a grading covered by the monthly allowance has nothing to refund).
 */
public interface UsageBillingPort {

    /** Pre-flight check (no charge): may this user run an AI grading now? */
    boolean canGrade(UUID userId);

    /** Charge one AI grading after it succeeds (idempotent by {@code reference}). */
    void chargeAiGrading(UUID userId, String reference);

    /** Reverse a previous {@link #chargeAiGrading} (idempotent; no-op if nothing was charged). */
    void refund(UUID userId, String reference);
}
