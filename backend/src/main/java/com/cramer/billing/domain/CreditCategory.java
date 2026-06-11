package com.cramer.billing.domain;

/**
 * Business category stored in {@code credit_transactions.category} (free varchar). SPEC-15 §3.
 * <strong>Fix:</strong> {@code ADMIN_ADJUSTMENT} is included — the old SQL used it but the enum
 * lacked it, breaking JPA reads.
 */
public enum CreditCategory {
    INITIAL_BONUS(TransactionType.BONUS),
    TIER_BONUS(TransactionType.BONUS),
    PURCHASE(TransactionType.PURCHASE),
    AI_GRADING(TransactionType.SPEND),
    ATTEMPT_OVERAGE(TransactionType.SPEND),
    VOCABULARY_TRANSLATION(TransactionType.SPEND),
    CHAT_EXTENSION(TransactionType.SPEND),
    SPEAKING_SESSION(TransactionType.SPEND),
    SPEAKING_REFUND(TransactionType.REFUND),
    ADMIN_ADJUSTMENT(TransactionType.ADMIN);

    private final TransactionType defaultType;

    CreditCategory(TransactionType defaultType) {
        this.defaultType = defaultType;
    }

    /** The natural transaction type for this category (callers may override for refunds). */
    public TransactionType defaultType() {
        return defaultType;
    }
}
