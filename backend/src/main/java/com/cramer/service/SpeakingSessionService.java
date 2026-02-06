package com.cramer.service;

import com.cramer.dto.*;
import com.cramer.entity.CreditTransaction;
import com.cramer.entity.SpeakingSession;
import com.cramer.entity.SpeakingTopic;
import com.cramer.entity.SpeakingTranscript;
import com.cramer.repository.SpeakingSessionRepository;
import com.cramer.repository.SpeakingTopicRepository;
import com.cramer.repository.SpeakingTranscriptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing speaking sessions.
 */
@Service
public class SpeakingSessionService {

    private static final Logger logger = LoggerFactory.getLogger(SpeakingSessionService.class);

    @Value("${speaking.session.lua-cost:15}")
    private int defaultLuaCost;

    private final SpeakingSessionRepository sessionRepository;
    private final SpeakingTopicRepository topicRepository;
    private final SpeakingTranscriptRepository transcriptRepository;
    private final SpeakingQuestionService questionService;
    private final CreditService creditService;
    private final GroqWhisperService groqWhisperService;

    public SpeakingSessionService(SpeakingSessionRepository sessionRepository,
                                  SpeakingTopicRepository topicRepository,
                                  SpeakingTranscriptRepository transcriptRepository,
                                  SpeakingQuestionService questionService,
                                  CreditService creditService,
                                  GroqWhisperService groqWhisperService) {
        this.sessionRepository = sessionRepository;
        this.topicRepository = topicRepository;
        this.transcriptRepository = transcriptRepository;
        this.questionService = questionService;
        this.creditService = creditService;
        this.groqWhisperService = groqWhisperService;
    }

    /**
     * Create a new speaking session.
     */
    @Transactional
    public SpeakingSessionDTO createSession(UUID userId, String mode, Long topicId) {
        logger.info("Creating speaking session for user {} with mode {} and topic {}", userId, mode, topicId);

        // Validate mode
        validateSessionMode(mode);

        // Calculate Lúa cost based on mode
        int luaCost = calculateLuaCost(mode);

        // Check if user has enough Lúa credits
        if (!creditService.hasEnoughCredits(userId, luaCost)) {
            throw new IllegalStateException("Insufficient Lúa credits. Required: " + luaCost);
        }

        // Validate topic if provided
        SpeakingTopic topic = null;
        if (topicId != null) {
            topic = topicRepository.findById(topicId)
                    .filter(SpeakingTopic::getIsActive)
                    .orElseThrow(() -> new IllegalArgumentException("Topic not found or inactive: " + topicId));
        }

        // Create session
        SpeakingSession session = new SpeakingSession(userId, mode.toUpperCase(), topicId, luaCost);
        session = sessionRepository.save(session);

        // Get questions for the session
        List<SpeakingQuestionDTO> questions = questionService.getQuestionsForSession(topicId, mode);

        // Build response DTO
        SpeakingSessionDTO dto = new SpeakingSessionDTO();
        dto.setSessionId(session.getId());
        dto.setStatus(session.getStatus());
        dto.setSessionMode(session.getSessionMode());
        dto.setTopicId(topicId);
        dto.setQuestions(questions);
        dto.setLuaCost(luaCost);
        dto.setStartedAt(session.getStartedAt());

        if (topic != null) {
            dto.setTopic(new SpeakingTopicDTO(
                    topic.getId(),
                    topic.getCode(),
                    topic.getNameVi(),
                    topic.getNameEn(),
                    topic.getIcon(),
                    topic.getColor()
            ));
        }

        logger.info("Created speaking session {} with {} questions", session.getId(), questions.size());
        return dto;
    }

    /**
     * Save a transcript for a question in a session.
     */
    @Transactional
    public SpeakingTranscriptDTO saveTranscript(Long sessionId, Long questionId, Integer part,
                                                 String audioUrl, Integer duration, String transcriptText,
                                                 UUID userId) {
        logger.info("Saving transcript for session {} question {} part {}", sessionId, questionId, part);

        // Verify session belongs to user and is in progress
        SpeakingSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found or unauthorized"));

        if (!"in_progress".equals(session.getStatus())) {
            throw new IllegalStateException("Session is not in progress");
        }

        // Create or update transcript
        SpeakingTranscript transcript = transcriptRepository.findBySessionIdAndQuestionId(sessionId, questionId)
                .orElse(new SpeakingTranscript(sessionId, questionId, part));

        transcript.setAudioUrl(audioUrl);
        transcript.setAudioDurationSeconds(duration);
        transcript.setTranscriptText(transcriptText);
        transcript.setRecordedAt(OffsetDateTime.now());

        transcript = transcriptRepository.save(transcript);

        // Transcribe audio if audioUrl provided and no transcript text yet
        String finalTranscriptText = transcriptText;
        if (audioUrl != null && !audioUrl.isBlank() && (transcriptText == null || transcriptText.isBlank())) {
            try {
                logger.info("Transcribing audio for session {} question {}", sessionId, questionId);
                String transcribed = groqWhisperService.transcribeAudio(audioUrl);
                if (transcribed != null && !transcribed.isBlank()) {
                    transcript.setTranscriptText(transcribed);
                    transcript = transcriptRepository.save(transcript);
                    finalTranscriptText = transcribed;
                    logger.info("Transcription successful for session {} question {}", sessionId, questionId);
                }
            } catch (Exception e) {
                logger.warn("Transcription failed for session {} question {}: {}", sessionId, questionId, e.getMessage());
                // Continue without transcription - not a fatal error
            }
        }

        // Build response
        SpeakingTranscriptDTO dto = new SpeakingTranscriptDTO();
        dto.setTranscriptId(transcript.getId());
        dto.setQuestionId(questionId);
        dto.setPart(part);
        dto.setAudioUrl(audioUrl);
        dto.setAudioDurationSeconds(duration);
        dto.setTranscriptText(finalTranscriptText);
        dto.setRecordedAt(transcript.getRecordedAt());

        logger.info("Saved transcript {} for session {}", transcript.getId(), sessionId);
        return dto;
    }

    /**
     * Complete a speaking session and trigger evaluation.
     */
    @Transactional
    public void completeSession(Long sessionId, UUID userId) {
        logger.info("Completing speaking session {}", sessionId);

        SpeakingSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found or unauthorized"));

        if (!"in_progress".equals(session.getStatus())) {
            throw new IllegalStateException("Session is not in progress");
        }

        // Calculate total duration
        Integer totalDuration = transcriptRepository.getTotalAudioDuration(sessionId);

        // Update session
        session.setStatus("completed");
        session.setCompletedAt(OffsetDateTime.now());
        session.setTotalDurationSeconds(totalDuration);

        // Deduct Lúa credits if not already deducted
        if (!Boolean.TRUE.equals(session.getLuaDeducted())) {
            creditService.spendCredits(userId, session.getLuaCost(),
                    CreditTransaction.Category.SPEAKING_SESSION, "Speaking session #" + sessionId);
            session.setLuaDeducted(true);
        }

        sessionRepository.save(session);
        logger.info("Completed speaking session {} with duration {}s", sessionId, totalDuration);
    }

    /**
     * Abandon a speaking session (user cancelled).
     */
    @Transactional
    public void abandonSession(Long sessionId, UUID userId) {
        logger.info("Abandoning speaking session {}", sessionId);

        SpeakingSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found or unauthorized"));

        if (!"in_progress".equals(session.getStatus())) {
            throw new IllegalStateException("Session is not in progress");
        }

        session.setStatus("abandoned");
        sessionRepository.save(session);

        logger.info("Abandoned speaking session {}", sessionId);
    }

    /**
     * Get session by ID for a user.
     */
    @Transactional(readOnly = true)
    public SpeakingSessionDTO getSession(Long sessionId, UUID userId) {
        SpeakingSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found or unauthorized"));

        return toSessionDTO(session);
    }

    /**
     * Get user's session history.
     */
    @Transactional(readOnly = true)
    public List<SpeakingSessionDTO> getUserSessionHistory(UUID userId) {
        return sessionRepository.findCompletedSessionsByUser(userId).stream()
                .map(this::toSessionDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get transcripts for a session.
     */
    @Transactional(readOnly = true)
    public List<SpeakingTranscriptDTO> getSessionTranscripts(Long sessionId, UUID userId) {
        // Verify access
        sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found or unauthorized"));

        return transcriptRepository.findBySessionIdOrderByPartAscCreatedAtAsc(sessionId).stream()
                .map(this::toTranscriptDTO)
                .collect(Collectors.toList());
    }

    // Helper methods
    private void validateSessionMode(String mode) {
        String upperMode = mode.toUpperCase();
        if (!List.of("FULL", "PART_1", "PART_2", "PART_3", "PART_2_3").contains(upperMode)) {
            throw new IllegalArgumentException("Invalid session mode: " + mode);
        }
    }

    private int calculateLuaCost(String mode) {
        // Different modes have different costs
        return switch (mode.toUpperCase()) {
            case "FULL" -> defaultLuaCost;
            case "PART_1" -> defaultLuaCost / 3;
            case "PART_2" -> defaultLuaCost / 3;
            case "PART_3" -> defaultLuaCost / 3;
            case "PART_2_3" -> (defaultLuaCost * 2) / 3;
            default -> defaultLuaCost;
        };
    }

    private SpeakingSessionDTO toSessionDTO(SpeakingSession session) {
        SpeakingSessionDTO dto = new SpeakingSessionDTO();
        dto.setSessionId(session.getId());
        dto.setStatus(session.getStatus());
        dto.setSessionMode(session.getSessionMode());
        dto.setTopicId(session.getTopicId());
        dto.setLuaCost(session.getLuaCost());
        dto.setStartedAt(session.getStartedAt());
        dto.setCompletedAt(session.getCompletedAt());
        dto.setTotalDurationSeconds(session.getTotalDurationSeconds());
        dto.setOverallBand(session.getOverallBand());
        dto.setFluencyBand(session.getFluencyBand());
        dto.setLexicalBand(session.getLexicalBand());
        dto.setGrammarBand(session.getGrammarBand());
        dto.setPronunciationBand(session.getPronunciationBand());

        // Load topic if present
        if (session.getTopicId() != null) {
            topicRepository.findById(session.getTopicId()).ifPresent(topic ->
                    dto.setTopic(new SpeakingTopicDTO(
                            topic.getId(),
                            topic.getCode(),
                            topic.getNameVi(),
                            topic.getNameEn(),
                            topic.getIcon(),
                            topic.getColor()
                    ))
            );
        }

        return dto;
    }

    private SpeakingTranscriptDTO toTranscriptDTO(SpeakingTranscript transcript) {
        SpeakingTranscriptDTO dto = new SpeakingTranscriptDTO();
        dto.setTranscriptId(transcript.getId());
        dto.setQuestionId(transcript.getQuestionId());
        dto.setPart(transcript.getPart());
        dto.setAudioUrl(transcript.getAudioUrl());
        dto.setAudioDurationSeconds(transcript.getAudioDurationSeconds());
        dto.setTranscriptText(transcript.getTranscriptText());
        dto.setTranscriptConfidence(transcript.getTranscriptConfidence());
        dto.setRecordedAt(transcript.getRecordedAt());
        return dto;
    }
}
