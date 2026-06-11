package com.cramer.billing.web.dto;

/**
 * Aggregate Lúa balance/stats (SPEC-15 §9 {@code /credits}, {@code /stats}).
 */
public record CreditStatsView(int balance, int lifetimeEarned, int lifetimeSpent) {
}
