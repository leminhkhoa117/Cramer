package com.cramer.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a writing submission for IELTS Writing tests.
 * Stores the essay text, word count, and AI grading results.
 */
@Entity
@Table(name = "writing_submissions", schema = "public")
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

    @Column(name = "essay_text", nullable = false, columnDefinition = "TEXT")
    private String essayText;

    @Column(name = "word_count", nullable = false)
    private Integer wordCount = 0;

    @Column(name = "grading_status", nullable = false)
    private String gradingStatus = "PENDING";

    @Column(name = "overall_band", precision = 2, scale = 1)
    private BigDecimal overallBand;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "band_scores", columnDefinition = "jsonb")
    private Map<String, Object> bandScores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_feedback", columnDefinition = "jsonb")
    private Map<String, Object> aiFeedback;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;

    @Column(name = "graded_at")
    private OffsetDateTime gradedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Constructors
    public WritingSubmission() {
        this.createdAt = OffsetDateTime.now();
        this.submittedAt = OffsetDateTime.now();
    }

    public WritingSubmission(Long attemptId, UUID userId, Integer taskNumber, String essayText) {
        this();
        this.attemptId = attemptId;
        this.userId = userId;
        this.taskNumber = taskNumber;
        this.essayText = essayText;
        this.wordCount = countWords(essayText);
    }

    // Utility methods
    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Integer getTaskNumber() {
        return taskNumber;
    }

    public void setTaskNumber(Integer taskNumber) {
        this.taskNumber = taskNumber;
    }

    public String getEssayText() {
        return essayText;
    }

    public void setEssayText(String essayText) {
        this.essayText = essayText;
        this.wordCount = countWords(essayText);
    }

    public Integer getWordCount() {
        return wordCount;
    }

    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }

    public String getGradingStatus() {
        return gradingStatus;
    }

    public void setGradingStatus(String gradingStatus) {
        this.gradingStatus = gradingStatus;
    }

    public BigDecimal getOverallBand() {
        return overallBand;
    }

    public void setOverallBand(BigDecimal overallBand) {
        this.overallBand = overallBand;
    }

    public Map<String, Object> getBandScores() {
        return bandScores;
    }

    public void setBandScores(Map<String, Object> bandScores) {
        this.bandScores = bandScores;
    }

    public Map<String, Object> getAiFeedback() {
        return aiFeedback;
    }

    public void setAiFeedback(Map<String, Object> aiFeedback) {
        this.aiFeedback = aiFeedback;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(OffsetDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public OffsetDateTime getGradedAt() {
        return gradedAt;
    }

    public void setGradedAt(OffsetDateTime gradedAt) {
        this.gradedAt = gradedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "WritingSubmission{" +
                "id=" + id +
                ", attemptId=" + attemptId +
                ", taskNumber=" + taskNumber +
                ", wordCount=" + wordCount +
                ", gradingStatus='" + gradingStatus + '\'' +
                ", overallBand=" + overallBand +
                '}';
    }
}
