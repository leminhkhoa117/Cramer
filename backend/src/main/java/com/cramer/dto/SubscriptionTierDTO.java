package com.cramer.dto;

import com.cramer.entity.SubscriptionTier;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import java.util.List;

/**
 * DTO for subscription tier information.
 * Includes the new ATTEMPT/ATTEMPT_AI system fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionTierDTO {

    private Long id;
    private String code;
    private String name;
    // removed duplicate nameEn
    private Integer priceVnd;

    // ATTEMPT system fields
    private Integer monthlyAttemptLimit;
    private Integer monthlyAttemptAiLimit;
    private Integer perSkillAttemptLimit;
    private Integer perSkillAttemptAiLimit;
    private Integer attemptOverageCost;
    private Integer attemptAiOverageCost;

    // Legacy AI gradings (kept for compatibility)
    private Integer includedAiGradings;

    // Chat/Translation limits
    private Integer dailyChatLimit;
    private Integer chatbotMonthlyLimit;
    private Integer monthlyTranslationLimit;
    private Integer chatbotOverageCost;
    private Integer translationOverageCost;

    // Vocabulary limits
    private Integer vocabAiDailyLimit;
    private Integer maxVocabularyEntries;

    // Lúa bonuses
    private Integer monthlyLuaBonus;
    private Integer initialLua;

    // Features
    private List<String> features;
    private Integer displayOrder;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Create a DTO from an entity.
     */
    public static SubscriptionTierDTO fromEntity(SubscriptionTier entity) {
        if (entity == null)
            return null;

        List<String> featureList = parseFeatures(entity.getFeatures());

        return SubscriptionTierDTO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .priceVnd(entity.getPriceVnd())
                // ATTEMPT system
                .monthlyAttemptLimit(entity.getMonthlyAttemptLimit())
                .monthlyAttemptAiLimit(entity.getMonthlyAttemptAiLimit())
                .perSkillAttemptLimit(entity.getPerSkillAttemptLimit())
                .perSkillAttemptAiLimit(entity.getPerSkillAttemptAiLimit())
                .attemptOverageCost(entity.getAttemptOverageCost())
                .attemptAiOverageCost(entity.getAttemptAiOverageCost())
                // Legacy
                .includedAiGradings(entity.getIncludedAiGradings())
                // Chat/Translation
                .dailyChatLimit(entity.getDailyChatLimit())
                .chatbotMonthlyLimit(entity.getChatbotMonthlyLimit())
                .monthlyTranslationLimit(entity.getMonthlyTranslationLimit())
                .chatbotOverageCost(entity.getChatbotOverageCost())
                .translationOverageCost(entity.getTranslationOverageCost())
                // Vocabulary
                .vocabAiDailyLimit(entity.getVocabAiDailyLimit())
                .maxVocabularyEntries(entity.getMaxVocabularyEntries())
                // Lúa
                .monthlyLuaBonus(entity.getMonthlyLuaBonus())
                .initialLua(entity.getInitialLua())
                // Features
                .features(featureList)
                .displayOrder(entity.getDisplayOrder())
                .build();
    }

    /**
     * Parse features JSON - handles both array and object formats.
     */
    private static List<String> parseFeatures(String featuresJson) {
        if (featuresJson == null || featuresJson.isEmpty()) {
            return List.of();
        }

        try {
            String trimmed = featuresJson.trim();
            if (trimmed.startsWith("[")) {
                // Parse as array of strings
                return objectMapper.readValue(trimmed, new TypeReference<List<String>>() {
                });
            } else if (trimmed.startsWith("{")) {
                // Parse as object and extract keys with true values
                java.util.Map<String, Object> featuresMap = objectMapper.readValue(trimmed,
                        new TypeReference<java.util.Map<String, Object>>() {
                        });
                java.util.List<String> result = new java.util.ArrayList<>();
                for (java.util.Map.Entry<String, Object> entry : featuresMap.entrySet()) {
                    if (Boolean.TRUE.equals(entry.getValue())) {
                        result.add(entry.getKey());
                    }
                }
                return result;
            }
        } catch (Exception e) {
            // Log would be nice here but DTO shouldn't have logger dependency
        }
        return List.of();
    }
}
