package com.cramer.dto;

import java.util.List;

/**
 * DTO for follow-up question selection request.
 */
public class FollowUpRequestDTO {

    private String candidateAnswer;
    private List<Long> askedQuestionIds;

    public FollowUpRequestDTO() {}

    public FollowUpRequestDTO(String candidateAnswer, List<Long> askedQuestionIds) {
        this.candidateAnswer = candidateAnswer;
        this.askedQuestionIds = askedQuestionIds;
    }

    public String getCandidateAnswer() {
        return candidateAnswer;
    }

    public void setCandidateAnswer(String candidateAnswer) {
        this.candidateAnswer = candidateAnswer;
    }

    public List<Long> getAskedQuestionIds() {
        return askedQuestionIds;
    }

    public void setAskedQuestionIds(List<Long> askedQuestionIds) {
        this.askedQuestionIds = askedQuestionIds;
    }
}
