package com.cramer.catalog.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * An individual IELTS test within a {@link TestSet}, table {@code tests} (SPEC-11 §1).
 * Unique {@code (set_id, test_number)}. FK ids are modelled as plain columns (FK-first, SPEC-11 §1.1).
 */
@Entity
@Table(name = "tests", schema = "public")
@Getter
@Setter
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "set_id", nullable = false)
    private Long setId;

    @Column(name = "test_number", nullable = false)
    private Integer testNumber;

    @Column(name = "name")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty")
    private Difficulty difficulty = Difficulty.INTERMEDIATE;

    @Column(name = "estimated_time_minutes")
    private Integer estimatedTimeMinutes = 170;

    @Column(name = "is_published")
    private Boolean isPublished = false;

    @Column(name = "is_ai_generated")
    private Boolean isAiGenerated = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "generation_metadata", columnDefinition = "jsonb")
    private JsonNode generationMetadata;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
