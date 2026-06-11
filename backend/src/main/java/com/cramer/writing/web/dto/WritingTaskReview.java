package com.cramer.writing.web.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** One task's full review (SPEC-13 §5). */
public record WritingTaskReview(
        Integer taskNumber,
        String status,
        String essayText,
        Integer wordCount,
        BigDecimal overallBand,
        JsonNode bandScores,
        JsonNode aiFeedback,
        String taskPrompt,
        String taskImageUrl,
        OffsetDateTime gradedAt) {
}
