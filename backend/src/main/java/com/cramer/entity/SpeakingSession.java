package com.cramer.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "speaking_sessions", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpeakingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "test_id", nullable = false)
    private Long testId;

    @Column(name = "session_mode", nullable = false, length = 20)
    private String sessionMode;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "in_progress";

    @Column(name = "accent", nullable = false, length = 20)
    private String accent;

    @Column(name = "speed", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal speed = BigDecimal.ONE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "session_blueprint", nullable = false, columnDefinition = "jsonb")
    private JsonNode sessionBlueprint;

    @Column(name = "is_finalized", nullable = false)
    @Builder.Default
    private Boolean isFinalized = false;

    @Column(name = "total_duration_seconds")
    private Integer totalDurationSeconds;

    @Column(name = "overall_band", precision = 2, scale = 1)
    private BigDecimal overallBand;

    @Column(name = "fluency_band", precision = 2, scale = 1)
    private BigDecimal fluencyBand;

    @Column(name = "lexical_band", precision = 2, scale = 1)
    private BigDecimal lexicalBand;

    @Column(name = "grammar_band", precision = 2, scale = 1)
    private BigDecimal grammarBand;

    @Column(name = "pronunciation_band", precision = 2, scale = 1)
    private BigDecimal pronunciationBand;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "grading_result", columnDefinition = "jsonb")
    private JsonNode gradingResult;

    @Column(name = "lua_cost", nullable = false)
    @Builder.Default
    private Integer luaCost = 0;

    @Column(name = "lua_deducted", nullable = false)
    @Builder.Default
    private Boolean luaDeducted = false;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "graded_at")
    private OffsetDateTime gradedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "grading_attempts")
    @Builder.Default
    private Integer gradingAttempts = 0;

    @Column(name = "last_grading_error", columnDefinition = "text")
    private String lastGradingError;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
