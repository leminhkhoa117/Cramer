package com.cramer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the Speaking Gemini Live integration.
 *
 * <p>These properties control whether the backend should attempt to proxy
 * real-time Speaking traffic to Gemini Live, which model/endpoint to use,
 * and a few protocol defaults such as audio sample rates and voice hints.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "speaking.gemini.live")
public class SpeakingGeminiLiveProperties {

    /**
     * Master feature flag for the Speaking Gemini Live transport.
     */
    private boolean enabled = true;

    /**
     * Gemini Live WebSocket host.
     *
     * <p>Example: generativelanguage.googleapis.com</p>
     */
    private String endpoint = "generativelanguage.googleapis.com";

    /**
     * Gemini Live model resource suffix, without the "models/" prefix.
     *
     * <p>Example: gemini-2.0-flash-live-preview-04-09</p>
     */
    private String model = "gemini-2.0-flash-live-preview-04-09";

    /**
     * WebSocket connect timeout in seconds for the upstream Gemini session.
     */
    private int connectTimeoutSeconds = 10;

    /**
     * Raw PCM input sample rate expected from the frontend microphone stream.
     */
    private int inputSampleRate = 16000;

    /**
     * Raw PCM output sample rate returned by Gemini Live audio output.
     */
    private int outputSampleRate = 24000;

    /**
     * Default prebuilt voice name used for examiner speech output.
     */
    private String voiceName = "Puck";

    /**
     * Optional frontend origins allowed to open the backend WebSocket endpoint.
     */
    private String[] allowedOrigins = new String[] {
            "http://localhost:3000",
            "http://localhost:3001",
            "http://localhost:5173",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:3001",
            "http://127.0.0.1:5173",
            "https://cramer.vn"
    };

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public int getInputSampleRate() {
        return inputSampleRate;
    }

    public void setInputSampleRate(int inputSampleRate) {
        this.inputSampleRate = inputSampleRate;
    }

    public int getOutputSampleRate() {
        return outputSampleRate;
    }

    public void setOutputSampleRate(int outputSampleRate) {
        this.outputSampleRate = outputSampleRate;
    }

    public String getVoiceName() {
        return voiceName;
    }

    public void setVoiceName(String voiceName) {
        this.voiceName = voiceName;
    }

    public String[] getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
