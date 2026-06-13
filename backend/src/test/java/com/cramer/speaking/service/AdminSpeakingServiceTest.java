package com.cramer.speaking.service;

import com.cramer.admin.service.AuditPort;
import com.cramer.speaking.domain.SpeakingSession;
import com.cramer.speaking.domain.SpeakingSessionStatus;
import com.cramer.speaking.repository.SpeakingSessionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSpeakingServiceTest {

    @Mock SpeakingSessionRepository sessions;
    @Mock AuditPort audit;

    private AdminSpeakingService service() {
        return new AdminSpeakingService(sessions, audit);
    }

    private SpeakingSession session(SpeakingSessionStatus status) {
        SpeakingSession s = new SpeakingSession();
        s.setId(7L);
        s.setUserId(UUID.randomUUID());
        s.setStatus(status);
        s.setGradingAttempts(3);
        s.setLastGradingError("old error");
        return s;
    }

    @Test
    void regradeFromFailedResetsToCompletedAndAudits() {
        SpeakingSession s = session(SpeakingSessionStatus.GRADING_FAILED);
        when(sessions.findByIdForUpdate(7L)).thenReturn(Optional.of(s));

        UUID admin = UUID.randomUUID();
        long id = service().regrade(admin, 7L, "FULL", false, "examiner dispute");

        assertThat(id).isEqualTo(7L);
        assertThat(s.getStatus()).isEqualTo(SpeakingSessionStatus.COMPLETED);
        assertThat(s.getGradingAttempts()).isZero();
        assertThat(s.getLastGradingError()).isNull();
        verify(audit).record(eq(admin), eq("SPEAKING_REGRADE"), eq("SPEAKING_SESSION"), eq("7"),
                eq("examiner dispute"), any(JsonNode.class), any(JsonNode.class));
    }

    @Test
    void regradeFromGradedRequiresForce() {
        SpeakingSession s = session(SpeakingSessionStatus.GRADED);
        when(sessions.findByIdForUpdate(7L)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> service().regrade(UUID.randomUUID(), 7L, null, false, "reason"))
                .isInstanceOf(com.cramer.platform.error.OperationNotAllowedException.class);
    }

    @Test
    void regradeRejectsBlankReason() {
        assertThatThrownBy(() -> service().regrade(UUID.randomUUID(), 7L, null, false, "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
