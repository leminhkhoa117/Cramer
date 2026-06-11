package com.cramer.platform.web;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error response body (SPEC-04 §2). Nullable fields are omitted by Jackson when null.
 *
 * @param timestamp     server time
 * @param status        HTTP status code
 * @param error         short status reason ("Not Found", "Validation Failed", ...)
 * @param message       human-readable detail (never a stack trace)
 * @param path          request path
 * @param fieldErrors   per-field validation messages (validation failures only)
 * @param blockType     quota block discriminator (402 only)
 * @param exceptionType exception class name (500 only)
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors,
        String blockType,
        String exceptionType) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, null, null, null);
    }

    public static ApiError validation(String message, String path, Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), 400, "Validation Failed", message, path, fieldErrors, null, null);
    }
}
