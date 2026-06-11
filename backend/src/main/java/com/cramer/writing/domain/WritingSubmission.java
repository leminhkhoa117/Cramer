package com.cramer.writing.domain;

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
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A graded (or pending) essay, table {@code writing_submissions} (SPEC-13 §1). One row per task
 * (1 or 2) of an attempt. {@code band_scores}/{@code ai_feedback} are JSONB.
 */
@Entity
@Table(name = "writing_submissions", schema = "public")
@Getter
@Setter
public class WritingSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "attempt_id", nullable = false)
    private Long attemptId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "task_number", nullable = false)
    private Integer taskNumber;

    @Column(name = "essay_text", columnDefinition = "TEXT", nullable = false)
    private String essayText;

    @Column(name = "word_count", nullable = false)
    private Integer wordCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "grading_status", nullable = false)
    private WritingStatus gradingStatus = WritingStatus.PENDING;

    @Column(name = "overall_band")
    private BigDecimal overallBand;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "band_scores", columnDefinition = "jsonb")
    private JsonNode bandScores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_feedback", columnDefinition = "jsonb")
    private JsonNode aiFeedback;

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private OffsetDateTime submittedAt;

    @Column(name = "graded_at")
    private OffsetDateTime gradedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
