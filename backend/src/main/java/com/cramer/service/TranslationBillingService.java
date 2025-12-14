package com.cramer.service;

import java.util.UUID;

/**
 * Service interface for translation billing operations.
 * Handles quota checking and Lúa charging for vocabulary translations.
 */
public interface TranslationBillingService {

    /**
     * Check if user can translate (within quota or has Lúa to pay).
     * 
     * @param userId the user's UUID
     * @return TranslationBillingResult with allowed status and cost info
     */
    TranslationBillingResult preCheckTranslation(UUID userId);

    /**
     * Process translation billing - check quota, charge if needed, track usage.
     * Call this BEFORE performing the translation.
     * 
     * @param userId the user's UUID
     * @return TranslationBillingResult with billing outcome
     * @throws RuntimeException if translation not allowed (no quota, no Lúa)
     */
    TranslationBillingResult processTranslationBilling(UUID userId);

    /**
     * Get remaining translations for current month.
     * Returns -1 if unlimited.
     * 
     * @param userId the user's UUID
     * @return remaining translations, or -1 if unlimited
     */
    int getRemainingTranslations(UUID userId);

    /**
     * Result of translation billing check/process.
     */
    record TranslationBillingResult(
            boolean allowed,
            boolean charged,
            int luaCost,
            int remaining,
            String message) {
        public static TranslationBillingResult allowed(int remaining) {
            return new TranslationBillingResult(true, false, 0, remaining, null);
        }

        public static TranslationBillingResult charged(int cost, int remaining) {
            return new TranslationBillingResult(true, true, cost, remaining,
                    "Đã trừ " + cost + " Lúa cho lượt dịch");
        }

        public static TranslationBillingResult blocked(String reason) {
            return new TranslationBillingResult(false, false, 0, 0, reason);
        }
    }
}
