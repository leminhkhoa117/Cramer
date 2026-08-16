package com.cramer.platform.integration.openrouter;

/**
 * Exception carrying a normalized {@link OpenRouterError}, its retryability, and an optional
 * upstream Retry-After hint in milliseconds (SPEC-24 §2).
 */
public class OpenRouterException extends RuntimeException {

    private final OpenRouterError error;
    private final Long retryAfterMs;

    public OpenRouterException(OpenRouterError error, String message) {
        this(error, message, null, null);
    }

    public OpenRouterException(OpenRouterError error, String message, Throwable cause) {
        this(error, message, null, cause);
    }

    public OpenRouterException(OpenRouterError error, String message, Long retryAfterMs, Throwable cause) {
        super(message, cause);
        this.error = error;
        this.retryAfterMs = retryAfterMs;
    }

    public OpenRouterError error() {
        return error;
    }

    public boolean retryable() {
        return error.retryable();
    }

    /** Upstream Retry-After hint in milliseconds, or null when the upstream did not send one. */
    public Long retryAfterMs() {
        return retryAfterMs;
    }
}
