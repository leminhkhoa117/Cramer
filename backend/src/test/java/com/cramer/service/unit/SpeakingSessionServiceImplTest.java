package com.cramer.service.unit;

import com.cramer.config.SpeakingSessionProperties;
import com.cramer.dto.CreateSpeakingSessionDTO;
import com.cramer.dto.SaveSpeakingTranscriptDTO;
import com.cramer.dto.SpeakingSessionActionDTO;
import com.cramer.dto.SpeakingSessionDTO;
import com.cramer.dto.SpeakingTurnDTO;
import com.cramer.entity.CreditTransaction;
import com.cramer.entity.SpeakingSession;
import com.cramer.entity.SpeakingTranscript;
import com.cramer.exception.QuotaExceededException;
import com.cramer.repository.SpeakingSessionRepository;
import com.cramer.repository.SpeakingTranscriptRepository;
import com.cramer.service.CreditService;
import com.cramer.service.SpeakingContentService;
import com.cramer.service.SpeakingEvaluationDispatchService;
import com.cramer.service.implement.SpeakingSessionServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpeakingSessionServiceImpl Unit Tests")
class SpeakingSessionServiceImplTest {

    @Mock
    private SpeakingSessionRepository speakingSessionRepository;

    @Mock
    private SpeakingTranscriptRepository speakingTranscriptRepository;

    @Mock
    private SpeakingContentService speakingContentService;

    @Mock
    private CreditService creditService;

    @Mock
    private SpeakingEvaluationDispatchService speakingEvaluationDispatchService;

    private SpeakingSessionServiceImpl speakingSessionService;
    private ObjectMapper objectMapper;
    private UUID userId;
    private ObjectNode turnSnapshot;
    private ObjectNode blueprint;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        userId = UUID.fromString("00000000-0000-0000-0000-000000000042");

        SpeakingSessionProperties properties = new SpeakingSessionProperties();
        properties.setLuaCost(15);
        properties.setLuaCheckOnCreate(true);
        properties.setLuaChargeOnComplete(true);

        speakingSessionService = new SpeakingSessionServiceImpl(
                speakingSessionRepository,
                speakingTranscriptRepository,
                speakingContentService,
                creditService,
                speakingEvaluationDispatchService,
                properties);

        turnSnapshot = objectMapper.createObjectNode();
        turnSnapshot.put("schemaVersion", 1);
        turnSnapshot.put("partType", "PART_1");
        turnSnapshot.put("promptText", "What do you do on weekends?");
        turnSnapshot.put("topicLabel", "Weekend");
        blueprint = sessionBlueprint("PART_1", 1, turnSnapshot);
    }

    @Test
    @DisplayName("createSession should persist normalized session state when user has enough Lua")
    void createSession_validRequest_savesSession() {
        CreateSpeakingSessionDTO request = CreateSpeakingSessionDTO.builder()
                .sessionMode("full")
                .testId(32L)
                .accent("British")
                .speed(new BigDecimal("1.0"))
                .build();

        SpeakingContentService.SpeakingContentPlan plan = new SpeakingContentService.SpeakingContentPlan(
                blueprint,
                List.of(turn(1, 1, 501L, turnSnapshot)));

        when(speakingContentService.buildSessionPlan(32L, "FULL", "british", new BigDecimal("1.00")))
                .thenReturn(plan);
        when(creditService.hasEnoughCredits(userId, 15)).thenReturn(true);
        when(speakingSessionRepository.save(any(SpeakingSession.class))).thenAnswer(invocation -> {
            SpeakingSession session = invocation.getArgument(0);
            session.setId(900L);
            session.setStartedAt(OffsetDateTime.now());
            session.setCreatedAt(OffsetDateTime.now());
            session.setUpdatedAt(OffsetDateTime.now());
            return session;
        });

        SpeakingSessionDTO result = speakingSessionService.createSession(request, userId);

        assertThat(result.getSessionId()).isEqualTo(900L);
        assertThat(result.getSessionMode()).isEqualTo("FULL");
        assertThat(result.getAccent()).isEqualTo("british");
        assertThat(result.getLuaCost()).isEqualTo(15);
        verify(creditService).hasEnoughCredits(userId, 15);

        ArgumentCaptor<SpeakingSession> captor = ArgumentCaptor.forClass(SpeakingSession.class);
        verify(speakingSessionRepository).save(captor.capture());
        assertThat(captor.getValue().getSessionMode()).isEqualTo("FULL");
        assertThat(captor.getValue().getSpeed()).isEqualByComparingTo("1.00");
    }

    @Test
    @DisplayName("createSession should block when user does not have enough Lua")
    void createSession_insufficientLua_throws() {
        CreateSpeakingSessionDTO request = CreateSpeakingSessionDTO.builder()
                .sessionMode("PART_1")
                .testId(32L)
                .accent("neutral")
                .speed(new BigDecimal("0.85"))
                .build();

        when(speakingContentService.buildSessionPlan(32L, "PART_1", "neutral", new BigDecimal("0.85")))
                .thenReturn(new SpeakingContentService.SpeakingContentPlan(blueprint, List.of(turn(1, 1, 501L, turnSnapshot))));
        when(creditService.hasEnoughCredits(userId, 15)).thenReturn(false);

        assertThatThrownBy(() -> speakingSessionService.createSession(request, userId))
                .isInstanceOf(QuotaExceededException.class)
                .hasMessageContaining("Insufficient Lúa balance");

        verify(speakingSessionRepository, never()).save(any(SpeakingSession.class));
    }

    @Test
    @DisplayName("saveTranscript should upsert a turn using blueprint runtime truth")
    void saveTranscript_matchingTurn_upsertsTranscript() {
        SpeakingSession session = speakingSession(77L, userId, blueprint, "PART_1");
        SpeakingTranscript existingTranscript = SpeakingTranscript.builder()
                .id(501L)
                .sessionId(77L)
                .turnIndex(1)
                .build();

        SaveSpeakingTranscriptDTO request = SaveSpeakingTranscriptDTO.builder()
                .sourceQuestionId(501L)
                .partNumber(1)
                .turnIndex(1)
                .questionSnapshot(turnSnapshot.deepCopy())
                .audioStoragePath("user/session/turn-001.webm")
                .transcriptText("I usually go out with friends.")
                .audioDurationSeconds(35)
                .transcriptConfidence(new BigDecimal("0.910"))
                .build();

        when(speakingSessionRepository.findAndLockByIdAndUserId(77L, userId)).thenReturn(Optional.of(session));
        existingTranscript.setRecordedAt(OffsetDateTime.now());
        when(speakingTranscriptRepository.findBySessionIdAndTurnIndex(77L, 1)).thenReturn(Optional.of(existingTranscript));
        var result = speakingSessionService.saveTranscript(77L, request, userId);

        assertThat(result.getTranscriptId()).isEqualTo(501L);
        assertThat(result.getStatus()).isEqualTo("saved");

        verify(speakingTranscriptRepository).upsertTranscript(
                eq(77L),
                eq(501L),
                eq(1),
                eq(1),
                eq(turnSnapshot.toString()),
                eq("user/session/turn-001.webm"),
                eq(35),
                eq("I usually go out with friends."),
                eq(new BigDecimal("0.910")),
                eq(null),
                any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("saveTranscript should reject unsafe audio storage paths")
    void saveTranscript_unsafeAudioPath_throws() {
        SpeakingSession session = speakingSession(79L, userId, blueprint, "PART_1");

        SaveSpeakingTranscriptDTO request = SaveSpeakingTranscriptDTO.builder()
                .sourceQuestionId(501L)
                .partNumber(1)
                .turnIndex(1)
                .questionSnapshot(turnSnapshot.deepCopy())
                .audioStoragePath("https://example.com/audio.webm")
                .transcriptText("Unsafe path")
                .audioDurationSeconds(12)
                .transcriptConfidence(new BigDecimal("0.900"))
                .build();

        when(speakingSessionRepository.findAndLockByIdAndUserId(79L, userId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> speakingSessionService.saveTranscript(79L, request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not contain a URL scheme");

        verify(speakingTranscriptRepository, never()).upsertTranscript(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("saveTranscript should materialize deferred Part 3 after Part 2 transcript in FULL mode")
    void saveTranscript_part2InFullSession_materializesDeferredPart3() {
        ObjectNode fullBlueprint = sessionBlueprint("FULL", 2, turnSnapshot);
        ObjectNode updatedBlueprint = sessionBlueprint("FULL", 2, turnSnapshot);
        updatedBlueprint.put("part3Ready", true);

        SpeakingSession session = speakingSession(78L, userId, fullBlueprint, "FULL");
        SpeakingTranscript transcript = SpeakingTranscript.builder()
                .id(601L)
                .sessionId(78L)
                .turnIndex(1)
                .build();

        SaveSpeakingTranscriptDTO request = SaveSpeakingTranscriptDTO.builder()
                .sourceQuestionId(501L)
                .partNumber(2)
                .turnIndex(1)
                .questionSnapshot(turnSnapshot.deepCopy())
                .audioStoragePath("user/session/turn-001.webm")
                .transcriptText("I like travelling because it helps me understand new places.")
                .audioDurationSeconds(90)
                .transcriptConfidence(new BigDecimal("0.950"))
                .build();

        when(speakingSessionRepository.findAndLockByIdAndUserId(78L, userId)).thenReturn(Optional.of(session));
        transcript.setRecordedAt(OffsetDateTime.now());
        when(speakingTranscriptRepository.findBySessionIdAndTurnIndex(78L, 1)).thenReturn(Optional.of(transcript));
        when(speakingContentService.hasPendingDeferredPart3(fullBlueprint)).thenReturn(true);
        when(speakingContentService.materializeDeferredPart3(fullBlueprint, request.getTranscriptText()))
                .thenReturn(new SpeakingContentService.SpeakingContentPlan(updatedBlueprint, List.of()));
        when(speakingSessionRepository.save(any(SpeakingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        speakingSessionService.saveTranscript(78L, request, userId);

        ArgumentCaptor<SpeakingSession> sessionCaptor = ArgumentCaptor.forClass(SpeakingSession.class);
        verify(speakingSessionRepository, times(1)).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getSessionBlueprint()).isEqualTo(updatedBlueprint);
    }

    @Test
    @DisplayName("completeSession should finalize, deduct Lua, and dispatch evaluation when all turns are recorded")
    void completeSession_allTurnsRecorded_finalizesAndDispatches() {
        SpeakingSession session = speakingSession(88L, userId, blueprint, "PART_1");
        session.setStartedAt(OffsetDateTime.now().minusMinutes(3));

        SpeakingTranscript transcript = SpeakingTranscript.builder()
                .id(701L)
                .sessionId(88L)
                .turnIndex(1)
                .build();

        when(speakingSessionRepository.findAndLockByIdAndUserId(88L, userId)).thenReturn(Optional.of(session));
        when(speakingContentService.hasPendingDeferredPart3(blueprint)).thenReturn(false);
        when(speakingTranscriptRepository.findBySessionIdOrderByTurnIndexAsc(88L)).thenReturn(List.of(transcript));
        when(speakingSessionRepository.save(any(SpeakingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SpeakingSessionActionDTO result = speakingSessionService.completeSession(88L, userId);

        assertThat(result.getStatus()).isEqualTo("completed");
        verify(creditService).spendCredits(eq(userId), eq(15), eq(CreditTransaction.Category.SPEAKING_SESSION),
                eq("Speaking session completion"), eq("88"));
        verify(speakingEvaluationDispatchService).dispatchEvaluation(88L, userId);
    }

    @Test
    @DisplayName("completeSession should reject when Part 3 selection is still pending")
    void completeSession_pendingPart3_throws() {
        SpeakingSession session = speakingSession(99L, userId, blueprint, "FULL");

        when(speakingSessionRepository.findAndLockByIdAndUserId(99L, userId)).thenReturn(Optional.of(session));
        when(speakingContentService.hasPendingDeferredPart3(blueprint)).thenReturn(true);

        assertThatThrownBy(() -> speakingSessionService.completeSession(99L, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Part 3 question selection is still pending");

        verify(creditService, never()).spendCredits(any(), any(Integer.class), any(), any(), any());
        verify(speakingEvaluationDispatchService, never()).dispatchEvaluation(any(), any());
    }

    @Test
    @DisplayName("getSession should return not found when the session is not owned by the user")
    void getSession_notOwned_throwsNotFound() {
        when(speakingSessionRepository.findByIdAndUserId(100L, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> speakingSessionService.getSession(100L, userId))
                .isInstanceOf(com.cramer.exception.ResourceNotFoundException.class)
                .hasMessageContaining("SpeakingSession not found");
    }

    private SpeakingSession speakingSession(Long id, UUID ownerId, ObjectNode sessionBlueprint, String sessionMode) {
        return SpeakingSession.builder()
                .id(id)
                .userId(ownerId)
                .testId(32L)
                .sessionMode(sessionMode)
                .status("in_progress")
                .accent("british")
                .speed(new BigDecimal("1.00"))
                .sessionBlueprint(sessionBlueprint)
                .isFinalized(false)
                .luaCost(15)
                .luaDeducted(false)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private ObjectNode sessionBlueprint(String sessionMode, int partNumber, ObjectNode... snapshots) {
        ObjectNode sessionBlueprint = objectMapper.createObjectNode();
        sessionBlueprint.put("schemaVersion", 1);
        sessionBlueprint.put("testId", 32L);
        sessionBlueprint.put("sessionMode", sessionMode);
        sessionBlueprint.put("accent", "british");
        sessionBlueprint.put("speed", new BigDecimal("1.00"));

        var turnsArray = objectMapper.createArrayNode();
        for (int index = 0; index < snapshots.length; index++) {
            ObjectNode turnNode = objectMapper.createObjectNode();
            turnNode.put("turnIndex", index + 1);
            turnNode.put("sourceQuestionId", 501L + index);
            turnNode.set("questionSnapshot", snapshots[index].deepCopy());
            turnsArray.add(turnNode);
        }

        ObjectNode partNode = objectMapper.createObjectNode();
        partNode.put("partNumber", partNumber);
        partNode.set("turns", turnsArray);

        var partsArray = objectMapper.createArrayNode();
        partsArray.add(partNode);
        sessionBlueprint.set("parts", partsArray);
        return sessionBlueprint;
    }

    private SpeakingTurnDTO turn(int turnIndex, int partNumber, Long sourceQuestionId, ObjectNode snapshot) {
        return SpeakingTurnDTO.builder()
                .turnIndex(turnIndex)
                .partNumber(partNumber)
                .sourceQuestionId(sourceQuestionId)
                .questionSnapshot(snapshot.deepCopy())
                .build();
    }
}
