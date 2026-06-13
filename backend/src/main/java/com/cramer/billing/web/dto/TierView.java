package com.cramer.billing.web.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A subscription tier for the catalog endpoints (SPEC-15 §9). Mirrors the DB-driven
 * {@code subscription_tiers} row the client needs to render pricing.
 */
public record TierView(
        Long id,
        String code,
        String name,
        int priceVnd,
        boolean premium,
        int monthlyAttemptLimit,
        int monthlyAttemptAiLimit,
        int perSkillAttemptLimit,
        int includedAiGradings,
        int chatbotMonthlyLimit,
        int monthlyTranslationLimit,
        int initialLua,
        int monthlyLuaBonus,
        JsonNode features) {
}
