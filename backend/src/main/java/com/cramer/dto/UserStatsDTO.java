package com.cramer.dto;

/**
 * DTO for user statistics response.
 */
public class UserStatsDTO {
    private long totalAnswers;
    private long correctAnswers;
    private long incorrectAnswers;
    private double accuracy;

    public UserStatsDTO() {
    }

    public UserStatsDTO(long totalAnswers, long correctAnswers, long incorrectAnswers, double accuracy) {
        this.totalAnswers = totalAnswers;
        this.correctAnswers = correctAnswers;
        this.incorrectAnswers = incorrectAnswers;
        this.accuracy = accuracy;
    }

    // Getters and Setters
    public long getTotalAnswers() {
        return totalAnswers;
    }

    public void setTotalAnswers(long totalAnswers) {
        this.totalAnswers = totalAnswers;
    }

    public long getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(long correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    public long getIncorrectAnswers() {
        return incorrectAnswers;
    }

    public void setIncorrectAnswers(long incorrectAnswers) {
        this.incorrectAnswers = incorrectAnswers;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }
}
