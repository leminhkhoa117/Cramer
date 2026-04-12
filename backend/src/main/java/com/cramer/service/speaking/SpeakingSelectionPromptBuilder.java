package com.cramer.service.speaking;

import com.cramer.service.SpeakingSelectionPlannerService.PlannerCandidate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Builds prompts for LLM-based Speaking question selection and validates
 * the LLM response against hard rules.
 *
 * <p>The LLM is instructed to select a subset of question IDs from an
 * authored bank. It is <strong>never</strong> allowed to generate new
 * questions. All output is validated before use; invalid responses trigger
 * a fallback to the heuristic planner.</p>
 *
 * @since 2026-04-05
 */
@Component
public class SpeakingSelectionPromptBuilder {

    private static final Logger log = LoggerFactory.getLogger(SpeakingSelectionPromptBuilder.class);

    private static final int PROMPT_TEXT_MAX_CHARS = 120;
    private static final int PART2_CONTEXT_MAX_CHARS = 500;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ────────────────────────────────────────────────────────────
    // Prompt building
    // ────────────────────────────────────────────────────────────

    /**
     * Build system + user prompts for Part 1 selection.
     */
    public PromptPair buildPart1Prompt(List<PlannerCandidate> bank, int targetTurnCount) {
        String system = buildSelectionSystemPrompt("IELTS Speaking Part 1", targetTurnCount);
        String user = buildBankUserPrompt(bank, targetTurnCount,
                "Select questions for Part 1. "
                + "Aim for 2-3 topic clusters to give coherence while keeping diversity.");
        return new PromptPair(system, user);
    }

    /**
     * Build system + user prompts for independent Part 3 selection.
     */
    public PromptPair buildIndependentPart3Prompt(List<PlannerCandidate> bank, int targetTurnCount) {
        String system = buildSelectionSystemPrompt("IELTS Speaking Part 3", targetTurnCount);
        String user = buildBankUserPrompt(bank, targetTurnCount,
                "Select questions for Part 3 (standalone). "
                + "Choose questions that form a coherent discussion cluster around related topics.");
        return new PromptPair(system, user);
    }

    /**
     * Build system + user prompts for Part 3 follow-up selection after Part 2.
     */
    public PromptPair buildFollowUpPart3Prompt(
            List<PlannerCandidate> bank,
            int targetTurnCount,
            JsonNode part2QuestionSnapshot,
            String part2TranscriptText) {
        String system = buildSelectionSystemPrompt("IELTS Speaking Part 3 (follow-up)", targetTurnCount);

        StringBuilder user = new StringBuilder();
        user.append("## Part 2 Context\n\n");
        user.append("The candidate answered a Part 2 cue card about:\n");
        user.append("Topic: ").append(textValue(part2QuestionSnapshot, "topicLabel")).append("\n");
        user.append("Prompt: ").append(truncate(textValue(part2QuestionSnapshot, "promptText"), PART2_CONTEXT_MAX_CHARS)).append("\n");

        if (part2TranscriptText != null && !part2TranscriptText.isBlank()) {
            user.append("\nCandidate's response (excerpt):\n");
            user.append(truncate(part2TranscriptText.trim(), PART2_CONTEXT_MAX_CHARS)).append("\n");
        }

        user.append("\n## Task\n\n");
        user.append("Select exactly ").append(targetTurnCount)
            .append(" follow-up questions that naturally extend the Part 2 discussion. ")
            .append("Questions should probe deeper into the themes from the cue card and the candidate's response.\n\n");

        user.append("## Available Questions\n\n");
        appendBankTable(user, bank);

        return new PromptPair(system, user.toString());
    }

    // ────────────────────────────────────────────────────────────
    // JSON Schema for structured output
    // ────────────────────────────────────────────────────────────

    /**
     * Returns the JSON Schema for the LLM selection response, suitable for
     * OpenRouter's {@code response_format.json_schema.schema} field.
     */
    public Map<String, Object> getSelectionResponseSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> idsProperty = new LinkedHashMap<>();
        idsProperty.put("type", "array");
        idsProperty.put("items", Map.of("type", "integer"));
        idsProperty.put("description", "Selected question IDs from the provided bank. Must be exactly targetTurnCount items, no duplicates.");
        properties.put("selectedQuestionIds", idsProperty);

        Map<String, Object> reasoningProperty = new LinkedHashMap<>();
        reasoningProperty.put("type", "string");
        reasoningProperty.put("description", "Brief explanation of why these questions were selected (1-2 sentences).");
        properties.put("reasoningSummary", reasoningProperty);

        schema.put("properties", properties);
        schema.put("required", List.of("selectedQuestionIds", "reasoningSummary"));
        schema.put("additionalProperties", false);

        return schema;
    }

    // ────────────────────────────────────────────────────────────
    // Response parsing and validation
    // ────────────────────────────────────────────────────────────

    /**
     * Parses the raw LLM response content and validates it against hard rules.
     *
     * @param llmContent       raw JSON string from the LLM
     * @param bank             the candidate bank that was sent to the LLM
     * @param targetTurnCount  the expected number of selected IDs
     * @return a {@link SelectionParseResult} indicating validity and parsed data
     */
    public SelectionParseResult parseAndValidate(
            String llmContent, List<PlannerCandidate> bank, int targetTurnCount) {

        if (llmContent == null || llmContent.isBlank()) {
            return SelectionParseResult.invalid("LLM returned empty content");
        }

        // Parse JSON
        JsonNode root;
        try {
            root = objectMapper.readTree(llmContent);
        } catch (Exception e) {
            return SelectionParseResult.invalid("Failed to parse LLM JSON: " + e.getMessage());
        }

        // Extract selectedQuestionIds
        JsonNode idsNode = root.get("selectedQuestionIds");
        if (idsNode == null || !idsNode.isArray()) {
            return SelectionParseResult.invalid("Missing or non-array 'selectedQuestionIds' field");
        }

        List<Long> selectedIds = new ArrayList<>();
        LinkedHashSet<Long> deduped = new LinkedHashSet<>();
        for (JsonNode idNode : idsNode) {
            if (!idNode.isNumber()) {
                return SelectionParseResult.invalid("Non-numeric value in selectedQuestionIds: " + idNode);
            }
            long id = idNode.longValue();
            selectedIds.add(id);
            deduped.add(id);
        }

        // Validation 1: correct count
        if (selectedIds.size() != targetTurnCount) {
            return SelectionParseResult.invalid(
                    "Expected " + targetTurnCount + " IDs but got " + selectedIds.size());
        }

        // Validation 2: no duplicates
        if (deduped.size() != selectedIds.size()) {
            return SelectionParseResult.invalid(
                    "Duplicate IDs found in selectedQuestionIds");
        }

        // Validation 3: all IDs exist in the bank
        Set<Long> validIds = bank.stream()
                .map(PlannerCandidate::sourceQuestionId)
                .collect(Collectors.toSet());
        for (Long id : selectedIds) {
            if (!validIds.contains(id)) {
                return SelectionParseResult.invalid(
                        "Unknown question ID " + id + " not in the provided bank");
            }
        }

        // Extract reasoning summary (optional, non-critical)
        String reasoning = "";
        JsonNode reasoningNode = root.get("reasoningSummary");
        if (reasoningNode != null && reasoningNode.isTextual()) {
            reasoning = reasoningNode.asText("");
        }

        return new SelectionParseResult(true, selectedIds, reasoning, null);
    }

    // ────────────────────────────────────────────────────────────
    // Private helpers
    // ────────────────────────────────────────────────────────────

    private String buildSelectionSystemPrompt(String partDescription, int targetTurnCount) {
        return "You are an IELTS Speaking test question selector.\n\n"
                + "## Rules\n"
                + "1. Select exactly " + targetTurnCount + " questions from the provided bank.\n"
                + "2. Return ONLY IDs that exist in the bank. Do NOT invent new IDs.\n"
                + "3. No duplicate IDs.\n"
                + "4. Do NOT generate or modify questions. You can ONLY select from what is given.\n"
                + "5. Aim for topic coherence while maintaining diversity.\n"
                + "6. For " + partDescription + ", prefer covering 2-3 related topic clusters rather than "
                + "being too scattered or too repetitive.\n\n"
                + "## Output Format\n"
                + "Return a JSON object with:\n"
                + "- \"selectedQuestionIds\": array of exactly " + targetTurnCount + " integer IDs\n"
                + "- \"reasoningSummary\": a brief 1-2 sentence explanation of your selection rationale";
    }

    private String buildBankUserPrompt(List<PlannerCandidate> bank, int targetTurnCount, String instruction) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Task\n\n");
        sb.append(instruction).append("\n");
        sb.append("Select exactly ").append(targetTurnCount).append(" questions.\n\n");
        sb.append("## Available Questions\n\n");
        appendBankTable(sb, bank);
        return sb.toString();
    }

    private void appendBankTable(StringBuilder sb, List<PlannerCandidate> bank) {
        sb.append("| ID | Topic | Prompt |\n");
        sb.append("|---|---|---|\n");
        for (PlannerCandidate candidate : bank) {
            sb.append("| ").append(candidate.sourceQuestionId());
            sb.append(" | ").append(textValue(candidate.questionSnapshot(), "topicLabel"));
            sb.append(" | ").append(truncate(textValue(candidate.questionSnapshot(), "promptText"), PROMPT_TEXT_MAX_CHARS));
            sb.append(" |\n");
        }
    }

    private String textValue(JsonNode node, String fieldName) {
        if (node == null || !node.hasNonNull(fieldName)) {
            return "(none)";
        }
        String value = node.get(fieldName).asText(null);
        return (value == null || value.isBlank()) ? "(none)" : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    // ────────────────────────────────────────────────────────────
    // Inner records
    // ────────────────────────────────────────────────────────────

    /**
     * A pair of system prompt and user prompt.
     */
    public record PromptPair(String systemPrompt, String userPrompt) {
    }

    /**
     * Result of parsing and validating the LLM selection response.
     */
    public record SelectionParseResult(
            boolean valid,
            List<Long> selectedIds,
            String reasoningSummary,
            String error) {

        public static SelectionParseResult invalid(String error) {
            return new SelectionParseResult(false, List.of(), "", error);
        }
    }
}
