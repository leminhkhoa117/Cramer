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
     * Validated at startup against OpenRouter /models; falls back via
     * AbtsModelCatalogService.validateConfiguredDefaults if not found.
     */
    private String generationModel = "deepseek/deepseek-v4-flash";

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
     * SSE emitter timeout in milliseconds for streaming generation endpoints.
     * Must be longer than a full multi-part generation. Default: 1800000 (30 minutes).
     * Aligned with the per-request read timeout so the client connection is not
     * closed before generation completes.
     */
    private long emitterTimeoutMs = 1800000L;

    /**
     * Soft per-part timeout (ms) for multi-part streaming generation. When a single
     * part (Reading/Listening/Writing) exceeds this budget it is recorded as a failed
     * part and generation continues with the remaining parts. Default: 600000 (10 min).
     */
    private long perPartTimeoutMs = 600000L;

    /**
     * Enable streaming for real-time generation view.
     * Default: true
     */
    private boolean streamingEnabled = true;

    /**
     * Hard cap on Agent 2 refinement loop iterations. Once a refinement request
     * arrives with round >= this value, the service refuses to refine again and
     * returns a failure. Prevents runaway refine loops. Default: 5.
     */
    private int maxRefinementRounds = 5;

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

    public long getEmitterTimeoutMs() {
        return emitterTimeoutMs;
    }

    public void setEmitterTimeoutMs(long emitterTimeoutMs) {
        this.emitterTimeoutMs = emitterTimeoutMs;
    }

    public long getPerPartTimeoutMs() {
        return perPartTimeoutMs;
    }

    public void setPerPartTimeoutMs(long perPartTimeoutMs) {
        this.perPartTimeoutMs = perPartTimeoutMs;
    }

    public boolean isStreamingEnabled() {
        return streamingEnabled;
    }

    public void setStreamingEnabled(boolean streamingEnabled) {
        this.streamingEnabled = streamingEnabled;
    }

    public int getMaxRefinementRounds() {
        return maxRefinementRounds;
    }

    public void setMaxRefinementRounds(int maxRefinementRounds) {
        this.maxRefinementRounds = maxRefinementRounds;
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
