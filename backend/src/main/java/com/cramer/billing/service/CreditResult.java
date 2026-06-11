package com.cramer.billing.service;

/**
 * Outcome of a Lúa mutation (SPEC-15 §3). {@code duplicate} is true when an idempotent repeat
 * (same reference) was detected and no new mutation was applied.
 */
public record CreditResult(int balanceAfter, boolean applied, boolean duplicate) {

    public static CreditResult applied(int balanceAfter) {
        return new CreditResult(balanceAfter, true, false);
    }

    public static CreditResult duplicate(int balanceAfter) {
        return new CreditResult(balanceAfter, false, true);
    }
}
