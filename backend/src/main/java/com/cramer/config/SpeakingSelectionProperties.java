package com.cramer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Speaking question selection strategy.
 *
 * <p>Controls whether questions are selected by heuristic algorithm or by an
 * LLM provider. When {@code provider} is set to {@code "llm"}, the system
 * calls the configured model via OpenRouter to choose a coherent subset of
 * questions from the authored bank. If the LLM call fails, times out, or
 * returns invalid output, the system falls back to the heuristic planner.</p>
 *
 * @since 2026-04-05
 */
@Configuration
@ConfigurationProperties(prefix = "speaking.selection")
public class SpeakingSelectionProperties {

    /**
     * Selection provider: {@code "heuristic"} (default) or {@code "llm"}.
     */
    private String provider = "heuristic";

    /**
     * OpenRouter model ID for LLM selection (e.g. {@code "deepseek/deepseek-chat-v3-0324"}).
     * Ignored when provider is {@code "heuristic"}.
     */
    private String model = "";

    /**
     * Read timeout in milliseconds for the LLM selection call.
     * Selection returns a small JSON payload and should be fast.
     * Default 12 000 ms (12 seconds).
     */
    private int timeoutMs = 12000;

    /**
     * Fallback strategy when LLM call fails. Currently only {@code "heuristic"} is supported.
     */
    private String fallback = "heuristic";

    /**
     * Sampling temperature for the LLM selection call.
     */
    private double temperature = 0.7;

    /**
     * Maximum tokens for the LLM selection response.
     * The response is a small JSON with integer IDs and a short summary.
     */
    private int maxTokens = 512;

    // --- Getters and Setters ---

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public String getFallback() {
        return fallback;
    }

    public void setFallback(String fallback) {
        this.fallback = fallback;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    /**
     * Returns {@code true} if a model is configured for LLM selection.
     */
    public boolean hasModel() {
        return model != null && !model.trim().isEmpty();
    }
}
