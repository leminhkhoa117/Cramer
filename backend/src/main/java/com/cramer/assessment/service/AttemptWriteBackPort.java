package com.cramer.assessment.service;

import java.util.UUID;

/**
 * Published cross-module contract (SPEC-04 §4) letting {@code writing} drive the shared attempt
 * shell without reaching into the {@code test_attempts} table. {@code writing} owns essays, not
 * attempts (SPEC-13 §3).
 */
public interface AttemptWriteBackPort {

    /**
     * Mark the attempt {@code COMPLETED} and cancel any sibling in-progress attempts for the same
     * user/source/test/skill (SPEC-13 §3). Owner-checked (403 if not the owner).
     *
     * @return the attempt's exam context (source/test/skill), needed by writing to fetch prompts.
     */
    AttemptContext completeForGrading(long attemptId, UUID userId);

    /** Read the owner-checked attempt context (for status/review/regrade). */
    AttemptContext requireOwnedContext(long attemptId, UUID userId);

    /** Minimal attempt context shared across the module boundary (records/primitives only). */
    record AttemptContext(long attemptId, UUID userId, String examSource, String testNumber,
                          String skill, String status) {
    }
}
