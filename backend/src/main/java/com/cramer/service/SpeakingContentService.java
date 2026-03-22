package com.cramer.service;

import com.cramer.dto.SpeakingTurnDTO;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;

public interface SpeakingContentService {

    SpeakingContentPlan buildSessionPlan(Long testId, String sessionMode, String accent, BigDecimal speed);

    SpeakingContentPlan materializeDeferredPart3(JsonNode sessionBlueprint, String part2TranscriptText);

    boolean hasPendingDeferredPart3(JsonNode sessionBlueprint);

    record SpeakingContentPlan(JsonNode sessionBlueprint, List<SpeakingTurnDTO> turns) {
    }
}
