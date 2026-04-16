package com.cramer.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cramer.config.SpeakingGeminiLiveProperties;
import com.cramer.dto.SpeakingSessionDTO;
import com.cramer.dto.SpeakingTurnDTO;
import com.cramer.service.SpeakingSessionService;
import com.cramer.util.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@DisplayName("SpeakingWebSocketHandler Unit Tests")
@SuppressWarnings("null")
class SpeakingWebSocketHandlerTest {

    private static final String VALID_TOKEN = "valid-token";

    private static final UUID USER_ID = UUID.fromString(
        "00000000-0000-0000-0000-000000000123"
    );

    private SpeakingSessionService speakingSessionService;
    private JwtUtil jwtUtil;
    private ObjectMapper objectMapper;
    private SpeakingGeminiLiveProperties geminiLiveProperties;

    @BeforeEach
    void setUp() {
        speakingSessionService = mock(SpeakingSessionService.class);
        jwtUtil = mock(JwtUtil.class);
        objectMapper = new ObjectMapper();

        geminiLiveProperties = new SpeakingGeminiLiveProperties();
        geminiLiveProperties.setEnabled(true);
    }

    @Test
    @DisplayName(
        "afterConnectionEstablished should emit connecting status and live callbacks in live mode"
    )
    void afterConnectionEstablished_liveMode_bridgesCallbacksToFrontend()
        throws Exception {
        SpeakingSessionDTO speakingSession = openSpeakingSession();
        WebSocketSession webSocketSession = mockWebSocketSession(
            "ws-live-1",
            URI.create("ws://localhost/ws/speaking/42?token=" + VALID_TOKEN),
            true
        );

        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.extractUserId(VALID_TOKEN)).thenReturn(USER_ID.toString());
        when(speakingSessionService.getSession(42L, USER_ID)).thenReturn(
            speakingSession
        );

        CapturedMessages capturedMessages = new CapturedMessages();
        captureMessages(webSocketSession, capturedMessages);

        FakeGeminiConnection fakeConnection = new FakeGeminiConnection();
        SpeakingWebSocketHandler handler = new SpeakingWebSocketHandler(
            speakingSessionService,
            jwtUtil,
            objectMapper,
            geminiLiveProperties,
            "gemini-api-key",
            (state, listener) -> {
                fakeConnection.listener = listener;
                return fakeConnection;
            }
        );

        handler.afterConnectionEstablished(webSocketSession);

        assertThat(fakeConnection.connectCalled).isTrue();
        assertThat(capturedMessages.textPayloads).hasSize(1);
        assertThat(capturedMessages.textPayloads.get(0))
            .contains("\"type\":\"status\"")
            .contains("\"status\":\"connecting\"");

        fakeConnection.listener.onReady();
        fakeConnection.listener.onExaminerSpeaking(true);
        fakeConnection.listener.onExaminerTranscript("Examiner prompt");
        fakeConnection.listener.onUserTranscript("Candidate answer");
        fakeConnection.listener.onExaminerAudio(
            new byte[] { 9, 8, 7 },
            "audio/pcm;rate=24000"
        );
        fakeConnection.listener.onExaminerSpeaking(false);
        fakeConnection.listener.onTurnComplete();

        assertThat(capturedMessages.textPayloads).hasSize(7);
        assertThat(capturedMessages.textPayloads.get(1)).contains(
            "\"status\":\"ready\""
        );
        assertThat(capturedMessages.textPayloads.get(2))
            .contains("\"type\":\"examiner_speaking\"")
            .contains("\"speaking\":true");
        assertThat(capturedMessages.textPayloads.get(3))
            .contains("\"type\":\"transcript\"")
            .contains("\"source\":\"examiner\"")
            .contains("Examiner prompt");
        assertThat(capturedMessages.textPayloads.get(4))
            .contains("\"type\":\"transcript\"")
            .contains("\"source\":\"user\"")
            .contains("Candidate answer");
        assertThat(capturedMessages.textPayloads.get(5))
            .contains("\"type\":\"examiner_speaking\"")
            .contains("\"speaking\":false");
        assertThat(capturedMessages.binaryPayloads).containsExactly(3);
        assertThat(capturedMessages.textPayloads).anySatisfy(payload ->
            assertThat(payload).contains("\"type\":\"turn_complete\"")
        );
    }

    @Test
    @DisplayName(
        "afterConnectionEstablished should trust handshake user id when interceptor already authenticated the socket"
    )
    void afterConnectionEstablished_usesHandshakeAuthenticatedUserId()
        throws Exception {
        SpeakingSessionDTO speakingSession = openSpeakingSession();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(
            SpeakingWebSocketAuthSupport.AUTHENTICATED_USER_ID_ATTR,
            USER_ID.toString()
        );
        attributes.put(
            SpeakingWebSocketAuthSupport.AUTH_TOKEN_SOURCE_ATTR,
            SpeakingWebSocketAuthSupport.AUTH_TOKEN_SOURCE_QUERY_PARAMETER
        );

        WebSocketSession webSocketSession = mockWebSocketSession(
            "ws-live-handshake",
            URI.create("ws://localhost/ws/speaking/42"),
            true,
            attributes
        );

        when(speakingSessionService.getSession(42L, USER_ID)).thenReturn(
            speakingSession
        );

        CapturedMessages capturedMessages = new CapturedMessages();
        captureMessages(webSocketSession, capturedMessages);

        FakeGeminiConnection fakeConnection = new FakeGeminiConnection();
        SpeakingWebSocketHandler handler = new SpeakingWebSocketHandler(
            speakingSessionService,
            jwtUtil,
            objectMapper,
            geminiLiveProperties,
            "gemini-api-key",
            (state, listener) -> {
                fakeConnection.listener = listener;
                return fakeConnection;
            }
        );

        handler.afterConnectionEstablished(webSocketSession);

        assertThat(fakeConnection.connectCalled).isTrue();
        verify(jwtUtil, never()).validateToken(any());
        verify(jwtUtil, never()).extractUserId(any());
        verify(speakingSessionService).getSession(42L, USER_ID);
        assertThat(capturedMessages.textPayloads)
            .singleElement()
            .satisfies(payload ->
                assertThat(payload).asString().contains("\"status\":\"connecting\"")
            );
    }

    @Test
    @DisplayName(
        "start_question should use refreshed session state instead of stale cached turns"
    )
    void handleStartQuestion_refreshesSessionStateBeforeResolvingTurn()
        throws Exception {
        SpeakingSessionDTO initialSession = openSpeakingSession();
        SpeakingSessionDTO refreshedSession = openSpeakingSession();
        refreshedSession.getTurns().get(0).getQuestionSnapshot().deepCopy();
        ObjectNode refreshedSnapshot = objectMapper.createObjectNode();
        refreshedSnapshot.put("schemaVersion", 1);
        refreshedSnapshot.put("partType", "PART_1");
        refreshedSnapshot.put("promptText", "What changed after refresh?");
        refreshedSnapshot.put("topicLabel", "Refresh");
        refreshedSession.setTurns(
            List.of(
                SpeakingTurnDTO.builder()
                    .turnIndex(1)
                    .partNumber(1)
                    .sourceQuestionId(999L)
                    .questionSnapshot(refreshedSnapshot)
                    .build()
            )
        );

        WebSocketSession webSocketSession = mockWebSocketSession(
            "ws-live-2",
            URI.create("ws://localhost/ws/speaking/42?token=" + VALID_TOKEN),
            true
        );

        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.extractUserId(VALID_TOKEN)).thenReturn(USER_ID.toString());
        when(speakingSessionService.getSession(42L, USER_ID))
            .thenReturn(initialSession)
            .thenReturn(refreshedSession);

        CapturedMessages capturedMessages = new CapturedMessages();
        captureMessages(webSocketSession, capturedMessages);

        FakeGeminiConnection fakeConnection = new FakeGeminiConnection();
        fakeConnection.open = true;
        fakeConnection.ready = true;

        SpeakingWebSocketHandler handler = new SpeakingWebSocketHandler(
            speakingSessionService,
            jwtUtil,
            objectMapper,
            geminiLiveProperties,
            "gemini-api-key",
            (state, listener) -> {
                fakeConnection.listener = listener;
                return fakeConnection;
            }
        );

        handler.afterConnectionEstablished(webSocketSession);
        handler.handleTextMessage(
            webSocketSession,
            new TextMessage("{\"type\":\"start_question\",\"turnIndex\":1}")
        );

        assertThat(fakeConnection.lastQuestionSnapshot).isNotNull();
        assertThat(
            fakeConnection.lastQuestionSnapshot.get("promptText").asText()
        ).isEqualTo("What changed after refresh?");
        verify(speakingSessionService, times(2)).getSession(42L, USER_ID);
    }

    @Test
    @DisplayName(
        "stale finalized session should emit error and closed status then close the socket"
    )
    void handleStartQuestion_whenSessionBecomesFinalized_failsClosed()
        throws Exception {
        SpeakingSessionDTO initialSession = openSpeakingSession();
        SpeakingSessionDTO finalizedSession = openSpeakingSession();
        finalizedSession.setIsFinalized(true);

        WebSocketSession webSocketSession = mockWebSocketSession(
            "ws-live-3",
            URI.create("ws://localhost/ws/speaking/42?token=" + VALID_TOKEN),
            true
        );

        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.extractUserId(VALID_TOKEN)).thenReturn(USER_ID.toString());
        when(speakingSessionService.getSession(42L, USER_ID))
            .thenReturn(initialSession)
            .thenReturn(finalizedSession);

        CapturedMessages capturedMessages = new CapturedMessages();
        captureMessages(webSocketSession, capturedMessages);

        FakeGeminiConnection fakeConnection = new FakeGeminiConnection();
        fakeConnection.open = true;
        fakeConnection.ready = true;

        SpeakingWebSocketHandler handler = new SpeakingWebSocketHandler(
            speakingSessionService,
            jwtUtil,
            objectMapper,
            geminiLiveProperties,
            "gemini-api-key",
            (state, listener) -> {
                fakeConnection.listener = listener;
                return fakeConnection;
            }
        );

        handler.afterConnectionEstablished(webSocketSession);
        handler.handleTextMessage(
            webSocketSession,
            new TextMessage("{\"type\":\"start_question\",\"turnIndex\":1}")
        );

        assertThat(capturedMessages.textPayloads).anySatisfy(payload ->
            assertThat(payload)
                .contains("\"type\":\"error\"")
                .contains("already finalized")
        );
        assertThat(capturedMessages.textPayloads).anySatisfy(payload ->
            assertThat(payload)
                .contains("\"type\":\"status\"")
                .contains("\"status\":\"closed\"")
        );
        verifyCloseCalled(webSocketSession);
    }

    @Test
    @DisplayName(
        "upstream error should downgrade the session to fallback text mode"
    )
    void liveMode_upstreamError_transitionsToFallback() throws Exception {
        SpeakingSessionDTO speakingSession = openSpeakingSession();
        WebSocketSession webSocketSession = mockWebSocketSession(
            "ws-live-4",
            URI.create("ws://localhost/ws/speaking/42?token=" + VALID_TOKEN),
            true
        );

        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.extractUserId(VALID_TOKEN)).thenReturn(USER_ID.toString());
        when(speakingSessionService.getSession(42L, USER_ID))
            .thenReturn(speakingSession)
            .thenReturn(speakingSession);

        CapturedMessages capturedMessages = new CapturedMessages();
        captureMessages(webSocketSession, capturedMessages);

        FakeGeminiConnection fakeConnection = new FakeGeminiConnection();
        fakeConnection.open = true;
        fakeConnection.ready = true;

        SpeakingWebSocketHandler handler = new SpeakingWebSocketHandler(
            speakingSessionService,
            jwtUtil,
            objectMapper,
            geminiLiveProperties,
            "gemini-api-key",
            (state, listener) -> {
                fakeConnection.listener = listener;
                return fakeConnection;
            }
        );

        handler.afterConnectionEstablished(webSocketSession);
        fakeConnection.listener.onError("Gemini upstream failed");

        assertThat(fakeConnection.closeCalled).isTrue();
        assertThat(capturedMessages.textPayloads).anySatisfy(payload ->
            assertThat(payload)
                .contains("\"type\":\"status\"")
                .contains("\"status\":\"fallback_text_mode\"")
                .contains("Gemini upstream failed")
        );

        handler.handleTextMessage(
            webSocketSession,
            new TextMessage("{\"type\":\"start_question\",\"turnIndex\":1}")
        );

        assertThat(capturedMessages.textPayloads).anySatisfy(payload ->
            assertThat(payload)
                .contains("\"type\":\"transcript\"")
                .contains("\"source\":\"examiner\"")
                .contains("What do you do on weekends?")
        );
        assertThat(capturedMessages.textPayloads).anySatisfy(payload ->
            assertThat(payload).contains("\"type\":\"turn_complete\"")
        );
    }

    @Test
    @DisplayName(
        "afterConnectionEstablished should enter fallback mode when Gemini API key is missing"
    )
    void afterConnectionEstablished_withoutApiKey_entersFallbackMode()
        throws Exception {
        SpeakingSessionDTO speakingSession = openSpeakingSession();
        WebSocketSession webSocketSession = mockWebSocketSession(
            "ws-1",
            URI.create("ws://localhost/ws/speaking/42?token=valid-token"),
            true
        );

        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractUserId("valid-token")).thenReturn(
            USER_ID.toString()
        );
        when(speakingSessionService.getSession(42L, USER_ID)).thenReturn(
            speakingSession
        );

        CapturedMessages capturedMessages = new CapturedMessages();
        captureMessages(webSocketSession, capturedMessages);

        SpeakingWebSocketHandler handler = new SpeakingWebSocketHandler(
            speakingSessionService,
            jwtUtil,
            objectMapper,
            geminiLiveProperties,
            ""
        );

        handler.afterConnectionEstablished(webSocketSession);

        assertThat(capturedMessages.textPayloads)
            .hasSize(1)
            .satisfies(messages ->
                assertThat(messages.get(0))
                    .contains("\"type\":\"status\"")
                    .contains("\"status\":\"fallback_text_mode\"")
            );
        verify(speakingSessionService).getSession(42L, USER_ID);
    }

    @Test
    @DisplayName(
        "start_question should emit examiner transcript and turn_complete in fallback mode"
    )
    void handleStartQuestion_inFallbackMode_returnsPromptAndTurnComplete()
        throws Exception {
        SpeakingSessionDTO speakingSession = openSpeakingSession();
        WebSocketSession webSocketSession = mockWebSocketSession(
            "ws-2",
            URI.create("ws://localhost/ws/speaking/42?token=valid-token"),
            true
        );

        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractUserId("valid-token")).thenReturn(
            USER_ID.toString()
        );
        when(speakingSessionService.getSession(42L, USER_ID)).thenReturn(
            speakingSession
        );

        CapturedMessages capturedMessages = new CapturedMessages();
        captureMessages(webSocketSession, capturedMessages);

        SpeakingWebSocketHandler handler = new SpeakingWebSocketHandler(
            speakingSessionService,
            jwtUtil,
            objectMapper,
            geminiLiveProperties,
            ""
        );

        handler.afterConnectionEstablished(webSocketSession);
        handler.handleTextMessage(
            webSocketSession,
            new TextMessage("{\"type\":\"start_question\",\"turnIndex\":1}")
        );

        assertThat(capturedMessages.textPayloads).hasSize(3);
        assertThat(capturedMessages.textPayloads.get(1))
            .contains("\"type\":\"transcript\"")
            .contains("\"source\":\"examiner\"")
            .contains("What do you do on weekends?");
        assertThat(capturedMessages.textPayloads.get(2)).contains(
            "\"type\":\"turn_complete\""
        );
    }

    @Test
    @DisplayName(
        "afterConnectionEstablished should reject invalid JWT and close the socket"
    )
    void afterConnectionEstablished_invalidJwt_closesSocket() throws Exception {
        WebSocketSession webSocketSession = mockWebSocketSession(
            "ws-3",
            URI.create("ws://localhost/ws/speaking/42?token=invalid-token"),
            true
        );

        when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

        CapturedMessages capturedMessages = new CapturedMessages();
        captureMessages(webSocketSession, capturedMessages);

        SpeakingWebSocketHandler handler = new SpeakingWebSocketHandler(
            speakingSessionService,
            jwtUtil,
            objectMapper,
            geminiLiveProperties,
            ""
        );

        handler.afterConnectionEstablished(webSocketSession);

        assertThat(capturedMessages.textPayloads).hasSize(1);
        assertThat(capturedMessages.textPayloads.get(0))
            .contains("\"type\":\"error\"")
            .contains("Invalid or expired JWT");
        verifyCloseCalled(webSocketSession);
        verify(speakingSessionService, never()).getSession(any(), any());
    }

    @Test
    @DisplayName(
        "afterConnectionEstablished should reject finalized speaking sessions"
    )
    void afterConnectionEstablished_finalizedSession_closesSocket()
        throws Exception {
        SpeakingSessionDTO speakingSession = openSpeakingSession();
        speakingSession.setIsFinalized(true);

        WebSocketSession webSocketSession = mockWebSocketSession(
            "ws-4",
            URI.create("ws://localhost/ws/speaking/77?token=valid-token"),
            true
        );

        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractUserId("valid-token")).thenReturn(
            USER_ID.toString()
        );
        when(speakingSessionService.getSession(77L, USER_ID)).thenReturn(
            speakingSession
        );

        CapturedMessages capturedMessages = new CapturedMessages();
        captureMessages(webSocketSession, capturedMessages);

        SpeakingWebSocketHandler handler = new SpeakingWebSocketHandler(
            speakingSessionService,
            jwtUtil,
            objectMapper,
            geminiLiveProperties,
            ""
        );

        handler.afterConnectionEstablished(webSocketSession);

        assertThat(capturedMessages.textPayloads).hasSize(1);
        assertThat(capturedMessages.textPayloads.get(0))
            .contains("\"type\":\"error\"")
            .contains("already finalized");
        verifyCloseCalled(webSocketSession);
    }

    @Test
    @DisplayName("binary audio should be ignored in fallback mode")
    void handleBinaryMessage_inFallbackMode_ignoresAudio() throws Exception {
        SpeakingSessionDTO speakingSession = openSpeakingSession();
        WebSocketSession webSocketSession = mockWebSocketSession(
            "ws-5",
            URI.create("ws://localhost/ws/speaking/42?token=valid-token"),
            true
        );

        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractUserId("valid-token")).thenReturn(
            USER_ID.toString()
        );
        when(speakingSessionService.getSession(42L, USER_ID)).thenReturn(
            speakingSession
        );

        CapturedMessages capturedMessages = new CapturedMessages();
        captureMessages(webSocketSession, capturedMessages);

        SpeakingWebSocketHandler handler = new SpeakingWebSocketHandler(
            speakingSessionService,
            jwtUtil,
            objectMapper,
            geminiLiveProperties,
            ""
        );

        handler.afterConnectionEstablished(webSocketSession);
        handler.handleBinaryMessage(
            webSocketSession,
            new BinaryMessage(new byte[] { 1, 2, 3, 4 })
        );

        assertThat(capturedMessages.binaryPayloads).isEmpty();
        assertThat(capturedMessages.textPayloads)
            .hasSize(1)
            .satisfies(messages ->
                assertThat(messages.get(0)).contains(
                    "\"status\":\"fallback_text_mode\""
                )
            );
    }

    @Test
    @DisplayName(
        "afterConnectionEstablished should reject when user does not own the speaking session"
    )
    void afterConnectionEstablished_crossUserOwnership_closesSocket()
        throws Exception {
        UUID otherUserId = UUID.fromString(
            "11111111-1111-1111-1111-111111111111"
        );

        WebSocketSession webSocketSession = mockWebSocketSession(
            "ws-cross-user",
            URI.create(
                "ws://localhost/ws/speaking/42?token=" + VALID_TOKEN
            ),
            true
        );

        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.extractUserId(VALID_TOKEN)).thenReturn(
            otherUserId.toString()
        );
        when(
            speakingSessionService.getSession(42L, otherUserId)
        ).thenThrow(
            new IllegalArgumentException(
                "Speaking session 42 not found for user."
            )
        );

        CapturedMessages capturedMessages = new CapturedMessages();
        captureMessages(webSocketSession, capturedMessages);

        FakeGeminiConnection fakeConnection = new FakeGeminiConnection();
        SpeakingWebSocketHandler handler = new SpeakingWebSocketHandler(
            speakingSessionService,
            jwtUtil,
            objectMapper,
            geminiLiveProperties,
            "gemini-api-key",
            (state, listener) -> {
                fakeConnection.listener = listener;
                return fakeConnection;
            }
        );

        handler.afterConnectionEstablished(webSocketSession);

        assertThat(fakeConnection.connectCalled).isFalse();
        verifyCloseCalled(webSocketSession);
    }

    @Test
    @DisplayName(
        "upstream onGoAway should also transition the session to fallback text mode"
    )
    void liveMode_upstreamGoAway_transitionsToFallback() throws Exception {
        SpeakingSessionDTO speakingSession = openSpeakingSession();
        WebSocketSession webSocketSession = mockWebSocketSession(
            "ws-goaway",
            URI.create("ws://localhost/ws/speaking/42?token=" + VALID_TOKEN),
            true
        );

        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.extractUserId(VALID_TOKEN)).thenReturn(USER_ID.toString());
        when(speakingSessionService.getSession(42L, USER_ID))
            .thenReturn(speakingSession);

        CapturedMessages capturedMessages = new CapturedMessages();
        captureMessages(webSocketSession, capturedMessages);

        FakeGeminiConnection fakeConnection = new FakeGeminiConnection();
        fakeConnection.open = true;
        fakeConnection.ready = true;

        SpeakingWebSocketHandler handler = new SpeakingWebSocketHandler(
            speakingSessionService,
            jwtUtil,
            objectMapper,
            geminiLiveProperties,
            "gemini-api-key",
            (state, listener) -> {
                fakeConnection.listener = listener;
                return fakeConnection;
            }
        );

        handler.afterConnectionEstablished(webSocketSession);
        fakeConnection.listener.onGoAway("Gemini quota exceeded");

        assertThat(fakeConnection.closeCalled).isTrue();
        assertThat(capturedMessages.textPayloads).anySatisfy(payload ->
            assertThat(payload)
                .contains("\"type\":\"status\"")
                .contains("\"status\":\"fallback_text_mode\"")
        );
    }

    private SpeakingSessionDTO openSpeakingSession() {
        ObjectNode questionSnapshot = objectMapper.createObjectNode();
        questionSnapshot.put("schemaVersion", 1);
        questionSnapshot.put("partType", "PART_1");
        questionSnapshot.put("promptText", "What do you do on weekends?");
        questionSnapshot.put("topicLabel", "Weekend");

        SpeakingTurnDTO turn = SpeakingTurnDTO.builder()
            .turnIndex(1)
            .partNumber(1)
            .sourceQuestionId(501L)
            .questionSnapshot(questionSnapshot)
            .build();

        return SpeakingSessionDTO.builder()
            .sessionId(42L)
            .sessionMode("FULL")
            .testId(32L)
            .status("in_progress")
            .isFinalized(false)
            .luaCost(15)
            .accent("british")
            .speed(new BigDecimal("1.00"))
            .turns(List.of(turn))
            .build();
    }

    private WebSocketSession mockWebSocketSession(
        String sessionId,
        URI uri,
        boolean open
    ) {
        return mockWebSocketSession(sessionId, uri, open, new HashMap<>());
    }

    private WebSocketSession mockWebSocketSession(
        String sessionId,
        URI uri,
        boolean open,
        Map<String, Object> attributes
    ) {
        WebSocketSession session = mock(WebSocketSession.class);
        HttpHeaders headers = new HttpHeaders();

        when(session.getId()).thenReturn(sessionId);
        when(session.getUri()).thenReturn(uri);
        when(session.getHandshakeHeaders()).thenReturn(headers);
        when(session.getAttributes()).thenReturn(attributes);
        when(session.isOpen()).thenReturn(open);

        return session;
    }

    private void captureMessages(
        WebSocketSession session,
        CapturedMessages capturedMessages
    ) throws Exception {
        doAnswer(invocation -> {
            Object message = invocation.getArgument(0);
            if (message instanceof TextMessage textMessage) {
                capturedMessages.textPayloads.add(textMessage.getPayload());
            } else if (message instanceof BinaryMessage binaryMessage) {
                capturedMessages.binaryPayloads.add(
                    binaryMessage.getPayloadLength()
                );
            }
            return null;
        })
            .when(session)
            .sendMessage(any());
    }

    @SuppressWarnings("null")
    private void verifyCloseCalled(WebSocketSession session) throws Exception {
        verify(session).close(any(CloseStatus.class));
    }

    private static final class CapturedMessages {

        private final java.util.List<String> textPayloads =
            new java.util.ArrayList<>();
        private final java.util.List<Integer> binaryPayloads =
            new java.util.ArrayList<>();
    }

    private static final class FakeGeminiConnection
        implements GeminiLiveConnection
    {

        private boolean connectCalled;
        private boolean closeCalled;
        private boolean open;
        private boolean ready;
        private JsonNode lastQuestionSnapshot;
        private GeminiLiveWebSocketClient.GeminiLiveListener listener;

        @Override
        public void connect() {
            this.connectCalled = true;
            this.open = true;
        }

        @Override
        public void close() {
            this.closeCalled = true;
            this.open = false;
            this.ready = false;
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public boolean isReady() {
            return ready;
        }

        @Override
        public void sendQuestionPrompt(JsonNode questionSnapshot) {
            this.lastQuestionSnapshot =
                questionSnapshot == null ? null : questionSnapshot.deepCopy();
        }

        @Override
        public void sendAudioChunk(byte[] audioData) {}

        @Override
        public void sendEndOfTurn() {}
    }
}
