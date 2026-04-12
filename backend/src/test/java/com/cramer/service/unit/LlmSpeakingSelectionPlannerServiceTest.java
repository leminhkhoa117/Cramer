package com.cramer.service.unit;

import com.cramer.config.OpenRouterConfig;
import com.cramer.config.SpeakingSelectionProperties;
import com.cramer.config.SpeakingSessionProperties;
import com.cramer.service.SpeakingSelectionPlannerService.PlannerCandidate;
import com.cramer.service.SpeakingSelectionPlannerService.SelectionResult;
import com.cramer.service.implement.HeuristicSpeakingSelectionPlannerService;
import com.cramer.service.implement.LlmSpeakingSelectionPlannerService;
import com.cramer.service.speaking.SpeakingSelectionPromptBuilder;
import com.cramer.service.speaking.SpeakingSelectionPromptBuilder.PromptPair;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LlmSpeakingSelectionPlannerService}.
 *
 * <p>Uses mocks for the OpenRouter HTTP layer and the prompt builder.
 * Verifies LLM success path, validation failures, and heuristic fallback.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LlmSpeakingSelectionPlannerService Unit Tests")
class LlmSpeakingSelectionPlannerServiceTest {

    @Mock
    private SpeakingSelectionPromptBuilder promptBuilder;

    @Mock
    private RestTemplate mockRestTemplate;

    private OpenRouterConfig openRouterConfig;
    private SpeakingSelectionProperties selectionProperties;
    private HeuristicSpeakingSelectionPlannerService heuristicFallback;
    private LlmSpeakingSelectionPlannerService llmPlanner;
    private ObjectMapper objectMapper;

    private SpeakingSessionProperties.PartPlan part1Config;
    private SpeakingSessionProperties.PartPlan part2Config;
    private SpeakingSessionProperties.PartPlan part3Config;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        openRouterConfig = new OpenRouterConfig();
        openRouterConfig.setApiKey("test-api-key");
        openRouterConfig.setBaseUrl("https://openrouter.ai/api/v1");
        openRouterConfig.setSiteUrl("https://cramer.vn");
        openRouterConfig.setSiteName("Cramer Test");

        selectionProperties = new SpeakingSelectionProperties();
        selectionProperties.setProvider("llm");
        selectionProperties.setModel("deepseek/deepseek-chat");
        selectionProperties.setTimeoutMs(12000);
        selectionProperties.setTemperature(0.7);
        selectionProperties.setMaxTokens(512);

        heuristicFallback = new HeuristicSpeakingSelectionPlannerService();

        llmPlanner = new LlmSpeakingSelectionPlannerService(
                openRouterConfig, selectionProperties, promptBuilder,
                heuristicFallback, mockRestTemplate);

        part1Config = new SpeakingSessionProperties.PartPlan(30, 8, 12, false);
        part2Config = new SpeakingSessionProperties.PartPlan(1, 1, 1, false);
        part3Config = new SpeakingSessionProperties.PartPlan(15, 3, 6, true);
    }

    // ────────────────────────────────────────────────────────────
    // True LLM success path
    // ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("selectPart1: LLM returns valid JSON -> strategy = llm_selection_v1, IDs from bank")
    void selectPart1_validLlmResponse_returnsLlmSelectionV1Strategy() {
        List<PlannerCandidate> bank = buildBank(30, "PART_1", "Work", "Study", "Hobbies");

        PromptPair fakePrompt = new PromptPair("system prompt", "user prompt");
        when(promptBuilder.buildPart1Prompt(eq(bank), anyInt())).thenReturn(fakePrompt);
        when(promptBuilder.getSelectionResponseSchema()).thenReturn(java.util.Map.of());

        // Simulate valid LLM response with 10 IDs from the bank (covers any targetTurnCount in [8,12])
        // We use parseAndValidate mock to return valid result with exactly the requested count
        when(promptBuilder.parseAndValidate(anyString(), eq(bank), anyInt())).thenAnswer(invocation -> {
            int requestedCount = invocation.getArgument(2);
            List<Long> ids = new ArrayList<>();
            for (int i = 0; i < requestedCount; i++) {
                ids.add(1000L + i);
            }
            return new SpeakingSelectionPromptBuilder.SelectionParseResult(
                    true, ids, "Selected diverse topic clusters.", null);
        });

        // Mock HTTP call to return a valid OpenRouter response
        String fakeResponseBody = """
                {
                  "choices": [{
                    "message": {
                      "content": "{\\"selectedQuestionIds\\":[1000,1001,1002],\\"reasoningSummary\\":\\"test\\"}"
                    }
                  }]
                }
                """;
        when(mockRestTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(fakeResponseBody, HttpStatus.OK));

        SelectionResult result = llmPlanner.selectPart1(bank, part1Config);

        assertThat(result).isNotNull();
        assertThat(result.strategy()).isEqualTo("llm_selection_v1");
        assertThat(result.targetTurnCount()).isBetween(8, 12);
        assertThat(result.selectedCandidates()).hasSize(result.targetTurnCount());

        // All selected IDs must be from the bank
        Set<Long> bankIds = bank.stream()
                .map(PlannerCandidate::sourceQuestionId)
                .collect(Collectors.toSet());
        for (PlannerCandidate selected : result.selectedCandidates()) {
            assertThat(bankIds).contains(selected.sourceQuestionId());
        }
    }

    @Test
    @DisplayName("selectPart1: LLM returns invalid selection -> falls back to heuristic")
    void selectPart1_invalidLlmResponse_fallsBackToHeuristic() {
        List<PlannerCandidate> bank = buildBank(30, "PART_1", "Work", "Study", "Hobbies");

        PromptPair fakePrompt = new PromptPair("system", "user");
        when(promptBuilder.buildPart1Prompt(eq(bank), anyInt())).thenReturn(fakePrompt);
        when(promptBuilder.getSelectionResponseSchema()).thenReturn(java.util.Map.of());

        // HTTP returns OK but parseAndValidate returns invalid
        String fakeResponseBody = """
                {
                  "choices": [{
                    "message": { "content": "{\\"selectedQuestionIds\\":[9999],\\"reasoningSummary\\":\\"bad\\"}" }
                  }]
                }
                """;
        when(mockRestTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(fakeResponseBody, HttpStatus.OK));
        when(promptBuilder.parseAndValidate(anyString(), eq(bank), anyInt()))
                .thenReturn(SpeakingSelectionPromptBuilder.SelectionParseResult.invalid("wrong count"));

        SelectionResult result = llmPlanner.selectPart1(bank, part1Config);

        assertThat(result).isNotNull();
        assertThat(result.strategy()).isEqualTo("topic_cluster_random_v1");
        assertThat(result.targetTurnCount()).isBetween(8, 12);
    }

    // ────────────────────────────────────────────────────────────
    // Fallback paths
    // ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("selectPart1: LLM HTTP exception -> falls back to heuristic")
    void selectPart1_llmException_fallsBackToHeuristic() {
        List<PlannerCandidate> bank = buildBank(30, "PART_1", "Work", "Study", "Hobbies");

        PromptPair fakePrompt = new PromptPair("system", "user");
        when(promptBuilder.buildPart1Prompt(eq(bank), anyInt())).thenReturn(fakePrompt);
        when(promptBuilder.getSelectionResponseSchema()).thenReturn(java.util.Map.of());

        // HTTP call throws exception
        when(mockRestTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        SelectionResult result = llmPlanner.selectPart1(bank, part1Config);

        assertThat(result).isNotNull();
        assertThat(result.targetTurnCount()).isBetween(8, 12);
        assertThat(result.selectedCandidates()).hasSizeBetween(8, 12);
        assertThat(result.strategy()).isEqualTo("topic_cluster_random_v1");
    }

    @Test
    @DisplayName("selectPart2: Always delegates to heuristic regardless of provider")
    void selectPart2_alwaysUsesHeuristic() {
        List<PlannerCandidate> bank = buildBank(1, "PART_2", "City life");

        SelectionResult result = llmPlanner.selectPart2(bank, part2Config);

        assertThat(result).isNotNull();
        assertThat(result.targetTurnCount()).isEqualTo(1);
        assertThat(result.selectedCandidates()).hasSize(1);
        assertThat(result.strategy()).isEqualTo("single_cue_card_v1");
    }

    @Test
    @DisplayName("selectIndependentPart3: LLM fails -> falls back to heuristic with 3..6 selections")
    void selectIndependentPart3_llmFails_fallsBackToHeuristic() {
        List<PlannerCandidate> bank = buildBank(15, "PART_3", "Education", "Technology");

        PromptPair fakePrompt = new PromptPair("system", "user");
        when(promptBuilder.buildIndependentPart3Prompt(eq(bank), anyInt())).thenReturn(fakePrompt);
        when(promptBuilder.getSelectionResponseSchema()).thenReturn(java.util.Map.of());

        when(mockRestTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("Timeout"));

        SelectionResult result = llmPlanner.selectIndependentPart3(bank, part3Config);

        assertThat(result).isNotNull();
        assertThat(result.targetTurnCount()).isBetween(3, 6);
        assertThat(result.selectedCandidates()).hasSizeBetween(3, 6);
        assertThat(result.strategy()).isEqualTo("topic_cluster_random_v1");
    }

    @Test
    @DisplayName("selectFollowUpPart3: LLM fails -> falls back to heuristic")
    void selectFollowUpPart3_llmFails_fallsBackToHeuristic() {
        List<PlannerCandidate> bank = buildBank(15, "PART_3", "Travel");
        ObjectNode part2Snapshot = buildSnapshot("PART_2", "Travel", "Describe a city you visited");

        PromptPair fakePrompt = new PromptPair("system", "user");
        when(promptBuilder.buildFollowUpPart3Prompt(eq(bank), anyInt(), any(), any())).thenReturn(fakePrompt);
        when(promptBuilder.getSelectionResponseSchema()).thenReturn(java.util.Map.of());

        when(mockRestTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("Timeout"));

        SelectionResult result = llmPlanner.selectFollowUpPart3(
                bank, part2Snapshot, "I visited London last year.", part3Config);

        assertThat(result).isNotNull();
        assertThat(result.targetTurnCount()).isBetween(3, 6);
        assertThat(result.selectedCandidates()).hasSizeBetween(3, 6);
        assertThat(result.strategy()).isEqualTo("follow_up_context_v1");
    }

    // ────────────────────────────────────────────────────────────
    // Guard conditions
    // ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("selectPart1: No model configured -> immediate heuristic fallback")
    void selectPart1_noModel_fallsBackImmediately() {
        selectionProperties.setModel("");
        llmPlanner = new LlmSpeakingSelectionPlannerService(
                openRouterConfig, selectionProperties, promptBuilder,
                heuristicFallback, mockRestTemplate);

        List<PlannerCandidate> bank = buildBank(30, "PART_1", "Work", "Study");

        SelectionResult result = llmPlanner.selectPart1(bank, part1Config);

        assertThat(result).isNotNull();
        assertThat(result.strategy()).isEqualTo("topic_cluster_random_v1");
        verify(promptBuilder, never()).buildPart1Prompt(anyList(), anyInt());
    }

    @Test
    @DisplayName("selectPart1: No API key configured -> immediate heuristic fallback")
    void selectPart1_noApiKey_fallsBackImmediately() {
        openRouterConfig.setApiKey("");
        llmPlanner = new LlmSpeakingSelectionPlannerService(
                openRouterConfig, selectionProperties, promptBuilder,
                heuristicFallback, mockRestTemplate);

        List<PlannerCandidate> bank = buildBank(30, "PART_1", "Work", "Study");

        SelectionResult result = llmPlanner.selectPart1(bank, part1Config);

        assertThat(result).isNotNull();
        assertThat(result.strategy()).isEqualTo("topic_cluster_random_v1");
        verify(promptBuilder, never()).buildPart1Prompt(anyList(), anyInt());
    }

    // ────────────────────────────────────────────────────────────
    // Fallback re-randomization
    // ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Heuristic fallback re-randomizes its own targetTurnCount")
    void fallback_reRandomizesTargetTurnCount() {
        List<PlannerCandidate> bank = buildBank(30, "PART_1", "Work", "Study", "Hobbies");

        PromptPair fakePrompt = new PromptPair("system", "user");
        when(promptBuilder.buildPart1Prompt(eq(bank), anyInt())).thenReturn(fakePrompt);
        when(promptBuilder.getSelectionResponseSchema()).thenReturn(java.util.Map.of());

        when(mockRestTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("Timeout"));

        // Run multiple times to show heuristic owns its own targetTurnCount
        boolean sawDifferentCounts = false;
        int firstCount = -1;
        for (int i = 0; i < 20; i++) {
            SelectionResult result = llmPlanner.selectPart1(bank, part1Config);
            assertThat(result.targetTurnCount()).isBetween(8, 12);
            assertThat(result.selectedCandidates()).hasSize(result.targetTurnCount());
            if (firstCount == -1) {
                firstCount = result.targetTurnCount();
            } else if (result.targetTurnCount() != firstCount) {
                sawDifferentCounts = true;
            }
        }
        // With 20 runs across [8,12], it's statistically near-certain we see variation
        assertThat(sawDifferentCounts)
                .as("Heuristic should re-randomize targetTurnCount independently")
                .isTrue();
    }

    // ────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────

    private List<PlannerCandidate> buildBank(int count, String partType, String... topics) {
        List<PlannerCandidate> bank = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String topic = topics[i % topics.length];
            ObjectNode snapshot = buildSnapshot(partType, topic, partType + " prompt " + (i + 1) + " about " + topic);
            bank.add(new PlannerCandidate(1000L + i, i + 1, snapshot));
        }
        return bank;
    }

    private ObjectNode buildSnapshot(String partType, String topicLabel, String promptText) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("schemaVersion", 1);
        node.put("partType", partType);
        node.put("promptText", promptText);
        node.put("topicLabel", topicLabel);
        return node;
    }
}
