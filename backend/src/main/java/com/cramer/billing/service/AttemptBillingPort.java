package com.cramer.billing.service;

import com.cramer.platform.common.ielts.Skill;

import java.util.UUID;

/**
 * Published billing contract consumed by {@code assessment} (SPEC-04 §4, SPEC-12 §3, SPEC-15).
 * Charges a test-attempt start against the user's monthly quota, falling back to Lúa overage.
 *
 * <p>Reading/Listening attempts are charged at start; Writing is charged at grading time
 * (SPEC-13) and therefore does not call this port. Implementation lives in {@code billing}.
 */
public interface AttemptBillingPort {

    /**
     * Charge one attempt start for the given skill.
     *
     * @param referenceId a stable per-attempt reference (e.g. {@code "attempt_<id>"}) used for
     *                    idempotent overage charging (SPEC-15 §3).
     * @throws com.cramer.platform.error.QuotaExceededException (→ 402) if quota is exhausted and
     *         overage cannot be charged.
     */
    void chargeAttemptStart(UUID userId, Skill skill, String referenceId);
}
