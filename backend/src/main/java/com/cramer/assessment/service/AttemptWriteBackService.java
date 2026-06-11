package com.cramer.assessment.service;

import com.cramer.assessment.domain.Attempt;
import com.cramer.assessment.domain.AttemptStatus;
import com.cramer.assessment.repository.AttemptRepository;
import com.cramer.platform.error.OperationNotAllowedException;
import com.cramer.platform.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Implements {@link AttemptWriteBackPort} (SPEC-13 §3). The only place outside the assessment
 * lifecycle that mutates {@code test_attempts}, kept inside assessment so writing never touches
 * the table directly.
 */
@Service
public class AttemptWriteBackService implements AttemptWriteBackPort {

    private final AttemptRepository attempts;

    public AttemptWriteBackService(AttemptRepository attempts) {
        this.attempts = attempts;
    }

    @Override
    @Transactional
    public AttemptContext completeForGrading(long attemptId, UUID userId) {
        Attempt a = requireOwned(attemptId, userId);

        // Cancel sibling in-progress attempts for the same key (prevents ghost attempts).
        for (Attempt sibling : attempts.lockByKey(userId, a.getExamSource(), a.getTestNumber(), a.getSkill())) {
            if (!sibling.getId().equals(a.getId()) && sibling.getStatus() == AttemptStatus.IN_PROGRESS) {
                sibling.setStatus(AttemptStatus.CANCELLED);
                attempts.save(sibling);
            }
        }

        a.setStatus(AttemptStatus.COMPLETED);
        a.setCompletedAt(OffsetDateTime.now());
        attempts.save(a);
        return context(a);
    }

    @Override
    @Transactional(readOnly = true)
    public AttemptContext requireOwnedContext(long attemptId, UUID userId) {
        return context(requireOwned(attemptId, userId));
    }

    private Attempt requireOwned(long attemptId, UUID userId) {
        Attempt a = attempts.findById(attemptId)
                .orElseThrow(() -> ResourceNotFoundException.of("Attempt", attemptId));
        if (!a.getUserId().equals(userId)) {
            throw new OperationNotAllowedException("Attempt does not belong to the current user");
        }
        return a;
    }

    private AttemptContext context(Attempt a) {
        return new AttemptContext(a.getId(), a.getUserId(), a.getExamSource(), a.getTestNumber(),
                a.getSkill(), a.getStatus() == null ? null : a.getStatus().name());
    }
}
