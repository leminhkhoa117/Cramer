package com.cramer.platform.integration.openrouter;

/**
 * Exception carrying a normalized {@link OpenRouterError} and its retryability (SPEC-24 §2).
 */
public class OpenRouterException extends RuntimeException {

    private final OpenRouterError error;

    public OpenRouterException(OpenRouterError error, String message) {
        super(message);
        this.error = error;
    }

    public OpenRouterException(OpenRouterError error, String message, Throwable cause) {
        super(message, cause);
        this.error = error;
    }

    public OpenRouterError error() {
        return error;
    }

    public boolean retryable() {
        return error.retryable();
    }
}
