package com.cramer.service;

import com.cramer.dto.SpeakingGradingResultDTO;
import com.cramer.dto.SpeakingResultDTO;
import com.cramer.entity.SpeakingSession;
import com.cramer.entity.SpeakingTranscript;
import com.cramer.repository.SpeakingQuestionRepository;
import com.cramer.repository.SpeakingSessionRepository;
import com.cramer.repository.SpeakingTranscriptRepository;
import com.cramer.service.abts.OpenRouterClient;
import com.cramer.service.abts.OpenRouterClient.AudioInput;
import com.cramer.service.abts.OpenRouterClient.AudioPart;
import com.cramer.service.abts.OpenRouterClient.ContentPart;
import com.cramer.service.abts.OpenRouterClient.TextPart;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service for evaluating speaking sessions using OpenRouter + Gemini AI.
 * Handles multimodal audio analysis for IELTS Speaking assessment.
 * 
 * Model selection (from speaking_session_foundations_vi.md §2.5):
 * - Multimodal audio analysis requires models with audio input capability
 * - Currently using Gemini 2.5 Flash for best multimodal performance
 * - DeepSeek Reasoner cannot be used here as it doesn't support audio input
 * - Pronunciation analysis ideally uses Gemini 3 Pro (when available)
 */
@Service
public class SpeakingEvaluationService {

    private static final Logger logger = LoggerFactory.getLogger(SpeakingEvaluationService.class);

    // Default model for multimodal audio analysis
    // Must be a model that supports audio input (Gemini family)
    private static final String DEFAULT_EVAL_MODEL = "google/gemini-2.5-flash";

    @Value("${speaking.evaluation.model:google/gemini-2.5-flash}")
    private String evaluationModel;

    private static final String SYSTEM_PROMPT = """
        You are an experienced IELTS Speaking examiner with 15+ years of experience.

        Your task is to grade an IELTS Speaking test based on the official IELTS Speaking Band Descriptors.

        ## Scoring Criteria (each 0-9, can use .5 increments):

        1. **Fluency and Coherence (FC)**:
           - Speech rate and flow
           - Hesitation and self-correction
           - Coherence and logical organization
           - Use of discourse markers

        2. **Lexical Resource (LR)**:
           - Vocabulary range and precision
           - Idiomatic language use
           - Ability to paraphrase
           - Word formation accuracy

        3. **Grammatical Range and Accuracy (GRA)**:
           - Variety of sentence structures
           - Accuracy of grammar
           - Error frequency and impact

        4. **Pronunciation (P)**:
           - Individual sounds clarity
           - Stress and intonation patterns
           - Rhythm and connected speech
           - Accent acceptability

        ## Response Requirements:
        - Analyze BOTH the audio quality AND content
        - Provide specific examples from the candidate's speech
        - Be constructive but honest in feedback
        - Overall band = average of 4 criteria (rounded to nearest 0.5)

        Listen carefully to ALL audio recordings before providing your assessment.
        """;

    private final OpenRouterClient openRouterClient;
    private final SpeakingSessionRepository sessionRepository;
    private final SpeakingTranscriptRepository transcriptRepository;
    private final SpeakingQuestionRepository questionRepository;
    private final SupabaseStorageService storageService;
    private final ObjectMapper objectMapper;

    public SpeakingEvaluationService(OpenRouterClient openRouterClient,
                                     SpeakingSessionRepository sessionRepository,
                                     SpeakingTranscriptRepository transcriptRepository,
                                     SpeakingQuestionRepository questionRepository,
                                     SupabaseStorageService storageService,
                                     ObjectMapper objectMapper) {
        this.openRouterClient = openRouterClient;
        this.sessionRepository = sessionRepository;
        this.transcriptRepository = transcriptRepository;
        this.questionRepository = questionRepository;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
    }

    /**
     * Evaluate a completed speaking session asynchronously.
     */
    @Async
    public CompletableFuture<SpeakingGradingResultDTO> evaluateSessionAsync(Long sessionId) {
        logger.info("Starting async evaluation for session {}", sessionId);

        try {
            SpeakingGradingResultDTO result = gradeSession(sessionId);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            logger.error("Failed to evaluate session {}: {}", sessionId, e.getMessage(), e);

            // Mark session as failed
            sessionRepository.findById(sessionId).ifPresent(session -> {
                session.setStatus("failed");
                sessionRepository.save(session);
            });

            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Grade a complete speaking session using OpenRouter + Gemini multimodal.
     */
    @Transactional
    public SpeakingGradingResultDTO gradeSession(Long sessionId) {
        logger.info("Starting grading for session: {}", sessionId);

        // 1. Load session and transcripts
        SpeakingSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        if (!"completed".equals(session.getStatus())) {
            throw new IllegalStateException("Session is not completed: " + session.getStatus());
        }

        List<SpeakingTranscript> transcripts = transcriptRepository
            .findBySessionIdOrderByPartAscCreatedAtAsc(sessionId);

        if (transcripts.isEmpty()) {
            throw new IllegalStateException("No transcripts found for session");
        }

        // Update status to grading
        session.setStatus("grading");
        sessionRepository.save(session);

        try {
            // 2. Build multimodal content (text + audio)
            List<ContentPart> contentParts = buildMultimodalContent(transcripts);

            // 3. Add grading instruction
            contentParts.add(new TextPart(
                "Please analyze all the audio recordings above and provide your IELTS Speaking assessment."
            ));

            // 4. Call Gemini via OpenRouter (using configurable model)
            String model = evaluationModel != null ? evaluationModel : DEFAULT_EVAL_MODEL;
            logger.info("Using evaluation model: {}", model);
            
            OpenRouterClient.OpenRouterResponse response = openRouterClient.callWithAudio(
                SYSTEM_PROMPT,
                contentParts,
                model,
                buildJsonSchema(),
                "speaking_grading_result"
            );

            // 5. Parse response
            String jsonContent = response.getContent();
            SpeakingGradingResultDTO result = parseGradingResult(jsonContent);

            // 6. Save to session
            saveGradingResult(session, result, jsonContent);

            logger.info("Grading completed for session: {}, overall band: {}",
                sessionId, result.getOverallBand());

            return result;

        } catch (Exception e) {
            // Mark as failed on error
            session.setStatus("failed");
            sessionRepository.save(session);
            throw new RuntimeException("Grading failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get evaluation results for a session.
     */
    @Transactional(readOnly = true)
    public SpeakingResultDTO getResults(Long sessionId, UUID userId) {
        SpeakingSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to session");
        }

        List<SpeakingTranscript> transcripts = transcriptRepository
            .findBySessionIdOrderByPartAscCreatedAtAsc(sessionId);

        return buildResultDTO(session, transcripts);
    }

    // Private helper methods

    private List<ContentPart> buildMultimodalContent(List<SpeakingTranscript> transcripts) {
        List<ContentPart> contentParts = new ArrayList<>();

        for (SpeakingTranscript transcript : transcripts) {
            // Get question text
            String questionText = "";
            if (transcript.getQuestionId() != null) {
                questionText = questionRepository.findById(transcript.getQuestionId())
                    .map(q -> q.getQuestionText())
                    .orElse("Question " + transcript.getQuestionId());
            }

            // Add question text
            String questionLabel = String.format(
                "Part %d, Question: %s",
                transcript.getPart(),
                questionText
            );
            contentParts.add(new TextPart(questionLabel));

            // Download and encode audio if available
            if (transcript.getAudioUrl() != null && !transcript.getAudioUrl().isEmpty()) {
                try {
                    byte[] audioBytes;

                    // Check if it's a full URL or storage path
                    if (transcript.getAudioUrl().startsWith("http")) {
                        audioBytes = storageService.downloadFromUrl(transcript.getAudioUrl());
                    } else {
                        audioBytes = storageService.download(transcript.getAudioUrl());
                    }

                    String base64Audio = Base64.getEncoder().encodeToString(audioBytes);
                    String format = SupabaseStorageService.extractFormat(transcript.getAudioUrl());

                    contentParts.add(new AudioPart(new AudioInput(base64Audio, format)));

                    logger.debug("Added audio for part {}, {} bytes, format: {}",
                        transcript.getPart(), audioBytes.length, format);

                } catch (Exception e) {
                    logger.warn("Failed to download audio for transcript {}: {}",
                        transcript.getId(), e.getMessage());

                    // Add transcript text as fallback
                    if (transcript.getTranscriptText() != null) {
                        contentParts.add(new TextPart(
                            "Candidate's response (transcript): " + transcript.getTranscriptText()
                        ));
                    }
                }
            } else if (transcript.getTranscriptText() != null) {
                // No audio URL, use transcript text
                contentParts.add(new TextPart(
                    "Candidate's response (transcript): " + transcript.getTranscriptText()
                ));
            }
        }

        return contentParts;
    }

    private Map<String, Object> buildJsonSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "overallBand", Map.of("type", "number", "minimum", 0, "maximum", 9),
                "fluencyCoherence", Map.of("type", "number", "minimum", 0, "maximum", 9),
                "lexicalResource", Map.of("type", "number", "minimum", 0, "maximum", 9),
                "grammaticalRange", Map.of("type", "number", "minimum", 0, "maximum", 9),
                "pronunciation", Map.of("type", "number", "minimum", 0, "maximum", 9),
                "feedback", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "strengths", Map.of(
                            "type", "array",
                            "items", Map.of("type", "string")
                        ),
                        "weaknesses", Map.of(
                            "type", "array",
                            "items", Map.of("type", "string")
                        ),
                        "recommendations", Map.of(
                            "type", "array",
                            "items", Map.of("type", "string")
                        ),
                        "fluencyNotes", Map.of("type", "string"),
                        "lexicalNotes", Map.of("type", "string"),
                        "grammarNotes", Map.of("type", "string"),
                        "pronunciationNotes", Map.of("type", "string")
                    ),
                    "required", List.of("strengths", "weaknesses", "recommendations")
                ),
                "partScores", Map.of(
                    "type", "array",
                    "items", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "partNumber", Map.of("type", "integer"),
                            "score", Map.of("type", "number"),
                            "notes", Map.of("type", "string")
                        )
                    )
                ),
                "transcript", Map.of(
                    "type", "string",
                    "description", "Full transcription of candidate's speech"
                )
            ),
            "required", List.of(
                "overallBand", "fluencyCoherence", "lexicalResource",
                "grammaticalRange", "pronunciation", "feedback"
            )
        );
    }

    private SpeakingGradingResultDTO parseGradingResult(String jsonContent) {
        try {
            // Clean up response - remove markdown code blocks if present
            String cleaned = jsonContent.trim();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
            }
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }

            return objectMapper.readValue(cleaned.trim(), SpeakingGradingResultDTO.class);
        } catch (Exception e) {
            logger.error("Failed to parse grading result: {}", e.getMessage());
            // Return mock result on parse failure
            return getMockGradingResult();
        }
    }

    private void saveGradingResult(SpeakingSession session, SpeakingGradingResultDTO result, String jsonContent) {
        // Update session with scores
        session.setOverallBand(toBigDecimal(result.getOverallBand()));
        session.setFluencyBand(toBigDecimal(result.getFluencyCoherence()));
        session.setLexicalBand(toBigDecimal(result.getLexicalResource()));
        session.setGrammarBand(toBigDecimal(result.getGrammaticalRange()));
        session.setPronunciationBand(toBigDecimal(result.getPronunciation()));

        // Store full result as JSONB
        try {
            Map<String, Object> gradingResult = objectMapper.readValue(jsonContent, Map.class);
            session.setGradingResult(gradingResult);
        } catch (Exception e) {
            logger.warn("Failed to parse grading result for storage: {}", e.getMessage());
        }

        // Update status and timestamps
        session.setStatus("graded");
        session.setGradedAt(OffsetDateTime.now());

        sessionRepository.save(session);
    }

    private BigDecimal toBigDecimal(Double value) {
        if (value == null) return null;
        return BigDecimal.valueOf(value).setScale(1, java.math.RoundingMode.HALF_UP);
    }

    private SpeakingGradingResultDTO getMockGradingResult() {
        SpeakingGradingResultDTO result = new SpeakingGradingResultDTO();
        result.setOverallBand(6.5);
        result.setFluencyCoherence(6.5);
        result.setLexicalResource(6.5);
        result.setGrammaticalRange(6.5);
        result.setPronunciation(6.5);

        SpeakingGradingResultDTO.Feedback feedback = new SpeakingGradingResultDTO.Feedback();
        feedback.setStrengths(List.of(
            "Good attempt at answering questions",
            "Shows willingness to communicate"
        ));
        feedback.setWeaknesses(List.of(
            "Could not fully evaluate audio quality",
            "Recommend re-recording for accurate assessment"
        ));
        feedback.setRecommendations(List.of(
            "Ensure clear audio recording",
            "Practice speaking for 2+ minutes on each topic"
        ));
        feedback.setFluencyNotes("Unable to fully assess fluency from available input");
        feedback.setLexicalNotes("Unable to fully assess vocabulary from available input");
        feedback.setGrammarNotes("Unable to fully assess grammar from available input");
        feedback.setPronunciationNotes("Unable to fully assess pronunciation from available input");

        result.setFeedback(feedback);
        result.setPartScores(new ArrayList<>());
        result.setTranscript("[Transcript not available]");

        return result;
    }

    private SpeakingResultDTO buildResultDTO(SpeakingSession session, List<SpeakingTranscript> transcripts) {
        SpeakingResultDTO dto = new SpeakingResultDTO();
        dto.setSessionId(session.getId());
        dto.setSessionMode(session.getSessionMode());
        dto.setSessionStatus(session.getStatus()); // Include status for polling
        dto.setCompletedAt(session.getCompletedAt());
        dto.setTotalDurationSeconds(session.getTotalDurationSeconds());
        dto.setOverallBand(session.getOverallBand());

        // Check if grading is complete
        Map<String, Object> gradingResult = session.getGradingResult();
        if (gradingResult != null && !gradingResult.isEmpty()) {
            // Parse feedback from JSONB
            try {
                String jsonStr = objectMapper.writeValueAsString(gradingResult);
                SpeakingGradingResultDTO grading = objectMapper.readValue(jsonStr, SpeakingGradingResultDTO.class);

                // Build criterion DTOs
                if (grading.getFluencyCoherence() != null) {
                    SpeakingResultDTO.CriterionDTO fluency = new SpeakingResultDTO.CriterionDTO();
                    fluency.setBand(toBigDecimal(grading.getFluencyCoherence()));
                    fluency.setLabel("Fluency & Coherence");
                    if (grading.getFeedback() != null) {
                        fluency.setNotes(grading.getFeedback().getFluencyNotes());
                    }
                    dto.setFluency(fluency);
                }

                if (grading.getLexicalResource() != null) {
                    SpeakingResultDTO.CriterionDTO lexical = new SpeakingResultDTO.CriterionDTO();
                    lexical.setBand(toBigDecimal(grading.getLexicalResource()));
                    lexical.setLabel("Lexical Resource");
                    if (grading.getFeedback() != null) {
                        lexical.setNotes(grading.getFeedback().getLexicalNotes());
                    }
                    dto.setLexical(lexical);
                }

                if (grading.getGrammaticalRange() != null) {
                    SpeakingResultDTO.CriterionDTO grammar = new SpeakingResultDTO.CriterionDTO();
                    grammar.setBand(toBigDecimal(grading.getGrammaticalRange()));
                    grammar.setLabel("Grammatical Range & Accuracy");
                    if (grading.getFeedback() != null) {
                        grammar.setNotes(grading.getFeedback().getGrammarNotes());
                    }
                    dto.setGrammar(grammar);
                }

                if (grading.getPronunciation() != null) {
                    SpeakingResultDTO.CriterionDTO pronunciation = new SpeakingResultDTO.CriterionDTO();
                    pronunciation.setBand(toBigDecimal(grading.getPronunciation()));
                    pronunciation.setLabel("Pronunciation");
                    if (grading.getFeedback() != null) {
                        pronunciation.setNotes(grading.getFeedback().getPronunciationNotes());
                    }
                    dto.setPronunciation(pronunciation);
                }

                // Set overall feedback
                if (grading.getFeedback() != null) {
                    SpeakingGradingResultDTO.Feedback fb = grading.getFeedback();
                    StringBuilder overallFeedback = new StringBuilder();
                    if (fb.getStrengths() != null && !fb.getStrengths().isEmpty()) {
                        overallFeedback.append("Strengths: ").append(String.join(", ", fb.getStrengths())).append(". ");
                    }
                    if (fb.getWeaknesses() != null && !fb.getWeaknesses().isEmpty()) {
                        overallFeedback.append("Areas for improvement: ").append(String.join(", ", fb.getWeaknesses())).append(". ");
                    }
                    dto.setOverallFeedback(overallFeedback.toString());
                    dto.setSuggestions(fb.getRecommendations());
                }

            } catch (Exception e) {
                logger.warn("Failed to parse grading result for DTO: {}", e.getMessage());
            }
        }

        // Build transcripts with questions
        List<SpeakingResultDTO.TranscriptWithQuestionDTO> transcriptDTOs = new ArrayList<>();
        for (SpeakingTranscript t : transcripts) {
            SpeakingResultDTO.TranscriptWithQuestionDTO tDto = new SpeakingResultDTO.TranscriptWithQuestionDTO();
            tDto.setTranscriptId(t.getId());
            tDto.setQuestionId(t.getQuestionId());
            tDto.setPart(t.getPart());
            tDto.setTranscriptText(t.getTranscriptText());
            tDto.setAudioUrl(t.getAudioUrl());
            tDto.setAudioDurationSeconds(t.getAudioDurationSeconds());

            // Get question text and TTS audio info
            if (t.getQuestionId() != null) {
                questionRepository.findById(t.getQuestionId())
                    .ifPresent(q -> {
                        tDto.setQuestionText(q.getQuestionText());
                        tDto.setExaminerAudioUrl(q.getExaminerAudioUrl());
                        tDto.setExaminerAudioDurationMs(q.getExaminerAudioDurationMs());
                    });
            }

            transcriptDTOs.add(tDto);
        }
        dto.setTranscripts(transcriptDTOs);

        return dto;
    }
}
