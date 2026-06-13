package com.cramer.speaking.service;

import com.cramer.catalog.service.ContentLookupPort;
import com.cramer.catalog.service.SpeakingQuestionRef;
import com.cramer.platform.common.json.Json;
import com.cramer.platform.error.OperationNotAllowedException;
import com.cramer.speaking.config.SpeakingSessionProperties;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Builds the frozen session blueprint (SPEC-14 §3): {@code { schemaVersion, testId, sessionMode,
 * accent, speed, parts[] }} where each part has {@code turns[]} of
 * {@code { turnIndex, sourceQuestionId, partNumber, questionSnapshot }}.
 *
 * <p>Uses a heuristic planner: Part 1 picks a random count in the configured range, Part 2 a
 * single cue card, Part 3 a coherent set. (The LLM planner and deferred-Part-3 optimization are
 * follow-ons; immediate selection is functionally valid and keeps the blueprint deterministic to
 * validate against on transcript upsert.)
 */
@Service
public class SpeakingBlueprintService {

    private static final int SCHEMA_VERSION = 1;

    private final ContentLookupPort content;
    private final SpeakingSessionProperties props;

    public SpeakingBlueprintService(ContentLookupPort content, SpeakingSessionProperties props) {
        this.content = content;
        this.props = props;
    }

    public ObjectNode build(long testId, String sessionMode, String accent, String speed) {
        List<Integer> parts = partsFor(sessionMode);
        Map<Integer, List<SpeakingQuestionRef>> bank = parts.stream()
                .collect(Collectors.toMap(p -> p, p -> new ArrayList<>(content.speakingBank(testId, p))));

        ObjectNode blueprint = Json.mapper().createObjectNode();
        blueprint.put("schemaVersion", SCHEMA_VERSION);
        blueprint.put("testId", testId);
        blueprint.put("sessionMode", sessionMode);
        blueprint.put("accent", accent);
        blueprint.put("speed", speed);
        ArrayNode partsNode = blueprint.putArray("parts");

        int turnIndex = 0;
        for (int part : parts) {
            List<SpeakingQuestionRef> available = bank.getOrDefault(part, List.of());
            if (available.isEmpty()) {
                throw new OperationNotAllowedException("No published Speaking questions for part " + part);
            }
            int target = targetCount(part);
            List<SpeakingQuestionRef> picked = pick(available, target);

            ObjectNode partNode = partsNode.addObject();
            partNode.put("partNumber", part);
            partNode.put("bankSize", available.size());
            partNode.put("selectionStrategy", "heuristic");
            partNode.put("targetTurnCount", picked.size());
            partNode.put("selectedTurnCount", picked.size());
            ArrayNode turns = partNode.putArray("turns");
            for (SpeakingQuestionRef q : picked) {
                ObjectNode turn = turns.addObject();
                turn.put("turnIndex", turnIndex++);
                turn.put("sourceQuestionId", q.questionId());
                turn.put("partNumber", part);
                turn.set("questionSnapshot", q.questionContent());
            }
        }
        return blueprint;
    }

    private List<Integer> partsFor(String mode) {
        return switch (mode) {
            case "FULL" -> List.of(1, 2, 3);
            case "PART_1" -> List.of(1);
            case "PART_2" -> List.of(2);
            case "PART_3" -> List.of(3);
            case "PART_2_AND_3" -> List.of(2, 3);
            default -> throw new OperationNotAllowedException("Unsupported session mode: " + mode);
        };
    }

    private int targetCount(int part) {
        return switch (part) {
            case 1 -> randomInRange(props.p1Min(), props.p1Max());
            case 2 -> 1;
            case 3 -> randomInRange(props.p3Min(), props.p3Max());
            default -> 1;
        };
    }

    private static int randomInRange(int min, int max) {
        if (max <= min) {
            return Math.max(1, min);
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static List<SpeakingQuestionRef> pick(List<SpeakingQuestionRef> available, int count) {
        List<SpeakingQuestionRef> shuffled = new ArrayList<>(available);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }
}
