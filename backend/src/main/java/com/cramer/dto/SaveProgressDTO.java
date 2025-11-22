package com.cramer.dto;

import java.util.Map;

public class SaveProgressDTO {
    private Integer timeLeft;
    private Integer currentPart;
    private Map<Long, String> answers; // Map of questionId to answer text

    public SaveProgressDTO() {
    }

    public SaveProgressDTO(Integer timeLeft, Integer currentPart, Map<Long, String> answers) {
        this.timeLeft = timeLeft;
        this.currentPart = currentPart;
        this.answers = answers;
    }

    public Integer getTimeLeft() {
        return timeLeft;
    }

    public void setTimeLeft(Integer timeLeft) {
        this.timeLeft = timeLeft;
    }

    public Integer getCurrentPart() {
        return currentPart;
    }

    public void setCurrentPart(Integer currentPart) {
        this.currentPart = currentPart;
    }

    public Map<Long, String> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<Long, String> answers) {
        this.answers = answers;
    }
}
