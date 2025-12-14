package com.cramer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing a user's global monthly quota usage.
 * Tracks attempt counts across all skills for Cramerie (free tier) users.
 * 
 * Quota Caps (Cramerie only):
 * - ATTEMPT: 60/month global
 * - ATTEMPT_AI: 30/month global
 */
@Entity
@Table(name = "user_quotas", schema = "public",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "quota_month"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * First day of the quota month (e.g., 2025-12-01).
     * Used to group quotas by calendar month.
     */
    @Column(name = "quota_month", nullable = false)
    private LocalDate quotaMonth;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "attempt_ai_count", nullable = false)
    @Builder.Default
    private Integer attemptAiCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // ===== QUOTA CAPS (constants) =====
    public static final int GLOBAL_ATTEMPT_CAP = 60;
    public static final int GLOBAL_ATTEMPT_AI_CAP = 30;

    /**
     * Check if global attempt cap is reached.
     */
    public boolean isAttemptCapReached() {
        return this.attemptCount >= GLOBAL_ATTEMPT_CAP;
    }

    /**
     * Check if global AI attempt cap is reached.
     */
    public boolean isAttemptAiCapReached() {
        return this.attemptAiCount >= GLOBAL_ATTEMPT_AI_CAP;
    }

    /**
     * Get remaining global attempts.
     */
    public int getRemainingAttempts() {
        return Math.max(0, GLOBAL_ATTEMPT_CAP - this.attemptCount);
    }

    /**
     * Get remaining global AI attempts.
     */
    public int getRemainingAttemptAi() {
        return Math.max(0, GLOBAL_ATTEMPT_AI_CAP - this.attemptAiCount);
    }

    /**
     * Increment attempt count (non-AI).
     */
    public void incrementAttempt() {
        this.attemptCount++;
    }

    /**
     * Increment AI attempt count.
     */
    public void incrementAttemptAi() {
        this.attemptAiCount++;
    }
}
