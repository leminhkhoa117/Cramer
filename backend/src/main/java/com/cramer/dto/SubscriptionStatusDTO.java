package com.cramer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Comprehensive DTO for subscription status page.
 * Combines subscription, credits, and usage information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionStatusDTO {

    // User info
    private UUID userId;

    // Current tier info
    private TierInfo tier;

    // Subscription dates and status
    private SubscriptionInfo subscription;

    // ATTEMPT usage (regular test attempts)
    private UsageInfo attempts;

    // ATTEMPT_AI usage (AI graded attempts)
    private UsageInfo attemptAis;

    // Chatbot usage (monthly)
    private UsageInfo chatbot;

    // Translation usage (daily)
    private UsageInfo translation;

    // Vocabulary entries
    private UsageInfo vocabulary;

    // Lúa credit balance
    private CreditInfo credits;

    // Features included in current tier
    private List<String> features;

    // Recent payment history
    private List<PaymentInfo> recentPayments;

    // Previous paid plan info (only populated when current tier is free AND user previously had a paid plan)
    // Used to surface a renewal CTA banner on the frontend.
    private PreviousPlanInfo previousPlan;

    /**
     * Tier information nested class.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TierInfo {
        private String code;
        private String name;
        // removed duplicate nameEn
        private String emoji;
        private Integer priceVnd;
        private Integer displayOrder;
        // @JsonProperty needed because Lombok generates getter `isFree()` (no `get` prefix
        // for `boolean is*`), and Jackson defaults to JSON key "free" — strips the `is`.
        // Frontend (SubscriptionPage.jsx, useSubscriptionStore.js) reads `isFree`/`isPremium`.
        @JsonProperty("isFree")
        private boolean isFree;

        @JsonIgnore
        public boolean isFree() {
            return isFree;
        }
    }

    /**
     * Subscription status info.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubscriptionInfo {
        private Long id;
        private String status; // ACTIVE, EXPIRED, CANCELLED
        private OffsetDateTime startedAt;
        private OffsetDateTime expiresAt;
        private Integer daysRemaining;
        private Double progressPercent; // 0-100, how much of subscription period used
        private Boolean autoRenew;
        private Boolean isLifetime; // Free tier is lifetime
        private Boolean aiGradingEnabled; // Whether AI grading (ATTEMPT_AI) is enabled
        private Boolean canEnableAiGrading; // Whether user CAN enable AI grading (Cramerich+ only)
    }

    /**
     * Usage information for AI gradings or daily chat.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UsageInfo {
        private Integer used;
        private Integer limit;
        private Integer remaining;
        private Double progressPercent; // 0-100
        private Boolean isUnlimited;
        private String resetInfo; // "Resets monthly" or "Resets daily"
    }

    /**
     * Credit (Lúa) balance info.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreditInfo {
        private Integer balance;
        private Integer lifetimeEarned;
        private Integer lifetimeSpent;
    }

    /**
     * Payment history item.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentInfo {
        private Long orderCode;
        private String type; // SUBSCRIPTION, LUA_PACK
        private Integer amountVnd;
        private String status; // PENDING, PAID, CANCELLED, EXPIRED
        private String description;
        private OffsetDateTime createdAt;
        private OffsetDateTime paidAt;
    }

    /**
     * Previous (expired) paid plan info — surfaces a renewal CTA on the frontend
     * when the user has been auto-downgraded to the free tier.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PreviousPlanInfo {
        private String tierCode;
        private String tierName;
        private Integer priceVnd;
        private OffsetDateTime expiredAt;
        private Long daysSinceExpired;
    }
}
