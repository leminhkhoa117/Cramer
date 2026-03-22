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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "speaking_transcripts", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    @Column(name = "question_snapshot", nullable = false, columnDefinition = "jsonb")
    private JsonNode questionSnapshot;

    @Column(name = "audio_storage_path", columnDefinition = "text")
    private String audioStoragePath;

    @Column(name = "audio_duration_seconds")
    private Integer audioDurationSeconds;

    @Column(name = "transcript_text", columnDefinition = "text")
    private String transcriptText;

    @Column(name = "transcript_confidence", precision = 4, scale = 3)
    private BigDecimal transcriptConfidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_evaluation", columnDefinition = "jsonb")
    private JsonNode questionEvaluation;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
