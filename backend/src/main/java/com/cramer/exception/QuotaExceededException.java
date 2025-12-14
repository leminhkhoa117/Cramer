package com.cramer.exception;

/**
 * Exception thrown when a user exceeds their quota and cannot proceed.
 * Used by QuotaBillingService when the user has insufficient Lua to pay overage.
 */
public class QuotaExceededException extends RuntimeException {

    private final String blockType;

    public QuotaExceededException(String message) {
        super(message);
        this.blockType = null;
    }

    public QuotaExceededException(String message, String blockType) {
        super(message);
        this.blockType = blockType;
    }

    public String getBlockType() {
        return blockType;
    }
}
