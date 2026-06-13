package com.cramer.platform.error;

/**
 * Thrown when an inbound payload exceeds the allowed size. Mapped to HTTP 413 (SPEC-04 §2.2).
 * Previously unmapped and surfaced as a generic 500 — this is the corrected behavior.
 */
public class PayloadTooLargeException extends RuntimeException {

    public PayloadTooLargeException(String message) {
        super(message);
    }
}
