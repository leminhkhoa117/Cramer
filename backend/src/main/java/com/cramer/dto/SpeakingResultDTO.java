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
public class SpeakingResultDTO {

    private Long sessionId;
    private String sessionMode;
    private Long testId;
    private String status;
    private BigDecimal overallBand;
    private BigDecimal fluencyBand;
    private BigDecimal lexicalBand;
    private BigDecimal grammarBand;
    private BigDecimal pronunciationBand;
    private JsonNode gradingResult;
    private OffsetDateTime gradedAt;
}
