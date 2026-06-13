package com.cramer.writing.service;

import com.cramer.assessment.service.AttemptCleanupParticipant;
import com.cramer.writing.repository.WritingSubmissionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the assessment {@link AttemptCleanupParticipant} SPI (SPEC-12 §3) so that
 * cancelling or deleting an attempt also removes its {@code writing_submissions}, preventing
 * foreign-key orphans. Writing owns this cleanup of its own table.
 */
@Component
public class WritingAttemptCleanup implements AttemptCleanupParticipant {

    private final WritingSubmissionRepository submissions;

    public WritingAttemptCleanup(WritingSubmissionRepository submissions) {
        this.submissions = submissions;
    }

    @Override
    @Transactional
    public void beforeAttemptDeletion(long attemptId) {
        submissions.deleteByAttemptId(attemptId);
    }
}
