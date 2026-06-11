package com.cramer.catalog.domain;

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
 * Top-level content collection (e.g. "Cambridge IELTS 17"), table {@code test_sets} (SPEC-11 §1).
 */
@Entity
@Table(name = "test_sets", schema = "public")
@Getter
@Setter
public class TestSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    /** Loosely-typed: cambridge / custom / ai_generated (lowercase in DB). */
    @Column(name = "source_type")
    private String sourceType = "custom";

    @Column(name = "is_published")
    private Boolean isPublished = false;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "is_system")
    private Boolean isSystem;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
