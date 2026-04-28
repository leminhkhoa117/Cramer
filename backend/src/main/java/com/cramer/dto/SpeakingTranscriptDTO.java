package com.cramer.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpeakingTranscriptDTO {

    private Long transcriptId;
    private Long sessionId;
    private Integer turnIndex;
    private String status;
    private OffsetDateTime recordedAt;
    private String audioStoragePath;
    private Integer audioDurationSeconds;
    private String transcriptText;
    private BigDecimal transcriptConfidence;
    private JsonNode questionEvaluation;
}
