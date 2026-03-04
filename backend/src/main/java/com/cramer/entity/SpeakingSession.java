package com.cramer.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a user's speaking practice session.
 * Tracks session state, duration, and evaluation results.
 */
@Entity
@Table(name = "speaking_sessions", schema = "public")
public class SpeakingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private Profile user;

    @Column(name = "session_mode", nullable = false, length = 20)
    private String sessionMode;

    @Column(name = "topic_id")
    private Long topicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", insertable = false, updatable = false)
    private SpeakingTopic topic;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "in_progress";

    // Session metadata
    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "total_duration_seconds")
    private Integer totalDurationSeconds;

    // Evaluation results (denormalized for quick access)
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

    // Detailed evaluation (JSONB for flexibility) - renamed from evaluation_details
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "grading_result", columnDefinition = "jsonb")
    private Map<String, Object> gradingResult;

    // Grading timestamp
    @Column(name = "graded_at")
    private OffsetDateTime gradedAt;

    // Credits
    @Column(name = "lua_cost", nullable = false)
    private Integer luaCost = 0;

    @Column(name = "lua_deducted")
    private Boolean luaDeducted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // Constructors
    public SpeakingSession() {
        this.startedAt = OffsetDateTime.now();
    }

    public SpeakingSession(UUID userId, String sessionMode, Long topicId, Integer luaCost) {
        this();
        this.userId = userId;
        this.sessionMode = sessionMode;
        this.topicId = topicId;
        this.luaCost = luaCost;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Profile getUser() {
        return user;
    }

    public void setUser(Profile user) {
        this.user = user;
    }

    public String getSessionMode() {
        return sessionMode;
    }

    public void setSessionMode(String sessionMode) {
        this.sessionMode = sessionMode;
    }

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }

    public SpeakingTopic getTopic() {
        return topic;
    }

    public void setTopic(SpeakingTopic topic) {
        this.topic = topic;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getTotalDurationSeconds() {
        return totalDurationSeconds;
    }

    public void setTotalDurationSeconds(Integer totalDurationSeconds) {
        this.totalDurationSeconds = totalDurationSeconds;
    }

    public BigDecimal getOverallBand() {
        return overallBand;
    }

    public void setOverallBand(BigDecimal overallBand) {
        this.overallBand = overallBand;
    }

    public BigDecimal getFluencyBand() {
        return fluencyBand;
    }

    public void setFluencyBand(BigDecimal fluencyBand) {
        this.fluencyBand = fluencyBand;
    }

    public BigDecimal getLexicalBand() {
        return lexicalBand;
    }

    public void setLexicalBand(BigDecimal lexicalBand) {
        this.lexicalBand = lexicalBand;
    }

    public BigDecimal getGrammarBand() {
        return grammarBand;
    }

    public void setGrammarBand(BigDecimal grammarBand) {
        this.grammarBand = grammarBand;
    }

    public BigDecimal getPronunciationBand() {
        return pronunciationBand;
    }

    public void setPronunciationBand(BigDecimal pronunciationBand) {
        this.pronunciationBand = pronunciationBand;
    }

    public Map<String, Object> getGradingResult() {
        return gradingResult;
    }

    public void setGradingResult(Map<String, Object> gradingResult) {
        this.gradingResult = gradingResult;
    }

    public OffsetDateTime getGradedAt() {
        return gradedAt;
    }

    public void setGradedAt(OffsetDateTime gradedAt) {
        this.gradedAt = gradedAt;
    }

    public Integer getLuaCost() {
        return luaCost;
    }

    public void setLuaCost(Integer luaCost) {
        this.luaCost = luaCost;
    }

    public Boolean getLuaDeducted() {
        return luaDeducted;
    }

    public void setLuaDeducted(Boolean luaDeducted) {
        this.luaDeducted = luaDeducted;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "SpeakingSession{" +
                "id=" + id +
                ", userId=" + userId +
                ", sessionMode='" + sessionMode + '\'' +
                ", topicId=" + topicId +
                ", status='" + status + '\'' +
                ", overallBand=" + overallBand +
                '}';
    }
}
