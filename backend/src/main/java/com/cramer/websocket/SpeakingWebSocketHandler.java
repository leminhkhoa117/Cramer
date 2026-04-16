package com.cramer.websocket;

import com.cramer.config.SpeakingGeminiLiveProperties;
import com.cramer.dto.SpeakingSessionDTO;
import com.cramer.dto.SpeakingTurnDTO;
import com.cramer.service.SpeakingSessionService;
import com.cramer.util.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

/**
 * WebSocket entry point for real-time Speaking sessions.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Consume handshake-authenticated user identity and ensure the speaking session belongs to the connected user</li>
 *   <li>Ensure the session is still open ({@code in_progress} and not finalized)</li>
 *   <li>Proxy real-time turn control and audio chunks to Gemini Live when enabled</li>
 *   <li>Expose a graceful fallback mode when Gemini Live is unavailable</li>
 * </ul>
 *
 * <p>The frontend connects to {@code /ws/speaking/{sessionId}} and sends:
 * <ul>
 *   <li>{@code {"type":"start_question","turnIndex":1}}</li>
 *   <li>{@code {"type":"end_turn"}}</li>
 *   <li>binary PCM audio chunks</li>
 * </ul>
 */
@Component
public class SpeakingWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(
        SpeakingWebSocketHandler.class
    );

    private static final @NonNull CloseStatus POLICY_VIOLATION_1008 = new CloseStatus(
        1008,
        "Policy violation"
    );
    private static final @NonNull CloseStatus INTERNAL_ERROR_1011 = new CloseStatus(
        1011,
        "Internal server error"
    );

    private final SpeakingSessionService speakingSessionService;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final SpeakingGeminiLiveProperties geminiLiveProperties;
    private final String geminiApiKey;
    private final GeminiConnectionFactory geminiConnectionFactory;

    private final Map<String, SessionState> activeSessions =
        new ConcurrentHashMap<>();

    public SpeakingWebSocketHandler(
        SpeakingSessionService speakingSessionService,
        JwtUtil jwtUtil,
        ObjectMapper objectMapper,
        SpeakingGeminiLiveProperties geminiLiveProperties,
        @Value("${gemini.api.key:}") String geminiApiKey
    ) {
        this(
            speakingSessionService,
            jwtUtil,
            objectMapper,
            geminiLiveProperties,
            geminiApiKey,
            null
        );
    }

    SpeakingWebSocketHandler(
        SpeakingSessionService speakingSessionService,
        JwtUtil jwtUtil,
        ObjectMapper objectMapper,
        SpeakingGeminiLiveProperties geminiLiveProperties,
        String geminiApiKey,
        GeminiConnectionFactory geminiConnectionFactory
    ) {
        this.speakingSessionService = speakingSessionService;
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.geminiLiveProperties = geminiLiveProperties;
        this.geminiApiKey = geminiApiKey;
        this.geminiConnectionFactory = geminiConnectionFactory;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session)
        throws Exception {
        try {
            Long speakingSessionId = extractSpeakingSessionId(session);
            UUID userId = extractAuthenticatedUserId(session);

            SpeakingSessionDTO speakingSession =
                speakingSessionService.getSession(speakingSessionId, userId);
            ensureSessionOpenForRealtime(speakingSession);

            SessionState state = new SessionState(
                new ConcurrentWebSocketSessionDecorator(
                    session,
                    10_000,
                    512 * 1024
                ),
                userId,
                speakingSession
            );
            activeSessions.put(session.getId(), state);

            log.info(
                "Speaking WebSocket connected: wsSessionId={}, speakingSessionId={}, userId={}, authSource={}",
                session.getId(),
                speakingSessionId,
                userId,
                extractAuthSource(session)
            );

            if (shouldUseFallbackMode()) {
                state.setFallbackMode(true);
                sendStatus(
                    state.getFrontendSession(),
                    "fallback_text_mode",
                    "Gemini Live is unavailable. Falling back to text-only speaking prompts."
                );
                return;
            }

            GeminiLiveConnection connection = createGeminiConnection(state);
            state.setGeminiConnection(connection);
            connection.connect();

            sendStatus(
                state.getFrontendSession(),
                "connecting",
                "Connecting to Gemini Live..."
            );
        } catch (Exception ex) {
            log.warn(
                "Failed to establish Speaking WebSocket session {}: {}",
                session.getId(),
                ex.getMessage(),
                ex
            );
            safeSendError(
                session,
                ex.getMessage() != null
                    ? ex.getMessage()
                    : "Unable to open speaking session."
            );
            safeClose(session, POLICY_VIOLATION_1008);
        }
    }

    @Override
    protected void handleTextMessage(
        @NonNull WebSocketSession session,
        @NonNull TextMessage message
    ) throws Exception {
        SessionState state = activeSessions.get(session.getId());
        if (state == null) {
            safeSendError(
                session,
                "Speaking WebSocket session state not found."
            );
            safeClose(session, POLICY_VIOLATION_1008);
            return;
        }

        try {
            JsonNode payload = objectMapper.readTree(message.getPayload());
            String type = payload.path("type").asText();

            switch (type) {
                case "start_question" -> handleStartQuestion(state, payload);
                case "end_turn" -> handleEndTurn(state);
                default -> sendError(
                    state.getFrontendSession(),
                    "Unsupported message type: " + type
                );
            }
        } catch (Exception ex) {
            log.warn(
                "Failed to handle text message for wsSessionId={}: {}",
                session.getId(),
                ex.getMessage(),
                ex
            );
            sendError(
                session,
                ex.getMessage() != null
                    ? ex.getMessage()
                    : "Invalid WebSocket message."
            );
        }
    }

    @Override
    protected void handleBinaryMessage(
        @NonNull WebSocketSession session,
        @NonNull BinaryMessage message
    ) throws Exception {
        SessionState state = activeSessions.get(session.getId());
        if (state == null) {
            safeSendError(
                session,
                "Speaking WebSocket session state not found."
            );
            safeClose(session, POLICY_VIOLATION_1008);
            return;
        }

        if (state.isFallbackMode()) {
            // Ignore audio in fallback text mode. FE can still upload recorded audio separately via REST/storage flow.
            return;
        }

        try {
            refreshSessionState(state);
        } catch (IllegalStateException ex) {
            return;
        }

        GeminiLiveConnection connection = state.getGeminiConnection();
        if (connection == null || !connection.isOpen()) {
            sendError(session, "Gemini Live connection is not open.");
            return;
        }

        ByteBuffer payload = message.getPayload();
        byte[] audioBytes = new byte[payload.remaining()];
        payload.get(audioBytes);

        if (audioBytes.length == 0) {
            return;
        }

        connection.sendAudioChunk(audioBytes);
    }

    @Override
    public void afterConnectionClosed(
        @NonNull WebSocketSession session,
        @NonNull CloseStatus status
    ) {
        SessionState state = activeSessions.remove(session.getId());
        if (state != null && state.getGeminiConnection() != null) {
            try {
                state.getGeminiConnection().close();
            } catch (Exception ex) {
                log.debug(
                    "Ignoring Gemini close error for wsSessionId={}: {}",
                    session.getId(),
                    ex.getMessage()
                );
            }
        }

        log.info(
            "Speaking WebSocket closed: wsSessionId={}, status={}",
            session.getId(),
            status
        );
    }

    @Override
    public void handleTransportError(
        @NonNull WebSocketSession session,
        @NonNull Throwable exception
    ) {
        log.warn(
            "Speaking WebSocket transport error: wsSessionId={}, message={}",
            session.getId(),
            exception.getMessage(),
            exception
        );
        safeSendError(session, "Realtime speaking connection error.");
        safeClose(session, INTERNAL_ERROR_1011);
    }

    private void handleStartQuestion(SessionState state, JsonNode payload)
        throws IOException {
        try {
            refreshSessionState(state);
        } catch (IllegalStateException ex) {
            return;
        }
        Integer turnIndex = readRequiredTurnIndex(payload);
        SpeakingTurnDTO turn = findTurn(state.getSpeakingSession(), turnIndex);
        if (turn == null) {
            sendError(
                state.getFrontendSession(),
                "turnIndex " +
                    turnIndex +
                    " does not exist in sessionBlueprint."
            );
            return;
        }

        state.setCurrentTurnIndex(turnIndex);
        state.setCurrentTurn(turn);

        if (state.isFallbackMode()) {
            String promptText = extractPromptText(turn.getQuestionSnapshot());
            if (promptText == null || promptText.isBlank()) {
                sendError(
                    state.getFrontendSession(),
                    "Selected turn does not contain promptText."
                );
                return;
            }

            sendTranscript(state.getFrontendSession(), "examiner", promptText);
            sendTurnComplete(state.getFrontendSession());
            return;
        }

        GeminiLiveConnection connection = state.getGeminiConnection();
        if (connection == null || !connection.isOpen()) {
            sendError(
                state.getFrontendSession(),
                "Gemini Live connection is not open."
            );
            return;
        }
        if (!connection.isReady()) {
            sendError(
                state.getFrontendSession(),
                "Gemini Live is not ready yet."
            );
            return;
        }

        connection.sendQuestionPrompt(turn.getQuestionSnapshot());
    }

    private void handleEndTurn(SessionState state) throws IOException {
        try {
            refreshSessionState(state);
        } catch (IllegalStateException ex) {
            return;
        }

        if (state.isFallbackMode()) {
            sendTurnComplete(state.getFrontendSession());
            return;
        }

        GeminiLiveConnection connection = state.getGeminiConnection();
        if (connection == null || !connection.isOpen()) {
            sendError(
                state.getFrontendSession(),
                "Gemini Live connection is not open."
            );
            return;
        }

        connection.sendEndOfTurn();
    }

    private GeminiLiveConnection createGeminiConnection(SessionState state) {
        GeminiLiveWebSocketClient.GeminiLiveListener liveListener =
            new GeminiLiveWebSocketClient.GeminiLiveListener() {
                @Override
                public void onReady() {
                    safeSendStatus(
                        state.getFrontendSession(),
                        "ready",
                        "Gemini Live connected and ready."
                    );
                }

                @Override
                public void onExaminerAudio(byte[] audioData, String mimeType) {
                    safeSendBinary(state.getFrontendSession(), audioData);
                }

                @Override
                public void onExaminerTranscript(String text) {
                    safeSendTranscript(
                        state.getFrontendSession(),
                        "examiner",
                        text
                    );
                }

                @Override
                public void onUserTranscript(String text) {
                    safeSendTranscript(
                        state.getFrontendSession(),
                        "user",
                        text
                    );
                }

                @Override
                public void onExaminerSpeaking(boolean speaking) {
                    safeSendExaminerSpeaking(
                        state.getFrontendSession(),
                        speaking
                    );
                }

                @Override
                public void onTurnComplete() {
                    safeSendTurnComplete(state.getFrontendSession());
                }

                @Override
                public void onError(String message) {
                    transitionToFallback(
                        state,
                        message == null || message.isBlank()
                            ? "Gemini Live error. Falling back to text-only mode."
                            : message
                    );
                }

                @Override
                public void onGoAway(String message) {
                    transitionToFallback(
                        state,
                        message == null || message.isBlank()
                            ? "Gemini Live connection closing soon. Falling back to text-only mode."
                            : message
                    );
                }

                @Override
                public void onClosed(String reason) {
                    transitionToFallback(
                        state,
                        reason == null || reason.isBlank()
                            ? "Gemini Live connection closed. Falling back to text-only mode."
                            : reason
                    );
                }
            };

        if (geminiConnectionFactory != null) {
            return geminiConnectionFactory.create(state, liveListener);
        }

        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(
                Duration.ofSeconds(
                    Math.max(1, geminiLiveProperties.getConnectTimeoutSeconds())
                )
            )
            .build();

        return new GeminiLiveWebSocketClient(
            httpClient,
            geminiLiveProperties,
            geminiApiKey,
            state.getSpeakingSession().getAccent(),
            state.getSpeakingSession().getSpeed() != null
                ? state.getSpeakingSession().getSpeed().toPlainString()
                : null,
            liveListener
        );
    }

    private boolean shouldUseFallbackMode() {
        return (
            !geminiLiveProperties.isEnabled() ||
            geminiApiKey == null ||
            geminiApiKey.isBlank()
        );
    }

    private void refreshSessionState(SessionState state) throws IOException {
        try {
            SpeakingSessionDTO refreshedSession =
                speakingSessionService.getSession(
                    state.getSpeakingSessionId(),
                    state.getUserId()
                );
            ensureSessionOpenForRealtime(refreshedSession);
            state.setSpeakingSession(refreshedSession);
        } catch (RuntimeException ex) {
            String message =
                ex.getMessage() != null
                    ? ex.getMessage()
                    : "Speaking session is no longer available for realtime use.";
            safeSendError(state.getFrontendSession(), message);
            safeSendStatus(state.getFrontendSession(), "closed", message);
            safeClose(state.getFrontendSession(), POLICY_VIOLATION_1008);
            throw new IllegalStateException(message, ex);
        }
    }

    private void transitionToFallback(SessionState state, String reason) {
        if (state.isFallbackMode()) {
            safeSendStatus(
                state.getFrontendSession(),
                "fallback_text_mode",
                reason == null || reason.isBlank()
                    ? "Gemini Live is unavailable. Falling back to text-only speaking prompts."
                    : reason
            );
            return;
        }

        state.setFallbackMode(true);

        GeminiLiveConnection existingConnection = state.getGeminiConnection();
        state.setGeminiConnection(null);

        if (existingConnection != null) {
            try {
                existingConnection.close();
            } catch (Exception ex) {
                log.debug(
                    "Ignoring Gemini close during fallback transition for wsSessionId={}: {}",
                    state.getFrontendSession().getId(),
                    ex.getMessage()
                );
            }
        }

        safeSendStatus(
            state.getFrontendSession(),
            "fallback_text_mode",
            reason == null || reason.isBlank()
                ? "Gemini Live is unavailable. Falling back to text-only speaking prompts."
                : reason
        );
    }

    private Long extractSpeakingSessionId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || uri.getPath() == null) {
            throw new IllegalArgumentException("Missing WebSocket URI path.");
        }

        String[] parts = uri.getPath().split("/");
        if (parts.length == 0) {
            throw new IllegalArgumentException(
                "Invalid Speaking WebSocket path."
            );
        }

        String rawId = parts[parts.length - 1];
        try {
            return Long.valueOf(rawId);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                "Invalid speaking session id in WebSocket path."
            );
        }
    }

    private UUID extractAuthenticatedUserId(WebSocketSession session) {
        UUID handshakeUserId = SpeakingWebSocketAuthSupport.getAuthenticatedUserId(
            session.getAttributes()
        );
        if (handshakeUserId != null) {
            return handshakeUserId;
        }

        SpeakingWebSocketAuthSupport.ExtractedToken extractedToken =
            SpeakingWebSocketAuthSupport.extractToken(
                session.getHandshakeHeaders(),
                session.getUri()
            );
        return SpeakingWebSocketAuthSupport.requireAuthenticatedUserId(
            jwtUtil,
            extractedToken != null ? extractedToken.value() : null
        );
    }

    @Nullable
    private String extractAuthSource(WebSocketSession session) {
        Map<String, Object> attributes = session.getAttributes();
        Object source = attributes != null
            ? attributes.get(SpeakingWebSocketAuthSupport.AUTH_TOKEN_SOURCE_ATTR)
            : null;
        if (source instanceof String sourceText && !sourceText.isBlank()) {
            return sourceText;
        }

        SpeakingWebSocketAuthSupport.ExtractedToken extractedToken =
            SpeakingWebSocketAuthSupport.extractToken(
                session.getHandshakeHeaders(),
                session.getUri()
            );
        return extractedToken != null ? extractedToken.source() : null;
    }

    private void ensureSessionOpenForRealtime(
        SpeakingSessionDTO speakingSession
    ) {
        if (speakingSession == null) {
            throw new IllegalArgumentException(
                "Speaking session does not exist."
            );
        }
        if (Boolean.TRUE.equals(speakingSession.getIsFinalized())) {
            throw new IllegalArgumentException(
                "Speaking session is already finalized."
            );
        }
        if (!"in_progress".equals(speakingSession.getStatus())) {
            throw new IllegalArgumentException(
                "Speaking WebSocket is only available for in-progress sessions."
            );
        }
    }

    private Integer readRequiredTurnIndex(JsonNode payload) {
        if (
            !payload.hasNonNull("turnIndex") ||
            !payload.get("turnIndex").canConvertToInt()
        ) {
            throw new IllegalArgumentException("turnIndex is required.");
        }
        int turnIndex = payload.get("turnIndex").asInt();
        if (turnIndex < 1) {
            throw new IllegalArgumentException("turnIndex must be >= 1.");
        }
        return turnIndex;
    }

    @Nullable
    private SpeakingTurnDTO findTurn(
        SpeakingSessionDTO speakingSession,
        Integer turnIndex
    ) {
        if (speakingSession.getTurns() == null) {
            return null;
        }
        return speakingSession
            .getTurns()
            .stream()
            .filter(turn -> Objects.equals(turn.getTurnIndex(), turnIndex))
            .findFirst()
            .orElse(null);
    }

    @Nullable
    private String extractPromptText(JsonNode questionSnapshot) {
        if (
            questionSnapshot == null ||
            !questionSnapshot.hasNonNull("promptText")
        ) {
            return null;
        }
        return questionSnapshot.get("promptText").asText();
    }

    private void sendStatus(
        WebSocketSession session,
        String status,
        String message
    ) throws IOException {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "status");
        payload.put("status", status);
        payload.put("message", message);
        session.sendMessage(
            new TextMessage(
                Objects.requireNonNull(objectMapper.writeValueAsString(payload))
            )
        );
    }

    private void sendError(WebSocketSession session, String message)
        throws IOException {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "error");
        payload.put("message", message);
        session.sendMessage(
            new TextMessage(
                Objects.requireNonNull(objectMapper.writeValueAsString(payload))
            )
        );
    }

    private void sendTranscript(
        WebSocketSession session,
        String source,
        String text
    ) throws IOException {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "transcript");
        payload.put("source", source);
        payload.put("text", text);
        session.sendMessage(
            new TextMessage(
                Objects.requireNonNull(objectMapper.writeValueAsString(payload))
            )
        );
    }

    private void sendExaminerSpeaking(
        WebSocketSession session,
        boolean speaking
    ) throws IOException {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "examiner_speaking");
        payload.put("speaking", speaking);
        session.sendMessage(
            new TextMessage(
                Objects.requireNonNull(objectMapper.writeValueAsString(payload))
            )
        );
    }

    private void sendTurnComplete(WebSocketSession session) throws IOException {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "turn_complete");
        session.sendMessage(
            new TextMessage(
                Objects.requireNonNull(objectMapper.writeValueAsString(payload))
            )
        );
    }

    private void safeSendStatus(
        WebSocketSession session,
        String status,
        String message
    ) {
        try {
            if (session.isOpen()) {
                sendStatus(session, status, message);
            }
        } catch (IOException ex) {
            log.debug(
                "Failed to send status to wsSessionId={}: {}",
                session.getId(),
                ex.getMessage()
            );
        }
    }

    private void safeSendError(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                sendError(session, message);
            }
        } catch (IOException ex) {
            log.debug(
                "Failed to send error to wsSessionId={}: {}",
                session.getId(),
                ex.getMessage()
            );
        }
    }

    private void safeSendTranscript(
        WebSocketSession session,
        String source,
        String text
    ) {
        try {
            if (session.isOpen()) {
                sendTranscript(session, source, text);
            }
        } catch (IOException ex) {
            log.debug(
                "Failed to send transcript to wsSessionId={}: {}",
                session.getId(),
                ex.getMessage()
            );
        }
    }

    private void safeSendExaminerSpeaking(
        WebSocketSession session,
        boolean speaking
    ) {
        try {
            if (session.isOpen()) {
                sendExaminerSpeaking(session, speaking);
            }
        } catch (IOException ex) {
            log.debug(
                "Failed to send examiner speaking state to wsSessionId={}: {}",
                session.getId(),
                ex.getMessage()
            );
        }
    }

    private void safeSendTurnComplete(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                sendTurnComplete(session);
            }
        } catch (IOException ex) {
            log.debug(
                "Failed to send turn_complete to wsSessionId={}: {}",
                session.getId(),
                ex.getMessage()
            );
        }
    }

    private void safeSendBinary(WebSocketSession session, byte[] audioData) {
        try {
            if (session.isOpen() && audioData != null && audioData.length > 0) {
                session.sendMessage(new BinaryMessage(audioData));
            }
        } catch (IOException ex) {
            log.debug(
                "Failed to send binary audio to wsSessionId={}: {}",
                session.getId(),
                ex.getMessage()
            );
        }
    }

    private void safeClose(
        WebSocketSession session,
        @NonNull CloseStatus closeStatus
    ) {
        try {
            if (session.isOpen()) {
                session.close(closeStatus);
            }
        } catch (IOException ex) {
            log.debug(
                "Failed to close wsSessionId={}: {}",
                session.getId(),
                ex.getMessage()
            );
        }
    }

    @FunctionalInterface
    interface GeminiConnectionFactory {
        GeminiLiveConnection create(
            SessionState state,
            GeminiLiveWebSocketClient.GeminiLiveListener listener
        );
    }

    static final class SessionState {

        private final WebSocketSession frontendSession;
        private final UUID userId;
        private final Long speakingSessionId;
        private volatile SpeakingSessionDTO speakingSession;
        private volatile boolean fallbackMode;
        private volatile Integer currentTurnIndex;
        private volatile SpeakingTurnDTO currentTurn;
        private volatile GeminiLiveConnection geminiConnection;
        private volatile OffsetDateTime connectedAt;

        private SessionState(
            WebSocketSession frontendSession,
            UUID userId,
            SpeakingSessionDTO speakingSession
        ) {
            this.frontendSession = frontendSession;
            this.userId = userId;
            this.speakingSessionId = speakingSession.getSessionId();
            this.speakingSession = speakingSession;
            this.connectedAt = OffsetDateTime.now();
        }

        public WebSocketSession getFrontendSession() {
            return frontendSession;
        }

        public UUID getUserId() {
            return userId;
        }

        public Long getSpeakingSessionId() {
            return speakingSessionId;
        }

        public SpeakingSessionDTO getSpeakingSession() {
            return speakingSession;
        }

        public void setSpeakingSession(SpeakingSessionDTO speakingSession) {
            this.speakingSession = speakingSession;
        }

        public boolean isFallbackMode() {
            return fallbackMode;
        }

        public void setFallbackMode(boolean fallbackMode) {
            this.fallbackMode = fallbackMode;
        }

        public Integer getCurrentTurnIndex() {
            return currentTurnIndex;
        }

        public void setCurrentTurnIndex(Integer currentTurnIndex) {
            this.currentTurnIndex = currentTurnIndex;
        }

        public SpeakingTurnDTO getCurrentTurn() {
            return currentTurn;
        }

        public void setCurrentTurn(SpeakingTurnDTO currentTurn) {
            this.currentTurn = currentTurn;
        }

        public GeminiLiveConnection getGeminiConnection() {
            return geminiConnection;
        }

        public void setGeminiConnection(GeminiLiveConnection geminiConnection) {
            this.geminiConnection = geminiConnection;
        }

        public OffsetDateTime getConnectedAt() {
            return connectedAt;
        }
    }
}
