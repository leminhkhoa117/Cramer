package com.cramer.service.abts;

import com.cramer.dto.abts.GenerationRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

final class JsonListeningValidator {

    private static final Logger logger = LoggerFactory.getLogger(JsonListeningValidator.class);

    private static final Map<Integer, int[]> LISTENING_WORD_COUNTS = Map.of(
            1, new int[] { 850, 1050 },
            2, new int[] { 950, 1150 },
            3, new int[] { 1050, 1250 },
            4, new int[] { 1050, 1250 });

    private static final Set<String> LISTENING_QUESTION_TYPES = Set.of(
            "FILL_IN_BLANK",
            "MULTIPLE_CHOICE",
            "MULTIPLE_CHOICE_MULTIPLE_ANSWERS",
            "MATCHING");

    private static final Set<String> LISTENING_BLOCK_TYPES = Set.of(
            "NOTE_COMPLETION",
            "INSTRUCTIONS_ONLY",
            "MATCHING_FEATURES",
            "PLAN_MAP_DIAGRAM_LABELING");

    private static final Set<String> VALID_WORD_LIMITS = Set.of(
            "ONE WORD ONLY",
            "NO MORE THAN TWO WORDS",
            "NO MORE THAN THREE WORDS",
            "ONE WORD AND/OR A NUMBER",
            "NO MORE THAN TWO WORDS AND/OR A NUMBER",
            "NO MORE THAN THREE WORDS AND/OR A NUMBER");

    private final ObjectMapper objectMapper;

    JsonListeningValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    JsonValidatorService.ValidationResult validateListeningContent(String jsonContent, GenerationRequestDTO request) {
        JsonValidatorService.ValidationResult result = new JsonValidatorService.ValidationResult();

        try {
            JsonNode root = objectMapper.readTree(jsonContent);
            Integer part = request.getPartNumber() != null ? request.getPartNumber() : 1;

            validateListeningSchema(root, part, result);

            if (!result.isValid()) {
                return result;
            }

            validateListeningTranscript(root, part, result);
            validateListeningQuestions(root, request, result);
            validateListeningBusinessRules(root, part, request, result);

        } catch (Exception e) {
            String snippet = jsonContent.length() > 500 ? jsonContent.substring(0, 500) + "..." : jsonContent;
            logger.error("Failed to parse Listening JSON for validation: {}. Content snippet: {}", e.getMessage(),
                    snippet);
            result.addSchemaError("Invalid JSON: " + e.getMessage());
        }

        return result;
    }

    private void validateListeningSchema(JsonNode root, int part, JsonValidatorService.ValidationResult result) {
        if (!root.has("transcript")) {
            result.addSchemaError("Missing required field: transcript");
        }

        if (!root.has("questions")) {
            result.addSchemaError("Missing required field: questions");
        } else if (!root.get("questions").isArray()) {
            result.addSchemaError("Field 'questions' must be an array");
        } else {
            JsonNode questions = root.get("questions");
            for (int i = 0; i < questions.size(); i++) {
                JsonNode question = questions.get(i);
                validateListeningQuestionSchema(question, i + 1, result);
            }
        }

        if (!root.has("section_layout")) {
            result.addContentError("Missing required field: section_layout");
        } else {
            JsonNode blocks = getListeningLayoutBlocks(root);
            if (blocks == null) {
                result.addContentError("section_layout must be an object with blocks array");
            }
        }
    }

    private void validateListeningQuestionSchema(JsonNode question, int index,
            JsonValidatorService.ValidationResult result) {
        List<String> requiredFields = List.of("question_number", "question_type", "question_content", "correct_answer",
                "explanation");

        for (String field : requiredFields) {
            if (!question.has(field)) {
                result.addSchemaError(String.format("Question %d missing required field: %s", index, field));
            }
        }

        if (question.has("correct_answer") && !question.get("correct_answer").isArray()) {
            result.addSchemaError(String.format("Question %d: correct_answer must be an array", index));
        }
    }

    private void validateListeningTranscript(JsonNode root, int part, JsonValidatorService.ValidationResult result) {
        if (!root.has("transcript"))
            return;

        String transcript = root.get("transcript").asText();
        int wordCount = JsonValidationSupport.countWords(transcript);

        int[] expectedRange = LISTENING_WORD_COUNTS.getOrDefault(part, new int[] { 500, 700 });
        if (wordCount < expectedRange[0]) {
            result.addContentError(String.format(
                    "Transcript word count (%d) is below minimum (%d) for Part %d",
                    wordCount, expectedRange[0], part));
        } else if (wordCount > expectedRange[1]) {
            result.addContentError(String.format(
                    "Transcript word count (%d) exceeds maximum (%d) for Part %d",
                    wordCount, expectedRange[1], part));
        }

        validateSpeakerLabels(transcript, part, result);
    }

    private void validateSpeakerLabels(String transcript, int part, JsonValidatorService.ValidationResult result) {
        boolean hasLabels = transcript.matches("(?s).*[A-Z][A-Z\\s]+:.*");

        if (!hasLabels) {
            result.addContentError("Transcript missing speaker labels (e.g., SPEAKER:, AGENT:)");
            return;
        }

        Set<String> speakers = new HashSet<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([A-Z][A-Z\\s]+):");
        java.util.regex.Matcher matcher = pattern.matcher(transcript);
        while (matcher.find()) {
            speakers.add(matcher.group(1).trim());
        }

        int speakerCount = speakers.size();
        switch (part) {
            case 1:
                if (speakerCount != 2) {
                    result.addWarning(String.format(
                            "Part 1 should have 2 speakers, found %d: %s",
                            speakerCount, speakers));
                }
                break;
            case 2:
            case 4:
                if (speakerCount != 1) {
                    result.addWarning(String.format(
                            "Part %d (monologue) should have 1 speaker, found %d",
                            part, speakerCount));
                }
                break;
            case 3:
                if (speakerCount < 2 || speakerCount > 4) {
                    result.addWarning(String.format(
                            "Part 3 should have 2-4 speakers, found %d",
                            speakerCount));
                }
                break;
        }
    }

    private void validateListeningQuestions(JsonNode root, GenerationRequestDTO request,
            JsonValidatorService.ValidationResult result) {
        JsonNode questions = root.get("questions");
        if (questions == null || !questions.isArray())
            return;

        int questionCount = questions.size();

        if (questionCount != 10) {
            result.addContentError(String.format(
                    "Listening part should have exactly 10 questions, found %d",
                    questionCount));
        }

        String transcript = root.has("transcript") ? root.get("transcript").asText().toLowerCase() : "";

        Set<Integer> questionNumbers = new HashSet<>();
        for (JsonNode question : questions) {
            int num = question.has("question_number") ? question.get("question_number").asInt() : 0;

            if (!questionNumbers.add(num)) {
                result.addContentError("Duplicate question number: " + num);
            }

            String type = question.has("question_type") ? question.get("question_type").asText().toUpperCase() : "";
            if (!LISTENING_QUESTION_TYPES.contains(type)) {
                result.addContentError("Question " + num + " has invalid question_type: " + type);
            }

            if (question.has("correct_answer") && question.get("correct_answer").isArray()) {
                JsonNode answers = question.get("correct_answer");
                for (JsonNode answer : answers) {
                    String answerText = answer.asText().toLowerCase();
                    if (answerText.length() > 1 && !transcript.contains(answerText)) {
                        result.addWarning(String.format(
                                "Question %d answer '%s' may not appear in transcript",
                                num, answer.asText()));
                    }
                }
            }

            String explanation = JsonValidationSupport.extractExplanationText(question.path("explanation"));
            if (explanation.isEmpty() || explanation.length() < 20) {
                result.addWarning(String.format(
                        "Question %d has short explanation (length: %d)",
                        num, explanation.length()));
            }

            JsonNode qContent = question.get("question_content");
            String text = JsonValidationSupport.extractQuestionText(qContent);

            if ("FILL_IN_BLANK".equals(type)) {
                String wordLimit = JsonValidationSupport.extractWordLimit(question);
                if ((wordLimit == null || wordLimit.isBlank()) && (text != null && !text.isBlank())) {
                    result.addContentError(String.format(
                            "Question %d: FILL_IN_BLANK requires word_limit",
                            num));
                } else if (wordLimit != null && !wordLimit.isBlank()
                        && !VALID_WORD_LIMITS.contains(wordLimit.toUpperCase())) {
                    result.addWarning(String.format(
                            "Question %d: unexpected word_limit value '%s'",
                            num, wordLimit));
                }

                if (text != null && !text.isBlank()) {
                    if (!text.contains("____")) {
                        result.addContentError(String.format(
                                "Question %d FILL_IN_BLANK text missing blank placeholder",
                                num));
                    }

                    if (!JsonValidationSupport.hasInlineNumber(text)) {
                        result.addWarning(String.format(
                                "Question %d text may be missing inline question number",
                                num));
                    }
                }
                JsonValidationSupport.validateAnswerWordLimit(
                        num, JsonValidationSupport.extractAnswers(question), wordLimit, result);
            } else if ("MULTIPLE_CHOICE".equals(type)) {
                JsonValidationSupport.validateMultipleChoice(num, qContent, question, 3, result);
            } else if ("MULTIPLE_CHOICE_MULTIPLE_ANSWERS".equals(type)) {
                JsonValidationSupport.validateMultipleChoiceMultiple(num, qContent, question, 5, result);
            } else if ("MATCHING".equals(type)) {
                if (text == null || text.isBlank()) {
                    result.addContentError(String.format(
                            "Question %d MATCHING requires question_content.text",
                            num));
                }
                List<String> answers = JsonValidationSupport.extractAnswers(question);
                if (answers.size() != 1) {
                    result.addContentError(String.format(
                            "Question %d MATCHING should have exactly one correct answer",
                            num));
                }
            }

            if (request.getExplanationLanguage() == GenerationRequestDTO.ExplanationLanguage.VI) {
                if (!JsonValidationSupport.containsVietnamese(explanation) && !explanation.isEmpty()) {
                    result.addWarning(String.format(
                            "Question %d explanation may not be in Vietnamese", num));
                }
            }
        }
    }

    private void validateListeningBusinessRules(JsonNode root, int part, GenerationRequestDTO request,
            JsonValidatorService.ValidationResult result) {
        JsonNode questions = root.get("questions");
        Map<Integer, JsonNode> questionMap = new HashMap<>();
        Map<String, Integer> typeCounts = new HashMap<>();
        if (questions != null && questions.isArray()) {
            int expectedStart = (part - 1) * 10 + 1;
            int expectedEnd = part * 10;

            for (JsonNode question : questions) {
                int num = question.has("question_number") ? question.get("question_number").asInt() : 0;
                String type = question.path("question_type").asText("").toUpperCase();
                if (!type.isBlank()) {
                    typeCounts.merge(type, 1, (a, b) -> Objects.requireNonNull(a) + Objects.requireNonNull(b));
                }
                questionMap.put(num, question);
                if (num < expectedStart || num > expectedEnd) {
                    result.addWarning(String.format(
                            "Question number %d out of expected range for Part %d (Q%d-Q%d)",
                            num, part, expectedStart, expectedEnd));
                }
            }
        }

        if (request.getQuestionTypeCounts() != null && !request.getQuestionTypeCounts().isEmpty()) {
            request.getQuestionTypeCounts().forEach((type, expectedCount) -> {
                int actual = typeCounts.getOrDefault(type, 0);
                if (actual != expectedCount) {
                    result.addWarning(String.format(
                            "Question type count mismatch for %s: expected %d, got %d",
                            type, expectedCount, actual));
                }
            });
        }

        if (request.getQuestionTypes() != null && !request.getQuestionTypes().isEmpty()) {
            for (String type : request.getQuestionTypes()) {
                if (!typeCounts.containsKey(type)) {
                    result.addWarning("Requested question type missing: " + type);
                }
            }
        }

        if (root.has("section_layout")) {
            JsonNode blocks = getListeningLayoutBlocks(root);
            if (blocks != null && blocks.isArray() && blocks.isEmpty()) {
                result.addWarning("section_layout.blocks is empty");
            }
            if (blocks != null && blocks.isArray()) {
                Set<Integer> assignedNumbers = new HashSet<>();
                int blockIndex = 0;
                for (JsonNode block : blocks) {
                    if (!block.has("block_type")) {
                        result.addContentError("section_layout block " + blockIndex + " missing block_type");
                        blockIndex++;
                        continue;
                    }

                    String rawBlockType = block.get("block_type").asText();

                    if (rawBlockType.length() > 50) {
                        result.addContentError("Block " + blockIndex + " has malformed block_type (truncated): "
                                + rawBlockType.substring(0, 50) + "...");
                        blockIndex++;
                        continue;
                    }

                    String blockType = rawBlockType.toUpperCase().trim();
                    if (!LISTENING_BLOCK_TYPES.contains(blockType)) {
                        result.addContentError("Block " + blockIndex + " has invalid block_type: '" + blockType
                                + "'. Must be one of: " + LISTENING_BLOCK_TYPES);
                        blockIndex++;
                        continue;
                    }

                    JsonNode numberNode = block.get("question_numbers");
                    if (numberNode == null || !numberNode.isArray() || numberNode.isEmpty()) {
                        result.addContentError("Block " + blockIndex + " missing question_numbers array");
                        blockIndex++;
                        continue;
                    }

                    List<JsonNode> blockQuestions = new ArrayList<>();
                    for (JsonNode n : numberNode) {
                        int qNum = n.asInt();
                        if (!assignedNumbers.add(qNum)) {
                            result.addContentError("Duplicate question number in section_layout: " + qNum);
                        }
                        JsonNode q = questionMap.get(qNum);
                        if (q == null) {
                            result.addWarning("section_layout references missing question: " + qNum);
                        } else {
                            blockQuestions.add(q);
                        }
                    }

                    JsonNode content = block.get("content");
                    String instructions = content != null ? content.path("instructions_text").asText("") : "";

                    switch (blockType) {
                        case "NOTE_COMPLETION":
                            if (content == null || content.path("instructions_text").asText("").isBlank()) {
                                result.addWarning("NOTE_COMPLETION block should include instructions_text");
                            }
                            for (JsonNode q : blockQuestions) {
                                String qType = q.path("question_type").asText("").toUpperCase();
                                if (!"FILL_IN_BLANK".equals(qType)) {
                                    result.addContentError(String.format(
                                            "NOTE_COMPLETION block expects FILL_IN_BLANK (question %d)",
                                            q.path("question_number").asInt(0)));
                                }
                            }
                            break;
                        case "MATCHING_FEATURES":
                            if (content == null || !content.has("options") || !content.get("options").isArray()) {
                                result.addContentError("MATCHING_FEATURES block requires options array");
                            }
                            for (JsonNode q : blockQuestions) {
                                String qType = q.path("question_type").asText("").toUpperCase();
                                if (!"MATCHING".equals(qType)) {
                                    result.addContentError(String.format(
                                            "MATCHING_FEATURES block expects MATCHING (question %d)",
                                            q.path("question_number").asInt(0)));
                                }
                            }
                            break;
                        case "PLAN_MAP_DIAGRAM_LABELING":
                            if (content == null || content.path("image_url").asText("").isBlank()) {
                                result.addContentError("PLAN_MAP_DIAGRAM_LABELING block requires image_url");
                            }
                            if (content == null || !content.has("options") || !content.get("options").isArray()) {
                                result.addContentError("PLAN_MAP_DIAGRAM_LABELING block requires options array");
                            }
                            for (JsonNode q : blockQuestions) {
                                String qType = q.path("question_type").asText("").toUpperCase();
                                if (!"MATCHING".equals(qType)) {
                                    result.addContentError(String.format(
                                            "PLAN_MAP_DIAGRAM_LABELING block expects MATCHING (question %d)",
                                            q.path("question_number").asInt(0)));
                                }
                            }
                            break;
                        case "INSTRUCTIONS_ONLY":
                            if (content == null || instructions.isBlank()) {
                                result.addWarning("INSTRUCTIONS_ONLY block should include instructions_text");
                            }
                            for (JsonNode q : blockQuestions) {
                                String qType = q.path("question_type").asText("").toUpperCase();
                                if (!Set.of("MULTIPLE_CHOICE", "MULTIPLE_CHOICE_MULTIPLE_ANSWERS", "MATCHING")
                                        .contains(qType)) {
                                    result.addWarning(String.format(
                                            "INSTRUCTIONS_ONLY block has unexpected type %s (question %d)",
                                            qType, q.path("question_number").asInt(0)));
                                }
                            }
                            break;
                        default:
                            break;
                    }

                    if (!instructions.isBlank() && instructions.toLowerCase().contains("choose two")) {
                        Map<String, Integer> stemCounts = new HashMap<>();
                        for (JsonNode q : blockQuestions) {
                            if ("MULTIPLE_CHOICE_MULTIPLE_ANSWERS".equals(
                                    q.path("question_type").asText("").toUpperCase())) {
                                String stem = JsonValidationSupport.extractQuestionText(q.get("question_content")).trim();
                                stemCounts.merge(stem, 1,
                                        (a, b) -> Objects.requireNonNull(a) + Objects.requireNonNull(b));
                            }
                        }
                        for (Map.Entry<String, Integer> entry : stemCounts.entrySet()) {
                            if (entry.getValue() != 2) {
                                result.addWarning("Choose TWO block should duplicate each stem twice");
                                break;
                            }
                        }
                    }

                    blockIndex++;
                }
                if (questionMap.size() > 0 && assignedNumbers.size() != questionMap.size()) {
                    result.addWarning("Some questions are not assigned to any section_layout block");
                }
            }
        }

        if (part == 2) {
            boolean needsFigure = hasListeningMapOrPlan(root);
            if (needsFigure && !root.has("figure_description")) {
                result.addContentError("Part 2 map/plan questions require figure_description");
            } else if (root.has("figure_description")) {
                JsonNode figDesc = root.get("figure_description");
                if (!figDesc.has("title")) {
                    result.addWarning("figure_description should have a title");
                }
                if (!figDesc.has("elements") || !figDesc.get("elements").isArray()) {
                    result.addWarning("figure_description should have elements array for map locations");
                }
            }
        }

        if (!root.has("audio_placeholder")) {
            result.addContentError("audio_placeholder is required for Listening metadata");
        } else {
            JsonNode audio = root.get("audio_placeholder");
            if (!audio.has("duration_estimate")) {
                result.addWarning("audio_placeholder should include duration_estimate");
            }
            if (!audio.has("speaker_count")) {
                result.addWarning("audio_placeholder should include speaker_count");
            }
            if (!audio.has("accent_recommendation")) {
                result.addWarning("audio_placeholder should include accent_recommendation");
            }
            if (!audio.has("pacing_notes")) {
                result.addWarning("audio_placeholder should include pacing_notes");
            }
            if (!audio.has("background_ambient")) {
                result.addWarning("audio_placeholder should include background_ambient");
            }
        }
    }

    private JsonNode getListeningLayoutBlocks(JsonNode root) {
        JsonNode layout = root.get("section_layout");
        if (layout == null) {
            return null;
        }
        if (layout.isArray()) {
            return layout;
        }
        if (layout.isObject() && layout.has("blocks") && layout.get("blocks").isArray()) {
            return layout.get("blocks");
        }
        return null;
    }

    private boolean hasListeningMapOrPlan(JsonNode root) {
        JsonNode blocks = getListeningLayoutBlocks(root);
        if (blocks == null || !blocks.isArray()) {
            return false;
        }
        for (JsonNode block : blocks) {
            String blockType = block.path("block_type").asText("").toUpperCase();
            if ("PLAN_MAP_DIAGRAM_LABELING".equals(blockType)) {
                return true;
            }
        }
        return false;
    }
}