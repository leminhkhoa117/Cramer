package com.cramer.service;

import com.cramer.config.SpeakingSessionProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public interface SpeakingSelectionPlannerService {

    SelectionResult selectPart1(List<PlannerCandidate> bank, SpeakingSessionProperties.PartPlan config);

    SelectionResult selectPart2(List<PlannerCandidate> bank, SpeakingSessionProperties.PartPlan config);

    SelectionResult selectIndependentPart3(List<PlannerCandidate> bank, SpeakingSessionProperties.PartPlan config);

    SelectionResult selectFollowUpPart3(
            List<PlannerCandidate> bank,
            JsonNode part2QuestionSnapshot,
            String part2TranscriptText,
            SpeakingSessionProperties.PartPlan config);

    record PlannerCandidate(Long sourceQuestionId, Integer sortOrder, JsonNode questionSnapshot) {
    }

    record SelectionResult(int targetTurnCount, List<PlannerCandidate> selectedCandidates, String strategy) {
    }
}
