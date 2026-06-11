package com.cramer.platform.error;

/**
 * Thrown when a user exceeds an allowance and the overage cannot be charged. Mapped to
 * HTTP 402 with a {@code blockType} discriminator in the error body (SPEC-04 §2.2, SPEC-15).
 */
public class QuotaExceededException extends RuntimeException {

    private final String blockType;

    public QuotaExceededException(String blockType, String message) {
        super(message);
        this.blockType = blockType;
    }

    /** Discriminator surfaced to the client (e.g. {@code ATTEMPT_QUOTA}, {@code INSUFFICIENT_LUA}). */
    public String blockType() {
        return blockType;
    }
}
