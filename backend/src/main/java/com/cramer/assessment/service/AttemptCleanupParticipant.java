package com.cramer.assessment.service;

/**
 * Inbound SPI (SPEC-12 §3): lets other modules clean up attempt-scoped data they own (e.g.
 * {@code writing} deleting {@code writing_submissions}) before an attempt is deleted/cancelled,
 * so no foreign-key orphans remain. Implementations are provided by those modules and injected;
 * absence is fine (objective-only attempts have nothing extra to clean).
 */
public interface AttemptCleanupParticipant {

    /** Called before an attempt's own rows are deleted. Must not throw for a missing/empty case. */
    void beforeAttemptDeletion(long attemptId);
}
