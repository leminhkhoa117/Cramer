package com.cramer.speaking.domain;

import com.cramer.platform.error.OperationNotAllowedException;

import java.util.Map;
import java.util.Set;

/**
 * Allowed Speaking session transitions (SPEC-14 §2). Centralizes the lifecycle so illegal
 * transitions are rejected consistently.
 *
 * <pre>
 *   in_progress     → completed | abandoned | expired
 *   completed       → grading
 *   grading         → graded | grading_failed
 *   grading_failed  → grading
 *   graded          → completed     (admin regrade only — the fix that makes regrade work)
 * </pre>
 *
 * <p><strong>Fix (SPEC-14 §7):</strong> admin regrade resets a session to {@code completed}
 * (a claimable state), not {@code grading} — the grading worker claims {@code completed}
 * sessions, so the old "set grading directly" path never actually re-graded.
 */
public final class SpeakingSessionStateMachine {

    private static final Map<SpeakingSessionStatus, Set<SpeakingSessionStatus>> ALLOWED = Map.of(
            SpeakingSessionStatus.IN_PROGRESS,
            Set.of(SpeakingSessionStatus.COMPLETED, SpeakingSessionStatus.ABANDONED, SpeakingSessionStatus.EXPIRED),
            SpeakingSessionStatus.COMPLETED, Set.of(SpeakingSessionStatus.GRADING),
            SpeakingSessionStatus.GRADING, Set.of(SpeakingSessionStatus.GRADED, SpeakingSessionStatus.GRADING_FAILED),
            SpeakingSessionStatus.GRADING_FAILED, Set.of(SpeakingSessionStatus.GRADING),
            SpeakingSessionStatus.GRADED, Set.of(SpeakingSessionStatus.COMPLETED),
            SpeakingSessionStatus.ABANDONED, Set.of(),
            SpeakingSessionStatus.EXPIRED, Set.of());

    private SpeakingSessionStateMachine() {
    }

    public static boolean canTransition(SpeakingSessionStatus from, SpeakingSessionStatus to) {
        return from != null && to != null && ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    /** @throws OperationNotAllowedException (→403) if the transition is not permitted. */
    public static void requireTransition(SpeakingSessionStatus from, SpeakingSessionStatus to) {
        if (!canTransition(from, to)) {
            throw new OperationNotAllowedException("Illegal speaking transition: " + from + " → " + to);
        }
    }

    /**
     * Resolve the target state for an admin regrade (SPEC-14 §7). From {@code grading_failed}
     * always; from {@code graded} only with {@code force}. Returns {@code COMPLETED} so the
     * worker (which claims {@code completed}) re-grades.
     */
    public static SpeakingSessionStatus regradeTarget(SpeakingSessionStatus current, boolean force) {
        if (current == SpeakingSessionStatus.GRADING_FAILED
                || (current == SpeakingSessionStatus.GRADED && force)) {
            return SpeakingSessionStatus.COMPLETED;
        }
        throw new OperationNotAllowedException(
                "Cannot regrade from " + current + (current == SpeakingSessionStatus.GRADED ? " without force" : ""));
    }
}
