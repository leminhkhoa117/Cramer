package com.cramer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing a user's per-skill monthly quota usage.
 * Prevents grinding a single skill by enforcing local caps.
 * 
 * Quota Caps (Cramerie only, per skill):
 * - ATTEMPT: 20/month per skill
 * - ATTEMPT_AI: 3/month per skill
 */
@Entity
@Table(name = "skill_quotas", schema = "public",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "skill", "quota_month"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillQuota {

    /**
     * Skill types supported in quota tracking.
     */
    public enum Skill {
        READING,
        LISTENING,
        WRITING,
        SPEAKING
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill", nullable = false, length = 20)
    private Skill skill;

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
    public static final int LOCAL_ATTEMPT_CAP = 20;
    public static final int LOCAL_ATTEMPT_AI_CAP = 3;

    /**
     * Check if local attempt cap is reached for this skill.
     */
    public boolean isAttemptCapReached() {
        return this.attemptCount >= LOCAL_ATTEMPT_CAP;
    }

    /**
     * Check if local AI attempt cap is reached for this skill.
     */
    public boolean isAttemptAiCapReached() {
        return this.attemptAiCount >= LOCAL_ATTEMPT_AI_CAP;
    }

    /**
     * Get remaining attempts for this skill.
     */
    public int getRemainingAttempts() {
        return Math.max(0, LOCAL_ATTEMPT_CAP - this.attemptCount);
    }

    /**
     * Get remaining AI attempts for this skill.
     */
    public int getRemainingAttemptAi() {
        return Math.max(0, LOCAL_ATTEMPT_AI_CAP - this.attemptAiCount);
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
