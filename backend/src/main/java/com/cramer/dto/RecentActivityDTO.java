package com.cramer.dto;

import java.time.OffsetDateTime;

public class RecentActivityDTO {
    private final Long questionId;
    private final OffsetDateTime submittedAt;
    private final Boolean correct;

    public RecentActivityDTO(Long questionId, OffsetDateTime submittedAt, Boolean correct) {
        this.questionId = questionId;
        this.submittedAt = submittedAt;
        this.correct = correct;
    }

    public Long getQuestionId() { return questionId; }
    public OffsetDateTime getSubmittedAt() { return submittedAt; }
    public Boolean getCorrect() { return correct; }
}
