package com.cramer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for returning feature access information based on user's subscription tier.
 * Used by frontend to gate features and display upgrade prompts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureAccessDTO {

    /**
     * Current subscription tier code (cramerie, cramerich, cramerous).
     */
    private String tierCode;

    /**
     * Map of feature codes to access status.
     * Feature codes include:
     * - all_tests: Access to all test content
     * - all_topics: Access to all topic content
     * - ai_writing_grading: AI grading for writing
     * - ai_reading_grading: AI grading for reading
     * - ai_listening_grading: AI grading for listening
     * - ai_speaking_grading: AI grading for speaking
     * - vocab_ai: AI vocabulary features
     * - chatbot: AI chatbot access
     * - normal_grading: Standard grading features
     * - vocabulary: Basic vocabulary features
     * - basic_progress: Basic progress tracking
     * - full_progress: Full progress and analytics
     * - analytics: Advanced analytics
     * - email_support: Email support access
     * - priority_support: Priority support access
     */
    private Map<String, Boolean> features;

    /**
     * Whether the current tier is a paid (premium) tier.
     * Cramerie = false, Cramerich/Cramerous = true.
     */
    private boolean isPremium;

    /**
     * Display name of the tier in Vietnamese.
     */
    private String tierNameVi;

    /**
     * Display name of the tier in English.
     */
    private String tierNameEn;
}
