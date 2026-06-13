package com.cramer.speaking.domain;

import com.cramer.platform.error.OperationNotAllowedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpeakingSessionStateMachineTest {

    @Test
    @DisplayName("the lifecycle transitions are exactly those allowed by SPEC-14 §2")
    void allowedTransitions() {
        assertThat(SpeakingSessionStateMachine.canTransition(
                SpeakingSessionStatus.IN_PROGRESS, SpeakingSessionStatus.COMPLETED)).isTrue();
        assertThat(SpeakingSessionStateMachine.canTransition(
                SpeakingSessionStatus.COMPLETED, SpeakingSessionStatus.GRADING)).isTrue();
        assertThat(SpeakingSessionStateMachine.canTransition(
                SpeakingSessionStatus.GRADING, SpeakingSessionStatus.GRADED)).isTrue();
        assertThat(SpeakingSessionStateMachine.canTransition(
                SpeakingSessionStatus.GRADING_FAILED, SpeakingSessionStatus.GRADING)).isTrue();
    }

    @Test
    @DisplayName("illegal transitions are rejected")
    void illegalTransitions() {
        assertThat(SpeakingSessionStateMachine.canTransition(
                SpeakingSessionStatus.IN_PROGRESS, SpeakingSessionStatus.GRADED)).isFalse();
        assertThat(SpeakingSessionStateMachine.canTransition(
                SpeakingSessionStatus.ABANDONED, SpeakingSessionStatus.GRADING)).isFalse();
        assertThatThrownBy(() -> SpeakingSessionStateMachine.requireTransition(
                SpeakingSessionStatus.GRADED, SpeakingSessionStatus.IN_PROGRESS))
                .isInstanceOf(OperationNotAllowedException.class);
    }

    @Test
    @DisplayName("regrade resets to COMPLETED (claimable) — the fix that makes regrade work")
    void regradeTargetsCompleted() {
        // from grading_failed: always
        assertThat(SpeakingSessionStateMachine.regradeTarget(SpeakingSessionStatus.GRADING_FAILED, false))
                .isEqualTo(SpeakingSessionStatus.COMPLETED);
        // from graded: only with force
        assertThat(SpeakingSessionStateMachine.regradeTarget(SpeakingSessionStatus.GRADED, true))
                .isEqualTo(SpeakingSessionStatus.COMPLETED);
    }

    @Test
    @DisplayName("regrade from graded without force, or from in_progress, is rejected")
    void regradeRejected() {
        assertThatThrownBy(() -> SpeakingSessionStateMachine.regradeTarget(SpeakingSessionStatus.GRADED, false))
                .isInstanceOf(OperationNotAllowedException.class);
        assertThatThrownBy(() -> SpeakingSessionStateMachine.regradeTarget(SpeakingSessionStatus.IN_PROGRESS, true))
                .isInstanceOf(OperationNotAllowedException.class);
    }
}
