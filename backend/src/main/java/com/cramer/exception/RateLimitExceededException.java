package com.cramer.exception;

/**
 * Exception thrown when a rate limit has been exceeded.
 * This results in an HTTP 429 Too Many Requests response.
 */
public class RateLimitExceededException extends RuntimeException {
    
    public RateLimitExceededException(String message) {
        super(message);
    }
    
    public RateLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
