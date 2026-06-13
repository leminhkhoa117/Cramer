package com.cramer.billing.web.dto;

/**
 * Tier-aware quota status (SPEC-15 §5). <strong>Fix:</strong> premium users see premium/unlimited
 * status instead of the free caps the old endpoint always returned. {@code limit < 0} = unlimited.
 */
public record QuotaStatusView(
        boolean premium,
        String tierCode,
        int globalLimit,
        int globalUsed,
        int globalAiLimit,
        int globalAiUsed,
        boolean unlimited) {
}
