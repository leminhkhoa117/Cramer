package com.cramer.platform.error;

/**
 * Thrown when a caller exceeds a configured rate limit. Mapped to HTTP 429 (SPEC-04 §2.2,
 * SPEC-18 §5).
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
