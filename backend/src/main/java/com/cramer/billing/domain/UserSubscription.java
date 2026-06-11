package com.cramer.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A user's subscription, table {@code user_subscriptions} (SPEC-15 §2). Monthly counters
 * ({@code attempts_used}, {@code attempt_ais_used}, {@code chatbot_used}) are reset on renewal /
 * by the monthly reset job (SPEC-15 §2 fix).
 */
@Entity
@Table(name = "user_subscriptions", schema = "public")
@Getter
@Setter
public class UserSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tier_id", nullable = false)
    private Long tierId;

    @Column(name = "status")
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private OffsetDateTime startedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "auto_renew")
    private Boolean autoRenew = false;

    @Column(name = "attempts_used", nullable = false)
    private Integer attemptsUsed = 0;

    @Column(name = "attempt_ais_used", nullable = false)
    private Integer attemptAisUsed = 0;

    @Column(name = "chatbot_used", nullable = false)
    private Integer chatbotUsed = 0;

    @Column(name = "ai_grading_enabled")
    private Boolean aiGradingEnabled = true;

    @Column(name = "payment_reference")
    private String paymentReference;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
