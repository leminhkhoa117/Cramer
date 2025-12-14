package com.cramer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing a user's monthly translation usage.
 * Tracks AI translation usage in Vocabulary Notebook.
 * 
 * When limit is exceeded, user can continue by paying Lúa (1 Lúa per translation).
 */
@Entity
@Table(name = "translation_usage", schema = "public",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "usage_month"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * First day of the usage month (e.g., 2025-12-01).
     * Used to group usage by calendar month.
     */
    @Column(name = "usage_month", nullable = false)
    private LocalDate usageMonth;

    @Column(name = "translations_used", nullable = false)
    @Builder.Default
    private Integer translationsUsed = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    /**
     * Increment translation usage count.
     */
    public void incrementUsage() {
        this.translationsUsed++;
    }

    /**
     * Check if usage has reached a specific limit.
     */
    public boolean hasReachedLimit(int limit) {
        return this.translationsUsed >= limit;
    }

    /**
     * Get remaining translations before hitting limit.
     */
    public int getRemaining(int limit) {
        return Math.max(0, limit - this.translationsUsed);
    }
}
