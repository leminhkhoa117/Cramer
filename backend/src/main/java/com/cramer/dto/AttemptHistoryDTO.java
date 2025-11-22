package com.cramer.dto;

import java.time.OffsetDateTime;

public class AttemptHistoryDTO {
    private Long attemptId;
    private OffsetDateTime completedAt;
    private Integer score;
    private String status;
    private Double bandScore;

    public AttemptHistoryDTO() {}

    public AttemptHistoryDTO(Long attemptId, OffsetDateTime completedAt, Integer score, String status, Double bandScore) {
        this.attemptId = attemptId;
        this.completedAt = completedAt;
        this.score = score;
        this.status = status;
        this.bandScore = bandScore;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
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
}
