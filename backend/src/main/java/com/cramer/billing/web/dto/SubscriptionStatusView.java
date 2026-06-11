package com.cramer.billing.web.dto;

import java.time.OffsetDateTime;

/**
 * The caller's current subscription status (SPEC-15 §9 {@code /current}, {@code /my-status}).
 */
public record SubscriptionStatusView(
        String tierCode,
        String tierName,
        boolean premium,
        String status,
        OffsetDateTime expiresAt,
        boolean autoRenew,
        int attemptsUsed,
        int attemptAisUsed,
        int chatbotUsed,
        boolean aiGradingEnabled,
        int gradingsRemaining,
        int chatMonthlyLimit) {
}
