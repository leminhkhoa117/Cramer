package com.cramer.service.implement;

import com.cramer.entity.SpeakingSessionStatus;
import java.util.Map;
import java.util.Set;

public class SpeakingSessionStatusTransitioner {

    private static final Map<SpeakingSessionStatus, Set<SpeakingSessionStatus>> ALLOWED = Map.of(
        SpeakingSessionStatus.IN_PROGRESS, Set.of(SpeakingSessionStatus.COMPLETED, SpeakingSessionStatus.ABANDONED, SpeakingSessionStatus.EXPIRED),
        SpeakingSessionStatus.COMPLETED, Set.of(SpeakingSessionStatus.GRADING),
        SpeakingSessionStatus.GRADING, Set.of(SpeakingSessionStatus.GRADED, SpeakingSessionStatus.GRADING_FAILED),
        SpeakingSessionStatus.GRADING_FAILED, Set.of(SpeakingSessionStatus.GRADING)
    );

    public static void transitionTo(String currentStatus, String nextStatus) {
        transitionTo(currentStatus, nextStatus, false);
    }

    public static void transitionTo(String currentStatus, String nextStatus, boolean force) {
        SpeakingSessionStatus from = SpeakingSessionStatus.fromDbValue(currentStatus);
        SpeakingSessionStatus to = SpeakingSessionStatus.fromDbValue(nextStatus);

        if (from == SpeakingSessionStatus.GRADED && to == SpeakingSessionStatus.GRADING && force) {
            return;
        }

        Set<SpeakingSessionStatus> allowedNext = ALLOWED.get(from);
        if (allowedNext == null || !allowedNext.contains(to)) {
            throw new IllegalStateException(
                String.format("Invalid status transition: %s → %s", currentStatus, nextStatus));
        }
    }
}
