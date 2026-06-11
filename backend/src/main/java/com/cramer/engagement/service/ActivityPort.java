package com.cramer.engagement.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

/**
 * Published cross-module contract (SPEC-04 §4, SPEC-16 §6) for appending to a user's activity
 * timeline ({@code user_activities}). Consumed by assessment, billing, identity. Records/primitives
 * only across the boundary.
 */
public interface ActivityPort {

    /** Log an activity. {@code activityType} e.g. TEST_COMPLETED, VOCAB_SAVED, SUBSCRIPTION_CHANGED. */
    void log(UUID userId, String activityType, String title, String description, JsonNode metadata);
}
