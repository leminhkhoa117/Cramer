package com.cramer.speaking.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Speaking session runtime state + frozen blueprint + grading lifecycle, table
 * {@code speaking_sessions} (SPEC-14 §1). {@code status} persists lowercase via
 * {@link SpeakingSessionStatusConverter}.
 */
@Entity
@Table(name = "speaking_sessions", schema = "public")
@Getter
@Setter
public class SpeakingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "test_id", nullable = false)
    private Long testId;

    @Column(name = "session_mode", nullable = false)
    private String sessionMode;

    @Convert(converter = SpeakingSessionStatusConverter.class)
    @Column(name = "status", nullable = false)
    private SpeakingSessionStatus status = SpeakingSessionStatus.IN_PROGRESS;

    @Column(name = "accent", nullable = false)
    private String accent;

    @Column(name = "speed", nullable = false)
    private BigDecimal speed = BigDecimal.valueOf(1.00);

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "session_blueprint", columnDefinition = "jsonb", nullable = false)
    private JsonNode sessionBlueprint;

    @Column(name = "is_finalized", nullable = false)
    private Boolean isFinalized = false;

    @Column(name = "total_duration_seconds")
    private Integer totalDurationSeconds;

    @Column(name = "overall_band")
    private BigDecimal overallBand;

    @Column(name = "fluency_band")
    private BigDecimal fluencyBand;

    @Column(name = "lexical_band")
    private BigDecimal lexicalBand;

    @Column(name = "grammar_band")
    private BigDecimal grammarBand;

    @Column(name = "pronunciation_band")
    private BigDecimal pronunciationBand;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "grading_result", columnDefinition = "jsonb")
    private JsonNode gradingResult;

    @Column(name = "lua_cost", nullable = false)
    private Integer luaCost = 0;

    @Column(name = "lua_deducted", nullable = false)
    private Boolean luaDeducted = false;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "graded_at")
    private OffsetDateTime gradedAt;

    @Column(name = "grading_attempts", nullable = false)
    private Integer gradingAttempts = 0;

    @Column(name = "last_grading_error", columnDefinition = "TEXT")
    private String lastGradingError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
