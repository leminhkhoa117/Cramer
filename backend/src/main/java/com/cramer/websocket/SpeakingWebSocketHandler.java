package com.cramer.websocket;

import com.cramer.service.SpeakingSessionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for Speaking sessions.
 * 
 * Acts as a proxy between frontend and Gemini Live API:
 * 1. Frontend connects via ws://localhost:8080/ws/speaking/{sessionId}
 * 2. Backend connects to Gemini Live API
 * 3. Audio is streamed bidirectionally
 * 
 * Message protocol (Frontend <-> Backend):
 * - Binary: Raw audio data (PCM 16kHz mono)
 * - Text JSON: Control messages
 *   - {type: "start", questionText: "..."} - Start asking a question
 *   - {type: "end_turn"} - User finished speaking
 *   - {type: "transcript", text: "..."} - Transcription update
 *   - {type: "examiner_audio", data: "base64..."} - Examiner audio chunk
 *   - {type: "examiner_speaking", speaking: true/false}
 *   - {type: "error", message: "..."}
 */
@Component
public class SpeakingWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(SpeakingWebSocketHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${speaking.gemini.live.enabled:true}")
    private boolean geminiLiveEnabled;

    private final SpeakingSessionService sessionService;

    // Track active sessions: sessionId -> SessionState
    private final Map<String, SessionState> activeSessions = new ConcurrentHashMap<>();

    /**
     * State for an active WebSocket session.
     */
    private static class SessionState {
        final WebSocketSession frontendSession;
        GeminiLiveWebSocketClient geminiClient;
        ByteArrayOutputStream audioBuffer;
        boolean isExaminerSpeaking;
        String currentQuestionText;

        SessionState(WebSocketSession frontendSession) {
            this.frontendSession = frontendSession;
            this.audioBuffer = new ByteArrayOutputStream();
            this.isExaminerSpeaking = false;
        }
    }

    public SpeakingWebSocketHandler(SpeakingSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = extractSessionId(session);
        logger.info("[WS] Connection established for session: {}", sessionId);

        // Create session state
        SessionState state = new SessionState(session);
        activeSessions.put(sessionId, state);

        // Check if Gemini Live is enabled and configured
        if (!geminiLiveEnabled || geminiApiKey == null || geminiApiKey.isEmpty()) {
            logger.warn("[WS] Gemini Live not configured, using fallback mode for session {}", sessionId);
            sendMessage(session, createStatusMessage("connected", "Gemini Live not configured. Using text-only mode."));
            return;
        }

        // Connect to Gemini Live API
        try {
            connectToGemini(sessionId, state);
            sendMessage(session, createStatusMessage("connecting", "Connecting to Gemini Live..."));
        } catch (Exception e) {
            logger.error("[WS] Failed to connect to Gemini for session {}: {}", sessionId, e.getMessage());
            sendMessage(session, createErrorMessage("Failed to connect to Gemini: " + e.getMessage()));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = extractSessionId(session);
        SessionState state = activeSessions.get(sessionId);

        if (state == null) {
            logger.warn("[WS] Received message for unknown session: {}", sessionId);
            return;
        }

        try {
            JsonNode json = objectMapper.readTree(message.getPayload());
            String type = json.has("type") ? json.get("type").asText() : "";

            switch (type) {
                case "start_question":
                    // Examiner should ask a specific question
                    String questionText = json.has("questionText") ? json.get("questionText").asText() : "";
                    handleStartQuestion(sessionId, state, questionText);
                    break;

                case "end_turn":
                    // User finished speaking
                    handleEndTurn(sessionId, state);
                    break;

                case "pause":
                    // Pause the session
                    handlePause(sessionId, state);
                    break;

                case "resume":
                    // Resume the session
                    handleResume(sessionId, state);
                    break;

                default:
                    logger.debug("[WS] Unknown message type: {} for session {}", type, sessionId);
            }
        } catch (Exception e) {
            logger.error("[WS] Error handling text message for session {}: {}", sessionId, e.getMessage());
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        String sessionId = extractSessionId(session);
        SessionState state = activeSessions.get(sessionId);

        if (state == null) {
            logger.warn("[WS] Received binary data for unknown session: {}", sessionId);
            return;
        }

        byte[] audioData = message.getPayload().array();
        logger.debug("[WS] Received {} bytes of audio from session {}", audioData.length, sessionId);

        // Forward to Gemini if connected
        if (state.geminiClient != null && state.geminiClient.isOpen()) {
            state.geminiClient.sendAudioChunk(audioData);
        }

        // Also buffer for potential transcript saving
        state.audioBuffer.write(audioData);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = extractSessionId(session);
        logger.info("[WS] Connection closed for session {}: {}", sessionId, status);

        SessionState state = activeSessions.remove(sessionId);
        if (state != null && state.geminiClient != null) {
            state.geminiClient.close();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = extractSessionId(session);
        logger.error("[WS] Transport error for session {}: {}", sessionId, exception.getMessage());
    }

    /**
     * Connect to Gemini Live API for a session.
     */
    private void connectToGemini(String sessionId, SessionState state) throws Exception {
        URI geminiUri = GeminiLiveWebSocketClient.createGeminiUri(geminiApiKey);

        GeminiLiveWebSocketClient geminiClient = new GeminiLiveWebSocketClient(
            geminiUri,
            sessionId,
            geminiApiKey,
            // onAudioReceived - forward to frontend
            (audioData) -> {
                try {
                    // Send as binary or base64 in JSON
                    sendAudioToFrontend(state, audioData);
                } catch (Exception e) {
                    logger.error("[WS] Failed to forward audio to frontend: {}", e.getMessage());
                }
            },
            // onTranscriptReceived - forward examiner's text
            (text) -> {
                try {
                    sendMessage(state.frontendSession, createTranscriptMessage("examiner", text));
                } catch (Exception e) {
                    logger.error("[WS] Failed to forward transcript: {}", e.getMessage());
                }
            },
            // onUserTranscriptReceived - forward user's speech transcript
            (text) -> {
                try {
                    sendMessage(state.frontendSession, createTranscriptMessage("user", text));
                } catch (Exception e) {
                    logger.error("[WS] Failed to forward user transcript: {}", e.getMessage());
                }
            },
            // onTurnStart
            (event) -> {
                state.isExaminerSpeaking = true;
                try {
                    sendMessage(state.frontendSession, createExaminerSpeakingMessage(true));
                } catch (Exception e) {
                    logger.error("[WS] Failed to send turn start: {}", e.getMessage());
                }
            },
            // onTurnEnd
            (event) -> {
                state.isExaminerSpeaking = false;
                try {
                    sendMessage(state.frontendSession, createExaminerSpeakingMessage(false));
                } catch (Exception e) {
                    logger.error("[WS] Failed to send turn end: {}", e.getMessage());
                }
            },
            // onSetupComplete
            () -> {
                try {
                    sendMessage(state.frontendSession, createStatusMessage("ready", "Gemini Live connected. Ready to start."));
                } catch (Exception e) {
                    logger.error("[WS] Failed to send ready status: {}", e.getMessage());
                }
            }
        );

        state.geminiClient = geminiClient;
        geminiClient.connect();
    }

    /**
     * Handle request to start asking a question.
     */
    private void handleStartQuestion(String sessionId, SessionState state, String questionText) {
        logger.info("[WS] Starting question for session {}: {}", sessionId, questionText);

        state.currentQuestionText = questionText;
        state.audioBuffer.reset();

        // Send to Gemini to speak the question
        if (state.geminiClient != null && state.geminiClient.isOpen()) {
            String prompt = String.format("Please ask the candidate: \"%s\"", questionText);
            state.geminiClient.sendTextMessage(prompt);
        }
    }

    /**
     * Handle user end of turn signal.
     */
    private void handleEndTurn(String sessionId, SessionState state) {
        logger.info("[WS] User ended turn for session {}", sessionId);

        if (state.geminiClient != null && state.geminiClient.isOpen()) {
            state.geminiClient.sendEndOfTurn();
        }
    }

    /**
     * Handle pause request.
     */
    private void handlePause(String sessionId, SessionState state) {
        logger.info("[WS] Pausing session {}", sessionId);
        // TODO: Implement pause logic
    }

    /**
     * Handle resume request.
     */
    private void handleResume(String sessionId, SessionState state) {
        logger.info("[WS] Resuming session {}", sessionId);
        // TODO: Implement resume logic
    }

    /**
     * Send audio data to frontend.
     */
    private void sendAudioToFrontend(SessionState state, byte[] audioData) throws IOException {
        // Option 1: Send as binary
        // state.frontendSession.sendMessage(new BinaryMessage(audioData));

        // Option 2: Send as base64 in JSON (more compatible)
        ObjectNode message = objectMapper.createObjectNode();
        message.put("type", "examiner_audio");
        message.put("data", Base64.getEncoder().encodeToString(audioData));
        message.put("format", "audio/pcm;rate=24000"); // Gemini outputs 24kHz
        sendMessage(state.frontendSession, message);
    }

    // Helper methods to create JSON messages

    private ObjectNode createStatusMessage(String status, String message) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", "status");
        node.put("status", status);
        node.put("message", message);
        return node;
    }

    private ObjectNode createErrorMessage(String message) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", "error");
        node.put("message", message);
        return node;
    }

    private ObjectNode createTranscriptMessage(String speaker, String text) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", "transcript");
        node.put("speaker", speaker);
        node.put("text", text);
        return node;
    }

    private ObjectNode createExaminerSpeakingMessage(boolean speaking) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", "examiner_speaking");
        node.put("speaking", speaking);
        return node;
    }

    private void sendMessage(WebSocketSession session, ObjectNode message) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        }
    }

    private String extractSessionId(WebSocketSession session) {
        String path = session.getUri().getPath();
        // Path format: /ws/speaking/{sessionId}
        String[] parts = path.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : "unknown";
    }
}
