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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Monthly translation usage, table {@code translation_usage} (SPEC-15 §1). Unique
 * {@code (user_id, usage_month)} where {@code usage_month} is the first day of the calendar month.
 */
@Entity
@Table(name = "translation_usage", schema = "public")
@Getter
@Setter
public class TranslationUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "usage_month", nullable = false)
    private LocalDate usageMonth;

    @Column(name = "translations_used", nullable = false)
    private Integer translationsUsed = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
