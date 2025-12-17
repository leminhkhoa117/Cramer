package com.cramer.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * DTO for writing submission data transfer.
 */
public class WritingSubmissionDTO {
    
    private Long id;
    private Long attemptId;
    private Integer taskNumber;
    private String essayText;
    private Integer wordCount;
    private String gradingStatus;
    private BigDecimal overallBand;
    private Map<String, Object> bandScores;
    private Map<String, Object> aiFeedback;
    private OffsetDateTime submittedAt;
    private OffsetDateTime gradedAt;

    // Constructors
    public WritingSubmissionDTO() {}

    public WritingSubmissionDTO(Long id, Long attemptId, Integer taskNumber, String essayText, 
                                Integer wordCount, String gradingStatus, BigDecimal overallBand) {
        this.id = id;
        this.attemptId = attemptId;
        this.taskNumber = taskNumber;
        this.essayText = essayText;
        this.wordCount = wordCount;
        this.gradingStatus = gradingStatus;
        this.overallBand = overallBand;
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
}
