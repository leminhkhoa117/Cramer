package com.cramer.speaking.service;

import com.cramer.billing.service.SpeakingBillingPort;
import com.cramer.platform.common.json.Json;
import com.cramer.platform.error.OperationNotAllowedException;
import com.cramer.platform.error.QuotaExceededException;
import com.cramer.platform.error.ResourceNotFoundException;
import com.cramer.speaking.config.SpeakingSessionProperties;
import com.cramer.speaking.domain.SpeakingSession;
import com.cramer.speaking.domain.SpeakingSessionStateMachine;
import com.cramer.speaking.domain.SpeakingSessionStatus;
import com.cramer.speaking.domain.SpeakingTranscript;
import com.cramer.speaking.repository.SpeakingSessionRepository;
import com.cramer.speaking.repository.SpeakingTranscriptRepository;
import com.cramer.speaking.web.dto.CreateSessionRequest;
import com.cramer.speaking.web.dto.SaveTranscriptRequest;
import com.cramer.speaking.web.dto.SpeakingSessionView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Speaking session lifecycle (SPEC-14 §2/§5): create (blueprint + Lúa check, no deduct),
 * transcript upsert (tamper-proof against the frozen blueprint), complete (gate on all turns +
 * deduct Lúa + dispatch grading after commit), abandon (no charge), get/history. Realtime (WS)
 * and the grading worker are separate components; this owns the REST-persisted truth.
 */
@Service
public class SpeakingSessionService {

    private final SpeakingSessionRepository sessions;
    private final SpeakingTranscriptRepository transcripts;
    private final SpeakingBlueprintService blueprints;
    private final SpeakingBillingPort billing;
    private final SpeakingSessionProperties props;
    private final ObjectProvider<SpeakingGradingTrigger> gradingTrigger;

    public SpeakingSessionService(SpeakingSessionRepository sessions,
                                  SpeakingTranscriptRepository transcripts,
                                  SpeakingBlueprintService blueprints,
                                  SpeakingBillingPort billing,
                                  SpeakingSessionProperties props,
                                  ObjectProvider<SpeakingGradingTrigger> gradingTrigger) {
        this.sessions = sessions;
        this.transcripts = transcripts;
        this.blueprints = blueprints;
        this.billing = billing;
        this.props = props;
        this.gradingTrigger = gradingTrigger;
    }

    // --- Create (SPEC-14 §2) ---

    @Transactional
    public SpeakingSessionView create(UUID userId, CreateSessionRequest req) {
        String mode = SpeakingInputs.normalizeMode(req.sessionMode());
        String accent = SpeakingInputs.normalizeAccent(req.accent());
        BigDecimal speed = SpeakingInputs.normalizeSpeed(req.speed());
        int luaCost = props.resolvedLuaCost();

        if (props.resolvedCheckOnCreate() && !billing.canAfford(userId, luaCost)) {
            throw new QuotaExceededException("INSUFFICIENT_LUA", "Not enough Lúa to start a speaking session");
        }

        ObjectNode blueprint = blueprints.build(req.testId(), mode, accent, speed.toPlainString());

        SpeakingSession s = new SpeakingSession();
        s.setUserId(userId);
        s.setTestId(req.testId());
        s.setSessionMode(mode);
        s.setStatus(SpeakingSessionStatus.IN_PROGRESS);
        s.setAccent(accent);
        s.setSpeed(speed);
        s.setSessionBlueprint(blueprint);
        s.setIsFinalized(false);
        s.setLuaCost(luaCost);
        s.setLuaDeducted(false);
        return view(sessions.save(s));
    }

    @Transactional(readOnly = true)
    public SpeakingSessionView get(UUID userId, long sessionId) {
        return view(owned(userId, sessionId));
    }

    // --- Transcript upsert (SPEC-14 §5) ---

    @Transactional
    public void saveTranscript(UUID userId, long sessionId, SaveTranscriptRequest req) {
        SpeakingSession s = lockOwned(userId, sessionId);
        if (s.getStatus() != SpeakingSessionStatus.IN_PROGRESS || Boolean.TRUE.equals(s.getIsFinalized())) {
            throw new OperationNotAllowedException("Session is not accepting transcripts");
        }
        JsonNode expectedTurn = findTurn(s.getSessionBlueprint(), req.turnIndex());
        if (expectedTurn == null) {
            throw new ResourceNotFoundException("No blueprint turn for index " + req.turnIndex());
        }
        // Tamper-proofing: the client cannot alter the frozen prompt (deep equality).
        if (!intEquals(expectedTurn.get("partNumber"), req.partNumber())
                || !longEquals(expectedTurn.get("sourceQuestionId"), req.sourceQuestionId())
                || !expectedTurn.get("questionSnapshot").equals(req.questionSnapshot())) {
            throw new OperationNotAllowedException("Transcript does not match the frozen blueprint turn");
        }
        String audioPath = req.audioStoragePath() == null ? null : AudioStoragePath.require(req.audioStoragePath());
        if (req.transcriptConfidence() != null
                && (req.transcriptConfidence() < 0 || req.transcriptConfidence() > 1)) {
            throw new IllegalArgumentException("confidence must be in [0,1]");
        }

        SpeakingTranscript t = transcripts.findBySessionIdAndTurnIndex(sessionId, req.turnIndex())
                .orElseGet(SpeakingTranscript::new);
        t.setSessionId(sessionId);
        t.setTurnIndex(req.turnIndex());
        t.setPartNumber(req.partNumber());
        t.setSourceQuestionId(req.sourceQuestionId());
        t.setQuestionSnapshot(req.questionSnapshot());
        t.setAudioStoragePath(audioPath);
        t.setAudioDurationSeconds(req.audioDurationSeconds());
        t.setTranscriptText(req.transcriptText() == null || req.transcriptText().isBlank()
                ? null : req.transcriptText().trim());
        t.setTranscriptConfidence(req.transcriptConfidence() == null ? null : BigDecimal.valueOf(req.transcriptConfidence()));
        transcripts.save(t);
    }

    // --- Complete / abandon (SPEC-14 §2) ---

    @Transactional
    public SpeakingSessionView complete(UUID userId, long sessionId, Integer durationSeconds) {
        SpeakingSession s = lockOwned(userId, sessionId);
        SpeakingSessionStateMachine.requireTransition(s.getStatus(), SpeakingSessionStatus.COMPLETED);

        int selectedTurns = countSelectedTurns(s.getSessionBlueprint());
        long saved = transcripts.countBySessionId(sessionId);
        if (saved < selectedTurns) {
            throw new OperationNotAllowedException(
                    "Cannot complete: " + saved + "/" + selectedTurns + " turns have transcripts");
        }

        s.setStatus(SpeakingSessionStatus.COMPLETED);
        s.setIsFinalized(true);
        s.setCompletedAt(OffsetDateTime.now());
        if (durationSeconds != null) {
            s.setTotalDurationSeconds(durationSeconds);
        }

        if (props.resolvedChargeOnComplete() && !Boolean.TRUE.equals(s.getLuaDeducted()) && s.getLuaCost() > 0) {
            billing.deduct(userId, sessionId, s.getLuaCost()); // 402 rolls back completion
            s.setLuaDeducted(true);
        }
        sessions.save(s);
        enqueueGradingAfterCommit(sessionId);
        return view(s);
    }

    @Transactional
    public SpeakingSessionView abandon(UUID userId, long sessionId) {
        SpeakingSession s = lockOwned(userId, sessionId);
        SpeakingSessionStateMachine.requireTransition(s.getStatus(), SpeakingSessionStatus.ABANDONED);
        s.setStatus(SpeakingSessionStatus.ABANDONED);
        s.setIsFinalized(true);
        s.setCompletedAt(OffsetDateTime.now());
        return view(sessions.save(s)); // no charge
    }

    // --- Results / history ---

    @Transactional(readOnly = true)
    public JsonNode results(UUID userId, long sessionId) {
        SpeakingSession s = owned(userId, sessionId);
        if (s.getStatus() != SpeakingSessionStatus.GRADED || s.getGradingResult() == null) {
            throw new IllegalStateException("Session is not graded yet");
        }
        return s.getGradingResult();
    }

    @Transactional(readOnly = true)
    public String gradingStatus(UUID userId, long sessionId) {
        return owned(userId, sessionId).getStatus().dbValue();
    }

    @Transactional(readOnly = true)
    public Page<SpeakingSessionView> history(UUID userId, int page, int size, String status) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        Page<SpeakingSession> result = (status == null || status.isBlank())
                ? sessions.findByUserIdOrderByStartedAtDesc(userId, pageable)
                : sessions.findByUserIdAndStatusOrderByStartedAtDesc(userId, SpeakingSessionStatus.from(status), pageable);
        return result.map(this::view);
    }

    // --- Helpers ---

    private void enqueueGradingAfterCommit(long sessionId) {
        SpeakingGradingTrigger trigger = gradingTrigger.getIfAvailable();
        if (trigger == null) {
            return; // worker not wired (integration-tier) — documented
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    trigger.enqueue(sessionId);
                }
            });
        } else {
            trigger.enqueue(sessionId);
        }
    }

    private int countSelectedTurns(JsonNode blueprint) {
        int count = 0;
        JsonNode parts = blueprint.path("parts");
        if (parts.isArray()) {
            for (JsonNode part : parts) {
                JsonNode turns = part.path("turns");
                if (turns.isArray()) {
                    count += turns.size();
                }
            }
        }
        return count;
    }

    private JsonNode findTurn(JsonNode blueprint, int turnIndex) {
        JsonNode parts = blueprint.path("parts");
        if (parts.isArray()) {
            for (JsonNode part : parts) {
                for (JsonNode turn : part.path("turns")) {
                    if (turn.path("turnIndex").asInt(-1) == turnIndex) {
                        return turn;
                    }
                }
            }
        }
        return null;
    }

    private static boolean intEquals(JsonNode node, Integer value) {
        return node != null && value != null && node.asInt() == value;
    }

    private static boolean longEquals(JsonNode node, Long value) {
        if (node == null || node.isNull()) {
            return value == null;
        }
        return value != null && node.asLong() == value;
    }

    private SpeakingSessionView view(SpeakingSession s) {
        return SpeakingSessionView.of(s, publicBlueprint(s.getSessionBlueprint()));
    }

    /** Strip any {@code _internal} deferred banks before exposing the blueprint (SPEC-14 §3). */
    private JsonNode publicBlueprint(JsonNode blueprint) {
        if (blueprint == null || !blueprint.isObject()) {
            return blueprint;
        }
        ObjectNode copy = blueprint.deepCopy();
        copy.remove("_internal");
        for (JsonNode part : copy.path("parts")) {
            if (part instanceof ObjectNode partObj) {
                partObj.remove("_internal");
            }
        }
        return copy;
    }

    private SpeakingSession owned(UUID userId, long sessionId) {
        SpeakingSession s = sessions.findById(sessionId)
                .orElseThrow(() -> ResourceNotFoundException.of("SpeakingSession", sessionId));
        requireOwner(s, userId);
        return s;
    }

    private SpeakingSession lockOwned(UUID userId, long sessionId) {
        SpeakingSession s = sessions.findByIdForUpdate(sessionId)
                .orElseThrow(() -> ResourceNotFoundException.of("SpeakingSession", sessionId));
        requireOwner(s, userId);
        return s;
    }

    private void requireOwner(SpeakingSession s, UUID userId) {
        if (!s.getUserId().equals(userId)) {
            throw new OperationNotAllowedException("Speaking session does not belong to the current user");
        }
    }
}
