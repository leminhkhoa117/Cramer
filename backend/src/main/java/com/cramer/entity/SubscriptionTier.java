package com.cramer.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing subscription tier definitions.
 * Contains static tier information like pricing, limits, and features.
 * 
 * Tiers (as of 2025-12-14):
 * - Cramerie (Free): Limited access, no AI grading
 * - Cramerich (69,000đ/month): Full access with ATTEMPT/ATTEMPT_AI system
 * 
 * ATTEMPT System:
 * - ATTEMPT: A basic test attempt with standard grading
 * - ATTEMPT_AI: A test attempt with AI-assisted personalized grading
 */
@Entity
@Table(name = "subscription_tiers", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code; // cramerie, cramerich

    @Column(name = "name_vi", nullable = false, length = 100)
    private String nameVi;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @Column(name = "price_vnd", nullable = false)
    private Integer priceVnd; // 0 for free tier, 69000 for Cramerich

    // ==================== ATTEMPT LIMITS ====================
    
    /** Global monthly limit for test attempts (0 = no free attempts, must pay) */
    @Column(name = "monthly_attempt_limit", nullable = false)
    @Builder.Default
    private Integer monthlyAttemptLimit = 0;

    /** Global monthly limit for AI-graded attempts */
    @Column(name = "monthly_attempt_ai_limit", nullable = false)
    @Builder.Default
    private Integer monthlyAttemptAiLimit = 0;

    /** Per-skill monthly limit for test attempts (0 = no limit or use global) */
    @Column(name = "per_skill_attempt_limit", nullable = false)
    @Builder.Default
    private Integer perSkillAttemptLimit = 0;

    /** Per-skill monthly limit for AI-graded attempts */
    @Column(name = "per_skill_attempt_ai_limit", nullable = false)
    @Builder.Default
    private Integer perSkillAttemptAiLimit = 0;

    /** Lúa cost per additional attempt beyond limit */
    @Column(name = "attempt_overage_cost", nullable = false)
    @Builder.Default
    private Integer attemptOverageCost = 10;

    /** Lúa cost per additional AI attempt beyond limit */
    @Column(name = "attempt_ai_overage_cost", nullable = false)
    @Builder.Default
    private Integer attemptAiOverageCost = 20;

    // ==================== LEGACY AI GRADINGS (kept for compatibility) ====================
    
    @Column(name = "included_ai_gradings", nullable = false)
    @Builder.Default
    private Integer includedAiGradings = 0;

    // ==================== CHATBOT & TRANSLATION LIMITS ====================

    @Column(name = "daily_chat_limit", nullable = false)
    @Builder.Default
    private Integer dailyChatLimit = 20; // -1 for unlimited (deprecated, use chatbotMonthlyLimit)

    @Column(name = "chatbot_monthly_limit", nullable = false)
    @Builder.Default
    private Integer chatbotMonthlyLimit = 0; // Monthly chatbot messages, -1 for unlimited

    @Column(name = "monthly_translation_limit", nullable = false)
    @Builder.Default
    private Integer monthlyTranslationLimit = 0; // Monthly vocabulary translations

    // ==================== OVERAGE COSTS ====================

    @Column(name = "chatbot_overage_cost", nullable = false)
    @Builder.Default
    private Integer chatbotOverageCost = 2; // Lúa cost per chatbot message after limit

    @Column(name = "translation_overage_cost", nullable = false)
    @Builder.Default
    private Integer translationOverageCost = 1; // Lúa cost per translation after limit

    // ==================== VOCABULARY LIMITS ====================

    @Column(name = "vocab_ai_daily_limit", nullable = false)
    @Builder.Default
    private Integer vocabAiDailyLimit = 0; // Daily uses for Vocab AI, -1 for unlimited

    @Column(name = "max_vocabulary_entries", nullable = false)
    @Builder.Default
    private Integer maxVocabularyEntries = 0; // Max vocab entries allowed

    // ==================== LÚA BONUSES ====================

    @Column(name = "monthly_lua_bonus", nullable = false)
    @Builder.Default
    private Integer monthlyLuaBonus = 0;

    @Column(name = "initial_lua", nullable = false)
    @Builder.Default
    private Integer initialLua = 50;

    // ==================== METADATA ====================

    @Column(name = "features", columnDefinition = "jsonb")
    private String features; // JSON array of feature keys

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Check if this tier has AI grading access.
     */
    public boolean hasAiGradingAccess() {
        return monthlyAttemptAiLimit > 0 || includedAiGradings > 0;
    }
    
    /**
     * Check if this is a paid tier.
     */
    public boolean isPaid() {
        return priceVnd != null && priceVnd > 0;
    }
    
    /**
     * Check if this tier has full test access.
     */
    public boolean hasFullTestAccess() {
        return isPaid() || "cramerich".equalsIgnoreCase(code);
    }
}
