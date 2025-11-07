package com.cramer.dto;

/**
 * Summarises a user's performance for a specific IELTS skill.
 */
public class SkillSummaryDTO {

    private String skill;
    private long attempts;
    private long correctAnswers;
    private long incorrectAnswers;
    private double accuracy; // percentage 0-100

    public SkillSummaryDTO() {
    }

    public SkillSummaryDTO(String skill, long attempts, long correctAnswers,
            long incorrectAnswers, double accuracy) {
        this.skill = skill;
        this.attempts = attempts;
        this.correctAnswers = correctAnswers;
        this.incorrectAnswers = incorrectAnswers;
        this.accuracy = accuracy;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public long getAttempts() {
        return attempts;
    }

    public void setAttempts(long attempts) {
        this.attempts = attempts;
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
