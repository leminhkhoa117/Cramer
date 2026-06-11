package com.cramer.platform.integration.openrouter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenRouterErrorTest {

    @Test
    @DisplayName("HTTP statuses map to the normalized error codes (SPEC-24 §2)")
    void httpMapping() {
        assertThat(OpenRouterError.fromHttpStatus(401)).isEqualTo(OpenRouterError.AUTH_FAILED);
        assertThat(OpenRouterError.fromHttpStatus(403)).isEqualTo(OpenRouterError.AUTH_FAILED);
        assertThat(OpenRouterError.fromHttpStatus(402)).isEqualTo(OpenRouterError.INSUFFICIENT_CREDITS);
        assertThat(OpenRouterError.fromHttpStatus(429)).isEqualTo(OpenRouterError.RATE_LIMITED);
        assertThat(OpenRouterError.fromHttpStatus(404)).isEqualTo(OpenRouterError.MODEL_UNAVAILABLE);
        assertThat(OpenRouterError.fromHttpStatus(503)).isEqualTo(OpenRouterError.NO_PROVIDERS);
        assertThat(OpenRouterError.fromHttpStatus(500)).isEqualTo(OpenRouterError.UPSTREAM_ERROR);
    }

    @Test
    @DisplayName("retryability matches the spec (auth/credits not retryable; rate/transport retryable)")
    void retryability() {
        assertThat(OpenRouterError.AUTH_FAILED.retryable()).isFalse();
        assertThat(OpenRouterError.INSUFFICIENT_CREDITS.retryable()).isFalse();
        assertThat(OpenRouterError.RATE_LIMITED.retryable()).isTrue();
        assertThat(OpenRouterError.MODEL_UNAVAILABLE.retryable()).isTrue();
        assertThat(OpenRouterError.NO_PROVIDERS.retryable()).isTrue();
        assertThat(OpenRouterError.UPSTREAM_ERROR.retryable()).isTrue();
    }
}
