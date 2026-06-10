package com.cramer.service.abts;

/**
 * FIX 9: raised when a single skill part exceeds its configured per-part deadline
 * ({@code openrouter.per-part-timeout-ms}). Distinct from generic failures so the
 * multi-part runner can record the part as a timeout (message tagged {@code [TIMEOUT]})
 * rather than an opaque error, and so callers can surface a precise reason to the client.
 */
public class PartTimeoutException extends RuntimeException {

    public PartTimeoutException(String message) {
        super(message);
    }
}
