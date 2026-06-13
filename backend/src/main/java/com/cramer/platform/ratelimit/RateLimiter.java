package com.cramer.platform.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory per-user, per-endpoint rate limiter (SPEC-18 §5) backed by Bucket4j. Keyed by
 * {@code userId:endpointType}. Limits: {@code grading} 5/min, {@code profile}/{@code auth}
 * 10/min, default 60/min.
 *
 * <p><strong>Limitation:</strong> in-memory and therefore non-distributed — each instance has
 * its own buckets. Documented; acceptable for the current single-instance deployment.
 */
@Component
public class RateLimiter {

    /** Endpoint type for AI grading submit/regrade (SPEC-13 §3). */
    public static final String GRADING = "grading";
    public static final String PROFILE = "profile";
    public static final String AUTH = "auth";

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /** @return true if a token was consumed; false when the limit is exceeded. */
    public boolean tryConsume(UUID userId, String endpointType) {
        String key = (userId == null ? "anon" : userId.toString()) + ":" + endpointType;
        return buckets.computeIfAbsent(key, k -> bucketFor(endpointType)).tryConsume(1);
    }

    private Bucket bucketFor(String endpointType) {
        int perMinute = switch (endpointType) {
            case GRADING -> 5;
            case PROFILE, AUTH -> 10;
            default -> 60;
        };
        Bandwidth limit = Bandwidth.builder()
                .capacity(perMinute)
                .refillGreedy(perMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
