package com.cramer.billing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PayOS gateway settings (SPEC-15 §8). Bound from {@code payos.*}. When client id / api key /
 * checksum key are absent the service operates in <strong>mock checkout</strong> mode (signature
 * verification skipped) so local/dev flows still work.
 */
@ConfigurationProperties(prefix = "payos")
public record PayOsProperties(
        String clientId,
        String apiKey,
        String checksumKey,
        String returnUrl,
        String cancelUrl) {

    public boolean configured() {
        return notBlank(clientId) && notBlank(apiKey) && notBlank(checksumKey);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
