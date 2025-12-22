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
            1, new int[] { 450, 550 },
            2, new int[] { 550, 650 },
            3, new int[] { 650, 750 },
            4, new int[] { 750, 850 });

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

        // Get expected range based on difficulty
        String difficulty = request.getDifficulty().name();
        int[] expectedRange = READING_WORD_COUNTS.getOrDefault(difficulty, new int[] { 850, 1000 });

        if (wordCount < expectedRange[0]) {
            // Changed from error to warning - don't fail generation for word count
            result.addWarning(String.format(
                    "Passage word count (%d) is below recommended minimum (%d) for %s difficulty",
                    wordCount, expectedRange[0], difficulty));
        } else if (wordCount > expectedRange[1]) {
            result.addWarning(String.format(
                    "Passage word count (%d) exceeds recommended maximum (%d) for %s difficulty",
                    wordCount, expectedRange[1], difficulty));
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

        // Validate each question
        Set<Integer> questionNumbers = new HashSet<>();
        for (JsonNode question : questions) {
            int num = question.has("question_number") ? question.get("question_number").asInt() : 0;

            // Check for duplicate question numbers
            if (!questionNumbers.add(num)) {
                result.addContentError("Duplicate question number: " + num);
            }

            // Check explanation is not empty
            String explanation = question.has("explanation") ? question.get("explanation").asText() : "";
            if (explanation.isEmpty() || explanation.length() < 20) {
                result.addContentError(String.format(
                        "Question %d has insufficient explanation (length: %d)",
                        num, explanation.length()));
            }

            // Validate blank placeholder for completion types
            String qType = question.has("question_type") ? question.get("question_type").asText() : "";
            if (qType.contains("COMPLETION") || qType.equals("FILL_IN_BLANK")) {
                JsonNode qContent = question.get("question_content");
                if (qContent != null && qContent.has("text")) {
                    String text = qContent.get("text").asText();
                    // Check for exactly 4 underscores
                    if (!text.contains("____")) {
                        result.addContentError(String.format(
                                "Question %d: Completion questions must have ____ (4 underscores) placeholder", num));
                    }
                    // Warn about incorrect underscore counts
                    if (text.contains("___") && !text.contains("____")) {
                        result.addWarning(String.format(
                                "Question %d: Found 3 underscores, use exactly 4: ____", num));
                    }
                    if (text.contains("_____") && !text.contains("______")) {
                        result.addWarning(String.format(
                                "Question %d: Found 5 underscores, use exactly 4: ____", num));
                    }
                }
            }
        }
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

        // Check for section_layout (recommended but not required)
        if (!root.has("section_layout")) {
            result.addWarning("section_layout is recommended for proper question display");
        }

        // Part 2 specific: figure_description for map labelling
        if (part == 2 && !root.has("figure_description")) {
            result.addWarning("Part 2 may need figure_description for map/plan labelling questions");
        }
    }

    /**
     * Validate Listening question schema.
     */
    private void validateListeningQuestionSchema(JsonNode question, int index, ValidationResult result) {
        List<String> requiredFields = List.of("question_number", "question_type", "correct_answer");

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

            // Check explanation language
            if (request.getExplanationLanguage() == GenerationRequestDTO.ExplanationLanguage.VI) {
                String explanation = question.has("explanation") ? question.get("explanation").asText() : "";
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
        if (questions != null && questions.isArray()) {
            int expectedStart = (part - 1) * 10 + 1;
            int expectedEnd = part * 10;

            for (JsonNode question : questions) {
                int num = question.has("question_number") ? question.get("question_number").asInt() : 0;
                if (num < expectedStart || num > expectedEnd) {
                    result.addWarning(String.format(
                            "Question number %d out of expected range for Part %d (Q%d-Q%d)",
                            num, part, expectedStart, expectedEnd));
                }
            }
        }

        // Check section_layout if present
        if (root.has("section_layout") && root.get("section_layout").isArray()) {
            JsonNode layout = root.get("section_layout");
            if (layout.isEmpty()) {
                result.addWarning("section_layout is empty");
            }
        }

        // For Part 2, check figure_description for map labelling
        if (part == 2 && root.has("figure_description")) {
            JsonNode figDesc = root.get("figure_description");
            if (!figDesc.has("title")) {
                result.addWarning("figure_description should have a title");
            }
            if (!figDesc.has("elements") || !figDesc.get("elements").isArray()) {
                result.addWarning("figure_description should have elements array for map locations");
            }
        }

        // Check for audio_placeholder
        if (!root.has("audio_placeholder")) {
            result.addWarning("audio_placeholder recommended for TTS generation metadata");
        }
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
                    if (!root.has("chart_data") && !root.has("figure_description")) {
                        result.addSchemaError("Academic Task 1 requires chart_data or figure_description");
                    } else if (root.has("chart_data")) {
                        validateChartData(root.get("chart_data"), result);
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

        // Check essay metadata if present
        if (root.has("essay_metadata")) {
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
    }

    /**
     * Validate General Training letter content.
     */
    private void validateLetterContent(JsonNode root, ValidationResult result) {
        if (!root.has("letter_context")) {
            result.addWarning("GT Task 1 should include letter_context");
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
                if (qNode.has("word_limit")) {
                    question.setWordLimit(qNode.get("word_limit").asText());
                }

                questions.add(question);
            }

            content.setQuestions(questions);
        }

        // Parse chart data if present (Writing Task 1)
        if (root.has("chart_data")) {
            content.setChartData(root.get("chart_data"));
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
            if (audioNode.has("accent_recommendation")) {
                audio.setAccentRecommendation(audioNode.get("accent_recommendation").asText());
            }
            if (audioNode.has("pacing_notes")) {
                audio.setPacingNotes(audioNode.get("pacing_notes").asText());
            }

            content.setAudioPlaceholder(audio);
        }

        return content;
    }
}
