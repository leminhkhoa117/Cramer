package com.cramer.speaking.service;

import com.cramer.billing.service.SpeakingBillingPort;
import com.cramer.platform.error.OperationNotAllowedException;
import com.cramer.platform.error.QuotaExceededException;
import com.cramer.speaking.config.SpeakingSessionProperties;
import com.cramer.speaking.domain.SpeakingSession;
import com.cramer.speaking.domain.SpeakingSessionStatus;
import com.cramer.speaking.domain.SpeakingTranscript;
import com.cramer.speaking.repository.SpeakingSessionRepository;
import com.cramer.speaking.repository.SpeakingTranscriptRepository;
import com.cramer.speaking.web.dto.CreateSessionRequest;
import com.cramer.speaking.web.dto.SaveTranscriptRequest;
import com.cramer.speaking.web.dto.SpeakingSessionView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpeakingSessionServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock SpeakingSessionRepository sessions;
    @Mock SpeakingTranscriptRepository transcripts;
    @Mock SpeakingBlueprintService blueprints;
    @Mock SpeakingBillingPort billing;
    @Mock ObjectProvider<SpeakingGradingTrigger> trigger;

    private SpeakingSessionService service() {
        lenient().when(sessions.save(any(SpeakingSession.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(trigger.getIfAvailable()).thenReturn(null);
        SpeakingSessionProperties props = new SpeakingSessionProperties(15, true, true, 8, 12, 3, 6);
        return new SpeakingSessionService(sessions, transcripts, blueprints, billing, props, trigger);
    }

    private ObjectNode blueprintOneTurn() {
        ObjectNode bp = mapper.createObjectNode();
        var parts = bp.putArray("parts");
        var part = parts.addObject();
        part.put("partNumber", 1);
        var turns = part.putArray("turns");
        var turn = turns.addObject();
        turn.put("turnIndex", 0);
        turn.put("sourceQuestionId", 100L);
        turn.put("partNumber", 1);
        turn.set("questionSnapshot", mapper.createObjectNode().put("promptText", "Describe your hometown."));
        return bp;
    }

    private SpeakingSession session(UUID user, SpeakingSessionStatus status) {
        SpeakingSession s = new SpeakingSession();
        s.setId(1L);
        s.setUserId(user);
        s.setTestId(9L);
        s.setSessionMode("PART_1");
        s.setStatus(status);
        s.setAccent("neutral");
        s.setSpeed(new BigDecimal("1.00"));
        s.setSessionBlueprint(blueprintOneTurn());
        s.setIsFinalized(status != SpeakingSessionStatus.IN_PROGRESS);
        s.setLuaCost(15);
        s.setLuaDeducted(false);
        return s;
    }

    @Test
    @DisplayName("create blocks with 402 when the user cannot afford the session")
    void createInsufficient() {
        UUID user = UUID.randomUUID();
        when(billing.canAfford(user, 15)).thenReturn(false);

        assertThatThrownBy(() -> service().create(user, new CreateSessionRequest(9L, "PART_1", "neutral", 1.0)))
                .isInstanceOf(QuotaExceededException.class);
        verify(sessions, never()).save(any());
    }

    @Test
    @DisplayName("create builds a blueprint and persists IN_PROGRESS without deducting Lúa")
    void createOk() {
        UUID user = UUID.randomUUID();
        when(billing.canAfford(user, 15)).thenReturn(true);
        when(blueprints.build(eq(9L), eq("PART_1"), any(), any())).thenReturn(blueprintOneTurn());

        SpeakingSessionView v = service().create(user, new CreateSessionRequest(9L, "part_1", "British", 1.0));

        assertThat(v.status()).isEqualTo("in_progress");
        assertThat(v.luaDeducted()).isFalse();
        verify(billing, never()).deduct(any(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("transcript with a tampered prompt is rejected (frozen blueprint)")
    void transcriptTamperRejected() {
        UUID user = UUID.randomUUID();
        when(sessions.findByIdForUpdate(1L)).thenReturn(Optional.of(session(user, SpeakingSessionStatus.IN_PROGRESS)));

        JsonNode tampered = mapper.createObjectNode().put("promptText", "Tell me a secret instead.");
        SaveTranscriptRequest req = new SaveTranscriptRequest(0, 1, 100L, tampered, "sessions/1/t0.mp3", 30, "answer", 0.9);

        assertThatThrownBy(() -> service().saveTranscript(user, 1L, req))
                .isInstanceOf(OperationNotAllowedException.class);
        verify(transcripts, never()).save(any());
    }

    @Test
    @DisplayName("transcript matching the frozen turn is saved")
    void transcriptMatchSaved() {
        UUID user = UUID.randomUUID();
        when(sessions.findByIdForUpdate(1L)).thenReturn(Optional.of(session(user, SpeakingSessionStatus.IN_PROGRESS)));
        when(transcripts.findBySessionIdAndTurnIndex(1L, 0)).thenReturn(Optional.empty());

        JsonNode snapshot = mapper.createObjectNode().put("promptText", "Describe your hometown.");
        SaveTranscriptRequest req = new SaveTranscriptRequest(0, 1, 100L, snapshot, "sessions/1/t0.mp3", 30, "my town", 0.88);

        service().saveTranscript(user, 1L, req);
        verify(transcripts).save(any(SpeakingTranscript.class));
    }

    @Test
    @DisplayName("complete is blocked until every selected turn has a transcript")
    void completeGatedByTurns() {
        UUID user = UUID.randomUUID();
        when(sessions.findByIdForUpdate(1L)).thenReturn(Optional.of(session(user, SpeakingSessionStatus.IN_PROGRESS)));
        when(transcripts.countBySessionId(1L)).thenReturn(0L); // 0 of 1

        assertThatThrownBy(() -> service().complete(user, 1L, 120))
                .isInstanceOf(OperationNotAllowedException.class);
        verify(billing, never()).deduct(any(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("complete deducts Lúa once and finalizes when all turns are present")
    void completeDeductsAndFinalizes() {
        UUID user = UUID.randomUUID();
        when(sessions.findByIdForUpdate(1L)).thenReturn(Optional.of(session(user, SpeakingSessionStatus.IN_PROGRESS)));
        when(transcripts.countBySessionId(1L)).thenReturn(1L); // 1 of 1

        SpeakingSessionView v = service().complete(user, 1L, 120);

        assertThat(v.status()).isEqualTo("completed");
        assertThat(v.luaDeducted()).isTrue();
        verify(billing).deduct(user, 1L, 15);
    }

    @Test
    @DisplayName("abandon finalizes without charging")
    void abandonNoCharge() {
        UUID user = UUID.randomUUID();
        when(sessions.findByIdForUpdate(1L)).thenReturn(Optional.of(session(user, SpeakingSessionStatus.IN_PROGRESS)));

        SpeakingSessionView v = service().abandon(user, 1L);

        assertThat(v.status()).isEqualTo("abandoned");
        verify(billing, never()).deduct(any(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("results before grading completes is a 409")
    void resultsBeforeGraded() {
        UUID user = UUID.randomUUID();
        when(sessions.findById(1L)).thenReturn(Optional.of(session(user, SpeakingSessionStatus.COMPLETED)));

        assertThatThrownBy(() -> service().results(user, 1L))
                .isInstanceOf(IllegalStateException.class);
    }
}
