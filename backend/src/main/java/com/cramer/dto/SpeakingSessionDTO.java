package com.cramer.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO for speaking session responses.
 */
public class SpeakingSessionDTO {

    private Long sessionId;
    private String status;
    private String sessionMode;
    private Long topicId;
    private SpeakingTopicDTO topic;
    private List<SpeakingQuestionDTO> questions;
    private Integer luaCost;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private Integer totalDurationSeconds;

    // Evaluation results (for completed sessions)
    private BigDecimal overallBand;
    private BigDecimal fluencyBand;
    private BigDecimal lexicalBand;
    private BigDecimal grammarBand;
    private BigDecimal pronunciationBand;

    // Constructors
    public SpeakingSessionDTO() {
    }

    // Getters and Setters
    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public SpeakingTopicDTO getTopic() {
        return topic;
    }

    public void setTopic(SpeakingTopicDTO topic) {
        this.topic = topic;
    }

    public List<SpeakingQuestionDTO> getQuestions() {
        return questions;
    }

    public void setQuestions(List<SpeakingQuestionDTO> questions) {
        this.questions = questions;
    }

    public Integer getLuaCost() {
        return luaCost;
    }

    public void setLuaCost(Integer luaCost) {
        this.luaCost = luaCost;
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
}
