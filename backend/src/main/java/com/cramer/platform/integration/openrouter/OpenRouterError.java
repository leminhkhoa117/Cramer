package com.cramer.platform.integration.openrouter;

/**
 * Normalized OpenRouter error codes (SPEC-24 §2). Each carries whether the failure is
 * {@code retryable} so the generation retry logic (SPEC-21 §2) can decide whether to retry.
 */
public enum OpenRouterError {

    AUTH_FAILED(false),
    INSUFFICIENT_CREDITS(false),
    RATE_LIMITED(true),
    MODEL_UNAVAILABLE(true),
    NO_PROVIDERS(true),
    UPSTREAM_ERROR(true),
    TIMEOUT(true);

    private final boolean retryable;

    OpenRouterError(boolean retryable) {
        this.retryable = retryable;
    }

    public boolean retryable() {
        return retryable;
    }

    /** Map an HTTP status to an error code (SPEC-24 §2). */
    public static OpenRouterError fromHttpStatus(int status) {
        return switch (status) {
            case 401, 403 -> AUTH_FAILED;
            case 402 -> INSUFFICIENT_CREDITS;
            case 429 -> RATE_LIMITED;
            case 404 -> MODEL_UNAVAILABLE;
            case 503 -> NO_PROVIDERS;
            default -> UPSTREAM_ERROR;
        };
    }
}
