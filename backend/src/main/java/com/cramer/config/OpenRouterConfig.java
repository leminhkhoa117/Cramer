package com.cramer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for OpenRouter unified AI API.
 * OpenRouter provides access to 400+ AI models (OpenAI, Anthropic, Google,
 * DeepSeek, Meta, etc.)
 * through a single unified API.
 * 
 * Set via environment variable: OPENROUTER_API_KEY
 * 
 * @see <a href="https://openrouter.ai/docs">OpenRouter Documentation</a>
 * @since 2025-12-20 - ABTS v2.0
 */
@Configuration
@ConfigurationProperties(prefix = "openrouter")
public class OpenRouterConfig {

    /**
     * OpenRouter API key.
     * Set via environment variable: OPENROUTER_API_KEY
     */
    private String apiKey;

    /**
     * OpenRouter API base URL.
     * Default: https://openrouter.ai/api/v1
     */
    private String baseUrl = "https://openrouter.ai/api/v1";

    /**
     * Site URL for OpenRouter attribution/rankings.
     * Default: https://cramer.vn
     */
    private String siteUrl = "https://cramer.vn";

    /**
     * Site name for OpenRouter attribution/rankings.
     * Default: Cramer ABTS
     */
    private String siteName = "Cramer ABTS";

    /**
     * Default model for full content generation (passage + questions).
     * Default: mistralai/devstral-2512:free (free tier for testing)
     * Alternative: deepseek/deepseek-r1 (with reasoning, paid)
     */
    private String generationModel = "mistralai/devstral-2512:free";

    /**
     * Default model for quick regeneration tasks.
     * Recommended: deepseek/deepseek-chat (fast, cheap)
     */
    private String regenerationModel = "deepseek/deepseek-chat";

    /**
     * Default model for JSON fixes and simple tasks.
     * Recommended: meta-llama/llama-3.1-70b-instruct:free (free tier)
     */
    private String jsonFixModel = "meta-llama/llama-3.1-70b-instruct:free";

    /**
     * API timeout in milliseconds.
     * Default: 120000 (2 minutes) - content generation can take time
     */
    private int timeoutMs = 120000;

    /**
     * Enable streaming for real-time generation view.
     * Default: true
     */
    private boolean streamingEnabled = true;

    // Getters and Setters

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getGenerationModel() {
        return generationModel;
    }

    public void setGenerationModel(String generationModel) {
        this.generationModel = generationModel;
    }

    public String getRegenerationModel() {
        return regenerationModel;
    }

    public void setRegenerationModel(String regenerationModel) {
        this.regenerationModel = regenerationModel;
    }

    public String getJsonFixModel() {
        return jsonFixModel;
    }

    public void setJsonFixModel(String jsonFixModel) {
        this.jsonFixModel = jsonFixModel;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public boolean isStreamingEnabled() {
        return streamingEnabled;
    }

    public void setStreamingEnabled(boolean streamingEnabled) {
        this.streamingEnabled = streamingEnabled;
    }

    /**
     * Check if OpenRouter API key is configured.
     * 
     * @return true if apiKey is set and not empty
     */
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
