package com.cramer.service.implement;

import com.cramer.config.SpeakingSessionProperties;
import com.cramer.dto.CreateSpeakingSessionDTO;
import com.cramer.dto.PageDTO;
import com.cramer.dto.SaveSpeakingTranscriptDTO;
import com.cramer.dto.SpeakingGradingStatusDTO;
import com.cramer.dto.SpeakingHistoryItemDTO;
import com.cramer.dto.SpeakingResultDTO;
import com.cramer.dto.SpeakingSessionActionDTO;
import com.cramer.dto.SpeakingSessionDTO;
import com.cramer.dto.SpeakingTranscriptDTO;
import com.cramer.dto.SpeakingTurnDTO;
import com.cramer.entity.CreditTransaction;
import com.cramer.entity.SpeakingSession;
import com.cramer.entity.SpeakingTranscript;
import com.cramer.exception.QuotaExceededException;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.repository.SpeakingSessionRepository;
import com.cramer.repository.SpeakingTranscriptRepository;
import com.cramer.service.CreditService;
import com.cramer.service.SpeakingContentService;
import com.cramer.service.SpeakingEvaluationDispatchService;
import com.cramer.service.SpeakingSessionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Transactional
public class SpeakingSessionServiceImpl implements SpeakingSessionService {

    private static final Logger logger = LoggerFactory.getLogger(SpeakingSessionServiceImpl.class);
    private static final Pattern URL_SCHEME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://.*");

    private final SpeakingSessionRepository speakingSessionRepository;
    private final SpeakingTranscriptRepository speakingTranscriptRepository;
    private final SpeakingContentService speakingContentService;
    private final CreditService creditService;
    private final SpeakingEvaluationDispatchService speakingEvaluationDispatchService;
    private final SpeakingSessionProperties speakingSessionProperties;

    public SpeakingSessionServiceImpl(
            SpeakingSessionRepository speakingSessionRepository,
            SpeakingTranscriptRepository speakingTranscriptRepository,
            SpeakingContentService speakingContentService,
            CreditService creditService,
            SpeakingEvaluationDispatchService speakingEvaluationDispatchService,
            SpeakingSessionProperties speakingSessionProperties) {
        this.speakingSessionRepository = speakingSessionRepository;
        this.speakingTranscriptRepository = speakingTranscriptRepository;
        this.speakingContentService = speakingContentService;
        this.creditService = creditService;
        this.speakingEvaluationDispatchService = speakingEvaluationDispatchService;
        this.speakingSessionProperties = speakingSessionProperties;
    }

    @Override
    public SpeakingSessionDTO createSession(CreateSpeakingSessionDTO request, UUID userId) {
        String sessionMode = normalizeSessionMode(request.getSessionMode());
        String accent = normalizeAccent(request.getAccent());
        BigDecimal speed = normalizeSpeed(request.getSpeed());

        SpeakingContentService.SpeakingContentPlan plan = speakingContentService.buildSessionPlan(
                request.getTestId(), sessionMode, accent, speed);

        int luaCost = speakingSessionProperties.getLuaCost();
        if (speakingSessionProperties.isLuaCheckOnCreate() && !creditService.hasEnoughCredits(userId, luaCost)) {
            throw new QuotaExceededException("Insufficient Lúa balance for a Speaking session.", "insufficient_lua");
        }

        SpeakingSession session = SpeakingSession.builder()
                .userId(userId)
                .testId(request.getTestId())
                .sessionMode(sessionMode)
                .status("in_progress")
                .accent(accent)
                .speed(speed)
                .sessionBlueprint(plan.sessionBlueprint())
                .isFinalized(false)
                .luaCost(luaCost)
                .luaDeducted(false)
                .build();

        SpeakingSession savedSession = speakingSessionRepository.save(session);
        logger.info("Created speaking session {} for user {} and test {}", savedSession.getId(), userId,
                savedSession.getTestId());
        return toSessionDTO(savedSession, plan.turns());
    }

    @Override
    @Transactional(readOnly = true)
    public SpeakingSessionDTO getSession(Long sessionId, UUID userId) {
        SpeakingSession session = getOwnedSession(sessionId, userId);
        return toSessionDTO(session, extractTurns(session.getSessionBlueprint()));
    }

    @Override
    public SpeakingTranscriptDTO saveTranscript(Long sessionId, SaveSpeakingTranscriptDTO request, UUID userId) {
        SpeakingSession session = getLockedOwnedSession(sessionId, userId);
        ensureOpenSessionForTranscriptWrite(session);

        ExpectedTurn expectedTurn = findExpectedTurn(session.getSessionBlueprint(), request.getTurnIndex());
        if (expectedTurn == null) {
            throw new IllegalArgumentException("turnIndex " + request.getTurnIndex() + " does not exist in sessionBlueprint");
        }

        validateTranscriptAgainstExpectedTurn(request, expectedTurn);

        SpeakingTranscript transcript = speakingTranscriptRepository
                .findBySessionIdAndTurnIndex(sessionId, request.getTurnIndex())
                .orElseGet(() -> SpeakingTranscript.builder()
                        .sessionId(sessionId)
                        .turnIndex(request.getTurnIndex())
                        .build());

        transcript.setSourceQuestionId(expectedTurn.sourceQuestionId());
        transcript.setPartNumber(expectedTurn.partNumber());
        transcript.setQuestionSnapshot(expectedTurn.questionSnapshot().deepCopy());
        transcript.setAudioStoragePath(normalizeAudioStoragePath(request.getAudioStoragePath()));
        transcript.setTranscriptText(normalizeNullableText(request.getTranscriptText()));
        transcript.setAudioDurationSeconds(request.getAudioDurationSeconds());
        transcript.setTranscriptConfidence(request.getTranscriptConfidence());
        transcript.setRecordedAt(OffsetDateTime.now());

        SpeakingTranscript savedTranscript = speakingTranscriptRepository.save(transcript);

        if (request.getPartNumber() == 2
                && "FULL".equals(session.getSessionMode())
                && speakingContentService.hasPendingDeferredPart3(session.getSessionBlueprint())) {
            SpeakingContentService.SpeakingContentPlan updatedPlan = speakingContentService.materializeDeferredPart3(
                    session.getSessionBlueprint(),
                    request.getTranscriptText());
            session.setSessionBlueprint(updatedPlan.sessionBlueprint());
            speakingSessionRepository.save(session);
        }

        logger.info("Saved speaking transcript {} for session {} turn {}", savedTranscript.getId(), sessionId,
                request.getTurnIndex());

        return SpeakingTranscriptDTO.builder()
                .transcriptId(savedTranscript.getId())
                .sessionId(savedTranscript.getSessionId())
                .turnIndex(savedTranscript.getTurnIndex())
                .status("saved")
                .recordedAt(savedTranscript.getRecordedAt())
                .build();
    }

    @Override
    public SpeakingSessionActionDTO completeSession(Long sessionId, UUID userId) {
        SpeakingSession session = getLockedOwnedSession(sessionId, userId);
        ensureCompletableSession(session);

        if (speakingContentService.hasPendingDeferredPart3(session.getSessionBlueprint())) {
            throw new IllegalStateException(
                    "Cannot complete Speaking session while Part 3 question selection is still pending after Part 2.");
        }

        List<SpeakingTurnDTO> turns = extractTurns(session.getSessionBlueprint());
        List<SpeakingTranscript> transcripts = speakingTranscriptRepository.findBySessionIdOrderByTurnIndexAsc(sessionId);
        validateTranscriptCoverage(turns, transcripts, session.getSessionMode());

        OffsetDateTime now = OffsetDateTime.now();
        session.setIsFinalized(true);
        session.setStatus("completed");
        session.setCompletedAt(now);
        session.setTotalDurationSeconds(calculateDurationSeconds(session, now));

        int luaCost = session.getLuaCost() != null ? session.getLuaCost() : 0;
        if (speakingSessionProperties.isLuaChargeOnComplete()
                && !Boolean.TRUE.equals(session.getLuaDeducted())
                && luaCost > 0) {
            creditService.spendCredits(userId, luaCost, CreditTransaction.Category.SPEAKING_SESSION,
                    "Speaking session completion", String.valueOf(sessionId));
            session.setLuaDeducted(true);
        }

        SpeakingSession savedSession = speakingSessionRepository.save(session);
        scheduleEvaluationDispatch(savedSession.getId(), userId);
        logger.info("Completed speaking session {} for user {}", sessionId, userId);

        return SpeakingSessionActionDTO.builder()
                .sessionId(savedSession.getId())
                .status(savedSession.getStatus())
                .message("Session completed. Evaluation queued.")
                .build();
    }

    @Override
    public SpeakingSessionActionDTO abandonSession(Long sessionId, UUID userId) {
        SpeakingSession session = getLockedOwnedSession(sessionId, userId);
        if (Boolean.TRUE.equals(session.getIsFinalized())) {
            throw new IllegalStateException("Speaking session has already been finalized.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        session.setIsFinalized(true);
        session.setStatus("abandoned");
        session.setCompletedAt(now);
        session.setTotalDurationSeconds(calculateDurationSeconds(session, now));

        SpeakingSession savedSession = speakingSessionRepository.save(session);
        logger.info("Abandoned speaking session {} for user {}", sessionId, userId);

        return SpeakingSessionActionDTO.builder()
                .sessionId(savedSession.getId())
                .status(savedSession.getStatus())
                .message("Session abandoned.")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SpeakingGradingStatusDTO getGradingStatus(Long sessionId, UUID userId) {
        SpeakingSession session = getOwnedSession(sessionId, userId);

        return SpeakingGradingStatusDTO.builder()
                .sessionId(session.getId())
                .status(session.getStatus())
                .progress(resolveProgressMessage(session))
                .estimatedSeconds(resolveEstimatedSeconds(session))
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SpeakingResultDTO getResults(Long sessionId, UUID userId) {
        SpeakingSession session = getOwnedSession(sessionId, userId);
        if (!"graded".equals(session.getStatus())) {
            throw new IllegalStateException("Speaking results are not available until grading is complete.");
        }

        return SpeakingResultDTO.builder()
                .sessionId(session.getId())
                .sessionMode(session.getSessionMode())
                .testId(session.getTestId())
                .status(session.getStatus())
                .overallBand(session.getOverallBand())
                .fluencyBand(session.getFluencyBand())
                .lexicalBand(session.getLexicalBand())
                .grammarBand(session.getGrammarBand())
                .pronunciationBand(session.getPronunciationBand())
                .gradingResult(copyJson(session.getGradingResult()))
                .gradedAt(session.getGradedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<SpeakingHistoryItemDTO> getHistory(UUID userId, Pageable pageable, String status) {
        String normalizedStatus = normalizeStatusFilter(status);
        Page<SpeakingHistoryItemDTO> history = speakingSessionRepository.findHistoryByUserId(userId, normalizedStatus, pageable)
                .map(this::toHistoryItemDTO);

        return new PageDTO<>(
                history.getContent(),
                history.getNumber(),
                history.getSize(),
                history.getTotalElements(),
                history.getTotalPages());
    }

    private SpeakingSession getOwnedSession(Long sessionId, UUID userId) {
        SpeakingSession session = speakingSessionRepository.findByIdAndUserId(
                        Objects.requireNonNull(sessionId),
                        Objects.requireNonNull(userId))
                .orElseThrow(() -> new ResourceNotFoundException("SpeakingSession", "id", sessionId));
        return session;
    }

    private SpeakingSession getLockedOwnedSession(Long sessionId, UUID userId) {
        SpeakingSession session = speakingSessionRepository.findAndLockByIdAndUserId(
                        Objects.requireNonNull(sessionId),
                        Objects.requireNonNull(userId))
                .orElseThrow(() -> new ResourceNotFoundException("SpeakingSession", "id", sessionId));
        return session;
    }

    private void ensureOpenSessionForTranscriptWrite(SpeakingSession session) {
        if (Boolean.TRUE.equals(session.getIsFinalized()) || !"in_progress".equals(session.getStatus())) {
            throw new IllegalStateException("Transcript writes are only allowed for in-progress speaking sessions.");
        }
    }

    private void ensureCompletableSession(SpeakingSession session) {
        if (Boolean.TRUE.equals(session.getIsFinalized())) {
            throw new IllegalStateException("Speaking session has already been finalized.");
        }
        if (!"in_progress".equals(session.getStatus())) {
            throw new IllegalStateException("Only in-progress speaking sessions can be completed.");
        }
    }

    private void validateTranscriptAgainstExpectedTurn(SaveSpeakingTranscriptDTO request, ExpectedTurn expectedTurn) {
        if (!Objects.equals(request.getPartNumber(), expectedTurn.partNumber())) {
            throw new IllegalArgumentException("partNumber does not match the session blueprint for turnIndex "
                    + request.getTurnIndex());
        }
        if (!Objects.equals(request.getSourceQuestionId(), expectedTurn.sourceQuestionId())) {
            throw new IllegalArgumentException("sourceQuestionId does not match the session blueprint for turnIndex "
                    + request.getTurnIndex());
        }
        if (!expectedTurn.questionSnapshot().equals(request.getQuestionSnapshot())) {
            throw new IllegalArgumentException("questionSnapshot does not match the session blueprint for turnIndex "
                    + request.getTurnIndex());
        }
    }

    private void validateTranscriptCoverage(
            List<SpeakingTurnDTO> turns,
            List<SpeakingTranscript> transcripts,
            String sessionMode) {
        Set<Integer> requiredTurnIndexes = new HashSet<>();
        for (SpeakingTurnDTO turn : turns) {
            requiredTurnIndexes.add(turn.getTurnIndex());
        }

        Set<Integer> recordedTurnIndexes = new HashSet<>();
        for (SpeakingTranscript transcript : transcripts) {
            recordedTurnIndexes.add(transcript.getTurnIndex());
        }

        List<Integer> missingTurnIndexes = requiredTurnIndexes.stream()
                .filter(turnIndex -> !recordedTurnIndexes.contains(turnIndex))
                .sorted()
                .toList();

        if (!missingTurnIndexes.isEmpty()) {
            throw new IllegalStateException("Cannot complete Speaking session " + sessionMode
                    + " because transcripts are missing for turnIndex: " + missingTurnIndexes + ".");
        }
    }

    private int calculateDurationSeconds(SpeakingSession session, OffsetDateTime completedAt) {
        OffsetDateTime startedAt = session.getStartedAt() != null ? session.getStartedAt() : session.getCreatedAt();
        if (startedAt == null) {
            return 0;
        }
        return (int) Math.max(0, Duration.between(startedAt, completedAt).getSeconds());
    }

    private void scheduleEvaluationDispatch(Long sessionId, UUID userId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    speakingEvaluationDispatchService.dispatchEvaluation(sessionId, userId);
                }
            });
            return;
        }

        speakingEvaluationDispatchService.dispatchEvaluation(sessionId, userId);
    }

    private SpeakingSessionDTO toSessionDTO(SpeakingSession session, List<SpeakingTurnDTO> turns) {
        return SpeakingSessionDTO.builder()
                .sessionId(session.getId())
                .sessionMode(session.getSessionMode())
                .testId(session.getTestId())
                .status(session.getStatus())
                .isFinalized(session.getIsFinalized())
                .luaCost(session.getLuaCost())
                .accent(session.getAccent())
                .speed(session.getSpeed())
                .startedAt(session.getStartedAt())
                .completedAt(session.getCompletedAt())
                .gradedAt(session.getGradedAt())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .sessionBlueprint(sanitizeBlueprintForClient(session.getSessionBlueprint()))
                .turns(turns)
                .build();
    }

    private SpeakingHistoryItemDTO toHistoryItemDTO(SpeakingSession session) {
        return SpeakingHistoryItemDTO.builder()
                .sessionId(session.getId())
                .testId(session.getTestId())
                .sessionMode(session.getSessionMode())
                .status(session.getStatus())
                .overallBand(session.getOverallBand())
                .createdAt(session.getCreatedAt())
                .completedAt(session.getCompletedAt())
                .build();
    }

    private List<SpeakingTurnDTO> extractTurns(JsonNode sessionBlueprint) {
        List<SpeakingTurnDTO> turns = new ArrayList<>();
        if (sessionBlueprint == null || !sessionBlueprint.has("parts") || !sessionBlueprint.get("parts").isArray()) {
            return turns;
        }

        for (JsonNode partNode : sessionBlueprint.get("parts")) {
            Integer partNumber = partNode.hasNonNull("partNumber") ? partNode.get("partNumber").asInt() : null;
            JsonNode turnNodes = partNode.get("turns");
            if (partNumber == null || turnNodes == null || !turnNodes.isArray()) {
                continue;
            }
            for (JsonNode turnNode : turnNodes) {
                if (!turnNode.hasNonNull("turnIndex") || !turnNode.hasNonNull("sourceQuestionId")
                        || !turnNode.hasNonNull("questionSnapshot")) {
                    continue;
                }
                turns.add(SpeakingTurnDTO.builder()
                        .turnIndex(turnNode.get("turnIndex").asInt())
                        .partNumber(partNumber)
                        .sourceQuestionId(turnNode.get("sourceQuestionId").asLong())
                        .questionSnapshot(copyJson(turnNode.get("questionSnapshot")))
                        .build());
            }
        }

        turns.sort((left, right) -> Integer.compare(left.getTurnIndex(), right.getTurnIndex()));
        return turns;
    }

    private ExpectedTurn findExpectedTurn(JsonNode sessionBlueprint, Integer turnIndex) {
        return extractTurns(sessionBlueprint).stream()
                .filter(turn -> Objects.equals(turn.getTurnIndex(), turnIndex))
                .findFirst()
                .map(turn -> new ExpectedTurn(
                        turn.getTurnIndex(),
                        turn.getPartNumber(),
                        turn.getSourceQuestionId(),
                        copyJson(turn.getQuestionSnapshot())))
                .orElse(null);
    }

    private JsonNode copyJson(JsonNode node) {
        return node == null ? null : node.deepCopy();
    }

    private JsonNode sanitizeBlueprintForClient(JsonNode sessionBlueprint) {
        JsonNode copy = copyJson(sessionBlueprint);
        if (copy instanceof ObjectNode objectNode) {
            objectNode.remove("_internal");
        }
        return copy;
    }

    private String resolveProgressMessage(SpeakingSession session) {
        JsonNode gradingResult = session.getGradingResult();
        if (gradingResult != null) {
            if (gradingResult.hasNonNull("progress")) {
                return gradingResult.get("progress").asText();
            }
            if (gradingResult.hasNonNull("message")) {
                return gradingResult.get("message").asText();
            }
            if ("grading_failed".equals(session.getStatus()) && gradingResult.hasNonNull("error")) {
                return gradingResult.get("error").asText();
            }
        }

        return switch (session.getStatus()) {
            case "completed" -> "Session completed. Waiting for evaluation.";
            case "grading" -> "Speaking evaluation is in progress.";
            case "graded" -> "Speaking evaluation is complete.";
            case "grading_failed" -> "Speaking evaluation failed.";
            case "abandoned" -> "Session was abandoned.";
            case "expired" -> "Session expired before completion.";
            default -> "Session is in progress.";
        };
    }

    private Integer resolveEstimatedSeconds(SpeakingSession session) {
        JsonNode gradingResult = session.getGradingResult();
        if (gradingResult != null && gradingResult.hasNonNull("estimatedSeconds")
                && gradingResult.get("estimatedSeconds").canConvertToInt()) {
            return gradingResult.get("estimatedSeconds").asInt();
        }

        return switch (session.getStatus()) {
            case "completed", "grading" -> 30;
            case "graded", "grading_failed" -> 0;
            default -> null;
        };
    }

    private String normalizeSessionMode(String sessionMode) {
        if (sessionMode == null || sessionMode.isBlank()) {
            throw new IllegalArgumentException("sessionMode is required");
        }
        String normalized = sessionMode.trim().toUpperCase(Locale.ROOT);
        if (!List.of("FULL", "PART_1", "PART_2", "PART_3").contains(normalized)) {
            throw new IllegalArgumentException("sessionMode must be one of FULL, PART_1, PART_2, PART_3");
        }
        return normalized;
    }

    private String normalizeAccent(String accent) {
        if (accent == null || accent.isBlank()) {
            throw new IllegalArgumentException("accent is required");
        }
        String normalized = accent.trim().toLowerCase(Locale.ROOT);
        if (!List.of("british", "american", "australian", "neutral").contains(normalized)) {
            throw new IllegalArgumentException("accent must be one of british, american, australian, neutral");
        }
        return normalized;
    }

    private BigDecimal normalizeSpeed(BigDecimal speed) {
        if (speed == null) {
            throw new IllegalArgumentException("speed is required");
        }
        for (BigDecimal allowedSpeed : List.of(new BigDecimal("0.85"), new BigDecimal("1.00"), new BigDecimal("1.15"))) {
            if (speed.compareTo(allowedSpeed) == 0) {
                return allowedSpeed;
            }
        }
        throw new IllegalArgumentException("speed must be one of 0.85, 1.00, 1.15");
    }

    private String normalizeStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (!List.of("in_progress", "completed", "grading", "graded", "grading_failed", "abandoned", "expired")
                .contains(normalized)) {
            throw new IllegalArgumentException(
                    "status must be one of in_progress, completed, grading, graded, grading_failed, abandoned, expired");
        }
        return normalized;
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeAudioStoragePath(String rawAudioStoragePath) {
        if (rawAudioStoragePath == null) {
            throw new IllegalArgumentException("audioStoragePath must not be null");
        }

        String normalized = rawAudioStoragePath.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("audioStoragePath must not be empty");
        }
        if (normalized.startsWith("/")) {
            throw new IllegalArgumentException("audioStoragePath must not be an absolute path");
        }
        if (normalized.contains("..")) {
            throw new IllegalArgumentException("audioStoragePath must not contain '..' segments");
        }
        if (normalized.contains("\\")) {
            throw new IllegalArgumentException("audioStoragePath must not contain backslashes");
        }
        if (URL_SCHEME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("audioStoragePath must not contain a URL scheme");
        }

        return normalized;
    }

    private record ExpectedTurn(int turnIndex, int partNumber, Long sourceQuestionId, JsonNode questionSnapshot) {
    }
}
