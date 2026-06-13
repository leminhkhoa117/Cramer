package com.cramer.platform.integration.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DeepSeek (OpenAI-compatible) settings (SPEC-18 §4, SPEC-13 §4.1). Bound from {@code llm.*} /
 * env. The server {@code DEEPSEEK_API_KEY} is the source of truth for writing grading and chat.
 *
 * @param baseUrl       API base (default {@code https://api.deepseek.com})
 * @param apiKey        server API key ({@code DEEPSEEK_API_KEY})
 * @param gradingModel  default model for grading ({@code deepseek-chat})
 * @param timeoutMs     read timeout in milliseconds
 */
@ConfigurationProperties(prefix = "llm")
public record LlmProperties(
        String baseUrl,
        String apiKey,
        String gradingModel,
        Integer timeoutMs) {

    public String resolvedBaseUrl() {
        return (baseUrl == null || baseUrl.isBlank()) ? "https://api.deepseek.com" : baseUrl.trim().replaceAll("/+$", "");
    }

    public String resolvedGradingModel() {
        return (gradingModel == null || gradingModel.isBlank()) ? "deepseek-chat" : gradingModel.trim();
    }

    public int resolvedTimeoutMs() {
        return (timeoutMs == null || timeoutMs <= 0) ? 600_000 : timeoutMs;
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
