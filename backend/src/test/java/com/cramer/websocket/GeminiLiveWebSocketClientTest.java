package com.cramer.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.cramer.config.SpeakingGeminiLiveProperties;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GeminiLiveWebSocketClient Unit Tests")
class GeminiLiveWebSocketClientTest {

    @Test
    @DisplayName(
        "createGeminiUri should build v1beta Gemini WebSocket endpoint and encode API key"
    )
    void createGeminiUri_buildsExpectedUri() {
        URI uri = GeminiLiveWebSocketClient.createGeminiUri(
            "generativelanguage.googleapis.com",
            "abc 123+/="
        );

        assertThat(uri.toString()).isEqualTo(
            "wss://generativelanguage.googleapis.com" +
                "/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent" +
                "?key=abc+123%2B%2F%3D"
        );
    }

    @Test
    @DisplayName(
        "handleServerMessage should mark ready when setupComplete is received"
    )
    void handleServerMessage_setupComplete_marksReadyAndNotifiesListener()
        throws Exception {
        RecordingListener listener = new RecordingListener();
        GeminiLiveWebSocketClient client = newClient(listener);

        invokeHandleServerMessage(client, "{\"setupComplete\":{}}");

        assertThat(client.isReady()).isTrue();
        assertThat(listener.readyCalled).isTrue();
    }

    @Test
    @DisplayName(
        "handleServerMessage should forward transcripts, audio, speaking state, and turn complete"
    )
    void handleServerMessage_serverContent_forwardsEvents() throws Exception {
        RecordingListener listener = new RecordingListener();
        GeminiLiveWebSocketClient client = newClient(listener);

        String audioBase64 = java.util.Base64.getEncoder().encodeToString(
            "pcm-audio".getBytes(StandardCharsets.UTF_8)
        );

        String payload = """
            {
              "serverContent": {
                "inputTranscription": { "text": "candidate answer" },
                "outputTranscription": { "text": "examiner intro" },
                "modelTurn": {
                  "parts": [
                    { "text": "examiner follow-up" },
                    { "inlineData": { "data": "%s", "mimeType": "audio/pcm;rate=24000" } }
                  ]
                },
                "turnComplete": true
              }
            }
            """.formatted(audioBase64);

        invokeHandleServerMessage(client, payload);

        assertThat(listener.examinerSpeakingEvents).containsExactly(
            true,
            false
        );
        assertThat(listener.userTranscripts).containsExactly(
            "candidate answer"
        );
        assertThat(listener.examinerTranscripts).containsExactly(
            "examiner intro",
            "examiner follow-up"
        );
        assertThat(listener.examinerAudio).hasSize(1);
        assertThat(listener.examinerAudio.get(0)).isEqualTo(
            "pcm-audio".getBytes(StandardCharsets.UTF_8)
        );
        assertThat(listener.examinerAudioMimeTypes).containsExactly(
            "audio/pcm;rate=24000"
        );
        assertThat(listener.turnCompleteCount).isEqualTo(1);
    }

    @Test
    @DisplayName("handleServerMessage should notify goAway message")
    void handleServerMessage_goAway_notifiesListener() throws Exception {
        RecordingListener listener = new RecordingListener();
        GeminiLiveWebSocketClient client = newClient(listener);

        invokeHandleServerMessage(
            client,
            """
            {
              "goAway": {
                "timeLeft": "30s"
              }
            }
            """
        );

        assertThat(listener.goAwayMessages)
            .singleElement()
            .asString()
            .contains("timeLeft=30s");
    }

    @Test
    @DisplayName(
        "handleServerMessage should notify parse errors without throwing"
    )
    void handleServerMessage_invalidJson_notifiesError() throws Exception {
        RecordingListener listener = new RecordingListener();
        GeminiLiveWebSocketClient client = newClient(listener);

        invokeHandleServerMessage(client, "{invalid-json");

        assertThat(listener.errors).hasSize(1);
        assertThat(listener.errors.get(0)).contains(
            "Failed to parse Gemini Live message"
        );
    }

    @Test
    @DisplayName(
        "onOpen should send setup payload with model, audio modality, and accent speed hints"
    )
    void onOpen_sendsSetupPayloadWithHints() throws Exception {
        RecordingListener listener = new RecordingListener();
        GeminiLiveWebSocketClient client = newClient(
            listener,
            "australian",
            "1.15"
        );
        RecordingWebSocket webSocket = new RecordingWebSocket();

        Object listenerInstance = newGeminiWebSocketListener(client);
        invokeOnOpen(listenerInstance, webSocket);

        assertThat(webSocket.sentTexts).isNotEmpty();
        assertThat(webSocket.sentTexts.get(0))
            .contains("\"setup\"")
            .contains("\"model\":\"models/gemini-3.1-flash-live-preview\"")
            .contains("\"responseModalities\":[\"AUDIO\"]")
            .contains("\"voiceName\":\"Puck\"")
            .contains("\"inputAudioTranscription\":{}")
            .contains("\"outputAudioTranscription\":{}")
            .contains("Preferred examiner accent hint: australian.")
            .contains("Preferred examiner speed hint: 1.15.");
        assertThat(webSocket.requestedCount).isGreaterThanOrEqualTo(1);
        assertThat(client.isOpen()).isTrue();
    }

    @Test
    @DisplayName("close should dispose client and reset open ready state")
    void close_disposesClientAndResetsFlags() throws Exception {
        RecordingListener listener = new RecordingListener();
        GeminiLiveWebSocketClient client = newClient(listener);
        RecordingWebSocket webSocket = new RecordingWebSocket();

        setAtomicBooleanField(client, "ready", true);
        setWebSocketField(client, webSocket);

        client.close();

        assertThat(client.isOpen()).isFalse();
        assertThat(client.isReady()).isFalse();
        assertThat(webSocket.closeReason).isEqualTo("closing");
    }

    @Test
    @DisplayName(
        "onClose should notify listener only when client is not disposed"
    )
    void onClose_notDisposed_notifiesListener() throws Exception {
        RecordingListener listener = new RecordingListener();
        GeminiLiveWebSocketClient client = newClient(listener);
        RecordingWebSocket webSocket = new RecordingWebSocket();

        setWebSocketField(client, webSocket);
        setAtomicBooleanField(client, "open", true);
        setAtomicBooleanField(client, "ready", true);

        Object listenerInstance = newGeminiWebSocketListener(client);
        invokeOnClose(listenerInstance, webSocket, 1000, "upstream closed");

        assertThat(client.isOpen()).isFalse();
        assertThat(client.isReady()).isFalse();
        assertThat(listener.closedReasons).containsExactly("upstream closed");
    }

    @Test
    @DisplayName("onClose should not notify listener after local dispose")
    void onClose_disposed_doesNotNotifyListener() throws Exception {
        RecordingListener listener = new RecordingListener();
        GeminiLiveWebSocketClient client = newClient(listener);
        RecordingWebSocket webSocket = new RecordingWebSocket();

        setWebSocketField(client, webSocket);
        setAtomicBooleanField(client, "open", true);
        setAtomicBooleanField(client, "ready", true);
        setAtomicBooleanField(client, "disposed", true);

        Object listenerInstance = newGeminiWebSocketListener(client);
        invokeOnClose(listenerInstance, webSocket, 1000, "disposed");

        assertThat(listener.closedReasons).isEmpty();
        assertThat(client.isOpen()).isFalse();
        assertThat(client.isReady()).isFalse();
    }

    private GeminiLiveWebSocketClient newClient(RecordingListener listener)
        throws Exception {
        return newClient(listener, null, null);
    }

    private GeminiLiveWebSocketClient newClient(
        RecordingListener listener,
        String accentHint,
        String speedHint
    ) throws Exception {
        SpeakingGeminiLiveProperties properties =
            new SpeakingGeminiLiveProperties();
        properties.setEndpoint("generativelanguage.googleapis.com");
        properties.setModel("gemini-3.1-flash-live-preview");
        properties.setVoiceName("Puck");
        properties.setConnectTimeoutSeconds(5);
        properties.setInputSampleRate(16000);
        properties.setOutputSampleRate(24000);

        GeminiLiveWebSocketClient client = new GeminiLiveWebSocketClient(
            HttpClient.newHttpClient(),
            properties,
            "test-api-key",
            accentHint,
            speedHint,
            listener
        );

        setAtomicBooleanField(client, "open", true);
        return client;
    }

    private void invokeHandleServerMessage(
        GeminiLiveWebSocketClient client,
        String payload
    ) throws Exception {
        Method method = GeminiLiveWebSocketClient.class.getDeclaredMethod(
            "handleServerMessage",
            String.class
        );
        method.setAccessible(true);
        method.invoke(client, payload);
    }

    private void setAtomicBooleanField(
        GeminiLiveWebSocketClient client,
        String fieldName,
        boolean value
    ) throws Exception {
        Field field = GeminiLiveWebSocketClient.class.getDeclaredField(
            fieldName
        );
        field.setAccessible(true);
        java.util.concurrent.atomic.AtomicBoolean atomicBoolean =
            (java.util.concurrent.atomic.AtomicBoolean) field.get(client);
        atomicBoolean.set(value);
    }

    private void setWebSocketField(
        GeminiLiveWebSocketClient client,
        RecordingWebSocket webSocket
    ) throws Exception {
        Field field = GeminiLiveWebSocketClient.class.getDeclaredField(
            "webSocketRef"
        );
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.concurrent.atomic.AtomicReference<
            java.net.http.WebSocket
        > reference = (java.util.concurrent.atomic.AtomicReference<
            java.net.http.WebSocket
        >) field.get(client);
        reference.set(webSocket);
    }

    private Object newGeminiWebSocketListener(GeminiLiveWebSocketClient client)
        throws Exception {
        Class<?> listenerClass = Class.forName(
            "com.cramer.websocket.GeminiLiveWebSocketClient$GeminiWebSocketListener"
        );
        java.lang.reflect.Constructor<?> constructor =
            listenerClass.getDeclaredConstructor(
                GeminiLiveWebSocketClient.class
            );
        constructor.setAccessible(true);
        return constructor.newInstance(client);
    }

    private void invokeOnOpen(
        Object listenerInstance,
        RecordingWebSocket webSocket
    ) throws Exception {
        Method method = listenerInstance
            .getClass()
            .getDeclaredMethod("onOpen", java.net.http.WebSocket.class);
        method.setAccessible(true);
        method.invoke(listenerInstance, webSocket);
    }

    private void invokeOnClose(
        Object listenerInstance,
        RecordingWebSocket webSocket,
        int statusCode,
        String reason
    ) throws Exception {
        Method method = listenerInstance
            .getClass()
            .getDeclaredMethod(
                "onClose",
                java.net.http.WebSocket.class,
                int.class,
                String.class
            );
        method.setAccessible(true);
        method.invoke(listenerInstance, webSocket, statusCode, reason);
    }

    private static final class RecordingWebSocket
        implements java.net.http.WebSocket
    {

        private final List<String> sentTexts = new ArrayList<>();
        private int requestedCount;
        private String closeReason;

        @Override
        public CompletableFuture<java.net.http.WebSocket> sendText(
            CharSequence data,
            boolean last
        ) {
            this.sentTexts.add(data.toString());
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<java.net.http.WebSocket> sendBinary(
            java.nio.ByteBuffer data,
            boolean last
        ) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<java.net.http.WebSocket> sendPing(
            java.nio.ByteBuffer message
        ) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<java.net.http.WebSocket> sendPong(
            java.nio.ByteBuffer message
        ) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<java.net.http.WebSocket> sendClose(
            int statusCode,
            String reason
        ) {
            this.closeReason = reason;
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public void request(long n) {
            this.requestedCount += (int) n;
        }

        @Override
        public String getSubprotocol() {
            return null;
        }

        @Override
        public boolean isOutputClosed() {
            return false;
        }

        @Override
        public boolean isInputClosed() {
            return false;
        }

        @Override
        public void abort() {}
    }

    private static class RecordingListener
        implements GeminiLiveWebSocketClient.GeminiLiveListener
    {

        private boolean readyCalled;
        private final List<byte[]> examinerAudio = new ArrayList<>();
        private final List<String> examinerAudioMimeTypes = new ArrayList<>();
        private final List<String> examinerTranscripts = new ArrayList<>();
        private final List<String> userTranscripts = new ArrayList<>();
        private final List<Boolean> examinerSpeakingEvents = new ArrayList<>();
        private int turnCompleteCount;
        private final List<String> errors = new ArrayList<>();
        private final List<String> goAwayMessages = new ArrayList<>();
        private final List<String> closedReasons = new ArrayList<>();

        @Override
        public void onReady() {
            this.readyCalled = true;
        }

        @Override
        public void onExaminerAudio(byte[] audioData, String mimeType) {
            this.examinerAudio.add(audioData);
            this.examinerAudioMimeTypes.add(mimeType);
        }

        @Override
        public void onExaminerTranscript(String text) {
            this.examinerTranscripts.add(text);
        }

        @Override
        public void onUserTranscript(String text) {
            this.userTranscripts.add(text);
        }

        @Override
        public void onExaminerSpeaking(boolean speaking) {
            this.examinerSpeakingEvents.add(speaking);
        }

        @Override
        public void onTurnComplete() {
            this.turnCompleteCount++;
        }

        @Override
        public void onError(String message) {
            this.errors.add(message);
        }

        @Override
        public void onGoAway(String message) {
            this.goAwayMessages.add(message);
        }

        @Override
        public void onClosed(String reason) {
            this.closedReasons.add(reason);
        }
    }
}
