package com.cramer.catalog.domain;

import com.cramer.platform.common.ielts.Skill;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

/**
 * A test section (Reading passage, Listening part, Writing task, or authored Speaking part),
 * table {@code sections} (SPEC-11 §1). Carries both the FK path ({@code test_id}) and the legacy
 * lookup shim ({@code exam_source}/{@code test_number}); {@code test_id} is canonical (SPEC-11 §1.1).
 */
@Entity
@Table(name = "sections", schema = "public")
@Getter
@Setter
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "test_id")
    private Long testId; // nullable for legacy rows

    @Column(name = "exam_source")
    private String examSource; // legacy lookup

    @Column(name = "test_number")
    private Integer testNumber; // legacy lookup

    @Convert(converter = SkillConverter.class)
    @Column(name = "skill")
    private Skill skill;

    @Column(name = "part_number")
    private Integer partNumber;

    @Column(name = "display_content_url")
    private String displayContentUrl;

    @Column(name = "passage_text", columnDefinition = "TEXT")
    private String passageText;

    @Column(name = "audio_url")
    private String audioUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "section_layout", columnDefinition = "jsonb")
    private JsonNode sectionLayout;

    @Column(name = "image_description", columnDefinition = "TEXT")
    private String imageDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SectionStatus status = SectionStatus.PUBLISHED;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
