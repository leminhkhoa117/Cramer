package com.cramer.dto;

import java.util.Map;

public class AnswerSubmissionDTO {
    // Map of questionId to the user's answer content
    private Map<Long, String> answers;

    public Map<Long, String> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<Long, String> answers) {
        this.answers = answers;
    }
}
