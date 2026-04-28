package com.cramer.service.unit;

import com.cramer.service.implement.SpeakingSessionStatusTransitioner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SpeakingSessionStatusTransitioner Unit Tests")
class SpeakingSessionStatusTransitionerTest {

    @Test
    @DisplayName("Should allow all valid status transitions")
    void shouldAllowValidTransitions() {
        assertDoesNotThrow(() -> SpeakingSessionStatusTransitioner.transitionTo("in_progress", "completed"));
        assertDoesNotThrow(() -> SpeakingSessionStatusTransitioner.transitionTo("in_progress", "abandoned"));
        assertDoesNotThrow(() -> SpeakingSessionStatusTransitioner.transitionTo("in_progress", "expired"));
        assertDoesNotThrow(() -> SpeakingSessionStatusTransitioner.transitionTo("completed", "grading"));
        assertDoesNotThrow(() -> SpeakingSessionStatusTransitioner.transitionTo("grading", "graded"));
        assertDoesNotThrow(() -> SpeakingSessionStatusTransitioner.transitionTo("grading", "grading_failed"));
        assertDoesNotThrow(() -> SpeakingSessionStatusTransitioner.transitionTo("grading_failed", "grading"));
    }

    @Test
    @DisplayName("Should reject illegal status transitions")
    void shouldRejectIllegalTransitions() {
        assertThrows(IllegalStateException.class,
                () -> SpeakingSessionStatusTransitioner.transitionTo("completed", "graded"));
        assertThrows(IllegalStateException.class,
                () -> SpeakingSessionStatusTransitioner.transitionTo("graded", "completed"));
        assertThrows(IllegalStateException.class,
                () -> SpeakingSessionStatusTransitioner.transitionTo("in_progress", "graded"));
        assertThrows(IllegalStateException.class,
                () -> SpeakingSessionStatusTransitioner.transitionTo("grading", "completed"));
    }

    @Test
    @DisplayName("Should allow admin regrade with force=true")
    void shouldAllowAdminRegradeWithForce() {
        assertDoesNotThrow(() ->
                SpeakingSessionStatusTransitioner.transitionTo("graded", "grading", true));
    }

    @Test
    @DisplayName("Should reject force transition for non-regrade paths")
    void shouldRejectForceOnNonRegrade() {
        assertThrows(IllegalStateException.class,
                () -> SpeakingSessionStatusTransitioner.transitionTo("completed", "graded", true));
    }

    @Test
    @DisplayName("Should reject invalid status string")
    void shouldRejectInvalidStatusString() {
        assertThrows(IllegalArgumentException.class,
                () -> SpeakingSessionStatusTransitioner.transitionTo("in_progress", "garbage"));
        assertThrows(IllegalArgumentException.class,
                () -> SpeakingSessionStatusTransitioner.transitionTo("garbage", "completed"));
    }
}
