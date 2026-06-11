package com.cramer.billing.web.dto;

/**
 * Pre-check whether the caller may start an attempt (SPEC-15 §9 {@code /can-attempt}). When the
 * monthly cap is reached, {@code requiresLua} is true and {@code allowed} reflects whether the
 * caller can cover the {@code luaCost} overage.
 */
public record CanAttemptView(
        boolean allowed,
        boolean premium,
        boolean requiresLua,
        int luaCost,
        int balance,
        String reason) {
}
