package com.cramer.billing.domain;

/**
 * Signed transaction type stored in {@code credit_transactions.type} (DB CHECK:
 * EARN, SPEND, PURCHASE, REFUND, BONUS, ADMIN). SPEC-15 §3.
 */
public enum TransactionType {
    EARN,
    SPEND,
    PURCHASE,
    REFUND,
    BONUS,
    ADMIN
}
