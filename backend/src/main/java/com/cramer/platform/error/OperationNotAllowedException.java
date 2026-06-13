package com.cramer.platform.error;

/**
 * Thrown when an operation is not permitted for the current actor or resource state
 * (e.g. accessing another user's resource, or an illegal lifecycle transition that is a
 * policy violation rather than a conflict). Mapped to HTTP 403 (SPEC-04 §2.2).
 */
public class OperationNotAllowedException extends RuntimeException {

    public OperationNotAllowedException(String message) {
        super(message);
    }
}
