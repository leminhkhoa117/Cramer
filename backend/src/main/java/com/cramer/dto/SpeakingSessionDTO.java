package com.cramer.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpeakingSessionDTO {

    private Long sessionId;
    private String sessionMode;
    private Long testId;
    private String status;
    private Boolean isFinalized;
    private Integer luaCost;
    private String accent;
    private BigDecimal speed;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime gradedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private JsonNode sessionBlueprint;
    private List<SpeakingTurnDTO> turns;
}
