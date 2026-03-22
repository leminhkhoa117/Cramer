package com.cramer.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpeakingGradingStatusDTO {

    private Long sessionId;
    private String status;
    private String progress;
    private Integer estimatedSeconds;
    private OffsetDateTime updatedAt;
}
