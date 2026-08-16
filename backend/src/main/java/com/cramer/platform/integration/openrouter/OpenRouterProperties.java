package com.cramer.platform.integration.openrouter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OpenRouter settings (SPEC-24 §3). Bound from {@code openrouter.*} / env. Used by ABTS
 * generation and Speaking grading.
 *
 * @param apiKey                  credential ({@code OPENROUTER_API_KEY})
 * @param baseUrl                 endpoint (default {@code https://openrouter.ai/api/v1})
 * @param defaultGenerationModel  default model for ABTS generation
 * @param apiTimeoutMs            per-call timeout (ms)
 * @param siteUrl                 HTTP-Referer attribution
 * @param siteName                X-Title attribution
 */
@ConfigurationProperties(prefix = "openrouter")
public record OpenRouterProperties(
        String apiKey,
        String baseUrl,
        String defaultGenerationModel,
        Integer apiTimeoutMs,
        String siteUrl,
        String siteName) {

    public String resolvedBaseUrl() {
        return (baseUrl == null || baseUrl.isBlank()) ? "https://openrouter.ai/api/v1" : baseUrl.trim().replaceAll("/+$", "");
    }

    public String resolvedDefaultModel() {
        return (defaultGenerationModel == null || defaultGenerationModel.isBlank())
                ? "deepseek/deepseek-v4-flash" : defaultGenerationModel.trim();
    }

    public int resolvedTimeoutMs() {
        return (apiTimeoutMs == null || apiTimeoutMs <= 0) ? 120_000 : apiTimeoutMs;
    }

    public String resolvedSiteUrl() {
        return (siteUrl == null || siteUrl.isBlank()) ? "https://cramer.vn" : siteUrl.trim();
    }

    public String resolvedSiteName() {
        return (siteName == null || siteName.isBlank()) ? "Cramer ABTS" : siteName.trim();
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
