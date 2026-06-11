package com.cramer.speaking.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

/**
 * Upsert a transcript turn (SPEC-14 §5). The {@code questionSnapshot}/{@code partNumber}/
 * {@code sourceQuestionId} must match the frozen blueprint turn (deep equality), else rejected.
 */
public record SaveTranscriptRequest(
        @NotNull Integer turnIndex,
        @NotNull Integer partNumber,
        Long sourceQuestionId,
        @NotNull JsonNode questionSnapshot,
        String audioStoragePath,
        Integer audioDurationSeconds,
        String transcriptText,
        Double transcriptConfidence) {
}
