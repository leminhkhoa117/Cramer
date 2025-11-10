package com.cramer.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class AnswerSubmissionDTO {
    // Map of questionId to the user's answer content
    private Map<Long, JsonNode> answers;

    public Map<Long, JsonNode> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<Long, JsonNode> answers) {
        this.answers = answers;
    }
}
