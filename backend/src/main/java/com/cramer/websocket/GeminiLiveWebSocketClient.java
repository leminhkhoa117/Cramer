package com.cramer.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.function.Consumer;

/**
 * WebSocket client for connecting to Gemini Live API.
 * 
 * This client handles bidirectional audio streaming with Gemini:
 * - Sends user audio to Gemini for processing
 * - Receives Gemini's audio response and transcription
 * - Handles turn detection and barge-in
 * 
 * Based on Gemini Live API documentation:
 * https://ai.google.dev/api/multimodal-live
 * 
 * Model: gemini-2.0-flash-live (native audio I/O)
 */
public class GeminiLiveWebSocketClient extends WebSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(GeminiLiveWebSocketClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String sessionId;
    private final String apiKey;
    private final Consumer<byte[]> onAudioReceived;
    private final Consumer<String> onTranscriptReceived;
    private final Consumer<String> onUserTranscriptReceived; // User speech transcript
    private final Consumer<String> onTurnStart;
    private final Consumer<String> onTurnEnd;
    private final Runnable onSetupComplete;
    
    private boolean isSetupComplete = false;

    /**
     * System instruction for the IELTS examiner role.
     */
    private static final String EXAMINER_SYSTEM_INSTRUCTION = """
        You are an IELTS Speaking examiner. Your role is to:
        1. Ask questions naturally, following IELTS Speaking test format
        2. Listen to candidate responses without interrupting unless necessary
        3. Use natural follow-up questions based on their answers
        4. Maintain a professional but friendly tone
        5. Keep track of time for each part (Part 1: 4-5 min, Part 2: 3-4 min, Part 3: 4-5 min)
        
        Voice style: Clear, professional British English accent.
        Do NOT provide feedback or scores during the test.
        When the candidate finishes speaking or pauses for too long, ask the next question.
        """;

    public GeminiLiveWebSocketClient(
            URI serverUri,
            String sessionId,
            String apiKey,
            Consumer<byte[]> onAudioReceived,
            Consumer<String> onTranscriptReceived,
            Consumer<String> onUserTranscriptReceived,
            Consumer<String> onTurnStart,
            Consumer<String> onTurnEnd,
            Runnable onSetupComplete) {
        super(serverUri);
        this.sessionId = sessionId;
        this.apiKey = apiKey;
        this.onAudioReceived = onAudioReceived;
        this.onTranscriptReceived = onTranscriptReceived;
        this.onUserTranscriptReceived = onUserTranscriptReceived;
        this.onTurnStart = onTurnStart;
        this.onTurnEnd = onTurnEnd;
        this.onSetupComplete = onSetupComplete;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        logger.info("[Session {}] Connected to Gemini Live API", sessionId);
        
        // Send setup message to configure the session
        sendSetupMessage();
    }

    @Override
    public void onMessage(String message) {
        try {
            JsonNode response = objectMapper.readTree(message);
            handleGeminiResponse(response);
        } catch (Exception e) {
            logger.error("[Session {}] Failed to parse Gemini response: {}", sessionId, e.getMessage());
        }
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        // Binary audio data from Gemini
        byte[] audioData = new byte[bytes.remaining()];
        bytes.get(audioData);
        
        logger.debug("[Session {}] Received {} bytes of audio", sessionId, audioData.length);
        
        if (onAudioReceived != null) {
            onAudioReceived.accept(audioData);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.info("[Session {}] Gemini connection closed: {} (code: {})", sessionId, reason, code);
    }

    @Override
    public void onError(Exception ex) {
        logger.error("[Session {}] Gemini WebSocket error: {}", sessionId, ex.getMessage(), ex);
    }

    /**
     * Send setup message to configure Gemini Live session.
     * This must be sent first before any other messages.
     */
    private void sendSetupMessage() {
        try {
            ObjectNode setup = objectMapper.createObjectNode();
            ObjectNode setupNode = setup.putObject("setup");
            
            // Model configuration
            setupNode.put("model", "models/gemini-2.0-flash-live");
            
            // Generation config for voice output
            ObjectNode generationConfig = setupNode.putObject("generation_config");
            generationConfig.put("response_modalities", "AUDIO");
            
            // Speech config - use a clear examiner voice
            ObjectNode speechConfig = generationConfig.putObject("speech_config");
            ObjectNode voiceConfig = speechConfig.putObject("voice_config");
            ObjectNode prebuiltVoiceConfig = voiceConfig.putObject("prebuilt_voice_config");
            prebuiltVoiceConfig.put("voice_name", "Puck"); // Clear, professional voice
            
            // System instruction for examiner role
            ObjectNode systemInstruction = setupNode.putObject("system_instruction");
            ArrayNode parts = systemInstruction.putArray("parts");
            ObjectNode textPart = parts.addObject();
            textPart.put("text", EXAMINER_SYSTEM_INSTRUCTION);
            
            String setupMessage = objectMapper.writeValueAsString(setup);
            logger.debug("[Session {}] Sending setup: {}", sessionId, setupMessage);
            send(setupMessage);
            
        } catch (Exception e) {
            logger.error("[Session {}] Failed to send setup message: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Send audio chunk to Gemini for processing.
     * Audio should be in PCM 16-bit, 16kHz, mono format.
     * 
     * @param audioData Raw audio bytes
     */
    public void sendAudioChunk(byte[] audioData) {
        if (!isSetupComplete) {
            logger.warn("[Session {}] Cannot send audio before setup is complete", sessionId);
            return;
        }
        
        try {
            ObjectNode message = objectMapper.createObjectNode();
            ObjectNode realtimeInput = message.putObject("realtime_input");
            ObjectNode mediaChunks = realtimeInput.putObject("media_chunks");
            
            // Audio data as base64
            mediaChunks.put("data", Base64.getEncoder().encodeToString(audioData));
            mediaChunks.put("mime_type", "audio/pcm;rate=16000");
            
            send(objectMapper.writeValueAsString(message));
            
        } catch (Exception e) {
            logger.error("[Session {}] Failed to send audio chunk: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Send a text message to Gemini (for triggering specific questions).
     * 
     * @param text Text prompt
     */
    public void sendTextMessage(String text) {
        if (!isSetupComplete) {
            logger.warn("[Session {}] Cannot send text before setup is complete", sessionId);
            return;
        }
        
        try {
            ObjectNode message = objectMapper.createObjectNode();
            ObjectNode clientContent = message.putObject("client_content");
            ArrayNode parts = clientContent.putArray("parts");
            ObjectNode textPart = parts.addObject();
            textPart.put("text", text);
            clientContent.put("turn_complete", true);
            
            send(objectMapper.writeValueAsString(message));
            logger.debug("[Session {}] Sent text message: {}", sessionId, text);
            
        } catch (Exception e) {
            logger.error("[Session {}] Failed to send text message: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Tell Gemini the user has finished speaking (end of turn).
     */
    public void sendEndOfTurn() {
        try {
            ObjectNode message = objectMapper.createObjectNode();
            ObjectNode clientContent = message.putObject("client_content");
            clientContent.put("turn_complete", true);
            
            send(objectMapper.writeValueAsString(message));
            logger.debug("[Session {}] Sent end of turn signal", sessionId);
            
        } catch (Exception e) {
            logger.error("[Session {}] Failed to send end of turn: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Handle response from Gemini Live API.
     */
    private void handleGeminiResponse(JsonNode response) {
        // Handle setup complete
        if (response.has("setupComplete")) {
            isSetupComplete = true;
            logger.info("[Session {}] Gemini setup complete", sessionId);
            if (onSetupComplete != null) {
                onSetupComplete.run();
            }
            return;
        }
        
        // Handle server content (Gemini's response)
        if (response.has("serverContent")) {
            JsonNode serverContent = response.get("serverContent");
            
            // Handle user input transcript (what user said, transcribed by Gemini)
            if (serverContent.has("inputTranscript") && onUserTranscriptReceived != null) {
                String userText = serverContent.get("inputTranscript").asText();
                logger.debug("[Session {}] User said: {}", sessionId, userText);
                onUserTranscriptReceived.accept(userText);
            }
            
            // Check for model turn
            if (serverContent.has("modelTurn")) {
                JsonNode modelTurn = serverContent.get("modelTurn");
                
                // Handle text parts (transcription of what Gemini said)
                if (modelTurn.has("parts")) {
                    for (JsonNode part : modelTurn.get("parts")) {
                        if (part.has("text") && onTranscriptReceived != null) {
                            String text = part.get("text").asText();
                            onTranscriptReceived.accept(text);
                            logger.debug("[Session {}] Gemini said: {}", sessionId, text);
                        }
                        
                        // Handle inline audio data
                        if (part.has("inlineData")) {
                            JsonNode inlineData = part.get("inlineData");
                            if (inlineData.has("data") && onAudioReceived != null) {
                                byte[] audioData = Base64.getDecoder().decode(
                                    inlineData.get("data").asText()
                                );
                                onAudioReceived.accept(audioData);
                            }
                        }
                    }
                }
            }
            
            // Turn complete indicator
            if (serverContent.has("turnComplete") && serverContent.get("turnComplete").asBoolean()) {
                logger.debug("[Session {}] Gemini turn complete", sessionId);
                if (onTurnEnd != null) {
                    onTurnEnd.accept("gemini_turn_end");
                }
            }
        }
        
        // Handle tool calls (for future extensions)
        if (response.has("toolCall")) {
            logger.debug("[Session {}] Received tool call: {}", sessionId, response.get("toolCall"));
            // Handle tool calls for session control, timer, etc.
        }
    }

    /**
     * Create Gemini Live API WebSocket URI with authentication.
     */
    public static URI createGeminiUri(String apiKey) throws Exception {
        String wsUri = String.format(
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=%s",
            apiKey
        );
        return new URI(wsUri);
    }
}
