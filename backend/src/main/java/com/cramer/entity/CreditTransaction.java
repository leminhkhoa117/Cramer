package com.cramer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing a credit (Lúa) transaction.
 * Records all credit movements for audit and history purposes.
 */
@Entity
@Table(name = "credit_transactions", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditTransaction {

    /**
     * Transaction type enum for credit movements
     */
    public enum Type {
        EARN,       // General credit earning
        SPEND,      // Spending credits
        BONUS,      // Bonus credits (signup, monthly, promotional)
        PURCHASE,   // Purchased Lúa pack
        REFUND      // Refunded credits
    }

    /**
     * Transaction categories for grouping and reporting
     */
    public enum Category {
        // Earning categories
        INITIAL_BONUS,          // Account creation bonus
        TIER_BONUS,             // Monthly subscription bonus
        STREAK_BONUS,           // Login streak bonus
        MILESTONE_REWARD,       // Milestone rewards (e.g., vocabulary count)
        PURCHASE,               // Purchased Lúa pack
        REFERRAL,               // Referral bonus
        // ACHIEVEMENT,         // Badge/achievement reward - REMOVED
        PROMOTION,              // Promotional/marketing bonus
        signup,                 // Account signup bonus (lowercase to match database)
        
        // Spending categories
        AI_GRADING,             // Extra AI grading beyond limit
        VOCABULARY_TRANSLATION, // Word translation
        PREMIUM_CONTENT,        // Unlock premium test pack
        ESSAY_FEEDBACK,         // Custom essay feedback
        CHAT_EXTENSION,         // Extra AI chat messages
        OTHER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "amount", nullable = false)
    private Integer amount; // Positive for earn, negative for spend

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private Category category;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "reference_id", length = 255)
    private String referenceId; // External reference (order ID, achievement ID, etc.)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
