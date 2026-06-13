package com.cramer.billing.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A subscription plan, table {@code subscription_tiers} (SPEC-15 §2). DB-driven; premium when
 * {@code price_vnd > 0}.
 */
@Entity
@Table(name = "subscription_tiers", schema = "public")
@Getter
@Setter
public class SubscriptionTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "price_vnd", nullable = false)
    private Integer priceVnd = 0;

    @Column(name = "included_ai_gradings")
    private Integer includedAiGradings = 0;

    @Column(name = "daily_chat_limit")
    private Integer dailyChatLimit;

    @Column(name = "chatbot_monthly_limit", nullable = false)
    private Integer chatbotMonthlyLimit = 0;

    @Column(name = "vocab_ai_daily_limit", nullable = false)
    private Integer vocabAiDailyLimit = 0;

    @Column(name = "monthly_attempt_limit", nullable = false)
    private Integer monthlyAttemptLimit = 0;

    @Column(name = "monthly_attempt_ai_limit", nullable = false)
    private Integer monthlyAttemptAiLimit = 0;

    @Column(name = "per_skill_attempt_limit", nullable = false)
    private Integer perSkillAttemptLimit = 0;

    @Column(name = "per_skill_attempt_ai_limit", nullable = false)
    private Integer perSkillAttemptAiLimit = 0;

    @Column(name = "monthly_translation_limit", nullable = false)
    private Integer monthlyTranslationLimit = 0;

    @Column(name = "max_vocabulary_entries", nullable = false)
    private Integer maxVocabularyEntries = 0;

    @Column(name = "attempt_overage_cost", nullable = false)
    private Integer attemptOverageCost = 10;

    @Column(name = "attempt_ai_overage_cost", nullable = false)
    private Integer attemptAiOverageCost = 20;

    @Column(name = "chatbot_overage_cost", nullable = false)
    private Integer chatbotOverageCost = 2;

    @Column(name = "translation_overage_cost", nullable = false)
    private Integer translationOverageCost = 1;

    @Column(name = "initial_lua", nullable = false)
    private Integer initialLua = 50;

    @Column(name = "monthly_lua_bonus")
    private Integer monthlyLuaBonus = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features", columnDefinition = "jsonb")
    private JsonNode features;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "display_order")
    private Integer displayOrder;

    /** Premium = paid tier (SPEC-15 §7). */
    public boolean isPremium() {
        return priceVnd != null && priceVnd > 0;
    }
}
