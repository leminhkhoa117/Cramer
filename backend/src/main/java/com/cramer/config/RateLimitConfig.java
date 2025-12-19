package com.cramer.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory rate limiting configuration.
 * For production, consider using Redis-backed rate limiting.
 */
@Component
public class RateLimitConfig {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitConfig.class);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Get or create a rate limit bucket for a specific user and endpoint type.
     * 
     * @param userId       The user's unique identifier
     * @param endpointType The type of endpoint (e.g., "grading", "profile",
     *                     "general")
     * @return A Bucket for rate limiting
     */
    public Bucket resolveBucket(String userId, String endpointType) {
        String key = userId + ":" + endpointType;
        return buckets.computeIfAbsent(key, k -> createBucket(endpointType));
    }

    private Bucket createBucket(String endpointType) {
        Bandwidth limit = switch (endpointType) {
            case "grading" ->
                // AI grading: 5 requests per minute (Gemini has rate limits)
                Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofMinutes(1)).build();
            case "profile" ->
                // Profile updates: 10 requests per minute
                Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofMinutes(1)).build();
            case "auth" ->
                // Auth operations: 10 requests per minute
                Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofMinutes(1)).build();
            default ->
                // General API: 60 requests per minute
                Bandwidth.builder().capacity(60).refillGreedy(60, Duration.ofMinutes(1)).build();
        };
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Check if a request should be allowed and consume a token if so.
     * 
     * @param userId       The user's unique identifier
     * @param endpointType The type of endpoint
     * @return true if the request is allowed, false if rate limited
     */
    public boolean tryConsume(String userId, String endpointType) {
        Bucket bucket = resolveBucket(userId, endpointType);
        boolean consumed = bucket.tryConsume(1);

        if (!consumed) {
            logger.warn("⚠️ Rate limit exceeded for user {} on endpoint type: {}", userId, endpointType);
        }

        return consumed;
    }

    /**
     * Get remaining tokens for a user and endpoint type.
     * Useful for including in response headers.
     * 
     * @param userId       The user's unique identifier
     * @param endpointType The type of endpoint
     * @return Number of remaining tokens
     */
    public long getRemainingTokens(String userId, String endpointType) {
        Bucket bucket = resolveBucket(userId, endpointType);
        return bucket.getAvailableTokens();
    }
}
