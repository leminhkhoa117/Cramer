package com.cramer.service.abts;

import com.cramer.dto.abts.GeneratedContentDTO;
import com.cramer.dto.abts.GenerationRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for validating AI-generated content against JSON schemas and business
 * rules.
 * 
 * Performs multi-layer validation:
 * 1. JSON Schema validation (structure)
 * 2. Content validation (word counts, question counts)
 * 3. Business rule validation (IELTS compliance)
 * 
 * @since 2025-12-20 - ABTS v2.0
 */
@Service
public class JsonValidatorService {

    private static final Logger logger = LoggerFactory.getLogger(JsonValidatorService.class);

    private final ObjectMapper objectMapper;

    // Reading passage word count limits per difficulty (lowered for model
    // compatibility)
    // Note: AI models vary in output length; these are flexible targets, not strict
    // requirements
    private static final Map<String, int[]> READING_WORD_COUNTS = Map.of(
            "BEGINNER", new int[] { 400, 700 },
            "LOWER_INTERMEDIATE", new int[] { 450, 800 },
            "INTERMEDIATE", new int[] { 500, 900 },
            "UPPER_INTERMEDIATE", new int[] { 550, 950 },
            "ADVANCED", new int[] { 600, 1000 });

    // Listening transcript word counts per part
    private static final Map<Integer, int[]> LISTENING_WORD_COUNTS = Map.of(
            1, new int[] { 850, 1050 },
            2, new int[] { 950, 1150 },
            3, new int[] { 1050, 1250 },
            4, new int[] { 1050, 1250 });

    private static final Set<String> READING_QUESTION_TYPES = Set.of(
            "FILL_IN_BLANK",
            "SUMMARY_COMPLETION",
            "TRUE_FALSE_NOT_GIVEN",
            "YES_NO_NOT_GIVEN",
            "MATCHING_INFORMATION",
            "MATCHING_HEADINGS",
            "MATCHING_FEATURES",
            "MATCHING_SENTENCE_ENDINGS",
            "MULTIPLE_CHOICE",
            "MULTIPLE_CHOICE_MULTIPLE_ANSWERS",
            "SUMMARY_COMPLETION_OPTIONS",
            "DIAGRAM_LABEL_COMPLETION",
            "TABLE_COMPLETION",
            "FLOW_CHART_COMPLETION");

    private static final Set<String> READING_COMPLETION_TYPES = Set.of(
            "FILL_IN_BLANK",
            "SUMMARY_COMPLETION",
            "TABLE_COMPLETION",
            "FLOW_CHART_COMPLETION",
            "DIAGRAM_LABEL_COMPLETION");

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

    public JsonValidatorService() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Validation result container.
     */
    public static class ValidationResult {
        private boolean valid;
        private List<String> schemaErrors;
        private List<String> contentErrors;
        private List<String> businessRuleErrors;
        private List<String> warnings;

        public ValidationResult() {
            this.valid = true;
            this.schemaErrors = new ArrayList<>();
            this.contentErrors = new ArrayList<>();
            this.businessRuleErrors = new ArrayList<>();
            this.warnings = new ArrayList<>();
        }

        public void addSchemaError(String error) {
            schemaErrors.add(error);
            valid = false;
        }

        public void addContentError(String error) {
            contentErrors.add(error);
            valid = false;
        }

        public void addBusinessRuleError(String error) {
            businessRuleErrors.add(error);
            valid = false;
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getSchemaErrors() {
            return schemaErrors;
        }

        public List<String> getContentErrors() {
            return contentErrors;
        }

        public List<String> getBusinessRuleErrors() {
            return businessRuleErrors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public List<String> getAllErrors() {
            List<String> all = new ArrayList<>();
            all.addAll(schemaErrors);
            all.addAll(contentErrors);
            all.addAll(businessRuleErrors);
            return all;
        }
    }

    // ==================== READING VALIDATION ====================

    /**
     * Validate Reading content from AI response.
     */
    public ValidationResult validateReadingContent(String jsonContent, GenerationRequestDTO request) {
        ValidationResult result = new ValidationResult();

        try {
            JsonNode root = objectMapper.readTree(jsonContent);

            // 1. Schema validation - check required fields
            validateReadingSchema(root, result);

            if (!result.isValid()) {
                return result; // Stop if schema is invalid
            }

            // 2. Content validation - word counts, question counts
            validateReadingPassage(root, request, result);
            validateReadingQuestions(root, request, result);

            // 3. Business rule validation - IELTS compliance
            validateReadingBusinessRules(root, request, result);

        } catch (Exception e) {
            logger.error("Failed to parse JSON for validation: {}", e.getMessage());
            result.addSchemaError("Invalid JSON: " + e.getMessage());
        }

        return result;
    }

    /**
     * Validate Reading JSON schema structure.
     */
    private void validateReadingSchema(JsonNode root, ValidationResult result) {
        // Check for section object
        if (!root.has("section")) {
            result.addSchemaError("Missing required field: section");
        } else {
            JsonNode section = root.get("section");
            if (!section.has("passage_text")) {
                result.addSchemaError("Missing required field: section.passage_text");
            }
        }

        // Check for questions array
        if (!root.has("questions")) {
            result.addSchemaError("Missing required field: questions");
        } else if (!root.get("questions").isArray()) {
            result.addSchemaError("Field 'questions' must be an array");
        } else {
            JsonNode questions = root.get("questions");
            for (int i = 0; i < questions.size(); i++) {
                JsonNode question = questions.get(i);
                validateQuestionSchema(question, i + 1, result);
            }
        }
    }

    /**
     * Validate individual question schema.
     */
    private void validateQuestionSchema(JsonNode question, int index, ValidationResult result) {
        List<String> requiredFields = List.of("question_number", "question_type", "question_content", "correct_answer",
                "explanation");

        for (String field : requiredFields) {
            if (!question.has(field)) {
                result.addSchemaError(String.format("Question %d missing required field: %s", index, field));
            }
        }

        // Validate correct_answer is an array
        if (question.has("correct_answer") && !question.get("correct_answer").isArray()) {
            result.addSchemaError(String.format("Question %d: correct_answer must be an array", index));
        }
    }

    /**
     * Validate Reading passage content.
     */
    private void validateReadingPassage(JsonNode root, GenerationRequestDTO request, ValidationResult result) {
        JsonNode section = root.get("section");
        if (section == null)
            return;

        String passageText = section.has("passage_text") ? section.get("passage_text").asText() : "";

        // Count words
        int wordCount = countWords(passageText);

        // Get expected range based on part and preference (fallback to difficulty)
        String difficulty = request.getDifficulty().name();
        int[] expectedRange = READING_WORD_COUNTS.getOrDefault(difficulty, new int[] { 850, 1000 });
        if (request.getPartNumber() != null) {
            expectedRange = getReadingPartRange(request.getPartNumber(), request.getPassageLength());
        }

        if (wordCount < expectedRange[0]) {
            // Changed from error to warning - don't fail generation for word count
            result.addWarning(String.format(
                    "Passage word count (%d) is below recommended minimum (%d) for this Part/Difficulty.",
                    wordCount, expectedRange[0]));
        } else if (wordCount > expectedRange[1]) {
            result.addWarning(String.format(
                    "Passage word count (%d) exceeds recommended maximum (%d) for this Part/Difficulty.",
                    wordCount, expectedRange[1]));
        }

        // Check for paragraph labels
        if (!passageText.contains("<strong>A.") && !passageText.contains("<strong>A</strong>")) {
            result.addWarning("Passage may be missing paragraph labels (A, B, C, etc.)");
        }
    }

    /**
     * Validate Reading questions content.
     */
    private void validateReadingQuestions(JsonNode root, GenerationRequestDTO request, ValidationResult result) {
        JsonNode questions = root.get("questions");
        if (questions == null || !questions.isArray())
            return;

        int questionCount = questions.size();

        Integer expectedTotal = request.getTotalQuestions();
        if (expectedTotal != null && expectedTotal > 0) {
            if (questionCount != expectedTotal) {
                result.addContentError(String.format(
                        "Question count (%d) does not match expected total (%d).",
                        questionCount, expectedTotal));
            }
        } else {
            // Check question count
            if (questionCount < 13) {
                result.addContentError(String.format(
                        "Too few questions (%d). Expected 13-14 for a Reading passage.",
                        questionCount));
            } else if (questionCount > 14) {
                result.addWarning(String.format(
                        "More questions (%d) than typical. Standard is 13-14.",
                        questionCount));
            }
        }

        List<JsonNode> questionList = new ArrayList<>();
        questions.forEach(questionList::add);
        questionList.sort(Comparator.comparingInt(q -> q.path("question_number").asInt()));

        String passageText = "";
        if (root.has("section") && root.get("section").has("passage_text")) {
            passageText = root.get("section").get("passage_text").asText("");
        }
        String passageLower = passageText.toLowerCase();

        // Validate each question
        Set<Integer> questionNumbers = new HashSet<>();
        List<Integer> sortedNumbers = new ArrayList<>();
        for (JsonNode question : questionList) {
            int num = question.path("question_number").asInt(0);
            sortedNumbers.add(num);

            // Check for duplicate question numbers
            if (!questionNumbers.add(num)) {
                result.addContentError("Duplicate question number: " + num);
            }

            String qTypeRaw = question.path("question_type").asText("");
            String qType = qTypeRaw.toUpperCase();
            if (!READING_QUESTION_TYPES.contains(qType)) {
                result.addContentError(String.format(
                        "Question %d has invalid question_type: %s",
                        num, qTypeRaw));
            }

            // Check explanation length (warning only)
            String explanation = question.path("explanation").asText("");
            if (explanation.isEmpty() || explanation.length() < 20) {
                result.addWarning(String.format(
                        "Question %d has short explanation (length: %d)",
                        num, explanation.length()));
            }

            JsonNode qContent = question.get("question_content");
            String text = extractQuestionText(qContent);

            String wordLimit = extractWordLimit(question);
            if (READING_COMPLETION_TYPES.contains(qType)) {
                if (wordLimit == null || wordLimit.isBlank()) {
                    result.addContentError(String.format(
                            "Question %d: completion types require word_limit",
                            num));
                } else if (!VALID_WORD_LIMITS.contains(wordLimit.toUpperCase())) {
                    result.addWarning(String.format(
                            "Question %d: unexpected word_limit value '%s'",
                            num, wordLimit));
                }

                if (text == null || text.isBlank()) {
                    // Allow empty text for grouped completion types (Master Question Strategy)
                    // But FILL_IN_BLANK should usually have text unless it's strictly part of a
                    // group (rare in current prompts)
                    if ("FILL_IN_BLANK".equals(qType)) {
                        result.addContentError(String.format(
                                "Question %d: FILL_IN_BLANK text cannot be empty",
                                num));
                    }
                    // For others (SUMMARY, TABLE, etc.), empty text is VALID for Q2+
                } else {
                    // Text is present, must have placeholder
                    if (!text.contains("____")) {
                        result.addContentError(String.format(
                                "Question %d: completion questions must include ____ placeholder",
                                num));
                    } else if (!hasInlineNumber(text)) {
                        result.addWarning(String.format(
                                "Question %d: completion text may be missing inline question number",
                                num));
                    }
                }

                List<String> answers = extractAnswers(question);
                if (!answers.isEmpty()) {
                    validateAnswerWordLimit(num, answers, wordLimit, result);
                    if (!passageLower.isBlank()) {
                        for (String ans : answers) {
                            if (ans.length() > 1 && !passageLower.contains(ans.toLowerCase())) {
                                result.addWarning(String.format(
                                        "Question %d answer '%s' may not appear in passage",
                                        num, ans));
                            }
                        }
                    }
                }
            }

            switch (qType) {
                case "TRUE_FALSE_NOT_GIVEN":
                    validateSingleAnswerInSet(num, question, Set.of("TRUE", "FALSE", "NOT GIVEN"), result);
                    break;
                case "YES_NO_NOT_GIVEN":
                    validateSingleAnswerInSet(num, question, Set.of("YES", "NO", "NOT GIVEN"), result);
                    break;
                case "MULTIPLE_CHOICE":
                    validateMultipleChoice(num, qContent, question, 4, result);
                    break;
                case "MULTIPLE_CHOICE_MULTIPLE_ANSWERS":
                    validateMultipleChoiceMultiple(num, qContent, question, 5, result);
                    break;
                case "MATCHING_INFORMATION":
                    validateMatchingOptions(num, qContent, question, result);
                    break;
                case "MATCHING_HEADINGS":
                case "MATCHING_FEATURES":
                case "MATCHING_SENTENCE_ENDINGS":
                case "SUMMARY_COMPLETION_OPTIONS":
                    validateMatchingOptions(num, qContent, question, result);
                    break;
                case "DIAGRAM_LABEL_COMPLETION":
                    String imageUrl = question.path("image_url").asText("");
                    if (imageUrl.isBlank()) {
                        result.addWarning(String.format(
                                "Question %d: DIAGRAM_LABEL_COMPLETION should include image_url",
                                num));
                    }
                    break;
                default:
                    break;
            }
        }

        validateReadingGroups(questionList, result);
        validateReadingNumbering(sortedNumbers, request, result);
    }

    /**
     * Validate Reading business rules.
     */
    private void validateReadingBusinessRules(JsonNode root, GenerationRequestDTO request, ValidationResult result) {
        // Check question type distribution
        Map<String, Integer> typeCounts = new HashMap<>();
        JsonNode questions = root.get("questions");

        if (questions != null) {
            for (JsonNode question : questions) {
                String type = question.has("question_type") ? question.get("question_type").asText() : "UNKNOWN";
                typeCounts.merge(type, 1, Integer::sum);
            }
        }

        // Validate question type distribution
        int totalTypes = typeCounts.size();
        if (totalTypes < 2) {
            result.addBusinessRuleError("Reading passage should have at least 2 different question types");
        }

        // Validate requested type counts if provided
        if (request.getQuestionTypeCounts() != null && !request.getQuestionTypeCounts().isEmpty()) {
            request.getQuestionTypeCounts().forEach((type, expectedCount) -> {
                int actual = typeCounts.getOrDefault(type, 0);
                if (actual != expectedCount) {
                    result.addBusinessRuleError(String.format(
                            "Question type count mismatch for %s: expected %d, got %d",
                            type, expectedCount, actual));
                }
            });
        }

        // Validate requested types are present
        if (request.getQuestionTypes() != null && !request.getQuestionTypes().isEmpty()) {
            for (String type : request.getQuestionTypes()) {
                if (!typeCounts.containsKey(type)) {
                    result.addWarning("Requested question type missing: " + type);
                }
            }
        }

        // Check for required explanation language
        if (request.getExplanationLanguage() == GenerationRequestDTO.ExplanationLanguage.VI) {
            for (JsonNode question : questions) {
                String explanation = question.has("explanation") ? question.get("explanation").asText() : "";
                // Simple check for Vietnamese characters
                if (!containsVietnamese(explanation)) {
                    result.addWarning(String.format(
                            "Question %d explanation may not be in Vietnamese",
                            question.has("question_number") ? question.get("question_number").asInt() : 0));
                }
            }
        }
    }

    // ==================== LISTENING VALIDATION (Phase 3 Enhanced)
    // ====================

    /**
     * Validate Listening content from AI response.
     * Enhanced for Phase 3 with comprehensive checks.
     */
    public ValidationResult validateListeningContent(String jsonContent, GenerationRequestDTO request) {
        ValidationResult result = new ValidationResult();

        try {
            JsonNode root = objectMapper.readTree(jsonContent);
            Integer part = request.getPartNumber() != null ? request.getPartNumber() : 1;

            // 1. Schema validation
            validateListeningSchema(root, part, result);

            if (!result.isValid()) {
                return result; // Stop if schema is invalid
            }

            // 2. Content validation - transcript word count, speaker labels, question count
            validateListeningTranscript(root, part, result);
            validateListeningQuestions(root, request, result);

            // 3. Business rule validation
            validateListeningBusinessRules(root, part, request, result);

        } catch (Exception e) {
            logger.error("Failed to parse Listening JSON for validation: {}", e.getMessage());
            result.addSchemaError("Invalid JSON: " + e.getMessage());
        }

        return result;
    }

    /**
     * Validate Listening JSON schema structure.
     */
    private void validateListeningSchema(JsonNode root, int part, ValidationResult result) {
        // Check for transcript
        if (!root.has("transcript")) {
            result.addSchemaError("Missing required field: transcript");
        }

        // Check for questions array
        if (!root.has("questions")) {
            result.addSchemaError("Missing required field: questions");
        } else if (!root.get("questions").isArray()) {
            result.addSchemaError("Field 'questions' must be an array");
        } else {
            // Validate each question
            JsonNode questions = root.get("questions");
            for (int i = 0; i < questions.size(); i++) {
                JsonNode question = questions.get(i);
                validateListeningQuestionSchema(question, i + 1, result);
            }
        }

        // Check for section_layout (required for Listening rendering)
        if (!root.has("section_layout")) {
            result.addContentError("Missing required field: section_layout");
        } else {
            JsonNode blocks = getListeningLayoutBlocks(root);
            if (blocks == null) {
                result.addContentError("section_layout must be an object with blocks array");
            }
        }
    }

    /**
     * Validate Listening question schema.
     */
    private void validateListeningQuestionSchema(JsonNode question, int index, ValidationResult result) {
        List<String> requiredFields = List.of("question_number", "question_type", "question_content", "correct_answer",
                "explanation");

        for (String field : requiredFields) {
            if (!question.has(field)) {
                result.addSchemaError(String.format("Question %d missing required field: %s", index, field));
            }
        }

        // Validate correct_answer is an array
        if (question.has("correct_answer") && !question.get("correct_answer").isArray()) {
            result.addSchemaError(String.format("Question %d: correct_answer must be an array", index));
        }
    }

    /**
     * Validate Listening transcript content.
     */
    private void validateListeningTranscript(JsonNode root, int part, ValidationResult result) {
        if (!root.has("transcript"))
            return;

        String transcript = root.get("transcript").asText();
        int wordCount = countWords(transcript);

        // Check word count
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

        // Check for speaker labels
        validateSpeakerLabels(transcript, part, result);
    }

    /**
     * Validate speaker labels in transcript.
     */
    private void validateSpeakerLabels(String transcript, int part, ValidationResult result) {
        // Check for speaker label pattern (e.g., "SPEAKER:", "AGENT:", "RECEPTIONIST:")
        boolean hasLabels = transcript.matches("(?s).*[A-Z][A-Z\\s]+:.*");

        if (!hasLabels) {
            result.addContentError("Transcript missing speaker labels (e.g., SPEAKER:, AGENT:)");
            return;
        }

        // Count unique speakers
        Set<String> speakers = new HashSet<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([A-Z][A-Z\\s]+):");
        java.util.regex.Matcher matcher = pattern.matcher(transcript);
        while (matcher.find()) {
            speakers.add(matcher.group(1).trim());
        }

        // Validate speaker count based on part
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

    /**
     * Validate Listening questions content.
     */
    private void validateListeningQuestions(JsonNode root, GenerationRequestDTO request, ValidationResult result) {
        JsonNode questions = root.get("questions");
        if (questions == null || !questions.isArray())
            return;

        int questionCount = questions.size();

        // Check question count (each part has 10 questions)
        if (questionCount != 10) {
            result.addContentError(String.format(
                    "Listening part should have exactly 10 questions, found %d",
                    questionCount));
        }

        // Get transcript for answer verification
        String transcript = root.has("transcript") ? root.get("transcript").asText().toLowerCase() : "";

        // Validate each question
        Set<Integer> questionNumbers = new HashSet<>();
        for (JsonNode question : questions) {
            int num = question.has("question_number") ? question.get("question_number").asInt() : 0;

            // Check for duplicate question numbers
            if (!questionNumbers.add(num)) {
                result.addContentError("Duplicate question number: " + num);
            }

            // Validate question type enum
            String type = question.has("question_type") ? question.get("question_type").asText().toUpperCase() : "";
            if (!LISTENING_QUESTION_TYPES.contains(type)) {
                result.addContentError("Question " + num + " has invalid question_type: " + type);
            }

            // Check that answer appears in transcript (for completion questions)
            if (question.has("correct_answer") && question.get("correct_answer").isArray()) {
                JsonNode answers = question.get("correct_answer");
                for (JsonNode answer : answers) {
                    String answerText = answer.asText().toLowerCase();
                    // Skip if answer is a letter choice (A, B, C, etc.)
                    if (answerText.length() > 1 && !transcript.contains(answerText)) {
                        result.addWarning(String.format(
                                "Question %d answer '%s' may not appear in transcript",
                                num, answer.asText()));
                    }
                }
            }

            String explanation = question.path("explanation").asText("");
            if (explanation.isEmpty() || explanation.length() < 20) {
                result.addWarning(String.format(
                        "Question %d has short explanation (length: %d)",
                        num, explanation.length()));
            }

            JsonNode qContent = question.get("question_content");
            String text = extractQuestionText(qContent);

            if ("FILL_IN_BLANK".equals(type)) {
                String wordLimit = extractWordLimit(question);
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

                // Allow empty text for Master Question Strategy (sub-questions)
                if (text != null && !text.isBlank()) {
                    if (!text.contains("____")) {
                        result.addContentError(String.format(
                                "Question %d FILL_IN_BLANK text missing blank placeholder",
                                num));
                    }
                    // Note: countPlaceholders check removed or relaxed because Master Question
                    // might have multiple blanks

                    if (!hasInlineNumber(text)) {
                        // Only warn if we expect inline numbers (which we do for Master Q)
                        // But Master Q has multiple numbers, so hasInlineNumber might need to be smart?
                        // Assuming hasInlineNumber checks for AT LEAST one number.
                        result.addWarning(String.format(
                                "Question %d text may be missing inline question number",
                                num));
                    }
                }
                validateAnswerWordLimit(num, extractAnswers(question), wordLimit, result);
            } else if ("MULTIPLE_CHOICE".equals(type)) {
                validateMultipleChoice(num, qContent, question, 3, result);
            } else if ("MULTIPLE_CHOICE_MULTIPLE_ANSWERS".equals(type)) {
                validateMultipleChoiceMultiple(num, qContent, question, 5, result);
            } else if ("MATCHING".equals(type)) {
                if (text == null || text.isBlank()) {
                    result.addContentError(String.format(
                            "Question %d MATCHING requires question_content.text",
                            num));
                }
                List<String> answers = extractAnswers(question);
                if (answers.size() != 1) {
                    result.addContentError(String.format(
                            "Question %d MATCHING should have exactly one correct answer",
                            num));
                }
            }

            // Check explanation language
            if (request.getExplanationLanguage() == GenerationRequestDTO.ExplanationLanguage.VI) {
                if (!containsVietnamese(explanation) && !explanation.isEmpty()) {
                    result.addWarning(String.format(
                            "Question %d explanation may not be in Vietnamese", num));
                }
            }
        }
    }

    /**
     * Validate Listening business rules.
     */
    private void validateListeningBusinessRules(JsonNode root, int part, GenerationRequestDTO request,
            ValidationResult result) {
        // Check question number range based on part
        JsonNode questions = root.get("questions");
        Map<Integer, JsonNode> questionMap = new HashMap<>();
        if (questions != null && questions.isArray()) {
            int expectedStart = (part - 1) * 10 + 1;
            int expectedEnd = part * 10;

            for (JsonNode question : questions) {
                int num = question.has("question_number") ? question.get("question_number").asInt() : 0;
                questionMap.put(num, question);
                if (num < expectedStart || num > expectedEnd) {
                    result.addWarning(String.format(
                            "Question number %d out of expected range for Part %d (Q%d-Q%d)",
                            num, part, expectedStart, expectedEnd));
                }
            }
        }

        // Check section_layout if present
        if (root.has("section_layout")) {
            JsonNode blocks = getListeningLayoutBlocks(root);
            if (blocks != null && blocks.isArray() && blocks.isEmpty()) {
                result.addWarning("section_layout.blocks is empty");
            }
            if (blocks != null && blocks.isArray()) {
                Set<Integer> assignedNumbers = new HashSet<>();
                for (JsonNode block : blocks) {
                    if (!block.has("block_type")) {
                        result.addWarning("section_layout block missing block_type");
                        continue;
                    }
                    String blockType = block.get("block_type").asText().toUpperCase();
                    if (!LISTENING_BLOCK_TYPES.contains(blockType)) {
                        result.addWarning("Unknown block_type: " + blockType);
                    }

                    JsonNode numberNode = block.get("question_numbers");
                    if (numberNode == null || !numberNode.isArray() || numberNode.isEmpty()) {
                        result.addContentError("section_layout block missing question_numbers array");
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
                                String stem = extractQuestionText(q.get("question_content")).trim();
                                stemCounts.merge(stem, 1, Integer::sum);
                            }
                        }
                        for (Map.Entry<String, Integer> entry : stemCounts.entrySet()) {
                            if (entry.getValue() != 2) {
                                result.addWarning("Choose TWO block should duplicate each stem twice");
                                break;
                            }
                        }
                    }
                }

                if (questionMap.size() > 0 && assignedNumbers.size() != questionMap.size()) {
                    result.addWarning("Some questions are not assigned to any section_layout block");
                }
            }
        }

        // For Part 2, require figure_description for map/plan labelling
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

        // Check for audio_placeholder
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

    /**
     * Extract Listening layout blocks from section_layout (object or array).
     */
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

    /**
     * Detect whether Listening content includes map/plan/diagram labeling
     * questions.
     */
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

    // ==================== WRITING VALIDATION ====================

    /**
     * Validate Writing content from AI response.
     * Enhanced for Phase 4 with Task 1 and Task 2 specific validation.
     */
    public ValidationResult validateWritingContent(String jsonContent, GenerationRequestDTO request) {
        ValidationResult result = new ValidationResult();

        try {
            JsonNode root = objectMapper.readTree(jsonContent);

            // Check for task prompt
            if (!root.has("task_prompt")) {
                result.addSchemaError("Missing required field: task_prompt");
                return result;
            }

            // Check for task_type
            if (!root.has("task_type")) {
                result.addWarning("Missing task_type field");
            }

            if (!root.has("word_requirement")) {
                result.addSchemaError("Missing required field: word_requirement");
                return result;
            }

            // Determine task number
            Integer part = request.getPartNumber() != null ? request.getPartNumber() : 1;
            String testType = request.getTestType() != null ? request.getTestType().name() : "ACADEMIC";

            if (part == 1) {
                // Task 1 validation
                if ("GENERAL_TRAINING".equals(testType)) {
                    // GT Writing Task 1 = Letter
                    validateLetterContent(root, result);
                } else {
                    // Academic Writing Task 1 = Chart/Graph
                    if (!root.has("chart_data")) {
                        result.addSchemaError("Academic Task 1 requires chart_data");
                    } else if (root.has("chart_data")) {
                        validateChartData(root.get("chart_data"), result);
                        JsonNode chartData = root.get("chart_data");
                        if (chartData.has("chart_type")) {
                            String chartType = chartData.get("chart_type").asText();
                            if (("process".equals(chartType) || "map".equals(chartType))
                                    && !root.has("figure_description")) {
                                result.addContentError("Process/Map chart requires figure_description");
                            }
                        }
                    }
                }
            } else {
                // Task 2 = Essay
                validateEssayContent(root, request, result);
            }

            // Validate sample_answer if present
            if (root.has("sample_answer")) {
                JsonNode sample = root.get("sample_answer");
                if (!sample.has("content") || sample.get("content").asText().isEmpty()) {
                    result.addWarning("sample_answer is present but has no content");
                } else {
                    int wordCount = countWords(sample.get("content").asText());
                    int minWords = (part == 1) ? 150 : 250;
                    if (wordCount < minWords) {
                        result.addWarning(String.format(
                                "sample_answer word count (%d) below minimum (%d)", wordCount, minWords));
                    }
                }
            }

        } catch (Exception e) {
            result.addSchemaError("Invalid JSON: " + e.getMessage());
        }

        return result;
    }

    /**
     * Validate chart data structure.
     * Enhanced for Phase 4 with comprehensive validation.
     */
    private void validateChartData(JsonNode chartData, ValidationResult result) {
        // Required fields for all chart types
        List<String> requiredFields = List.of("chart_type", "title");

        for (String field : requiredFields) {
            if (!chartData.has(field)) {
                result.addContentError("Chart data missing required field: " + field);
            }
        }

        // Get chart type
        String chartType = chartData.has("chart_type") ? chartData.get("chart_type").asText() : "";

        // Validate based on chart type
        if (chartType.isEmpty()) {
            result.addContentError("chart_type is required");
            return;
        }

        // Validate chart types that need axes and series
        if (chartType.contains("bar") || chartType.contains("line")) {
            validateAxisChartData(chartData, result);
        } else if (chartType.equals("pie_standard")) {
            validatePieChartData(chartData, result);
        } else if (chartType.equals("table")) {
            validateTableData(chartData, result);
        } else if (chartType.equals("process") || chartType.equals("map")) {
            // These need figure_description instead
            if (!chartData.has("elements") && !chartData.has("figure_description")) {
                result.addWarning("Process/Map charts should have elements or figure_description");
            }
        }

        // Check for data source
        if (!chartData.has("source")) {
            result.addWarning("Chart data should include source attribution");
        }
    }

    /**
     * Validate axis-based chart data (bar, line).
     */
    private void validateAxisChartData(JsonNode chartData, ValidationResult result) {
        // Check for required axis objects
        if (!chartData.has("x_axis")) {
            result.addContentError("Bar/Line chart requires x_axis");
        } else {
            JsonNode xAxis = chartData.get("x_axis");
            if (!xAxis.has("values") || !xAxis.get("values").isArray()) {
                result.addContentError("x_axis must have values array");
            } else {
                int xValues = xAxis.get("values").size();
                if (xValues < 3) {
                    result.addWarning("x_axis should have at least 3 data points for meaningful comparison");
                }
            }
        }

        if (!chartData.has("y_axis")) {
            result.addContentError("Bar/Line chart requires y_axis");
        } else {
            JsonNode yAxis = chartData.get("y_axis");
            if (!yAxis.has("label")) {
                result.addWarning("y_axis should have a label");
            }
        }

        // Check series data
        if (!chartData.has("series") || !chartData.get("series").isArray()) {
            result.addContentError("Chart requires series array with data");
        } else {
            JsonNode series = chartData.get("series");
            if (series.isEmpty()) {
                result.addContentError("series array is empty");
            } else {
                validateSeriesData(series, chartData, result);
            }
        }
    }

    /**
     * Validate series data consistency.
     */
    private void validateSeriesData(JsonNode series, JsonNode chartData, ValidationResult result) {
        int expectedLength = 0;
        if (chartData.has("x_axis") && chartData.get("x_axis").has("values")) {
            expectedLength = chartData.get("x_axis").get("values").size();
        }

        for (int i = 0; i < series.size(); i++) {
            JsonNode s = series.get(i);

            if (!s.has("name")) {
                result.addContentError("Series " + i + " missing name");
            }

            if (!s.has("values") || !s.get("values").isArray()) {
                result.addContentError("Series " + i + " missing values array");
            } else if (expectedLength > 0 && s.get("values").size() != expectedLength) {
                result.addContentError("Series " + i + " has " + s.get("values").size() +
                        " values but x_axis has " + expectedLength + " values");
            }

            if (!s.has("color")) {
                result.addWarning("Series " + i + " should have a color for visualization");
            }
        }
    }

    /**
     * Validate pie chart data.
     */
    private void validatePieChartData(JsonNode chartData, ValidationResult result) {
        if (!chartData.has("series") || !chartData.get("series").isArray()) {
            result.addContentError("Pie chart requires series array");
            return;
        }

        JsonNode series = chartData.get("series");
        if (series.isEmpty()) {
            result.addContentError("Pie chart series is empty");
            return;
        }

        // Check if values sum approximately to 100
        double total = 0;
        for (JsonNode category : series) {
            if (category.has("values") && category.get("values").isArray() && !category.get("values").isEmpty()) {
                total += category.get("values").get(0).asDouble();
            } else if (category.has("value")) {
                total += category.get("value").asDouble();
            }
        }

        if (total > 0 && (total < 95 || total > 105)) {
            result.addWarning("Pie chart values sum to " + total + "%, expected approximately 100%");
        }
    }

    /**
     * Validate table data.
     */
    private void validateTableData(JsonNode chartData, ValidationResult result) {
        if (!chartData.has("rows") && !chartData.has("data")) {
            result.addContentError("Table chart requires rows or data field");
        }

        if (!chartData.has("headers") && !chartData.has("columns")) {
            result.addWarning("Table should have headers or columns defined");
        }
    }

    /**
     * Validate Task 2 essay content.
     */
    private void validateEssayContent(JsonNode root, GenerationRequestDTO request, ValidationResult result) {
        // Check task prompt exists and has content
        if (!root.has("task_prompt")) {
            result.addSchemaError("Missing task_prompt for essay question");
            return;
        }

        String taskPrompt = root.get("task_prompt").asText();

        // Check minimum length
        if (taskPrompt.length() < 50) {
            result.addContentError("Essay prompt too short (minimum 50 characters)");
        }

        // Check for essay type indicators
        boolean hasInstruction = false;
        String[] instructionPatterns = {
                "agree or disagree",
                "discuss both views",
                "advantages and disadvantages",
                "problems and solutions",
                "what are the",
                "give your opinion",
                "to what extent"
        };

        String lowerPrompt = taskPrompt.toLowerCase();
        for (String pattern : instructionPatterns) {
            if (lowerPrompt.contains(pattern)) {
                hasInstruction = true;
                break;
            }
        }

        if (!hasInstruction) {
            result.addWarning("Essay prompt may be missing clear task instruction (agree/disagree, discuss, etc.)");
        }

        // Check for word requirement
        if (!lowerPrompt.contains("250") && !lowerPrompt.contains("word")) {
            result.addWarning("Essay prompt should mention minimum word count (250 words)");
        }

        // Essay metadata is required for Task 2
        if (!root.has("essay_metadata")) {
            result.addContentError("Task 2 requires essay_metadata");
            return;
        }

        JsonNode meta = root.get("essay_metadata");
        List<String> validEssayTypes = List.of(
                "opinion", "discussion", "advantages_disadvantages",
                "problem_solution", "two_part");

        if (meta.has("essay_type")) {
            String essayType = meta.get("essay_type").asText().toLowerCase();
            if (!validEssayTypes.contains(essayType)) {
                result.addWarning("Unknown essay_type: " + essayType);
            }
        }
    }

    /**
     * Validate General Training letter content.
     */
    private void validateLetterContent(JsonNode root, ValidationResult result) {
        if (!root.has("letter_context")) {
            result.addContentError("GT Task 1 requires letter_context");
            return;
        }

        JsonNode ctx = root.get("letter_context");

        if (!ctx.has("recipient")) {
            result.addContentError("letter_context should specify recipient");
        }

        if (!ctx.has("relationship")) {
            result.addWarning("letter_context should specify relationship (formal/informal/semi-formal)");
        } else {
            String relationship = ctx.get("relationship").asText().toLowerCase();
            if (!relationship.equals("formal") && !relationship.equals("informal")
                    && !relationship.equals("semi-formal")) {
                result.addWarning("letter_context relationship should be formal, informal, or semi-formal");
            }
        }

        if (!ctx.has("purpose")) {
            result.addWarning("letter_context should specify purpose");
        }
    }

    // ==================== UTILITY METHODS ====================

    private void validateReadingGroups(List<JsonNode> questions, ValidationResult result) {
        if (questions == null || questions.isEmpty()) {
            return;
        }

        int index = 0;
        while (index < questions.size()) {
            String type = questions.get(index).path("question_type").asText("").toUpperCase();
            int start = index;
            while (index < questions.size()
                    && type.equals(questions.get(index).path("question_type").asText("").toUpperCase())) {
                index++;
            }
            List<JsonNode> group = questions.subList(start, index);

            switch (type) {
                case "MATCHING_INFORMATION":
                case "MATCHING_HEADINGS":
                case "MATCHING_FEATURES":
                case "MATCHING_SENTENCE_ENDINGS":
                case "SUMMARY_COMPLETION_OPTIONS":
                    validateOptionsConsistency(group, type, result);
                    if ("SUMMARY_COMPLETION_OPTIONS".equals(type)) {
                        for (JsonNode question : group) {
                            int num = question.path("question_number").asInt(0);
                            String text = extractQuestionText(question.get("question_content"));
                            if (countPlaceholders(text) != 1) {
                                result.addWarning(String.format(
                                        "Question %d: SUMMARY_COMPLETION_OPTIONS should contain exactly one blank",
                                        num));
                            }
                        }
                    }
                    break;
                case "TABLE_COMPLETION":
                case "FLOW_CHART_COMPLETION":
                    validateFirstOnlyHasText(group, type, result);
                    break;
                default:
                    break;
            }
        }
    }

    private void validateOptionsConsistency(List<JsonNode> group, String type, ValidationResult result) {
        if (group == null || group.isEmpty()) {
            return;
        }

        int expectedOptionCount = -1;
        boolean firstOptionIsObject = false; // Track if options are {letter, text} objects
        int startNum = group.get(0).path("question_number").asInt(0);

        for (JsonNode question : group) {
            JsonNode options = question.path("question_content").path("options");
            if (options.isMissingNode() || !options.isArray() || options.isEmpty()) {
                result.addContentError(String.format(
                        "Question %d: %s requires non-empty options array",
                        question.path("question_number").asInt(0), type));
                continue;
            }

            int currentCount = options.size();
            boolean currentIsObject = options.get(0).isObject();

            if (expectedOptionCount == -1) {
                // First question - establish baseline
                expectedOptionCount = currentCount;
                firstOptionIsObject = currentIsObject;
            } else {
                // Subsequent questions - check consistency
                if (currentCount != expectedOptionCount) {
                    result.addContentError(String.format(
                            "%s group starting at question %d has inconsistent option counts (%d vs %d)",
                            type, startNum, expectedOptionCount, currentCount));
                    break;
                }
                if (currentIsObject != firstOptionIsObject) {
                    result.addContentError(String.format(
                            "%s group starting at question %d has inconsistent option format",
                            type, startNum));
                    break;
                }
            }
        }
    }

    private void validateFirstOnlyHasText(List<JsonNode> group, String type, ValidationResult result) {
        if (group == null || group.isEmpty()) {
            return;
        }
        JsonNode first = group.get(0);
        String firstText = extractQuestionText(first.get("question_content"));
        if (firstText == null || firstText.isBlank()) {
            result.addContentError(String.format(
                    "%s: first question in the group must contain the HTML content",
                    type));
        }
        for (int i = 1; i < group.size(); i++) {
            JsonNode q = group.get(i);
            String text = extractQuestionText(q.get("question_content"));
            if (text != null && !text.isBlank()) {
                result.addContentError(String.format(
                        "%s: only the first question should contain the shared content (question %d has text)",
                        type, q.path("question_number").asInt(0)));
            }
        }
    }

    private void validateReadingNumbering(List<Integer> numbers, GenerationRequestDTO request,
            ValidationResult result) {
        if (numbers == null || numbers.isEmpty()) {
            return;
        }
        for (int i = 1; i < numbers.size(); i++) {
            int prev = numbers.get(i - 1);
            int current = numbers.get(i);
            if (current != prev + 1) {
                result.addContentError(String.format(
                        "Question numbers should be sequential; found gap between %d and %d",
                        prev, current));
                break;
            }
        }

        if (request.getPartNumber() != null) {
            int expectedStart = (request.getPartNumber() - 1) * 13 + 1;
            if (numbers.get(0) != expectedStart) {
                result.addWarning(String.format(
                        "Question numbering for Part %d should start at %d (found %d)",
                        request.getPartNumber(), expectedStart, numbers.get(0)));
            }
        }
    }

    private void validateSingleAnswerInSet(int num, JsonNode question, Set<String> allowed,
            ValidationResult result) {
        List<String> answers = extractAnswers(question);
        if (answers.size() != 1) {
            result.addContentError(String.format(
                    "Question %d should have exactly one correct_answer value",
                    num));
            return;
        }
        String value = answers.get(0).toUpperCase();
        if (!allowed.contains(value)) {
            result.addContentError(String.format(
                    "Question %d has invalid correct_answer '%s'",
                    num, answers.get(0)));
        }
    }

    private void validateMultipleChoice(int num, JsonNode qContent, JsonNode question, int expectedOptions,
            ValidationResult result) {
        JsonNode options = qContent != null ? qContent.get("options") : null;
        if (options == null || !options.isArray()) {
            result.addContentError(String.format(
                    "Question %d: MULTIPLE_CHOICE requires options array",
                    num));
            return;
        }
        if (options.size() != expectedOptions) {
            result.addWarning(String.format(
                    "Question %d: expected %d options, found %d",
                    num, expectedOptions, options.size()));
        }
        Set<String> optionLetters = extractOptionLetters(options);
        List<String> answers = extractAnswers(question);
        if (answers.size() != 1) {
            result.addContentError(String.format(
                    "Question %d: MULTIPLE_CHOICE should have exactly one correct answer",
                    num));
            return;
        }
        if (!optionLetters.contains(answers.get(0).toUpperCase())) {
            result.addContentError(String.format(
                    "Question %d: correct_answer '%s' not found in options",
                    num, answers.get(0)));
        }
    }

    private void validateMultipleChoiceMultiple(int num, JsonNode qContent, JsonNode question, int expectedOptions,
            ValidationResult result) {
        JsonNode options = qContent != null ? qContent.get("options") : null;
        if (options == null || !options.isArray()) {
            result.addContentError(String.format(
                    "Question %d: MULTIPLE_CHOICE_MULTIPLE_ANSWERS requires options array",
                    num));
            return;
        }
        if (options.size() < 4) {
            result.addWarning(String.format(
                    "Question %d: expected at least 4 options, found %d",
                    num, options.size()));
        }
        List<String> answers = extractAnswers(question);
        if (answers.size() != 2) {
            result.addContentError(String.format(
                    "Question %d: MULTIPLE_CHOICE_MULTIPLE_ANSWERS should have TWO correct answers",
                    num));
            return;
        }
        Set<String> optionLetters = extractOptionLetters(options);
        for (String ans : answers) {
            if (!optionLetters.contains(ans.toUpperCase())) {
                result.addContentError(String.format(
                        "Question %d: correct_answer '%s' not found in options",
                        num, ans));
            }
        }
    }

    private void validateMatchingOptions(int num, JsonNode qContent, JsonNode question, ValidationResult result) {
        JsonNode options = qContent != null ? qContent.get("options") : null;
        if (options == null || !options.isArray() || options.isEmpty()) {
            result.addContentError(String.format(
                    "Question %d: matching types require options array",
                    num));
            return;
        }
        Set<String> optionLetters = extractOptionLetters(options);
        List<String> answers = extractAnswers(question);
        if (answers.size() != 1) {
            result.addContentError(String.format(
                    "Question %d: matching types should have exactly one correct answer",
                    num));
            return;
        }
        if (!optionLetters.contains(answers.get(0).toUpperCase())
                && !optionLetters.contains(answers.get(0))) {
            result.addContentError(String.format(
                    "Question %d: correct_answer '%s' not found in options",
                    num, answers.get(0)));
        }
    }

    private String extractQuestionText(JsonNode qContent) {
        if (qContent == null || qContent.isMissingNode() || qContent.isNull()) {
            return "";
        }
        if (qContent.has("text")) {
            return qContent.get("text").asText("");
        }
        if (qContent.has("statement")) {
            return qContent.get("statement").asText("");
        }
        if (qContent.has("question")) {
            return qContent.get("question").asText("");
        }
        if (qContent.has("sentence")) {
            return qContent.get("sentence").asText("");
        }
        if (qContent.has("prompt")) {
            return qContent.get("prompt").asText("");
        }
        if (qContent.has("item")) {
            return qContent.get("item").asText("");
        }
        if (qContent.has("paragraph")) {
            return qContent.get("paragraph").asText("");
        }
        if (qContent.has("heading")) {
            return qContent.get("heading").asText("");
        }
        return "";
    }

    private List<String> extractAnswers(JsonNode question) {
        List<String> answers = new ArrayList<>();
        JsonNode correct = question.get("correct_answer");
        if (correct != null && correct.isArray()) {
            for (JsonNode ans : correct) {
                if (!ans.isNull()) {
                    answers.add(ans.asText());
                }
            }
        }
        return answers;
    }

    private String extractWordLimit(JsonNode question) {
        if (question.has("word_limit") && !question.get("word_limit").isNull()) {
            return question.get("word_limit").asText();
        }
        return null;
    }

    private boolean hasInlineNumber(String text) {
        if (text == null) {
            return false;
        }
        return text.matches(".*\\b\\d+\\b.*");
    }

    private int countPlaceholders(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = text.indexOf("____", index)) != -1) {
            count++;
            index += 4;
        }
        return count;
    }

    private void validateAnswerWordLimit(int num, List<String> answers, String wordLimit, ValidationResult result) {
        if (wordLimit == null || wordLimit.isBlank()) {
            return;
        }
        Integer maxWords = deriveMaxWords(wordLimit);
        if (maxWords == null) {
            return;
        }
        boolean allowNumber = wordLimit.toUpperCase().contains("NUMBER");

        for (String answer : answers) {
            if (answer == null || answer.isBlank()) {
                continue;
            }
            int tokenCount = countAnswerTokens(answer, allowNumber);
            if (tokenCount > maxWords) {
                result.addContentError(String.format(
                        "Question %d answer '%s' exceeds word_limit (%s)",
                        num, answer, wordLimit));
            }
        }
    }

    private Integer deriveMaxWords(String wordLimit) {
        if (wordLimit == null) {
            return null;
        }
        String upper = wordLimit.toUpperCase();
        if (upper.contains("ONE WORD")) {
            return 1;
        }
        if (upper.contains("TWO WORDS")) {
            return 2;
        }
        if (upper.contains("THREE WORDS")) {
            return 3;
        }
        return null;
    }

    private int countAnswerTokens(String answer, boolean allowNumber) {
        String normalized = answer == null ? "" : answer.trim();
        if (allowNumber) {
            for (int i = 0; i < 3; i++) {
                normalized = normalized.replaceAll("(\\d)\\s+(\\d)", "$1$2");
            }
        }
        normalized = normalized.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return 0;
        }
        return normalized.split("\\s+").length;
    }

    private int[] getReadingPartRange(int partNumber, String passageLength) {
        int min;
        int max;
        // Updated based on User Feedback: 850-900, 900-950, etc.
        switch (partNumber) {
            case 1:
                min = 850;
                max = 1000;
                break;
            case 2:
                min = 950;
                max = 1100;
                break;
            case 3:
                min = 1050;
                max = 1250;
                break;
            default:
                min = 850;
                max = 1250;
                break;
        }

        if (passageLength != null) {
            switch (passageLength.toUpperCase()) {
                case "SHORT":
                    // Reduce max, but keep range valid
                    max = Math.max(min + 150, max - 100);
                    break;
                case "MEDIUM":
                    // Standard range, slightly tighter
                    min = min + 25;
                    max = max - 25;
                    break;
                case "LONG":
                    // Increase max, ensure min is higher
                    min = Math.min(min + 100, max - 50);
                    max = max + 100;
                    break;
            }
        }
        return new int[] { min, max };
    }

    private Set<String> extractOptionLetters(JsonNode options) {
        Set<String> letters = new HashSet<>();
        if (options == null || !options.isArray()) {
            return letters;
        }
        for (JsonNode opt : options) {
            if (opt.isTextual()) {
                String text = opt.asText().trim();
                if (!text.isEmpty()) {
                    letters.add(text.substring(0, 1).toUpperCase());
                }
            } else if (opt.isObject()) {
                String letter = opt.path("letter").asText(null);
                if (letter != null && !letter.isBlank()) {
                    letters.add(letter.trim().toUpperCase());
                }
            }
        }
        return letters;
    }

    /**
     * Count words in text (handles HTML).
     */
    public int countWords(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // Remove HTML tags
        String plainText = text.replaceAll("<[^>]+>", " ");
        // Remove extra whitespace
        plainText = plainText.replaceAll("\\s+", " ").trim();

        if (plainText.isEmpty()) {
            return 0;
        }

        return plainText.split("\\s+").length;
    }

    /**
     * Check if text contains Vietnamese characters.
     */
    private boolean containsVietnamese(String text) {
        if (text == null)
            return false;

        // Vietnamese diacritic characters
        String vietnamese = "àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ";
        String upperVietnamese = vietnamese.toUpperCase();

        for (char c : text.toLowerCase().toCharArray()) {
            if (vietnamese.indexOf(c) >= 0 || upperVietnamese.indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parse AI response content to GeneratedContentDTO.
     * Handles Reading, Listening, and Writing content structures.
     */
    public GeneratedContentDTO parseGeneratedContent(String jsonContent) throws Exception {
        JsonNode root = objectMapper.readTree(jsonContent);
        GeneratedContentDTO content = new GeneratedContentDTO();

        // Parse section (Reading passage) or create from transcript (Listening)
        if (root.has("section")) {
            // Reading format
            JsonNode sectionNode = root.get("section");
            GeneratedContentDTO.GeneratedSectionDTO section = new GeneratedContentDTO.GeneratedSectionDTO();

            if (sectionNode.has("passage_text")) {
                section.setPassageText(sectionNode.get("passage_text").asText());
                section.setWordCount(countWords(section.getPassageText()));
            }
            if (sectionNode.has("word_count_valid")) {
                section.setWordCountValid(sectionNode.get("word_count_valid").asBoolean());
            }

            content.setSection(section);
        } else if (root.has("transcript")) {
            // Listening format - convert transcript to section
            GeneratedContentDTO.GeneratedSectionDTO section = new GeneratedContentDTO.GeneratedSectionDTO();
            section.setPassageText(root.get("transcript").asText());
            section.setWordCount(countWords(section.getPassageText()));

            // Parse section_layout for Listening
            if (root.has("section_layout")) {
                section.setSectionLayout(root.get("section_layout"));
            }

            content.setSection(section);
        }

        // Writing format - map task_prompt to section
        if (root.has("task_prompt")) {
            GeneratedContentDTO.GeneratedSectionDTO section = content.getSection();
            if (section == null) {
                section = new GeneratedContentDTO.GeneratedSectionDTO();
                content.setSection(section);
            }
            String taskPrompt = root.get("task_prompt").asText();
            section.setTaskText(taskPrompt);
            if (section.getPassageText() == null || section.getPassageText().isBlank()) {
                section.setPassageText(taskPrompt);
                section.setWordCount(countWords(taskPrompt));
            }
        }

        // Parse section_layout if present (Listening or hybrid)
        if (root.has("section_layout") && content.getSection() != null) {
            content.getSection().setSectionLayout(root.get("section_layout"));
        }

        // Parse questions
        if (root.has("questions") && root.get("questions").isArray()) {
            List<GeneratedContentDTO.GeneratedQuestionDTO> questions = new ArrayList<>();

            for (JsonNode qNode : root.get("questions")) {
                GeneratedContentDTO.GeneratedQuestionDTO question = new GeneratedContentDTO.GeneratedQuestionDTO();

                if (qNode.has("question_number")) {
                    question.setQuestionNumber(qNode.get("question_number").asInt());
                }
                if (qNode.has("question_type")) {
                    question.setQuestionType(qNode.get("question_type").asText());
                }
                if (qNode.has("question_content")) {
                    question.setQuestionContent(qNode.get("question_content"));
                }
                if (qNode.has("correct_answer") && qNode.get("correct_answer").isArray()) {
                    List<String> answers = new ArrayList<>();
                    for (JsonNode ans : qNode.get("correct_answer")) {
                        answers.add(ans.asText());
                    }
                    question.setCorrectAnswer(answers);
                }
                if (qNode.has("explanation")) {
                    question.setExplanation(qNode.get("explanation").asText());
                }
                if (qNode.has("word_limit") && !qNode.get("word_limit").isNull()) {
                    question.setWordLimit(qNode.get("word_limit").asText());
                }
                if (qNode.has("image_url") && !qNode.get("image_url").isNull()) {
                    question.setImageUrl(qNode.get("image_url").asText());
                } else if (qNode.has("imageUrl") && !qNode.get("imageUrl").isNull()) {
                    question.setImageUrl(qNode.get("imageUrl").asText());
                }

                questions.add(question);
            }

            content.setQuestions(questions);
        }

        // Parse chart data if present (Writing Task 1)
        if (root.has("chart_data")) {
            content.setChartData(root.get("chart_data"));
        }

        if (root.has("task_type")) {
            content.setTaskType(root.get("task_type").asText());
        }
        if (root.has("word_requirement")) {
            content.setWordRequirement(root.get("word_requirement").asInt());
        }
        if (root.has("letter_context")) {
            content.setLetterContext(root.get("letter_context"));
        }
        if (root.has("essay_metadata")) {
            content.setEssayMetadata(root.get("essay_metadata"));
        }

        // Parse figure description if present (Listening Part 2 maps, Writing diagrams)
        if (root.has("figure_description")) {
            content.setFigureDescription(root.get("figure_description"));
        }

        // Parse audio placeholder if present (Listening)
        if (root.has("audio_placeholder")) {
            JsonNode audioNode = root.get("audio_placeholder");
            GeneratedContentDTO.AudioPlaceholderDTO audio = new GeneratedContentDTO.AudioPlaceholderDTO();

            if (audioNode.has("duration_estimate")) {
                audio.setDurationEstimate(audioNode.get("duration_estimate").asText());
            }
            if (audioNode.has("speaker_count")) {
                audio.setSpeakerCount(audioNode.get("speaker_count").asInt());
            }
            if (audioNode.has("speaker_genders") && audioNode.get("speaker_genders").isArray()) {
                List<String> genders = new ArrayList<>();
                for (JsonNode gender : audioNode.get("speaker_genders")) {
                    genders.add(gender.asText());
                }
                audio.setSpeakerGenders(genders);
            }
            if (audioNode.has("accent_recommendation")) {
                audio.setAccentRecommendation(audioNode.get("accent_recommendation").asText());
            }
            if (audioNode.has("pacing_notes")) {
                audio.setPacingNotes(audioNode.get("pacing_notes").asText());
            }
            if (audioNode.has("background_ambient")) {
                audio.setBackgroundAmbient(audioNode.get("background_ambient").asText());
            }
            if (audioNode.has("tts_ready")) {
                audio.setTtsReady(audioNode.get("tts_ready").asBoolean());
            }

            content.setAudioPlaceholder(audio);
        }

        return content;
    }
}
