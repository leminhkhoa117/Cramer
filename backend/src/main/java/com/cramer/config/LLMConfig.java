package com.cramer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for LLM (Large Language Model) settings.
 * Supports DeepSeek API with OpenAI-compatible format.
 * 
 * Server-side API key can be set via environment variable DEEPSEEK_API_KEY.
 * Users can optionally override with their own key in profile settings.
 * 
 * @since 2025-12-13
 */
@Configuration
@ConfigurationProperties(prefix = "llm")
public class LLMConfig {

    /**
     * Server-side DeepSeek API key.
     * Falls back to user's profile key if not set.
     * Set via environment variable: DEEPSEEK_API_KEY
     */
    private String apiKey;

    /**
     * Default model to use (legacy, for backward compatibility).
     * Options: deepseek-chat (fast), deepseek-reasoner (accurate)
     */
    private String model = "deepseek-chat";

    /**
     * Model to use for writing grading (more accurate).
     * Default: deepseek-reasoner (Thinking mode for better accuracy)
     */
    private String gradingModel = "deepseek-reasoner";

    /**
     * Model to use for chat, translation, and other tasks (faster).
     * Default: deepseek-chat (Non-thinking mode for speed)
     */
    private String chatModel = "deepseek-chat";

    /**
     * Base URL for DeepSeek API.
     * Default: https://api.deepseek.com
     */
    private String baseUrl = "https://api.deepseek.com";

    // Getters and Setters

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getGradingModel() {
        return gradingModel;
    }

    public void setGradingModel(String gradingModel) {
        this.gradingModel = gradingModel;
    }

    public String getChatModel() {
        return chatModel;
    }

    public void setChatModel(String chatModel) {
        this.chatModel = chatModel;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Check if server-side API key is configured.
     * @return true if apiKey is set and not empty
     */
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
