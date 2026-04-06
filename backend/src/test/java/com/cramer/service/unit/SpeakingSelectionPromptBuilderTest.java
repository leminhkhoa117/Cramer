package com.cramer.service.unit;

import com.cramer.service.SpeakingSelectionPlannerService.PlannerCandidate;
import com.cramer.service.speaking.SpeakingSelectionPromptBuilder;
import com.cramer.service.speaking.SpeakingSelectionPromptBuilder.PromptPair;
import com.cramer.service.speaking.SpeakingSelectionPromptBuilder.SelectionParseResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SpeakingSelectionPromptBuilder}.
 *
 * <p>Tests prompt construction, JSON schema generation, and response
 * parsing/validation logic. No Spring context or mocks needed -- pure
 * unit tests.</p>
 */
@DisplayName("SpeakingSelectionPromptBuilder Unit Tests")
class SpeakingSelectionPromptBuilderTest {

    private SpeakingSelectionPromptBuilder promptBuilder;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        promptBuilder = new SpeakingSelectionPromptBuilder();
        objectMapper = new ObjectMapper();
    }

    // ────────────────────────────────────────────────────────────
    // parseAndValidate: success
    // ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("parseAndValidate with valid response returns valid=true and correct IDs")
    void parseAndValidate_validResponse_returnsValid() {
        List<PlannerCandidate> bank = buildBank(10);
        String json = """
                {
                  "selectedQuestionIds": [1000, 1001, 1002, 1003, 1004],
                  "reasoningSummary": "Balanced selection across topics."
                }
                """;

        SelectionParseResult result = promptBuilder.parseAndValidate(json, bank, 5);

        assertThat(result.valid()).isTrue();
        assertThat(result.selectedIds()).containsExactly(1000L, 1001L, 1002L, 1003L, 1004L);
        assertThat(result.reasoningSummary()).isEqualTo("Balanced selection across topics.");
        assertThat(result.error()).isNull();
    }

    // ────────────────────────────────────────────────────────────
    // parseAndValidate: validation failures
    // ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("parseAndValidate with wrong count returns valid=false")
    void parseAndValidate_wrongCount_returnsInvalid() {
        List<PlannerCandidate> bank = buildBank(10);
        String json = """
                {
                  "selectedQuestionIds": [1000, 1001, 1002],
                  "reasoningSummary": "Short selection."
                }
                """;

        SelectionParseResult result = promptBuilder.parseAndValidate(json, bank, 5);

        assertThat(result.valid()).isFalse();
        assertThat(result.error()).contains("Expected 5 IDs but got 3");
    }

    @Test
    @DisplayName("parseAndValidate with unknown IDs returns valid=false")
    void parseAndValidate_unknownIds_returnsInvalid() {
        List<PlannerCandidate> bank = buildBank(10);
        String json = """
                {
                  "selectedQuestionIds": [1000, 1001, 9999, 1003, 1004],
                  "reasoningSummary": "Has unknown ID."
                }
                """;

        SelectionParseResult result = promptBuilder.parseAndValidate(json, bank, 5);

        assertThat(result.valid()).isFalse();
        assertThat(result.error()).contains("Unknown question ID 9999");
    }

    @Test
    @DisplayName("parseAndValidate with duplicate IDs returns valid=false")
    void parseAndValidate_duplicateIds_returnsInvalid() {
        List<PlannerCandidate> bank = buildBank(10);
        String json = """
                {
                  "selectedQuestionIds": [1000, 1001, 1001, 1003, 1004],
                  "reasoningSummary": "Has duplicate."
                }
                """;

        SelectionParseResult result = promptBuilder.parseAndValidate(json, bank, 5);

        assertThat(result.valid()).isFalse();
        assertThat(result.error()).contains("Duplicate IDs");
    }

    @Test
    @DisplayName("parseAndValidate with malformed JSON returns valid=false")
    void parseAndValidate_malformedJson_returnsInvalid() {
        List<PlannerCandidate> bank = buildBank(10);
        String json = "this is not valid json {{{";

        SelectionParseResult result = promptBuilder.parseAndValidate(json, bank, 5);

        assertThat(result.valid()).isFalse();
        assertThat(result.error()).contains("Failed to parse LLM JSON");
    }

    @Test
    @DisplayName("parseAndValidate with empty content returns valid=false")
    void parseAndValidate_emptyContent_returnsInvalid() {
        List<PlannerCandidate> bank = buildBank(10);

        SelectionParseResult result = promptBuilder.parseAndValidate("", bank, 5);

        assertThat(result.valid()).isFalse();
        assertThat(result.error()).contains("empty content");
    }

    @Test
    @DisplayName("parseAndValidate with null content returns valid=false")
    void parseAndValidate_nullContent_returnsInvalid() {
        List<PlannerCandidate> bank = buildBank(10);

        SelectionParseResult result = promptBuilder.parseAndValidate(null, bank, 5);

        assertThat(result.valid()).isFalse();
        assertThat(result.error()).contains("empty content");
    }

    @Test
    @DisplayName("parseAndValidate with missing selectedQuestionIds field returns valid=false")
    void parseAndValidate_missingField_returnsInvalid() {
        List<PlannerCandidate> bank = buildBank(10);
        String json = """
                {
                  "reasoningSummary": "No IDs field."
                }
                """;

        SelectionParseResult result = promptBuilder.parseAndValidate(json, bank, 5);

        assertThat(result.valid()).isFalse();
        assertThat(result.error()).contains("Missing or non-array");
    }

    // ────────────────────────────────────────────────────────────
    // Prompt building
    // ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("buildPart1Prompt includes bank data in user prompt and rules in system prompt")
    void buildPart1Prompt_containsBankData() {
        List<PlannerCandidate> bank = buildBank(5);

        PromptPair prompt = promptBuilder.buildPart1Prompt(bank, 3);

        // System prompt should contain rules
        assertThat(prompt.systemPrompt()).contains("Select exactly 3 questions");
        assertThat(prompt.systemPrompt()).contains("Do NOT generate or modify questions");
        assertThat(prompt.systemPrompt()).contains("No duplicate IDs");

        // User prompt should list all candidates
        assertThat(prompt.userPrompt()).contains("1000");
        assertThat(prompt.userPrompt()).contains("1004");
        assertThat(prompt.userPrompt()).contains("Work");
        assertThat(prompt.userPrompt()).contains("Study");
    }

    @Test
    @DisplayName("buildPart1Prompt handles null topicLabel candidate without NPE")
    void buildPart1Prompt_nullTopicLabel_noCrash() {
        List<PlannerCandidate> bank = new ArrayList<>();
        ObjectNode snapshot = objectMapper.createObjectNode();
        snapshot.put("schemaVersion", 1);
        snapshot.put("partType", "PART_1");
        snapshot.put("promptText", "Tell me about your hometown.");
        // topicLabel intentionally omitted
        bank.add(new PlannerCandidate(2000L, 1, snapshot));

        PromptPair prompt = promptBuilder.buildPart1Prompt(bank, 1);

        assertThat(prompt.userPrompt()).contains("2000");
        assertThat(prompt.userPrompt()).contains("(none)");
    }

    @Test
    @DisplayName("buildFollowUpPart3Prompt includes Part 2 context")
    void buildFollowUpPart3Prompt_includesPart2Context() {
        List<PlannerCandidate> bank = buildBank(5);
        ObjectNode part2Snapshot = objectMapper.createObjectNode();
        part2Snapshot.put("topicLabel", "Travel");
        part2Snapshot.put("promptText", "Describe a memorable trip.");

        PromptPair prompt = promptBuilder.buildFollowUpPart3Prompt(
                bank, 3, part2Snapshot, "I visited London and enjoyed the museums.");

        assertThat(prompt.userPrompt()).contains("Travel");
        assertThat(prompt.userPrompt()).contains("Describe a memorable trip.");
        assertThat(prompt.userPrompt()).contains("I visited London");
    }

    // ────────────────────────────────────────────────────────────
    // JSON Schema
    // ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getSelectionResponseSchema returns valid schema with required fields")
    @SuppressWarnings("unchecked")
    void getSelectionResponseSchema_hasRequiredFields() {
        Map<String, Object> schema = promptBuilder.getSelectionResponseSchema();

        assertThat(schema).containsKey("type");
        assertThat(schema.get("type")).isEqualTo("object");
        assertThat(schema).containsKey("properties");
        assertThat(schema).containsKey("required");

        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertThat(properties).containsKey("selectedQuestionIds");
        assertThat(properties).containsKey("reasoningSummary");

        List<String> required = (List<String>) schema.get("required");
        assertThat(required).contains("selectedQuestionIds", "reasoningSummary");

        Map<String, Object> idsSchema = (Map<String, Object>) properties.get("selectedQuestionIds");
        assertThat(idsSchema.get("type")).isEqualTo("array");

        Map<String, Object> reasoningSchema = (Map<String, Object>) properties.get("reasoningSummary");
        assertThat(reasoningSchema.get("type")).isEqualTo("string");
    }

    // ────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────

    private List<PlannerCandidate> buildBank(int count) {
        List<PlannerCandidate> bank = new ArrayList<>();
        String[] topics = {"Work", "Study", "Hobbies"};
        for (int i = 0; i < count; i++) {
            String topic = topics[i % topics.length];
            ObjectNode snapshot = objectMapper.createObjectNode();
            snapshot.put("schemaVersion", 1);
            snapshot.put("partType", "PART_1");
            snapshot.put("promptText", "Prompt " + (i + 1) + " about " + topic);
            snapshot.put("topicLabel", topic);
            bank.add(new PlannerCandidate(1000L + i, i + 1, snapshot));
        }
        return bank;
    }
}
