package com.cramer.exception;

/**
 * Exception thrown when an operation is not allowed.
 * E.g., deleting a system test set, publishing an empty test, etc.
 */
public class OperationNotAllowedException extends RuntimeException {
    
    public OperationNotAllowedException(String message) {
        super(message);
    }
    
    public OperationNotAllowedException(String operation, String reason) {
        super(String.format("Operation '%s' not allowed: %s", operation, reason));
    }
}
