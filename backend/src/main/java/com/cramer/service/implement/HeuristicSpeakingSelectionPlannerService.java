package com.cramer.service.implement;

import com.cramer.config.SpeakingSessionProperties;
import com.cramer.service.SpeakingSelectionPlannerService;
import com.fasterxml.jackson.databind.JsonNode;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class HeuristicSpeakingSelectionPlannerService implements SpeakingSelectionPlannerService {

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^a-z0-9]+");

    private final SecureRandom random = new SecureRandom();

    @Override
    public SelectionResult selectPart1(List<PlannerCandidate> bank, SpeakingSessionProperties.PartPlan config) {
        int targetTurnCount = pickTargetTurnCount(config);
        List<PlannerCandidate> selectedCandidates = selectPart1TopicCluster(bank, targetTurnCount);
        return new SelectionResult(targetTurnCount, selectedCandidates, "topic_cluster_random_v1");
    }

    @Override
    public SelectionResult selectPart2(List<PlannerCandidate> bank, SpeakingSessionProperties.PartPlan config) {
        int targetTurnCount = pickTargetTurnCount(config);
        List<PlannerCandidate> selectedCandidates = shuffleCopy(bank).stream()
                .limit(targetTurnCount)
                .toList();
        return new SelectionResult(targetTurnCount, selectedCandidates, "single_cue_card_v1");
    }

    @Override
    public SelectionResult selectIndependentPart3(List<PlannerCandidate> bank, SpeakingSessionProperties.PartPlan config) {
        int targetTurnCount = pickTargetTurnCount(config);
        List<PlannerCandidate> selectedCandidates = selectIndependentPart3Subset(bank, targetTurnCount);
        return new SelectionResult(targetTurnCount, selectedCandidates, "topic_cluster_random_v1");
    }

    @Override
    public SelectionResult selectFollowUpPart3(
            List<PlannerCandidate> bank,
            JsonNode part2QuestionSnapshot,
            String part2TranscriptText,
            SpeakingSessionProperties.PartPlan config) {
        int targetTurnCount = pickTargetTurnCount(config);
        String part2TopicLabel = extractTopicLabel(part2QuestionSnapshot);
        Set<String> contextTokens = new HashSet<>();
        contextTokens.addAll(tokenize(part2TopicLabel));
        contextTokens.addAll(tokenize(textValue(part2QuestionSnapshot, "promptText")));
        contextTokens.addAll(tokenize(part2TranscriptText));

        List<PlannerCandidate> shuffled = shuffleCopy(bank);
        shuffled.sort(Comparator
                .comparingInt((PlannerCandidate candidate) -> scoreFollowUpCandidate(candidate, part2TopicLabel, contextTokens))
                .reversed()
                .thenComparing(this::sortOrder));

        List<PlannerCandidate> selectedCandidates = shuffled.stream()
                .limit(targetTurnCount)
                .toList();

        return new SelectionResult(targetTurnCount, selectedCandidates, "follow_up_context_v1");
    }

    private List<PlannerCandidate> selectPart1TopicCluster(List<PlannerCandidate> bank, int targetTurnCount) {
        Map<String, List<PlannerCandidate>> groups = groupByTopic(bank);
        if (groups.size() < 2) {
            return randomSubset(bank, targetTurnCount);
        }

        List<List<PlannerCandidate>> topicGroups = new ArrayList<>(groups.values());
        Collections.shuffle(topicGroups, random);
        topicGroups.sort(Comparator.comparingInt((List<PlannerCandidate> group) -> group.size()).reversed());

        int targetTopicCount = Math.min(topicGroups.size(), randomBetween(2, Math.min(3, topicGroups.size())));
        List<List<PlannerCandidate>> chosenGroups = new ArrayList<>(topicGroups.subList(0, targetTopicCount));

        List<PlannerCandidate> selected = allocateAcrossGroups(chosenGroups, targetTurnCount);
        if (selected.size() < targetTurnCount) {
            selected = topUpSelection(selected, bank, targetTurnCount);
        }
        return selected;
    }

    private List<PlannerCandidate> selectIndependentPart3Subset(List<PlannerCandidate> bank, int targetTurnCount) {
        Map<String, List<PlannerCandidate>> groups = groupByTopic(bank);
        if (groups.isEmpty()) {
            return randomSubset(bank, targetTurnCount);
        }

        List<List<PlannerCandidate>> topicGroups = new ArrayList<>(groups.values());
        Collections.shuffle(topicGroups, random);
        topicGroups.sort(Comparator.comparingInt((List<PlannerCandidate> group) -> group.size()).reversed());

        List<PlannerCandidate> selected = new ArrayList<>(topicGroups.get(0).stream().limit(targetTurnCount).toList());
        if (selected.size() < targetTurnCount) {
            selected = topUpSelection(selected, bank, targetTurnCount);
        }
        selected.sort(Comparator.comparing(this::sortOrder));
        return selected;
    }

    private List<PlannerCandidate> allocateAcrossGroups(List<List<PlannerCandidate>> groups, int targetTurnCount) {
        List<PlannerCandidate> selected = new ArrayList<>();
        int remaining = targetTurnCount;

        for (int index = 0; index < groups.size() && remaining > 0; index++) {
            List<PlannerCandidate> group = new ArrayList<>(groups.get(index));
            group.sort(Comparator.comparing(this::sortOrder));
            int groupsLeft = groups.size() - index;
            int desiredCount = (int) Math.ceil((double) remaining / groupsLeft);
            int takeCount = Math.min(desiredCount, group.size());
            selected.addAll(group.subList(0, takeCount));
            remaining -= takeCount;
        }

        selected.sort(Comparator.comparing(this::sortOrder));
        return selected;
    }

    private List<PlannerCandidate> topUpSelection(
            List<PlannerCandidate> currentSelection,
            List<PlannerCandidate> bank,
            int targetTurnCount) {
        Set<Long> selectedIds = currentSelection.stream()
                .map(PlannerCandidate::sourceQuestionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<PlannerCandidate> toppedUp = new ArrayList<>(currentSelection);
        for (PlannerCandidate candidate : shuffleCopy(bank)) {
            if (toppedUp.size() >= targetTurnCount) {
                break;
            }
            if (selectedIds.add(candidate.sourceQuestionId())) {
                toppedUp.add(candidate);
            }
        }

        toppedUp.sort(Comparator.comparing(this::sortOrder));
        return toppedUp;
    }

    private List<PlannerCandidate> randomSubset(List<PlannerCandidate> bank, int targetTurnCount) {
        List<PlannerCandidate> shuffled = shuffleCopy(bank);
        List<PlannerCandidate> selected = shuffled.stream().limit(targetTurnCount).collect(Collectors.toCollection(ArrayList::new));
        selected.sort(Comparator.comparing(this::sortOrder));
        return selected;
    }

    private int scoreFollowUpCandidate(
            PlannerCandidate candidate,
            String part2TopicLabel,
            Set<String> contextTokens) {
        int score = 0;
        String candidateTopic = extractTopicLabel(candidate.questionSnapshot());
        if (part2TopicLabel != null && part2TopicLabel.equalsIgnoreCase(candidateTopic)) {
            score += 100;
        }

        Set<String> candidateTokens = new HashSet<>();
        candidateTokens.addAll(tokenize(candidateTopic));
        candidateTokens.addAll(tokenize(textValue(candidate.questionSnapshot(), "promptText")));

        for (String token : candidateTokens) {
            if (contextTokens.contains(token)) {
                score += 10;
            }
        }
        return score;
    }

    private Map<String, List<PlannerCandidate>> groupByTopic(List<PlannerCandidate> bank) {
        Map<String, List<PlannerCandidate>> grouped = new LinkedHashMap<>();
        for (PlannerCandidate candidate : bank) {
            String topicKey = normalizeTopicKey(extractTopicLabel(candidate.questionSnapshot()));
            grouped.computeIfAbsent(topicKey, ignored -> new ArrayList<>()).add(candidate);
        }
        return grouped;
    }

    private String normalizeTopicKey(String topicLabel) {
        if (topicLabel == null || topicLabel.isBlank()) {
            return "__untagged__";
        }
        return topicLabel.trim().toLowerCase(Locale.ROOT);
    }

    private List<PlannerCandidate> shuffleCopy(Collection<PlannerCandidate> candidates) {
        List<PlannerCandidate> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled, random);
        return shuffled;
    }

    private int pickTargetTurnCount(SpeakingSessionProperties.PartPlan config) {
        int minSelected = config.getMinSelected();
        int maxSelected = config.getMaxSelected();
        if (minSelected <= 0 || maxSelected <= 0 || maxSelected < minSelected) {
            throw new IllegalStateException("Invalid Speaking selection window configuration.");
        }
        return randomBetween(minSelected, maxSelected);
    }

    private int randomBetween(int minInclusive, int maxInclusive) {
        if (minInclusive == maxInclusive) {
            return minInclusive;
        }
        return random.nextInt(maxInclusive - minInclusive + 1) + minInclusive;
    }

    private Integer sortOrder(PlannerCandidate candidate) {
        return candidate.sortOrder() == null ? Integer.MAX_VALUE : candidate.sortOrder();
    }

    private String extractTopicLabel(JsonNode questionSnapshot) {
        return textValue(questionSnapshot, "topicLabel");
    }

    private String textValue(JsonNode node, String fieldName) {
        if (node == null || !node.hasNonNull(fieldName)) {
            return null;
        }
        String value = node.get(fieldName).asText(null);
        return value == null || value.isBlank() ? null : value;
    }

    private Set<String> tokenize(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }

        Map<String, Integer> tokens = new HashMap<>();
        for (String token : TOKEN_SPLIT.split(value.toLowerCase(Locale.ROOT))) {
            if (token.length() < 3) {
                continue;
            }
            tokens.merge(token, 1, Integer::sum);
        }
        return tokens.keySet();
    }
}
