package com.cramer.speaking.web.dto;

import com.cramer.speaking.domain.SpeakingSession;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;

/**
 * Speaking session projection (SPEC-14 §8). The blueprint's {@code _internal} deferred banks are
 * stripped before serialization by the service.
 */
public record SpeakingSessionView(
        Long id,
        Long testId,
        String sessionMode,
        String status,
        String accent,
        Double speed,
        JsonNode blueprint,
        Integer luaCost,
        Boolean luaDeducted,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt) {

    public static SpeakingSessionView of(SpeakingSession s, JsonNode publicBlueprint) {
        return new SpeakingSessionView(
                s.getId(), s.getTestId(), s.getSessionMode(),
                s.getStatus() == null ? null : s.getStatus().dbValue(),
                s.getAccent(), s.getSpeed() == null ? null : s.getSpeed().doubleValue(),
                publicBlueprint, s.getLuaCost(), s.getLuaDeducted(), s.getStartedAt(), s.getCompletedAt());
    }
}
