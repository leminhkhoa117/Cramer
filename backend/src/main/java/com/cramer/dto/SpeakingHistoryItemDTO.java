package com.cramer.dto;

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
public class SpeakingHistoryItemDTO {

    private Long sessionId;
    private Long testId;
    private String sessionMode;
    private String status;
    private BigDecimal overallBand;
    private OffsetDateTime createdAt;
    private OffsetDateTime completedAt;
}
