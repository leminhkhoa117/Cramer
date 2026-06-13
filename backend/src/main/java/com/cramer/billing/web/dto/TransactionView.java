package com.cramer.billing.web.dto;

import java.time.OffsetDateTime;

/**
 * A single Lúa transaction for history endpoints (SPEC-15 §9 {@code /transactions}).
 */
public record TransactionView(
        Long id,
        int amount,
        int balanceAfter,
        String type,
        String category,
        String description,
        String referenceId,
        OffsetDateTime createdAt) {
}
