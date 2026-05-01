package com.cramer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing a user's Lúa (credit) balance.
 * Tracks current balance and lifetime statistics.
 */
@Entity
@Table(name = "user_credits", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "balance")
    @Builder.Default
    private Integer balance = 0;

    @Column(name = "lifetime_earned")
    @Builder.Default
    private Integer lifetimeEarned = 0;

    @Column(name = "lifetime_spent")
    @Builder.Default
    private Integer lifetimeSpent = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    /**
     * Add credits to the user's balance.
     * @param amount the amount to add
     */
    public void addCredits(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        this.balance += amount;
        this.lifetimeEarned += amount;
    }

    /**
     * Spend credits from the user's balance.
     * @param amount the amount to spend
     * @throws IllegalStateException if insufficient balance
     */
    public void spendCredits(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (this.balance < amount) {
            throw new IllegalStateException("Insufficient Lúa balance. Required: " + amount + ", Available: " + balance);
        }
        this.balance -= amount;
        this.lifetimeSpent += amount;
    }

    /**
     * Check if user has enough credits for a transaction.
     */
    public boolean hasEnoughCredits(int amount) {
        return this.balance >= amount;
    }
}
