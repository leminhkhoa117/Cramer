package com.cramer.speaking.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
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

/**
 * One persisted Speaking turn, table {@code speaking_transcripts} (SPEC-14 §5). Unique
 * {@code (session_id, turn_index)}. {@code question_snapshot} is the frozen prompt (tamper-proof).
 */
@Entity
@Table(name = "speaking_transcripts", schema = "public")
@Getter
@Setter
public class SpeakingTranscript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "source_question_id")
    private Long sourceQuestionId;

    @Column(name = "part_number", nullable = false)
    private Integer partNumber;

    @Column(name = "turn_index", nullable = false)
    private Integer turnIndex;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_snapshot", columnDefinition = "jsonb", nullable = false)
    private JsonNode questionSnapshot;

    @Column(name = "audio_storage_path", columnDefinition = "TEXT")
    private String audioStoragePath;

    @Column(name = "audio_duration_seconds")
    private Integer audioDurationSeconds;

    @Column(name = "transcript_text", columnDefinition = "TEXT")
    private String transcriptText;

    @Column(name = "transcript_confidence")
    private BigDecimal transcriptConfidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_evaluation", columnDefinition = "jsonb")
    private JsonNode questionEvaluation;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private OffsetDateTime recordedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
