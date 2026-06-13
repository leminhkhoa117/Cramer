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
 * A user's Lúa balance, table {@code user_credits} (SPEC-15 §3). One row per user
 * ({@code user_id} unique). DB CHECK enforces {@code balance >= 0}.
 */
@Entity
@Table(name = "user_credits", schema = "public")
@Getter
@Setter
public class UserCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "balance", nullable = false)
    private Integer balance = 0;

    @Column(name = "lifetime_earned", nullable = false)
    private Integer lifetimeEarned = 0;

    @Column(name = "lifetime_spent", nullable = false)
    private Integer lifetimeSpent = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
