package com.cramer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entity representing daily chatbot usage for a user.
 * Used to enforce daily message limits based on subscription tier.
 */
@Entity
@Table(name = "chatbot_usage", schema = "public",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "usage_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "messages_used")
    @Builder.Default
    private Integer messagesUsed = 0;

    /**
     * Increment message count for today.
     */
    public void incrementUsage() {
        this.messagesUsed++;
    }

    /**
     * Check if user has remaining messages for their limit.
     * @param limit the daily limit (-1 for unlimited)
     * @return true if user can send more messages
     */
    public boolean hasRemainingMessages(int limit) {
        if (limit < 0) return true; // Unlimited
        return messagesUsed < limit;
    }

    /**
     * Get remaining messages for the day.
     * @param limit the daily limit (-1 for unlimited)
     * @return remaining count, or -1 for unlimited
     */
    public int getRemainingMessages(int limit) {
        if (limit < 0) return -1; // Unlimited
        return Math.max(0, limit - messagesUsed);
    }
}
