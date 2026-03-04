package com.cramer.controller;

import com.cramer.dto.*;
import com.cramer.service.FollowUpSelectionService;
import com.cramer.service.RealtimeASRService;
import com.cramer.service.SampleAnswerService;
import com.cramer.service.SpeakingEvaluationService;
import com.cramer.service.SpeakingQuestionService;
import com.cramer.service.SpeakingSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for IELTS Speaking practice feature.
 * Provides endpoints for topics, questions, sessions, and evaluations.
 */
@RestController
@RequestMapping("/api/speaking")
@Tag(name = "Speaking API", description = "API for IELTS Speaking practice and AI evaluation")
public class SpeakingController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(SpeakingController.class);

    private final SpeakingQuestionService questionService;
    private final SpeakingSessionService sessionService;
    private final SpeakingEvaluationService evaluationService;
    private final RealtimeASRService asrService;
    private final FollowUpSelectionService followUpService;
    private final SampleAnswerService sampleAnswerService;

    public SpeakingController(SpeakingQuestionService questionService,
                              SpeakingSessionService sessionService,
                              SpeakingEvaluationService evaluationService,
                              RealtimeASRService asrService,
                              FollowUpSelectionService followUpService,
                              SampleAnswerService sampleAnswerService) {
        this.questionService = questionService;
        this.sessionService = sessionService;
        this.evaluationService = evaluationService;
        this.asrService = asrService;
        this.followUpService = followUpService;
        this.sampleAnswerService = sampleAnswerService;
    }

    // ==================== TOPICS & QUESTIONS ====================

    /**
     * GET /api/speaking/topics - Get all available speaking topics
     */
    @GetMapping("/topics")
    @Operation(summary = "Get all topics", description = "Get all available speaking topics")
    public ResponseEntity<Map<String, Object>> getTopics() {
        logger.info("📥 GET /api/speaking/topics");

        List<SpeakingTopicDTO> topics = questionService.getAllActiveTopics();

        logger.info("✅ Returning {} topics", topics.size());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", topics
        ));
    }

    /**
     * GET /api/speaking/questions - Get questions for a topic and mode
     */
    @GetMapping("/questions")
    @Operation(summary = "Get questions", description = "Get questions for a topic and session mode")
    public ResponseEntity<Map<String, Object>> getQuestions(
            @Parameter(description = "Topic ID") @RequestParam Long topicId,
            @Parameter(description = "Session mode: FULL, PART_1, PART_2, PART_3, PART_2_3")
            @RequestParam String mode) {

        logger.info("📥 GET /api/speaking/questions - topicId={}, mode={}", topicId, mode);

        List<SpeakingQuestionDTO> questions = questionService.getQuestionsForSession(topicId, mode);

        logger.info("✅ Returning {} questions for topic {} mode {}", questions.size(), topicId, mode);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", questions
        ));
    }

    // ==================== SESSION MANAGEMENT ====================

    /**
     * POST /api/speaking/sessions - Create a new speaking session
     */
    @PostMapping("/sessions")
    @Operation(summary = "Create session", description = "Create a new speaking practice session")
    public ResponseEntity<Map<String, Object>> createSession(
            @Valid @RequestBody CreateSpeakingSessionDTO request,
            Authentication authentication) {

        logger.info("📥 POST /api/speaking/sessions - mode={}, topicId={}",
                request.getMode(), request.getTopicId());

        UUID userId = getCurrentUserId(authentication);
        SpeakingSessionDTO session = sessionService.createSession(userId, request.getMode(), request.getTopicId());

        logger.info("✅ Created speaking session {} for user {}", session.getSessionId(), userId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", session
        ));
    }

    /**
     * GET /api/speaking/sessions/{id} - Get session details
     */
    @GetMapping("/sessions/{id}")
    @Operation(summary = "Get session", description = "Get details of a speaking session")
    public ResponseEntity<Map<String, Object>> getSession(
            @PathVariable @Min(1) Long id,
            Authentication authentication) {

        logger.info("📥 GET /api/speaking/sessions/{}", id);

        UUID userId = getCurrentUserId(authentication);
        SpeakingSessionDTO session = sessionService.getSession(id, userId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", session
        ));
    }

    /**
     * POST /api/speaking/sessions/{id}/transcripts - Save a transcript for a question
     */
    @PostMapping("/sessions/{id}/transcripts")
    @Operation(summary = "Save transcript", description = "Save audio transcript for a question in the session")
    public ResponseEntity<Map<String, Object>> saveTranscript(
            @PathVariable @Min(1) Long id,
            @RequestParam Long questionId,
            @RequestParam Integer part,
            @RequestParam(required = false) String audioUrl,
            @RequestParam(required = false) Integer duration,
            @RequestBody(required = false) String transcriptText,
            Authentication authentication) {

        logger.info("📥 POST /api/speaking/sessions/{}/transcripts - questionId={}, part={}",
                id, questionId, part);

        UUID userId = getCurrentUserId(authentication);
        SpeakingTranscriptDTO transcript = sessionService.saveTranscript(
                id, questionId, part, audioUrl, duration, transcriptText, userId);

        logger.info("✅ Saved transcript {} for session {}", transcript.getTranscriptId(), id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", transcript
        ));
    }

    /**
     * POST /api/speaking/sessions/{id}/complete - Mark session as complete and trigger evaluation
     */
    @PostMapping("/sessions/{id}/complete")
    @Operation(summary = "Complete session", description = "Mark session as complete and start AI evaluation")
    public ResponseEntity<Map<String, Object>> completeSession(
            @PathVariable @Min(1) Long id,
            Authentication authentication) {

        logger.info("📥 POST /api/speaking/sessions/{}/complete", id);

        UUID userId = getCurrentUserId(authentication);
        sessionService.completeSession(id, userId);

        // Trigger async evaluation
        evaluationService.evaluateSessionAsync(id);

        logger.info("✅ Completed session {} and started evaluation", id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Session completed. Evaluation in progress."
        ));
    }

    /**
     * POST /api/speaking/sessions/{id}/abandon - Abandon a session (user cancelled)
     */
    @PostMapping("/sessions/{id}/abandon")
    @Operation(summary = "Abandon session", description = "Abandon an in-progress speaking session")
    public ResponseEntity<Map<String, Object>> abandonSession(
            @PathVariable @Min(1) Long id,
            Authentication authentication) {

        logger.info("📥 POST /api/speaking/sessions/{}/abandon", id);

        UUID userId = getCurrentUserId(authentication);
        sessionService.abandonSession(id, userId);

        logger.info("✅ Abandoned session {}", id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Session abandoned."
        ));
    }

    /**
     * GET /api/speaking/sessions/{id}/results - Get evaluation results
     */
    @GetMapping("/sessions/{id}/results")
    @Operation(summary = "Get results", description = "Get AI evaluation results for a completed session")
    public ResponseEntity<Map<String, Object>> getResults(
            @PathVariable @Min(1) Long id,
            Authentication authentication) {

        logger.info("📥 GET /api/speaking/sessions/{}/results", id);

        UUID userId = getCurrentUserId(authentication);
        SpeakingResultDTO results = evaluationService.getResults(id, userId);

        logger.info("✅ Returning results for session {}", id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", results
        ));
    }

    /**
     * GET /api/speaking/history - Get user's session history
     */
    @GetMapping("/history")
    @Operation(summary = "Get history", description = "Get user's speaking session history")
    public ResponseEntity<Map<String, Object>> getHistory(Authentication authentication) {

        logger.info("📥 GET /api/speaking/history");

        UUID userId = getCurrentUserId(authentication);
        List<SpeakingSessionDTO> sessions = sessionService.getUserSessionHistory(userId);

        logger.info("✅ Returning {} sessions in history", sessions.size());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", sessions
        ));
    }

    // ==================== FOLLOW-UP QUESTION SELECTION ====================

    /**
     * POST /api/speaking/samples - Generate sample answers for questions
     * 
     * Creates Band 6 and Band 7-8 sample answers for learning.
     */
    @PostMapping("/samples")
    @Operation(summary = "Generate samples", description = "Generate sample answers at different band levels")
    public ResponseEntity<Map<String, Object>> generateSamples(
            @RequestParam Long topicId,
            @RequestBody List<SpeakingQuestionDTO> questions,
            Authentication authentication) {

        logger.info("📥 POST /api/speaking/samples - topicId={}, questionCount={}", 
                   topicId, questions.size());

        if (!sampleAnswerService.isEnabled()) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", "Sample generation is disabled"
            ));
        }

        // Get topic name
        String topicName = "General";
        try {
            topicName = questionService.getTopicById(topicId).getNameEn();
        } catch (Exception e) {
            logger.warn("Could not get topic name for {}", topicId);
        }

        Map<Long, Map<String, String>> samples = sampleAnswerService.generateSamplesForQuestions(
            questions, topicName, null
        );

        logger.info("✅ Generated samples for {} questions", samples.size());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", samples
        ));
    }

    /**
     * POST /api/speaking/sessions/{id}/samples - Generate samples for session questions
     */
    @PostMapping("/sessions/{id}/samples")
    @Operation(summary = "Generate session samples", description = "Generate samples based on session transcripts")
    public ResponseEntity<Map<String, Object>> generateSessionSamples(
            @PathVariable @Min(1) Long id,
            Authentication authentication) {

        logger.info("📥 POST /api/speaking/sessions/{}/samples", id);

        UUID userId = getCurrentUserId(authentication);
        SpeakingSessionDTO session = sessionService.getSession(id, userId);

        if (!sampleAnswerService.isEnabled()) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", "Sample generation is disabled"
            ));
        }

        // Get transcripts for this session
        List<SpeakingTranscriptDTO> transcripts = sessionService.getSessionTranscripts(id, userId);

        Map<Long, Map<String, String>> samples = sampleAnswerService.generateSamplesForSession(
            id, session.getTopicId(), transcripts
        );

        logger.info("✅ Generated samples for session {}", id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", samples
        ));
    }

    /**
     * GET /api/speaking/samples/status - Check if sample generation is enabled
     */
    @GetMapping("/samples/status")
    @Operation(summary = "Sample status", description = "Check if sample answer generation is enabled")
    public ResponseEntity<Map<String, Object>> getSampleStatus() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "enabled", sampleAnswerService.isEnabled()
                )
        ));
    }

    /**
     * POST /api/speaking/follow-up - Select an appropriate follow-up question
     * 
     * Uses AI to analyze conversation context and select the most relevant
     * follow-up question from the question bank.
     */
    @PostMapping("/follow-up")
    @Operation(summary = "Select follow-up", description = "AI-selected follow-up question based on conversation")
    public ResponseEntity<Map<String, Object>> selectFollowUp(
            @RequestParam Long topicId,
            @RequestParam Integer part,
            @RequestParam String previousQuestion,
            @RequestBody(required = false) FollowUpRequestDTO request,
            Authentication authentication) {

        logger.info("📥 POST /api/speaking/follow-up - topicId={}, part={}", topicId, part);

        String candidateAnswer = request != null ? request.getCandidateAnswer() : null;
        List<Long> askedQuestionIds = request != null && request.getAskedQuestionIds() != null 
            ? request.getAskedQuestionIds() 
            : List.of();

        SpeakingQuestionDTO followUp = followUpService.selectFollowUp(
            topicId, 
            part, 
            previousQuestion, 
            candidateAnswer, 
            askedQuestionIds
        );

        if (followUp != null) {
            logger.info("✅ Selected follow-up question: {}", followUp.getId());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", followUp
            ));
        } else {
            logger.warn("No follow-up questions available");
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", "No follow-up questions available"
            ));
        }
    }

    /**
     * GET /api/speaking/follow-up/status - Check if follow-up AI is enabled
     */
    @GetMapping("/follow-up/status")
    @Operation(summary = "Follow-up status", description = "Check if AI follow-up selection is enabled")
    public ResponseEntity<Map<String, Object>> getFollowUpStatus() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "enabled", followUpService.isEnabled()
                )
        ));
    }

    // ==================== ASR (Speech-to-Text) ====================

    /**
     * POST /api/speaking/transcribe - Transcribe audio data
     * 
     * Supports real-time transcription of audio chunks for live feedback.
     * Uses Gemini 2.5 Flash Lite via OpenRouter for low-latency ASR.
     */
    @PostMapping("/transcribe")
    @Operation(summary = "Transcribe audio", description = "Transcribe audio data to text using AI")
    public ResponseEntity<Map<String, Object>> transcribeAudio(
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam(value = "format", defaultValue = "webm") String format,
            Authentication authentication) {

        logger.info("📥 POST /api/speaking/transcribe - size={} bytes, format={}",
                audioFile.getSize(), format);

        if (!asrService.isConfigured()) {
            logger.warn("ASR service not configured");
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", "ASR service not available"
            ));
        }

        try {
            byte[] audioData = audioFile.getBytes();
            String transcript = asrService.transcribe(audioData, format);

            if (transcript != null) {
                logger.info("✅ Transcription successful: {} chars", transcript.length());
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "data", Map.of(
                                "transcript", transcript,
                                "provider", asrService.getProvider()
                        )
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "error", "Transcription failed"
                ));
            }

        } catch (Exception e) {
            logger.error("Transcription error: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", "Transcription error: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /api/speaking/transcribe-url - Transcribe audio from URL
     * 
     * Useful for transcribing audio files already uploaded to storage.
     */
    @PostMapping("/transcribe-url")
    @Operation(summary = "Transcribe from URL", description = "Transcribe audio from a public URL")
    public ResponseEntity<Map<String, Object>> transcribeFromUrl(
            @RequestParam("url") String audioUrl,
            Authentication authentication) {

        logger.info("📥 POST /api/speaking/transcribe-url - url={}", audioUrl);

        if (!asrService.isConfigured()) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", "ASR service not available"
            ));
        }

        try {
            String transcript = asrService.transcribeFromUrl(audioUrl);

            if (transcript != null) {
                logger.info("✅ URL transcription successful: {} chars", transcript.length());
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "data", Map.of(
                                "transcript", transcript,
                                "provider", asrService.getProvider()
                        )
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "error", "Transcription failed"
                ));
            }

        } catch (Exception e) {
            logger.error("URL transcription error: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", "Transcription error: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/speaking/asr/status - Check ASR service status
     */
    @GetMapping("/asr/status")
    @Operation(summary = "ASR status", description = "Check if ASR service is available")
    public ResponseEntity<Map<String, Object>> getASRStatus() {
        logger.info("📥 GET /api/speaking/asr/status");

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "configured", asrService.isConfigured(),
                        "provider", asrService.getProvider(),
                        "realtimeAvailable", asrService.isRealtimeAvailable()
                )
        ));
    }
}
