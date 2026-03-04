package com.cramer.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Entity representing a transcript for a speaking response.
 * Stores audio URL, transcription text, and per-question evaluation.
 */
@Entity
@Table(name = "speaking_transcripts", schema = "public")
public class SpeakingTranscript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", insertable = false, updatable = false)
    private SpeakingSession session;

    @Column(name = "question_id")
    private Long questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", insertable = false, updatable = false)
    private SpeakingQuestion question;

    @Column(name = "part", nullable = false)
    private Integer part;

    // Audio data
    @Column(name = "audio_url", columnDefinition = "TEXT")
    private String audioUrl;

    @Column(name = "audio_duration_seconds")
    private Integer audioDurationSeconds;

    // Transcript
    @Column(name = "transcript_text", columnDefinition = "TEXT")
    private String transcriptText;

    @Column(name = "transcript_confidence", precision = 3, scale = 2)
    private BigDecimal transcriptConfidence;

    // Timing
    @Column(name = "recorded_at")
    private OffsetDateTime recordedAt;

    // Per-question evaluation (optional)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_evaluation", columnDefinition = "jsonb")
    private Map<String, Object> questionEvaluation;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Constructors
    public SpeakingTranscript() {
        this.recordedAt = OffsetDateTime.now();
    }

    public SpeakingTranscript(Long sessionId, Long questionId, Integer part) {
        this();
        this.sessionId = sessionId;
        this.questionId = questionId;
        this.part = part;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public SpeakingSession getSession() {
        return session;
    }

    public void setSession(SpeakingSession session) {
        this.session = session;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public SpeakingQuestion getQuestion() {
        return question;
    }

    public void setQuestion(SpeakingQuestion question) {
        this.question = question;
    }

    public Integer getPart() {
        return part;
    }

    public void setPart(Integer part) {
        this.part = part;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public Integer getAudioDurationSeconds() {
        return audioDurationSeconds;
    }

    public void setAudioDurationSeconds(Integer audioDurationSeconds) {
        this.audioDurationSeconds = audioDurationSeconds;
    }

    public String getTranscriptText() {
        return transcriptText;
    }

    public void setTranscriptText(String transcriptText) {
        this.transcriptText = transcriptText;
    }

    public BigDecimal getTranscriptConfidence() {
        return transcriptConfidence;
    }

    public void setTranscriptConfidence(BigDecimal transcriptConfidence) {
        this.transcriptConfidence = transcriptConfidence;
    }

    public OffsetDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(OffsetDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public Map<String, Object> getQuestionEvaluation() {
        return questionEvaluation;
    }

    public void setQuestionEvaluation(Map<String, Object> questionEvaluation) {
        this.questionEvaluation = questionEvaluation;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "SpeakingTranscript{" +
                "id=" + id +
                ", sessionId=" + sessionId +
                ", questionId=" + questionId +
                ", part=" + part +
                ", audioDurationSeconds=" + audioDurationSeconds +
                '}';
    }
}
