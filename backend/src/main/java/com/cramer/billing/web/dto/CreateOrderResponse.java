package com.cramer.billing.web.dto;

/**
 * Response after creating a payment order (SPEC-15 §8). When PayOS is unconfigured the
 * {@code checkoutUrl} is a mock URL and {@code mock} is true.
 */
public record CreateOrderResponse(
        long orderCode,
        String checkoutUrl,
        int amountVnd,
        String status,
        boolean mock) {
}
