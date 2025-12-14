package com.cramer.service;

import java.util.UUID;

/**
 * Service interface for chatbot billing operations.
 * Handles quota checking and Lúa charging for chatbot messages.
 */
public interface ChatBillingService {

    /**
     * Check and process chatbot message billing.
     * If within quota: allowed, no charge.
     * If over quota with Lúa: allowed, charge 2 Lúa.
     * If over quota without Lúa: blocked.
     * 
     * @param userId the user's UUID
     * @return ChatBillingResult with billing outcome
     */
    ChatBillingResult processChatBilling(UUID userId);

    /**
     * Get remaining chat messages for current month.
     * Returns -1 if unlimited.
     * 
     * @param userId the user's UUID
     * @return remaining messages, or -1 if unlimited
     */
    int getRemainingMessages(UUID userId);

    /**
     * Result of chatbot billing check/process.
     */
    record ChatBillingResult(
            boolean allowed,
            boolean charged,
            int luaCost,
            int remaining,
            String message) {
        public static ChatBillingResult allowed(int remaining) {
            return new ChatBillingResult(true, false, 0, remaining, null);
        }

        public static ChatBillingResult charged(int cost, int remaining) {
            return new ChatBillingResult(true, true, cost, remaining,
                    "Đã trừ " + cost + " Lúa cho tin nhắn");
        }

        public static ChatBillingResult blocked(String reason) {
            return new ChatBillingResult(false, false, 0, 0, reason);
        }
    }
}
