package com.cramer.dto;

import java.time.OffsetDateTime;

/**
 * Represents aggregated progress for a Cambridge exam course (exam source +
 * test number + skill).
 */
public class CourseProgressDTO {

    private Long attemptId;
    private String examSource;
    private Integer testNumber;
    private String skill;
    private String setName; // Human-readable set name from test_sets
    private String testName; // Human-readable test name from tests
    private int totalQuestions;
    private int answersAttempted;
    private int correctAnswers;
    private OffsetDateTime lastAttempt;
    private double completionRate; // 0-1
    private String status;
    private Double bandScore;
    private String coverImageUrl;
    private java.util.List<AttemptHistoryDTO> history;

    public CourseProgressDTO() {
    }

    public CourseProgressDTO(Long attemptId, String examSource, Integer testNumber, String skill,
            String setName, String testName,
            int totalQuestions, int answersAttempted, int correctAnswers,
            OffsetDateTime lastAttempt, double completionRate, String status, Double bandScore,
            String coverImageUrl,
            java.util.List<AttemptHistoryDTO> history) {
        this.attemptId = attemptId;
        this.examSource = examSource;
        this.testNumber = testNumber;
        this.skill = skill;
        this.setName = setName;
        this.testName = testName;
        this.totalQuestions = totalQuestions;
        this.answersAttempted = answersAttempted;
        this.correctAnswers = correctAnswers;
        this.lastAttempt = lastAttempt;
        this.completionRate = completionRate;
        this.status = status;
        this.bandScore = bandScore;
        this.coverImageUrl = coverImageUrl;
        this.history = history;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public String getExamSource() {
        return examSource;
    }

    public void setExamSource(String examSource) {
        this.examSource = examSource;
    }

    public Integer getTestNumber() {
        return testNumber;
    }

    public void setTestNumber(Integer testNumber) {
        this.testNumber = testNumber;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public String getSetName() {
        return setName;
    }

    public void setSetName(String setName) {
        this.setName = setName;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getAnswersAttempted() {
        return answersAttempted;
    }

    public void setAnswersAttempted(int answersAttempted) {
        this.answersAttempted = answersAttempted;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(int correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    public OffsetDateTime getLastAttempt() {
        return lastAttempt;
    }

    public void setLastAttempt(OffsetDateTime lastAttempt) {
        this.lastAttempt = lastAttempt;
    }

    public double getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(double completionRate) {
        this.completionRate = completionRate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getBandScore() {
        return bandScore;
    }

    public void setBandScore(Double bandScore) {
        this.bandScore = bandScore;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public java.util.List<AttemptHistoryDTO> getHistory() {
        return history;
    }

    public void setHistory(java.util.List<AttemptHistoryDTO> history) {
        this.history = history;
    }
}
