package com.cramer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for PayOS payment gateway integration.
 * PayOS is a Vietnamese payment gateway supporting QR codes, bank transfers, and more.
 * 
 * Required environment variables:
 * - PAYOS_CLIENT_ID: Client ID from PayOS dashboard
 * - PAYOS_API_KEY: API Key from PayOS dashboard
 * - PAYOS_CHECKSUM_KEY: Checksum Key for signature generation
 * 
 * @since 2025-12-13
 */
@Configuration
@ConfigurationProperties(prefix = "payos")
public class PayOSConfig {

    /**
     * PayOS Client ID.
     * Get from PayOS Merchant Dashboard.
     */
    private String clientId;

    /**
     * PayOS API Key.
     * Get from PayOS Merchant Dashboard.
     */
    private String apiKey;

    /**
     * PayOS Checksum Key for HMAC-SHA256 signature generation/verification.
     * Get from PayOS Merchant Dashboard.
     */
    private String checksumKey;

    /**
     * PayOS API Base URL.
     * Default: https://api-merchant.payos.vn
     */
    private String baseUrl = "https://api-merchant.payos.vn";

    /**
     * Return URL after successful payment.
     * User is redirected here after completing payment.
     */
    private String returnUrl = "http://localhost:5173/payment/success";

    /**
     * Cancel URL when user cancels payment.
     * User is redirected here if they cancel the payment.
     */
    private String cancelUrl = "http://localhost:5173/payment/cancel";

    /**
     * Webhook URL for PayOS to notify payment status.
     * Must be publicly accessible in production.
     */
    private String webhookUrl;

    // Getters and Setters

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getChecksumKey() {
        return checksumKey;
    }

    public void setChecksumKey(String checksumKey) {
        this.checksumKey = checksumKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    /**
     * Check if PayOS is properly configured.
     * @return true if all required credentials are set
     */
    public boolean isConfigured() {
        return clientId != null && !clientId.isEmpty()
                && apiKey != null && !apiKey.isEmpty()
                && checksumKey != null && !checksumKey.isEmpty();
    }
}
