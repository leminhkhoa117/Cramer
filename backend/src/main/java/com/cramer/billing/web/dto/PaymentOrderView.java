package com.cramer.billing.web.dto;

import java.time.OffsetDateTime;

/**
 * A payment order projection for status/history (SPEC-15 §9 {@code /status/{orderCode}},
 * {@code /history}).
 */
public record PaymentOrderView(
        Long orderCode,
        String type,
        String tierCode,
        Integer luaAmount,
        int amountVnd,
        String status,
        String checkoutUrl,
        OffsetDateTime createdAt,
        OffsetDateTime paidAt,
        OffsetDateTime expiresAt) {
}
