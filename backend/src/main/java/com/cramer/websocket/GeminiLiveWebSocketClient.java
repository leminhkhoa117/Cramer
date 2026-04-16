package com.cramer.websocket;

import com.cramer.config.SpeakingGeminiLiveProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gemini Live WebSocket client backed by the JDK {@link java.net.http.WebSocket}.
 *
 * <p>This client is responsible for:
 * <ul>
 *   <li>opening a server-to-server WebSocket session to Gemini Live,</li>
 *   <li>sending the mandatory setup frame,</li>
 *   <li>forwarding question prompts and user audio,</li>
 *   <li>parsing upstream messages into higher-level speaking events.</li>
 * </ul>
 *
 * <p>The implementation intentionally hides the raw Gemini protocol behind
 * {@link GeminiLiveConnection} so the Spring WebSocket handler can focus on
 * session ownership, runtime validation, and FE/BE protocol orchestration.
 */
public class GeminiLiveWebSocketClient implements GeminiLiveConnection {

    private static final Logger logger = LoggerFactory.getLogger(
        GeminiLiveWebSocketClient.class
    );
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private final SpeakingGeminiLiveProperties properties;
    private final String apiKey;
    private final String accentHint;
    private final String speedHint;
    private final GeminiLiveListener listener;

    private final AtomicBoolean open = new AtomicBoolean(false);
    private final AtomicBoolean ready = new AtomicBoolean(false);
    private final AtomicBoolean disposed = new AtomicBoolean(false);
    private final AtomicBoolean examinerSpeaking = new AtomicBoolean(false);
    private final AtomicReference<WebSocket> webSocketRef =
        new AtomicReference<>();

    private final StringBuilder textBuffer = new StringBuilder();

    /**
     * Cap for {@link #textBuffer} reassembly. If upstream floods fragmented text frames
     * beyond this size we bail out and notify an error instead of letting the buffer grow
     * the heap. 1 MiB is far above any realistic Gemini Live response.
     */
    private static final int MAX_TEXT_BUFFER_CHARS = 1_048_576;

    /**
     * Callback contract used by the backend speaking handler.
     */
    public interface GeminiLiveListener {
        void onReady();

        void onExaminerAudio(byte[] audioData, String mimeType);

        void onExaminerTranscript(String text);

        void onUserTranscript(String text);

        void onExaminerSpeaking(boolean speaking);

        void onTurnComplete();

        void onError(String message);

        void onGoAway(String message);

        void onClosed(String reason);
    }

    public GeminiLiveWebSocketClient(
        HttpClient httpClient,
        SpeakingGeminiLiveProperties properties,
        String apiKey,
        GeminiLiveListener listener
    ) {
        this(httpClient, properties, apiKey, null, null, listener);
    }

    public GeminiLiveWebSocketClient(
        HttpClient httpClient,
        SpeakingGeminiLiveProperties properties,
        String apiKey,
        String accentHint,
        String speedHint,
        GeminiLiveListener listener
    ) {
        this.httpClient = Objects.requireNonNull(
            httpClient,
            "httpClient must not be null"
        );
        this.properties = Objects.requireNonNull(
            properties,
            "properties must not be null"
        );
        this.apiKey = requireNonBlank(apiKey, "apiKey must not be blank");
        this.accentHint = normalizeNullableText(accentHint);
        this.speedHint = normalizeNullableText(speedHint);
        this.listener = Objects.requireNonNull(
            listener,
            "listener must not be null"
        );
    }

    @Override
    public void connect() {
        if (disposed.get()) {
            logger.debug(
                "Ignoring connect because Gemini Live client is already disposed"
            );
            return;
        }
        if (open.get()) {
            logger.debug("Gemini Live WebSocket is already open");
            return;
        }

        URI uri = createGeminiUri(properties.getEndpoint(), apiKey);
        logger.info("Connecting to Gemini Live endpoint {}", uri.getHost());

        httpClient
            .newWebSocketBuilder()
            .connectTimeout(
                Duration.ofSeconds(
                    Math.max(1, properties.getConnectTimeoutSeconds())
                )
            )
            .buildAsync(uri, new GeminiWebSocketListener())
            .whenComplete((webSocket, throwable) -> {
                if (throwable != null) {
                    open.set(false);
                    ready.set(false);
                    notifyError(
                        "Failed to connect to Gemini Live: " +
                            throwable.getMessage()
                    );
                    return;
                }

                if (disposed.get()) {
                    try {
                        webSocket.sendClose(
                            WebSocket.NORMAL_CLOSURE,
                            "disposed before connect completed"
                        );
                    } catch (Exception ex) {
                        logger.debug(
                            "Ignoring Gemini close-after-dispose error: {}",
                            ex.getMessage()
                        );
                    }
                    return;
                }

                webSocketRef.compareAndSet(null, webSocket);
                open.set(true);

                // Re-check disposed after publishing the socket. If close() raced
                // with handshake completion, the getAndSet(null) there returned null
                // and left this socket unreferenced. Clean it up here to avoid
                // leaking the upstream connection (file descriptor + heap).
                if (disposed.get()) {
                    WebSocket orphan = webSocketRef.getAndSet(null);
                    open.set(false);
                    ready.set(false);
                    if (orphan != null) {
                        try {
                            orphan.sendClose(
                                WebSocket.NORMAL_CLOSURE,
                                "disposed after connect completed"
                            );
                        } catch (Exception ex) {
                            logger.debug(
                                "Ignoring orphan Gemini close error: {}",
                                ex.getMessage()
                            );
                        }
                    }
                }
            });
    }

    @Override
    public void close() {
        disposed.set(true);
        WebSocket webSocket = webSocketRef.getAndSet(null);
        ready.set(false);
        open.set(false);
        if (examinerSpeaking.getAndSet(false)) {
            safeOnExaminerSpeaking(false);
        }

        if (webSocket != null) {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "closing");
            } catch (Exception ex) {
                logger.debug("Gemini Live close ignored: {}", ex.getMessage());
            }
        }
    }

    @Override
    public boolean isOpen() {
        return open.get();
    }

    @Override
    public boolean isReady() {
        return ready.get();
    }

    @Override
    public void sendQuestionPrompt(JsonNode questionSnapshot) {
        if (!isReady()) {
            logger.warn(
                "Ignoring question prompt because Gemini Live setup is not ready"
            );
            return;
        }
        if (questionSnapshot == null || questionSnapshot.isNull()) {
            throw new IllegalArgumentException(
                "questionSnapshot must not be null"
            );
        }

        String promptText = questionSnapshot
            .path("promptText")
            .asText("")
            .trim();
        if (promptText.isEmpty()) {
            throw new IllegalArgumentException(
                "questionSnapshot.promptText must not be blank"
            );
        }

        String partType = questionSnapshot.path("partType").asText("").trim();
        String topicLabel = questionSnapshot
            .path("topicLabel")
            .asText("")
            .trim();

        ObjectNode textPart = OBJECT_MAPPER.createObjectNode();
        textPart.put(
            "text",
            buildExaminerPrompt(
                questionSnapshot,
                partType,
                topicLabel,
                promptText
            )
        );

        ArrayNode parts = OBJECT_MAPPER.createArrayNode();
        parts.add(textPart);

        ObjectNode turn = OBJECT_MAPPER.createObjectNode();
        turn.put("role", "user");
        turn.set("parts", parts);

        ArrayNode turns = OBJECT_MAPPER.createArrayNode();
        turns.add(turn);

        ObjectNode clientContent = OBJECT_MAPPER.createObjectNode();
        clientContent.set("turns", turns);
        clientContent.put("turnComplete", true);

        ObjectNode payload = OBJECT_MAPPER.createObjectNode();
        payload.set("clientContent", clientContent);

        examinerSpeaking.set(false);
        sendJson(payload);
    }

    @Override
    public void sendAudioChunk(byte[] audioData) {
        if (!isReady()) {
            logger.debug(
                "Ignoring audio chunk because Gemini Live setup is not ready"
            );
            return;
        }
        if (audioData == null || audioData.length == 0) {
            return;
        }

        ObjectNode audio = OBJECT_MAPPER.createObjectNode();
        audio.put("data", Base64.getEncoder().encodeToString(audioData));
        audio.put(
            "mimeType",
            "audio/pcm;rate=" + properties.getInputSampleRate()
        );

        ObjectNode realtimeInput = OBJECT_MAPPER.createObjectNode();
        realtimeInput.set("audio", audio);

        ObjectNode payload = OBJECT_MAPPER.createObjectNode();
        payload.set("realtimeInput", realtimeInput);

        sendJson(payload);
    }

    @Override
    public void sendEndOfTurn() {
        if (!isReady()) {
            logger.debug(
                "Ignoring end-of-turn because Gemini Live setup is not ready"
            );
            return;
        }

        ObjectNode realtimeInput = OBJECT_MAPPER.createObjectNode();
        realtimeInput.put("audioStreamEnd", true);

        ObjectNode payload = OBJECT_MAPPER.createObjectNode();
        payload.set("realtimeInput", realtimeInput);

        sendJson(payload);
    }

    /**
     * Builds the authenticated Gemini Live URI for raw WebSocket usage.
     */
    public static URI createGeminiUri(String endpoint, String apiKey) {
        String normalizedEndpoint = requireNonBlank(
            endpoint,
            "endpoint must not be blank"
        );
        String normalizedApiKey = requireNonBlank(
            apiKey,
            "apiKey must not be blank"
        );
        String encodedApiKey = URLEncoder.encode(
            normalizedApiKey,
            StandardCharsets.UTF_8
        );

        String uri =
            "wss://" +
            normalizedEndpoint +
            "/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent" +
            "?key=" +
            encodedApiKey;
        return URI.create(uri);
    }

    private void sendSetupMessage() {
        ObjectNode setup = OBJECT_MAPPER.createObjectNode();
        setup.put("model", ensureModelResourceName(properties.getModel()));

        ObjectNode generationConfig = OBJECT_MAPPER.createObjectNode();
        ArrayNode responseModalities = OBJECT_MAPPER.createArrayNode();
        responseModalities.add("AUDIO");
        generationConfig.set("responseModalities", responseModalities);

        ObjectNode speechConfig = OBJECT_MAPPER.createObjectNode();
        ObjectNode voiceConfig = OBJECT_MAPPER.createObjectNode();
        ObjectNode prebuiltVoiceConfig = OBJECT_MAPPER.createObjectNode();
        prebuiltVoiceConfig.put("voiceName", properties.getVoiceName());
        voiceConfig.set("prebuiltVoiceConfig", prebuiltVoiceConfig);
        speechConfig.set("voiceConfig", voiceConfig);
        generationConfig.set("speechConfig", speechConfig);

        setup.set("generationConfig", generationConfig);

        ObjectNode systemInstruction = OBJECT_MAPPER.createObjectNode();
        ArrayNode systemParts = OBJECT_MAPPER.createArrayNode();
        ObjectNode systemText = OBJECT_MAPPER.createObjectNode();
        systemText.put("text", examinerSystemInstruction());
        systemParts.add(systemText);
        systemInstruction.set("parts", systemParts);
        setup.set("systemInstruction", systemInstruction);

        ObjectNode inputAudioTranscription = OBJECT_MAPPER.createObjectNode();
        ObjectNode outputAudioTranscription = OBJECT_MAPPER.createObjectNode();
        setup.set("inputAudioTranscription", inputAudioTranscription);
        setup.set("outputAudioTranscription", outputAudioTranscription);

        ObjectNode payload = OBJECT_MAPPER.createObjectNode();
        payload.set("setup", setup);

        sendJson(payload);
    }

    private void sendJson(JsonNode payload) {
        WebSocket webSocket = webSocketRef.get();
        if (webSocket == null) {
            throw new IllegalStateException(
                "Gemini Live WebSocket is not connected"
            );
        }

        try {
            String json = OBJECT_MAPPER.writeValueAsString(payload);
            webSocket
                .sendText(json, true)
                .exceptionally(throwable -> {
                    notifyError(
                        "Failed to send Gemini Live frame: " +
                            throwable.getMessage()
                    );
                    return null;
                });
        } catch (Exception ex) {
            throw new IllegalStateException(
                "Failed to serialize Gemini Live payload",
                ex
            );
        }
    }

    void handleServerMessage(String rawJson) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(rawJson);

            if (root.has("setupComplete")) {
                ready.set(true);
                listener.onReady();
                return;
            }

            if (root.has("goAway")) {
                JsonNode goAway = root.get("goAway");
                String message = "Gemini Live requested graceful shutdown";
                if (goAway != null && goAway.hasNonNull("timeLeft")) {
                    message =
                        message +
                        " (timeLeft=" +
                        goAway.get("timeLeft").asText() +
                        ")";
                }
                listener.onGoAway(message);
                return;
            }

            if (root.has("serverContent")) {
                handleServerContent(root.get("serverContent"));
            }
        } catch (Exception ex) {
            notifyError(
                "Failed to parse Gemini Live message: " + ex.getMessage()
            );
        }
    }

    private void handleServerContent(JsonNode serverContent) {
        if (serverContent == null || serverContent.isNull()) {
            return;
        }

        if (serverContent.path("interrupted").asBoolean(false)) {
            if (examinerSpeaking.getAndSet(false)) {
                safeOnExaminerSpeaking(false);
            }
        }

        JsonNode inputTranscription = serverContent.get("inputTranscription");
        if (
            inputTranscription != null && inputTranscription.hasNonNull("text")
        ) {
            listener.onUserTranscript(inputTranscription.get("text").asText());
        }

        JsonNode outputTranscription = serverContent.get("outputTranscription");
        if (
            outputTranscription != null &&
            outputTranscription.hasNonNull("text")
        ) {
            markExaminerSpeakingStarted();
            listener.onExaminerTranscript(
                outputTranscription.get("text").asText()
            );
        }

        JsonNode modelTurn = serverContent.get("modelTurn");
        if (
            modelTurn != null &&
            modelTurn.has("parts") &&
            modelTurn.get("parts").isArray()
        ) {
            for (JsonNode part : modelTurn.get("parts")) {
                if (part.hasNonNull("text")) {
                    markExaminerSpeakingStarted();
                    listener.onExaminerTranscript(part.get("text").asText());
                }

                JsonNode inlineData = part.get("inlineData");
                if (inlineData != null && inlineData.hasNonNull("data")) {
                    markExaminerSpeakingStarted();
                    String mimeType = inlineData.hasNonNull("mimeType")
                        ? inlineData.get("mimeType").asText()
                        : "audio/pcm;rate=" + properties.getOutputSampleRate();
                    byte[] audioData = Base64.getDecoder().decode(
                        inlineData.get("data").asText()
                    );
                    listener.onExaminerAudio(audioData, mimeType);
                }
            }
        }

        if (serverContent.path("turnComplete").asBoolean(false)) {
            if (examinerSpeaking.getAndSet(false)) {
                safeOnExaminerSpeaking(false);
            }
            listener.onTurnComplete();
        }
    }

    private void markExaminerSpeakingStarted() {
        if (examinerSpeaking.compareAndSet(false, true)) {
            safeOnExaminerSpeaking(true);
        }
    }

    private void safeOnExaminerSpeaking(boolean speaking) {
        try {
            listener.onExaminerSpeaking(speaking);
        } catch (Exception ex) {
            logger.warn(
                "GeminiLiveListener.onExaminerSpeaking failed: {}",
                ex.getMessage()
            );
        }
    }

    private void notifyError(String message) {
        logger.warn(message);
        try {
            listener.onError(message);
        } catch (Exception ex) {
            logger.warn(
                "GeminiLiveListener.onError failed: {}",
                ex.getMessage()
            );
        }
    }

    private String buildExaminerPrompt(
        JsonNode questionSnapshot,
        String partType,
        String topicLabel,
        String promptText
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are conducting an IELTS Speaking test. ");
        prompt.append(
            "Ask the candidate exactly one question naturally and clearly. "
        );
        prompt.append("Do not explain the scoring rubric. ");
        prompt.append("Do not add unrelated commentary. ");

        if (!partType.isBlank()) {
            prompt.append("This is ").append(partType).append(". ");
        }
        if (!topicLabel.isBlank()) {
            prompt.append("Topic: ").append(topicLabel).append(". ");
        }

        prompt.append("Question: ").append(promptText).append(". ");

        if (
            "PART_2".equalsIgnoreCase(partType) &&
            questionSnapshot.has("cueCardBullets")
        ) {
            prompt.append(
                "This is a cue card. Read the cue card and its bullet points once, clearly. "
            );
            JsonNode bullets = questionSnapshot.get("cueCardBullets");
            if (bullets.isArray() && !bullets.isEmpty()) {
                prompt.append("Bullet points: ");
                for (int i = 0; i < bullets.size(); i++) {
                    if (i > 0) {
                        prompt.append(" | ");
                    }
                    prompt.append(bullets.get(i).asText());
                }
                prompt.append(". ");
            }
            if (questionSnapshot.hasNonNull("prepTimeSeconds")) {
                prompt
                    .append("Prep time is ")
                    .append(questionSnapshot.get("prepTimeSeconds").asInt())
                    .append(" seconds. ");
            }
            if (questionSnapshot.hasNonNull("talkTimeSeconds")) {
                prompt
                    .append("Talk time is ")
                    .append(questionSnapshot.get("talkTimeSeconds").asInt())
                    .append(" seconds. ");
            }
        }

        prompt.append("Respond with spoken examiner output only.");
        return prompt.toString();
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String examinerSystemInstruction() {
        StringBuilder instruction = new StringBuilder();
        instruction.append(
            """
            You are an IELTS Speaking examiner.
            Follow the frozen runtime prompt exactly.
            Speak clearly and professionally.
            Keep examiner output concise and natural.
            Do not grade the candidate during the live session.
            Do not invent extra tasks beyond the provided prompt.
            """
        );

        if (accentHint != null) {
            instruction
                .append(System.lineSeparator())
                .append("Preferred examiner accent hint: ")
                .append(accentHint)
                .append('.');
        }

        if (speedHint != null) {
            instruction
                .append(System.lineSeparator())
                .append("Preferred examiner speed hint: ")
                .append(speedHint)
                .append('.');
        }

        return instruction.toString();
    }

    private static String ensureModelResourceName(String model) {
        String normalized = requireNonBlank(
            model,
            "model must not be blank"
        ).trim();
        return normalized.startsWith("models/")
            ? normalized
            : "models/" + normalized;
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private final class GeminiWebSocketListener implements WebSocket.Listener {

        @Override
        public void onOpen(WebSocket webSocket) {
            logger.info("Gemini Live WebSocket opened");
            webSocketRef.compareAndSet(null, webSocket);
            open.set(true);
            WebSocket.Listener.super.onOpen(webSocket);

            if (disposed.get()) {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "disposed");
                webSocket.request(1);
                return;
            }

            sendSetupMessage();
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(
            WebSocket webSocket,
            CharSequence data,
            boolean last
        ) {
            textBuffer.append(data);
            if (textBuffer.length() > MAX_TEXT_BUFFER_CHARS) {
                int overflowSize = textBuffer.length();
                textBuffer.setLength(0);
                notifyError(
                    "Upstream text frame exceeded maximum buffer size (" +
                    overflowSize +
                    " > " +
                    MAX_TEXT_BUFFER_CHARS +
                    " chars); dropping."
                );
                webSocket.request(1);
                return CompletableFuture.completedFuture(null);
            }
            if (last) {
                String payload = textBuffer.toString();
                textBuffer.setLength(0);
                handleServerMessage(payload);
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onBinary(
            WebSocket webSocket,
            ByteBuffer data,
            boolean last
        ) {
            // Gemini Live server messages are expected as JSON text frames.
            // Binary frames are ignored defensively.
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(
            WebSocket webSocket,
            int statusCode,
            String reason
        ) {
            logger.info(
                "Gemini Live WebSocket closed: code={}, reason={}",
                statusCode,
                reason
            );
            open.set(false);
            ready.set(false);
            if (examinerSpeaking.getAndSet(false)) {
                safeOnExaminerSpeaking(false);
            }
            webSocketRef.compareAndSet(webSocket, null);
            if (!disposed.get()) {
                listener.onClosed(reason);
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            open.set(false);
            ready.set(false);
            webSocketRef.compareAndSet(webSocket, null);
            if (examinerSpeaking.getAndSet(false)) {
                safeOnExaminerSpeaking(false);
            }
            if (!disposed.get()) {
                notifyError(
                    "Gemini Live WebSocket error: " + error.getMessage()
                );
            }
        }
    }
}
