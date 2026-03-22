package com.cramer.service.implement;

import com.cramer.config.SpeakingSessionProperties;
import com.cramer.dto.SpeakingTurnDTO;
import com.cramer.entity.IeltsTest;
import com.cramer.entity.Question;
import com.cramer.entity.Section;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.repository.IeltsTestRepository;
import com.cramer.repository.QuestionRepository;
import com.cramer.repository.SectionRepository;
import com.cramer.service.SpeakingContentService;
import com.cramer.service.SpeakingSelectionPlannerService;
import com.cramer.service.SpeakingSelectionPlannerService.PlannerCandidate;
import com.cramer.service.SpeakingSelectionPlannerService.SelectionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SpeakingContentServiceImpl implements SpeakingContentService {

    private static final Logger logger = LoggerFactory.getLogger(SpeakingContentServiceImpl.class);

    private static final String INTERNAL_FIELD = "_internal";
    private static final String DEFERRED_PART3_FIELD = "deferredPart3";
    private static final String PENDING_AFTER_PART2 = "pending_after_part_2";

    private final IeltsTestRepository ieltsTestRepository;
    private final SectionRepository sectionRepository;
    private final QuestionRepository questionRepository;
    private final SpeakingSelectionPlannerService selectionPlannerService;
    private final SpeakingSessionProperties speakingSessionProperties;
    private final ObjectMapper objectMapper;

    public SpeakingContentServiceImpl(
            IeltsTestRepository ieltsTestRepository,
            SectionRepository sectionRepository,
            QuestionRepository questionRepository,
            SpeakingSelectionPlannerService selectionPlannerService,
            SpeakingSessionProperties speakingSessionProperties,
            ObjectMapper objectMapper) {
        this.ieltsTestRepository = ieltsTestRepository;
        this.sectionRepository = sectionRepository;
        this.questionRepository = questionRepository;
        this.selectionPlannerService = selectionPlannerService;
        this.speakingSessionProperties = speakingSessionProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public SpeakingContentPlan buildSessionPlan(Long testId, String sessionMode, String accent, BigDecimal speed) {
        IeltsTest test = ieltsTestRepository.findById(Objects.requireNonNull(testId))
                .orElseThrow(() -> new ResourceNotFoundException("IeltsTest", "id", testId));

        if (!Boolean.TRUE.equals(test.getIsPublished())) {
            throw new ResourceNotFoundException("Published Speaking test not found with id: '" + testId + "'");
        }

        List<Integer> requiredParts = requiredPartsForMode(sessionMode);
        Map<Integer, List<PlannerCandidate>> banksByPart = loadSpeakingBanks(testId, requiredParts);
        validateBankCoverage(testId, sessionMode, banksByPart);

        ObjectNode blueprint = objectMapper.createObjectNode();
        blueprint.put("schemaVersion", 1);
        blueprint.put("testId", testId);
        blueprint.put("sessionMode", sessionMode);
        blueprint.put("accent", accent);
        blueprint.put("speed", speed);

        ArrayNode partsNode = objectMapper.createArrayNode();
        List<SpeakingTurnDTO> selectedTurns = new ArrayList<>();
        int nextTurnIndex = 1;

        for (Integer partNumber : requiredParts) {
            List<PlannerCandidate> bank = banksByPart.getOrDefault(partNumber, List.of());

            if (partNumber == 1) {
                SelectionResult selection = selectionPlannerService.selectPart1(bank, speakingSessionProperties.getPart1());
                PartBuildResult result = buildSelectedPartNode(partNumber, bank.size(), selection, nextTurnIndex);
                partsNode.add(result.partNode());
                selectedTurns.addAll(result.turns());
                nextTurnIndex = result.nextTurnIndex();
                continue;
            }

            if (partNumber == 2) {
                SelectionResult selection = selectionPlannerService.selectPart2(bank, speakingSessionProperties.getPart2());
                PartBuildResult result = buildSelectedPartNode(partNumber, bank.size(), selection, nextTurnIndex);
                partsNode.add(result.partNode());
                selectedTurns.addAll(result.turns());
                nextTurnIndex = result.nextTurnIndex();
                continue;
            }

            if (partNumber == 3 && shouldDeferPart3Selection(sessionMode)) {
                partsNode.add(buildPendingPart3Node(bank.size()));
                blueprint.set(INTERNAL_FIELD, buildDeferredPart3State(bank));
                continue;
            }

            SelectionResult selection = selectionPlannerService.selectIndependentPart3(bank, speakingSessionProperties.getPart3());
            PartBuildResult result = buildSelectedPartNode(partNumber, bank.size(), selection, nextTurnIndex);
            partsNode.add(result.partNode());
            selectedTurns.addAll(result.turns());
            nextTurnIndex = result.nextTurnIndex();
        }

        blueprint.set("parts", partsNode);
        return new SpeakingContentPlan(blueprint, selectedTurns);
    }

    @Override
    public SpeakingContentPlan materializeDeferredPart3(JsonNode sessionBlueprint, String part2TranscriptText) {
        ObjectNode blueprintCopy = requireBlueprintObject(sessionBlueprint);
        if (!hasPendingDeferredPart3(blueprintCopy)) {
            return new SpeakingContentPlan(blueprintCopy, extractSelectedTurns(blueprintCopy));
        }

        List<PlannerCandidate> candidateBank = readDeferredPart3Bank(blueprintCopy);
        validateSingleBankRequirement(
                blueprintCopy.path("testId").asLong(),
                3,
                candidateBank.size(),
                speakingSessionProperties.getPart3().getBankSize());

        JsonNode part2QuestionSnapshot = findSelectedPart2Snapshot(blueprintCopy);
        if (part2QuestionSnapshot == null) {
            throw new IllegalStateException("Cannot materialize Speaking Part 3 without a selected Part 2 cue card.");
        }

        SelectionResult selection = selectionPlannerService.selectFollowUpPart3(
                candidateBank,
                part2QuestionSnapshot,
                part2TranscriptText,
                speakingSessionProperties.getPart3());

        int nextTurnIndex = maxSelectedTurnIndex(blueprintCopy) + 1;
        PartBuildResult result = buildSelectedPartNode(3, candidateBank.size(), selection, nextTurnIndex);
        replacePartNode(blueprintCopy, result.partNode());
        clearDeferredPart3State(blueprintCopy);

        return new SpeakingContentPlan(blueprintCopy, extractSelectedTurns(blueprintCopy));
    }

    @Override
    public boolean hasPendingDeferredPart3(JsonNode sessionBlueprint) {
        if (sessionBlueprint == null || !sessionBlueprint.has("parts") || !sessionBlueprint.get("parts").isArray()) {
            return false;
        }

        for (JsonNode partNode : sessionBlueprint.get("parts")) {
            if (partNode.path("partNumber").asInt() == 3
                    && PENDING_AFTER_PART2.equals(partNode.path("selectionStatus").asText(null))) {
                return true;
            }
        }

        return false;
    }

    private Map<Integer, List<PlannerCandidate>> loadSpeakingBanks(Long testId, List<Integer> requiredParts) {
        List<Section> speakingSections = sectionRepository.findByIeltsTestId(testId).stream()
                .filter(this::isPublishedSpeakingSection)
                .sorted(Comparator.comparing(Section::getPartNumber, Comparator.nullsLast(Integer::compareTo)))
                .toList();

        if (speakingSections.isEmpty()) {
            throw new IllegalArgumentException("Test " + testId + " does not have published Speaking content.");
        }

        Map<Integer, List<PlannerCandidate>> banksByPart = new LinkedHashMap<>();
        for (Integer partNumber : requiredParts) {
            List<PlannerCandidate> bank = new ArrayList<>();
            int fallbackOrder = 1;

            for (Section section : speakingSections) {
                if (!Objects.equals(section.getPartNumber(), partNumber)) {
                    continue;
                }
                List<Question> questions = questionRepository.findBySectionId(section.getId()).stream()
                        .sorted(questionComparator())
                        .toList();
                for (Question question : questions) {
                    PlannerCandidate candidate = toPlannerCandidate(question, partNumber, fallbackOrder);
                    if (candidate != null) {
                        bank.add(candidate);
                        fallbackOrder++;
                    }
                }
            }

            banksByPart.put(partNumber, bank);
        }

        return banksByPart;
    }

    private PlannerCandidate toPlannerCandidate(Question question, Integer partNumber, int fallbackOrder) {
        if (question == null || question.getId() == null) {
            return null;
        }
        if (!expectedQuestionType(partNumber).equalsIgnoreCase(String.valueOf(question.getQuestionType()))) {
            return null;
        }

        ObjectNode questionSnapshot = buildQuestionSnapshot(question, partNumber);
        if (questionSnapshot == null) {
            return null;
        }

        Integer sortOrder = question.getQuestionNumber() != null ? question.getQuestionNumber() : fallbackOrder;
        return new PlannerCandidate(question.getId(), sortOrder, questionSnapshot);
    }

    private void validateBankCoverage(Long testId, String sessionMode, Map<Integer, List<PlannerCandidate>> banksByPart) {
        List<Integer> requiredParts = requiredPartsForMode(sessionMode);
        for (Integer partNumber : requiredParts) {
            int availableCount = banksByPart.getOrDefault(partNumber, List.of()).size();
            validateSingleBankRequirement(testId, partNumber, availableCount, requiredBankSize(partNumber));
        }
    }

    private void validateSingleBankRequirement(Long testId, Integer partNumber, int availableCount, int requiredCount) {
        if (availableCount < requiredCount) {
            throw new IllegalArgumentException("Speaking test " + testId + " requires at least " + requiredCount
                    + " published " + expectedQuestionType(partNumber) + " prompts, but only " + availableCount
                    + " are currently available.");
        }
    }

    private int requiredBankSize(Integer partNumber) {
        return switch (partNumber) {
            case 1 -> speakingSessionProperties.getPart1().getBankSize();
            case 2 -> speakingSessionProperties.getPart2().getBankSize();
            case 3 -> speakingSessionProperties.getPart3().getBankSize();
            default -> throw new IllegalArgumentException("Unsupported Speaking part number: " + partNumber);
        };
    }

    private boolean shouldDeferPart3Selection(String sessionMode) {
        return "FULL".equalsIgnoreCase(sessionMode) && speakingSessionProperties.getPart3().isDeferUntilContext();
    }

    private PartBuildResult buildSelectedPartNode(
            int partNumber,
            int bankSize,
            SelectionResult selection,
            int nextTurnIndex) {
        ObjectNode partNode = objectMapper.createObjectNode();
        partNode.put("partNumber", partNumber);
        partNode.put("bankSize", bankSize);
        partNode.put("selectionStatus", "selected");
        partNode.put("selectionStrategy", selection.strategy());
        partNode.put("targetTurnCount", selection.targetTurnCount());

        ArrayNode turnNodes = objectMapper.createArrayNode();
        List<SpeakingTurnDTO> turns = new ArrayList<>();
        int currentTurnIndex = nextTurnIndex;

        for (PlannerCandidate candidate : selection.selectedCandidates()) {
            ObjectNode turnNode = objectMapper.createObjectNode();
            turnNode.put("turnIndex", currentTurnIndex);
            turnNode.put("sourceQuestionId", candidate.sourceQuestionId());
            turnNode.set("questionSnapshot", candidate.questionSnapshot().deepCopy());
            turnNodes.add(turnNode);

            turns.add(SpeakingTurnDTO.builder()
                    .turnIndex(currentTurnIndex)
                    .partNumber(partNumber)
                    .sourceQuestionId(candidate.sourceQuestionId())
                    .questionSnapshot(candidate.questionSnapshot().deepCopy())
                    .build());
            currentTurnIndex++;
        }

        partNode.put("selectedTurnCount", turns.size());
        partNode.set("turns", turnNodes);

        return new PartBuildResult(partNode, turns, currentTurnIndex);
    }

    private ObjectNode buildPendingPart3Node(int bankSize) {
        ObjectNode partNode = objectMapper.createObjectNode();
        partNode.put("partNumber", 3);
        partNode.put("bankSize", bankSize);
        partNode.put("selectionStatus", PENDING_AFTER_PART2);
        partNode.put("selectionStrategy", "follow_up_context_v1");
        partNode.put("minTurnCount", speakingSessionProperties.getPart3().getMinSelected());
        partNode.put("maxTurnCount", speakingSessionProperties.getPart3().getMaxSelected());
        partNode.set("turns", objectMapper.createArrayNode());
        return partNode;
    }

    private ObjectNode buildDeferredPart3State(List<PlannerCandidate> candidateBank) {
        ObjectNode internalNode = objectMapper.createObjectNode();
        ObjectNode deferredPart3Node = objectMapper.createObjectNode();
        deferredPart3Node.put("selectionStatus", PENDING_AFTER_PART2);
        deferredPart3Node.set("candidateBank", toCandidateArray(candidateBank));
        internalNode.set(DEFERRED_PART3_FIELD, deferredPart3Node);
        return internalNode;
    }

    private ArrayNode toCandidateArray(List<PlannerCandidate> candidateBank) {
        ArrayNode candidateArray = objectMapper.createArrayNode();
        for (PlannerCandidate candidate : candidateBank) {
            ObjectNode candidateNode = objectMapper.createObjectNode();
            candidateNode.put("sourceQuestionId", candidate.sourceQuestionId());
            candidateNode.put("sortOrder", candidate.sortOrder());
            candidateNode.set("questionSnapshot", candidate.questionSnapshot().deepCopy());
            candidateArray.add(candidateNode);
        }
        return candidateArray;
    }

    private List<PlannerCandidate> readDeferredPart3Bank(ObjectNode blueprintCopy) {
        JsonNode deferredNode = blueprintCopy.path(INTERNAL_FIELD).path(DEFERRED_PART3_FIELD).path("candidateBank");
        if (!deferredNode.isArray()) {
            throw new IllegalStateException("Deferred Speaking Part 3 bank is missing from session blueprint.");
        }

        List<PlannerCandidate> candidateBank = new ArrayList<>();
        for (JsonNode candidateNode : deferredNode) {
            if (!candidateNode.hasNonNull("sourceQuestionId") || !candidateNode.hasNonNull("questionSnapshot")) {
                continue;
            }
            candidateBank.add(new PlannerCandidate(
                    candidateNode.get("sourceQuestionId").asLong(),
                    candidateNode.hasNonNull("sortOrder") ? candidateNode.get("sortOrder").asInt() : null,
                    candidateNode.get("questionSnapshot").deepCopy()));
        }
        return candidateBank;
    }

    private JsonNode findSelectedPart2Snapshot(ObjectNode blueprintCopy) {
        JsonNode part2Node = findPartNode(blueprintCopy, 2);
        if (part2Node == null || !part2Node.has("turns") || !part2Node.get("turns").isArray() || part2Node.get("turns").isEmpty()) {
            return null;
        }

        JsonNode turnNode = part2Node.get("turns").get(0);
        return turnNode.hasNonNull("questionSnapshot") ? turnNode.get("questionSnapshot").deepCopy() : null;
    }

    private JsonNode findPartNode(ObjectNode blueprintCopy, int partNumber) {
        JsonNode partsNode = blueprintCopy.get("parts");
        if (partsNode == null || !partsNode.isArray()) {
            return null;
        }

        for (JsonNode partNode : partsNode) {
            if (partNode.path("partNumber").asInt() == partNumber) {
                return partNode;
            }
        }
        return null;
    }

    private void replacePartNode(ObjectNode blueprintCopy, ObjectNode replacementNode) {
        ArrayNode partsNode = (ArrayNode) blueprintCopy.get("parts");
        for (int index = 0; index < partsNode.size(); index++) {
            if (partsNode.get(index).path("partNumber").asInt() == replacementNode.path("partNumber").asInt()) {
                partsNode.set(index, replacementNode);
                return;
            }
        }
        partsNode.add(replacementNode);
    }

    private void clearDeferredPart3State(ObjectNode blueprintCopy) {
        if (!blueprintCopy.has(INTERNAL_FIELD) || !blueprintCopy.get(INTERNAL_FIELD).isObject()) {
            return;
        }

        ObjectNode internalNode = (ObjectNode) blueprintCopy.get(INTERNAL_FIELD);
        internalNode.remove(DEFERRED_PART3_FIELD);
        if (internalNode.isEmpty()) {
            blueprintCopy.remove(INTERNAL_FIELD);
        }
    }

    private int maxSelectedTurnIndex(ObjectNode blueprintCopy) {
        return extractSelectedTurns(blueprintCopy).stream()
                .map(SpeakingTurnDTO::getTurnIndex)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
    }

    private List<SpeakingTurnDTO> extractSelectedTurns(JsonNode sessionBlueprint) {
        List<SpeakingTurnDTO> turns = new ArrayList<>();
        if (sessionBlueprint == null || !sessionBlueprint.has("parts") || !sessionBlueprint.get("parts").isArray()) {
            return turns;
        }

        for (JsonNode partNode : sessionBlueprint.get("parts")) {
            Integer partNumber = partNode.hasNonNull("partNumber") ? partNode.get("partNumber").asInt() : null;
            JsonNode turnNodes = partNode.get("turns");
            if (partNumber == null || turnNodes == null || !turnNodes.isArray()) {
                continue;
            }

            for (JsonNode turnNode : turnNodes) {
                if (!turnNode.hasNonNull("turnIndex") || !turnNode.hasNonNull("sourceQuestionId")
                        || !turnNode.hasNonNull("questionSnapshot")) {
                    continue;
                }
                turns.add(SpeakingTurnDTO.builder()
                        .turnIndex(turnNode.get("turnIndex").asInt())
                        .partNumber(partNumber)
                        .sourceQuestionId(turnNode.get("sourceQuestionId").asLong())
                        .questionSnapshot(turnNode.get("questionSnapshot").deepCopy())
                        .build());
            }
        }

        turns.sort(Comparator.comparing(SpeakingTurnDTO::getTurnIndex));
        return turns;
    }

    private ObjectNode requireBlueprintObject(JsonNode sessionBlueprint) {
        if (sessionBlueprint == null || !sessionBlueprint.isObject()) {
            throw new IllegalStateException("Speaking session blueprint is missing or invalid.");
        }
        return sessionBlueprint.deepCopy();
    }

    private boolean isPublishedSpeakingSection(Section section) {
        return section != null
                && section.getPartNumber() != null
                && "speaking".equalsIgnoreCase(section.getSkill())
                && (section.getStatus() == null || "PUBLISHED".equalsIgnoreCase(section.getStatus()));
    }

    private Comparator<Question> questionComparator() {
        return Comparator.comparing(Question::getQuestionNumber, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(Question::getId, Comparator.nullsLast(Long::compareTo));
    }

    private ObjectNode buildQuestionSnapshot(Question question, Integer partNumber) {
        JsonNode source = question.getQuestionContent();
        if (source == null || !source.isObject()) {
            logger.warn("Skipping speaking question {} because question_content is missing or not an object.", question.getId());
            return null;
        }

        ObjectNode snapshot = source.deepCopy();
        String expectedPartType = expectedQuestionType(partNumber);
        JsonNode promptText = snapshot.get("promptText");
        if (promptText == null || promptText.isNull() || promptText.asText().isBlank()) {
            logger.warn("Skipping speaking question {} because promptText is missing.", question.getId());
            return null;
        }

        JsonNode existingPartType = snapshot.get("partType");
        if (existingPartType != null && !existingPartType.isNull()
                && !expectedPartType.equalsIgnoreCase(existingPartType.asText())) {
            logger.warn("Skipping speaking question {} because partType {} does not match expected {}.",
                    question.getId(), existingPartType.asText(), expectedPartType);
            return null;
        }

        if (!snapshot.hasNonNull("schemaVersion")) {
            snapshot.put("schemaVersion", 1);
        }
        snapshot.put("partType", expectedPartType);
        return snapshot;
    }

    private String expectedQuestionType(Integer partNumber) {
        return switch (partNumber) {
            case 1 -> "PART_1";
            case 2 -> "PART_2";
            case 3 -> "PART_3";
            default -> throw new IllegalArgumentException("Unsupported Speaking part number: " + partNumber);
        };
    }

    private List<Integer> requiredPartsForMode(String sessionMode) {
        String normalizedMode = normalizeSessionMode(sessionMode);
        return switch (normalizedMode) {
            case "FULL" -> List.of(1, 2, 3);
            case "PART_1" -> List.of(1);
            case "PART_2" -> List.of(2);
            case "PART_3" -> List.of(3);
            default -> throw new IllegalArgumentException("Unsupported sessionMode: " + sessionMode);
        };
    }

    private String normalizeSessionMode(String sessionMode) {
        if (sessionMode == null || sessionMode.isBlank()) {
            throw new IllegalArgumentException("sessionMode is required");
        }
        String normalized = sessionMode.trim().toUpperCase(Locale.ROOT);
        if (!List.of("FULL", "PART_1", "PART_2", "PART_3").contains(normalized)) {
            throw new IllegalArgumentException("sessionMode must be one of FULL, PART_1, PART_2, PART_3");
        }
        return normalized;
    }

    private record PartBuildResult(ObjectNode partNode, List<SpeakingTurnDTO> turns, int nextTurnIndex) {
    }
}
