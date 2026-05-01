package com.cramer.service.implement;

import com.cramer.dto.SpeakingGradingResultV2DTO;
import com.cramer.dto.SpeakingTranscriptDTO;
import com.cramer.dto.SpeakingTurnDTO;
import com.cramer.entity.CreditTransaction;
import com.cramer.entity.SpeakingSession;
import com.cramer.entity.SpeakingTranscript;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.repository.SpeakingSessionRepository;
import com.cramer.repository.SpeakingTranscriptRepository;
import com.cramer.service.CreditService;
import com.cramer.service.abts.OpenRouterClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SpeakingGradingWorker {

    private static final Logger logger = LoggerFactory.getLogger(SpeakingGradingWorker.class);

    @Value("${speaking.evaluation.retry.max-attempts:2}")
    private int maxRetryAttempts;

    private final SpeakingSessionRepository speakingSessionRepository;
    private final SpeakingTranscriptRepository speakingTranscriptRepository;
    private final SpeakingAudioPreparer speakingAudioPreparer;
    private final SpeakingGradingPromptBuilder speakingGradingPromptBuilder;
    private final OpenRouterClient openRouterClient;
    private final CreditService creditService;
    private final ObjectMapper objectMapper;

    public SpeakingGradingWorker(
            SpeakingSessionRepository speakingSessionRepository,
            SpeakingTranscriptRepository speakingTranscriptRepository,
            SpeakingAudioPreparer speakingAudioPreparer,
            SpeakingGradingPromptBuilder speakingGradingPromptBuilder,
            OpenRouterClient openRouterClient,
            CreditService creditService) {
        this.speakingSessionRepository = speakingSessionRepository;
        this.speakingTranscriptRepository = speakingTranscriptRepository;
        this.speakingAudioPreparer = speakingAudioPreparer;
        this.speakingGradingPromptBuilder = speakingGradingPromptBuilder;
        this.openRouterClient = openRouterClient;
        this.creditService = creditService;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public void gradeSession(Long sessionId, UUID userId) {
        long startTime = System.currentTimeMillis();
        try {
            doGradeSession(sessionId, startTime);
        } catch (Exception e) {
            logger.error("metric=speaking_grading_worker_crash sessionId={} error={}",
                    sessionId, truncate(e.getMessage(), 500), e);
        }
    }

    private void doGradeSession(Long sessionId, long startTime) {
        SpeakingSession session = speakingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("SpeakingSession", "id", sessionId));

        UUID sessionOwnerId = session.getUserId();

        if (!"completed".equals(session.getStatus())) {
            logger.warn("metric=speaking_grading_skipped sessionId={} status={}", sessionId, session.getStatus());
            return;
        }

        SpeakingSessionStatusTransitioner.transitionTo(session.getStatus(), "grading");
        session.setStatus("grading");
        session = speakingSessionRepository.save(session);

        session.setGradingResult(objectMapper.createObjectNode()
                .put("progress", "Đang chuẩn bị chấm điểm...")
                .put("estimatedSeconds", 90));
        session = speakingSessionRepository.save(session);

        List<SpeakingTranscript> transcriptEntities = speakingTranscriptRepository
                .findBySessionIdOrderByTurnIndexAsc(sessionId);

        List<SpeakingTranscriptDTO> transcriptDTOs = transcriptEntities.stream()
                .filter(t -> t.getSessionId() != null && t.getSessionId().equals(sessionId))
                .map(this::toTranscriptDTO)
                .toList();

        List<SpeakingTurnDTO> turns = extractTurnsFromBlueprint(session.getSessionBlueprint());
        List<Integer> partsIncluded = extractPartsIncluded(session.getSessionBlueprint());
        String model = speakingGradingPromptBuilder.getDefaultModel();

        logger.info("metric=speaking_grading_started sessionId={} userId={} attempt=1 model={} mode={}",
                sessionId, sessionOwnerId, model, "multimodal");
        Map<String, Object> schemaMap = loadGradingSchema();

        boolean success = false;
        boolean textOnly = false;
        Exception lastException = null;
        OpenRouterClient.OpenRouterResponse lastResponse = null;

        int effectiveMaxAttempts = Math.max(1, maxRetryAttempts);
        for (int attempt = 0; attempt < effectiveMaxAttempts; attempt++) {
            textOnly = (attempt > 0);
            try {
                List<SpeakingAudioPreparer.PreparedAudio> audios = null;
                if (!textOnly) {
                    session.setGradingResult(objectMapper.createObjectNode()
                            .put("progress", "Đang tải audio..."));
                    session = speakingSessionRepository.save(session);

                    audios = speakingAudioPreparer.prepare(transcriptDTOs);
                }

                String systemPrompt = speakingGradingPromptBuilder.buildSystemPrompt(
                        session.getSessionMode(), partsIncluded);
                List<OpenRouterClient.ContentPart> parts = speakingGradingPromptBuilder.buildUserContent(
                        turns, transcriptDTOs, audios, session.getSessionBlueprint(), textOnly);

                String promptHash = sha256(systemPrompt + parts.stream()
                        .map(p -> p instanceof OpenRouterClient.TextPart tp ? tp.text() : "[audio]")
                        .collect(Collectors.joining()));

                session.setGradingResult(objectMapper.createObjectNode()
                        .put("progress", "Đang gọi model AI...")
                        .put("estimatedSeconds", 60));
                session = speakingSessionRepository.save(session);

                OpenRouterClient.OpenRouterResponse response = openRouterClient.callWithAudio(
                        systemPrompt, parts, model, schemaMap, "speaking_grading_v2", 16384, "deny");

                lastResponse = response;

                String content = response.getContent();
                if (content == null || content.isBlank()) {
                    throw new IllegalStateException("OpenRouter returned empty content for speaking grading");
                }
                content = stripMarkdownCodeFences(content.trim());

                SpeakingGradingResultV2DTO result = objectMapper.readValue(content, SpeakingGradingResultV2DTO.class);
                if (result.getSchemaVersion() == null) {
                    result.setSchemaVersion("2.0");
                }

                validateHalfStepBands(result);

                session.setGradingResult(objectMapper.createObjectNode()
                        .put("progress", "Đang lưu kết quả..."));
                session = speakingSessionRepository.save(session);

                persistGradingResult(session, result, response, audios, model, promptHash, textOnly);

                long latencyMs = System.currentTimeMillis() - startTime;
                String finishReason = "stop"; // OpenRouterResponse does not currently expose finish_reason
                logger.info("metric=speaking_grading_completed sessionId={} outcome=graded latencyMs={} tokensIn={} tokensOut={} model={} finishReason={}",
                        sessionId, latencyMs, response.getPromptTokens(), response.getCompletionTokens(),
                        response.getModelUsed(), finishReason);

                success = true;
                break;

            } catch (Exception e) {
                lastException = e;

                session.setGradingResult(objectMapper.createObjectNode()
                        .putNull("progress")
                        .put("error", "Grading failed, retrying..."));
                session = speakingSessionRepository.save(session);

                int currentAttempts = session.getGradingAttempts() != null ? session.getGradingAttempts() + 1 : 1;
                session.setGradingAttempts(currentAttempts);
                session.setLastGradingError(truncate(e.getMessage(), 500));

                if (attempt == 0 && effectiveMaxAttempts > 1) {
                    logger.warn("metric=speaking_grading_retry sessionId={} attempt={} errorClass={} error={}",
                            sessionId, attempt + 1, e.getClass().getSimpleName(), truncate(e.getMessage(), 500));
                }

                session = speakingSessionRepository.save(session);
            }
        }

        if (!success) {
            int currentAttempts = session.getGradingAttempts() != null ? session.getGradingAttempts() + 1 : 1;
            SpeakingSessionStatusTransitioner.transitionTo(session.getStatus(), "grading_failed");
            session.setStatus("grading_failed");
            session.setGradingAttempts(currentAttempts);
            session.setLastGradingError(lastException != null
                    ? truncate(lastException.getMessage(), 500) : "Unknown error");
            session.setGradedAt(OffsetDateTime.now());
            speakingSessionRepository.save(session);

            Integer luaCost = session.getLuaCost();
            if (luaCost != null && luaCost > 0 && Boolean.TRUE.equals(session.getLuaDeducted())) {
                creditService.refundCredits(sessionOwnerId, luaCost,
                        CreditTransaction.Category.SPEAKING_REFUND,
                        "Speaking grading failure refund",
                        "refund_session_" + sessionId);
                logger.info("metric=speaking_grading_refund sessionId={} amount={}", sessionId, luaCost);
            }

            Integer refundAmount = (luaCost != null && luaCost > 0 && Boolean.TRUE.equals(session.getLuaDeducted())) ? luaCost : 0;
            logger.error("metric=speaking_grading_failed sessionId={} attempts={} refundAmount={} errorClass={} error={}",
                    sessionId, session.getGradingAttempts(), refundAmount,
                    lastException != null ? lastException.getClass().getSimpleName() : "Unknown",
                    truncate(lastException != null ? lastException.getMessage() : "Unknown", 500));
        }
    }

    private void persistGradingResult(
            SpeakingSession session,
            SpeakingGradingResultV2DTO result,
            OpenRouterClient.OpenRouterResponse response,
            List<SpeakingAudioPreparer.PreparedAudio> audios,
            String model,
            String promptHash,
            boolean textOnly) {

        session.setOverallBand(result.getOverallBand());

        if (result.getCriteria() != null) {
            if (result.getCriteria().getFluencyCoherence() != null) {
                session.setFluencyBand(result.getCriteria().getFluencyCoherence().getBand());
            }
            if (result.getCriteria().getLexicalResource() != null) {
                session.setLexicalBand(result.getCriteria().getLexicalResource().getBand());
            }
            if (result.getCriteria().getGrammaticalRangeAccuracy() != null) {
                session.setGrammarBand(result.getCriteria().getGrammaticalRangeAccuracy().getBand());
            }
            if (result.getCriteria().getPronunciation() != null) {
                session.setPronunciationBand(result.getCriteria().getPronunciation().getBand());
            }
        }

        Map<String, Object> resultMap = objectMapper.convertValue(result,
                new TypeReference<LinkedHashMap<String, Object>>() {});
        Map<String, Object> merged = new LinkedHashMap<>(resultMap);

        Map<String, Object> workerMeta = new LinkedHashMap<>();
        workerMeta.put("model", response.getModelUsed() != null ? response.getModelUsed() : model);
        workerMeta.put("gradingMode", textOnly ? "text_only" : "multimodal");
        workerMeta.put("promptHash", promptHash);
        if (audios != null) {
            Map<String, Object> audioMeta = new LinkedHashMap<>();
            audioMeta.put("count", audios.size());
            audioMeta.put("totalDurationSec", audios.stream()
                    .mapToInt(SpeakingAudioPreparer.PreparedAudio::durationSec).sum());
            workerMeta.put("audioMeta", audioMeta);
        }
        workerMeta.put("tokensIn", response.getPromptTokens());
        workerMeta.put("tokensOut", response.getCompletionTokens());
        workerMeta.put("totalTokens", response.getTotalTokens());
        workerMeta.put("latencyMs", response.getDurationMs());
        workerMeta.put("gradedAtIso", OffsetDateTime.now().toString());
        workerMeta.put("blueprintVersion", 1);
        merged.put("_worker", workerMeta);

        session.setGradingResult(objectMapper.valueToTree(merged));
        SpeakingSessionStatusTransitioner.transitionTo(session.getStatus(), "graded");
        session.setStatus("graded");
        session.setGradedAt(OffsetDateTime.now());

        speakingSessionRepository.save(session);

        String gradingMode = textOnly ? "text_only" : "multimodal";
        logger.info("metric=speaking_grading_persisted sessionId={} overallBand={} gradingMode={} attempts={}",
                session.getId(), result.getOverallBand(), gradingMode, session.getGradingAttempts());
    }

    private void validateHalfStepBands(SpeakingGradingResultV2DTO result) {
        validateBand(result.getOverallBand(), "overallBand");
        if (result.getCriteria() != null) {
            if (result.getCriteria().getFluencyCoherence() != null) {
                validateBand(result.getCriteria().getFluencyCoherence().getBand(), "fluencyCoherence");
            }
            if (result.getCriteria().getLexicalResource() != null) {
                validateBand(result.getCriteria().getLexicalResource().getBand(), "lexicalResource");
            }
            if (result.getCriteria().getGrammaticalRangeAccuracy() != null) {
                validateBand(result.getCriteria().getGrammaticalRangeAccuracy().getBand(), "grammaticalRangeAccuracy");
            }
            if (result.getCriteria().getPronunciation() != null) {
                validateBand(result.getCriteria().getPronunciation().getBand(), "pronunciation");
            }
        }
    }

    public static boolean isValidBand(BigDecimal band) {
        if (band == null) return true;
        double raw = band.doubleValue();
        double times2 = raw * 2.0;
        if (Math.abs(times2 - Math.round(times2)) > 0.001) return false;
        if (raw < 0.0 || raw > 9.0) return false;
        return true;
    }

    private void validateBand(BigDecimal band, String name) {
        if (band == null) {
            throw new IllegalArgumentException(name + " is null in grading response");
        }
        double raw = band.doubleValue();
        double times2 = raw * 2.0;
        if (Math.abs(times2 - Math.round(times2)) > 0.001) {
            throw new IllegalArgumentException(
                    name + "=" + band + " is not a half-step band (0.5 increments required)");
        }
        if (raw < 0.0 || raw > 9.0) {
            throw new IllegalArgumentException(
                    name + "=" + band + " is out of range [0.0, 9.0]");
        }
    }

    private List<SpeakingTurnDTO> extractTurnsFromBlueprint(JsonNode sessionBlueprint) {
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
                        .questionSnapshot(turnNode.get("questionSnapshot").deepCopy())
                        .build());
            }
        }

        turns.sort((left, right) -> Integer.compare(left.getTurnIndex(), right.getTurnIndex()));
        return turns;
    }

    private List<Integer> extractPartsIncluded(JsonNode sessionBlueprint) {
        List<Integer> parts = new ArrayList<>();
        if (sessionBlueprint == null || !sessionBlueprint.has("parts") || !sessionBlueprint.get("parts").isArray()) {
            return List.of(1, 2, 3);
        }
        for (JsonNode partNode : sessionBlueprint.get("parts")) {
            if (partNode.hasNonNull("partNumber")) {
                parts.add(partNode.get("partNumber").asInt());
            }
        }
        return parts.isEmpty() ? List.of(1, 2, 3) : parts.stream().sorted().toList();
    }

    private SpeakingTranscriptDTO toTranscriptDTO(SpeakingTranscript entity) {
        return SpeakingTranscriptDTO.builder()
                .transcriptId(entity.getId())
                .sessionId(entity.getSessionId())
                .turnIndex(entity.getTurnIndex())
                .recordedAt(entity.getRecordedAt())
                .audioStoragePath(entity.getAudioStoragePath())
                .audioDurationSeconds(entity.getAudioDurationSeconds())
                .transcriptText(entity.getTranscriptText())
                .transcriptConfidence(entity.getTranscriptConfidence())
                .questionEvaluation(entity.getQuestionEvaluation())
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadGradingSchema() {
        try {
            InputStream is = getClass().getClassLoader()
                    .getResourceAsStream("speaking/grading_json_schema.json");
            if (is == null) {
                throw new IllegalStateException("grading_json_schema.json not found on classpath");
            }
            JsonNode root = objectMapper.readTree(is);
            JsonNode schemaNode = root.path("json_schema").path("schema");
            if (schemaNode.isMissingNode()) {
                throw new IllegalStateException(
                        "Invalid grading_json_schema.json: missing json_schema.schema");
            }
            return objectMapper.convertValue(schemaNode, Map.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load grading_json_schema.json: " + e.getMessage(), e);
        }
    }

    private String stripMarkdownCodeFences(String content) {
        String stripped = content;
        if (stripped.startsWith("```")) {
            int newlineIdx = stripped.indexOf('\n');
            if (newlineIdx > 0) {
                stripped = stripped.substring(newlineIdx + 1);
            } else {
                stripped = stripped.substring(3);
            }
        }
        if (stripped.endsWith("```")) {
            stripped = stripped.substring(0, stripped.length() - 3).trim();
        }
        return stripped;
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            return "unknown";
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}
