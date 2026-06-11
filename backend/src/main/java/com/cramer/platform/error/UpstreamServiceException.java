package com.cramer.platform.error;

/**
 * Thrown when a required upstream dependency (e.g. the Supabase Auth Admin API) fails or is
 * unavailable. Mapped to HTTP 503 (SPEC-04 §2.2). Used so callers never fabricate a misleading
 * success/empty result on upstream failure (e.g. email-existence check, SPEC-10 §2.1).
 */
public class UpstreamServiceException extends RuntimeException {

    public UpstreamServiceException(String message) {
        super(message);
    }

    public UpstreamServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
