package com.cramer.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class TestReviewDTO {
    private Long attemptId;
    private String examSource;
    private String testNumber;
    private String skill;
    private Integer score;
    private Integer totalQuestions;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private Double bandScore;
    private List<QuestionReviewDTO> questions;

    // Constructors
    public TestReviewDTO() {}

    // Getters and Setters
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

    public String getTestNumber() {
        return testNumber;
    }

    public void setTestNumber(String testNumber) {
        this.testNumber = testNumber;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
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

    public Double getBandScore() {
        return bandScore;
    }

    public void setBandScore(Double bandScore) {
        this.bandScore = bandScore;
    }

    public List<QuestionReviewDTO> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionReviewDTO> questions) {
        this.questions = questions;
    }
}
