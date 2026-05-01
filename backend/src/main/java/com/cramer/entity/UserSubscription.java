package com.cramer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing a user's active subscription.
 * Tracks subscription period, usage, and payment references.
 */
@Entity
@Table(name = "user_subscriptions", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscription {

    public enum Status {
        ACTIVE,
        EXPIRED,
        CANCELLED,
        PENDING
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tier_id", nullable = false)
    private SubscriptionTier tier;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    // Note: ai_gradings_used column was dropped - replaced by attempt_ais_used

    // ATTEMPT system: Regular test attempts used this month
    @Column(name = "attempts_used", nullable = false)
    @Builder.Default
    private Integer attemptsUsed = 0;

    // ATTEMPT_AI system: AI graded attempts used this month
    @Column(name = "attempt_ais_used", nullable = false)
    @Builder.Default
    private Integer attemptAisUsed = 0;

    // Chatbot messages used this month
    @Column(name = "chatbot_used", nullable = false)
    @Builder.Default
    private Integer chatbotUsed = 0;

    @Column(name = "payment_reference", length = 255)
    private String paymentReference;

    @Column(name = "auto_renew")
    @Builder.Default
    private Boolean autoRenew = false;

    /**
     * Whether AI grading (ATTEMPT_AI) is enabled for this user.
     * Only Cramerich+ users can enable this.
     * When enabled, Writing submissions are graded by AI.
     * When disabled, Writing submissions are saved but not graded.
     */
    @Column(name = "ai_grading_enabled")
    @Builder.Default
    private Boolean aiGradingEnabled = true; // Default ON for Cramerich, ignored for Cramerie

    /**
     * Check if this subscription is currently active.
     */
    public boolean isActive() {
        if (status != Status.ACTIVE) {
            return false;
        }
        if (expiresAt == null) {
            // Free tier has no expiry
            return true;
        }
        return OffsetDateTime.now().isBefore(expiresAt);
    }

    // Note: getRemainingAiGradings() removed - use getRemainingAttemptAis() instead

    /**
     * Get remaining ATTEMPTs for this billing period.
     */
    public int getRemainingAttempts() {
        if (tier == null) return 0;
        int limit = tier.getMonthlyAttemptLimit() != null ? tier.getMonthlyAttemptLimit() : 0;
        return Math.max(0, limit - attemptsUsed);
    }

    /**
     * Get remaining ATTEMPT_AIs for this billing period.
     */
    public int getRemainingAttemptAis() {
        if (tier == null) return 0;
        int limit = tier.getMonthlyAttemptAiLimit() != null ? tier.getMonthlyAttemptAiLimit() : 0;
        return Math.max(0, limit - attemptAisUsed);
    }

    /**
     * Get remaining chatbot messages for this month.
     */
    public int getRemainingChatbot() {
        if (tier == null) return 0;
        int limit = tier.getChatbotMonthlyLimit() != null ? tier.getChatbotMonthlyLimit() : 0;
        return Math.max(0, limit - chatbotUsed);
    }
}
