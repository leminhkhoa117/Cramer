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
    private final JsonListeningValidator listeningValidator;
    private final JsonWritingValidator writingValidator;
    private final GeneratedContentParser contentParser;

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

    private static final Set<String> VALID_WORD_LIMITS = Set.of(
            "ONE WORD ONLY",
            "NO MORE THAN TWO WORDS",
            "NO MORE THAN THREE WORDS",
            "ONE WORD AND/OR A NUMBER",
            "NO MORE THAN TWO WORDS AND/OR A NUMBER",
            "NO MORE THAN THREE WORDS AND/OR A NUMBER");

    public JsonValidatorService() {
        this.objectMapper = new ObjectMapper();
        // Enable lenient parsing features to handle common AI JSON quirks
        this.objectMapper.enable(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature());
        this.objectMapper.enable(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_JAVA_COMMENTS.mappedFeature());
        this.objectMapper.enable(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_YAML_COMMENTS.mappedFeature());
        this.objectMapper
                .enable(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES.mappedFeature());
        this.objectMapper.enable(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature());
        this.objectMapper
                .enable(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature());
        this.listeningValidator = new JsonListeningValidator(this.objectMapper);
        this.writingValidator = new JsonWritingValidator(this.objectMapper);
        this.contentParser = new GeneratedContentParser(this.objectMapper);
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
        // FIX 3: structured, machine-consumable view of every error/warning.
        // Each addXxx call appends a ValidationIssue with a stable id so the
        // frontend + refinement agent can target individual issues instead of
        // re-parsing free-text strings.
        private final List<ValidationIssue> issues;

        public ValidationResult() {
            this.valid = true;
            this.schemaErrors = new ArrayList<>();
            this.contentErrors = new ArrayList<>();
            this.businessRuleErrors = new ArrayList<>();
            this.warnings = new ArrayList<>();
            this.issues = new ArrayList<>();
        }

        public void addSchemaError(String error) {
            schemaErrors.add(error);
            issues.add(ValidationIssue.of("schema", "error", error));
            valid = false;
        }

        public void addContentError(String error) {
            contentErrors.add(error);
            issues.add(ValidationIssue.of("content", "error", error));
            valid = false;
        }

        public void addBusinessRuleError(String error) {
            businessRuleErrors.add(error);
            issues.add(ValidationIssue.of("business_rule", "error", error));
            valid = false;
        }

        public void addWarning(String warning) {
            warnings.add(warning);
            issues.add(ValidationIssue.of("warning", "warning", warning));
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

        /** FIX 3: structured issues (stable id, severity, category, paths). */
        public List<ValidationIssue> getIssues() {
            return issues;
        }

        public List<String> getAllErrors() {
            List<String> all = new ArrayList<>();
            all.addAll(schemaErrors);
            all.addAll(contentErrors);
            all.addAll(businessRuleErrors);
            return all;
        }
    }

    /**
     * FIX 3: a single structured validation finding.
     *
     * <p>{@code id} is a deterministic SHA-1-derived hash of category+message so
     * the same finding keeps the same id across validation runs (lets the UI
     * preserve accept/reject selections). {@code affectedPaths} are RFC 6901 JSON
     * Pointers into the generated content, derived best-effort from the message
     * text by {@link #deriveAffectedPaths(String)}: a single question reference
     * maps to {@code /questions/{index}} (0-based), multiple questions map to
     * {@code /questions}, and transcript/passage mentions map to
     * {@code /section/transcript} / {@code /section/passage_text}. Messages with
     * no recognizable anchor yield an empty list.
     */
    public static final class ValidationIssue {
        private final String id;
        private final String category;   // schema | content | business_rule | warning
        private final String severity;   // error | warning
        private final String message;
        private final List<String> affectedPaths;

        private ValidationIssue(String id, String category, String severity, String message,
                List<String> affectedPaths) {
            this.id = id;
            this.category = category;
            this.severity = severity;
            this.message = message;
            this.affectedPaths = affectedPaths != null ? affectedPaths : new ArrayList<>();
        }

        static ValidationIssue of(String category, String severity, String message) {
            return new ValidationIssue(stableId(category, message), category, severity, message,
                    deriveAffectedPaths(message));
        }

        /**
         * Best-effort mapping of a free-text validation message to RFC 6901 JSON
         * Pointers. A single question reference (e.g. "Question 5", "Q5",
         * "question_number 5") maps to {@code /questions/{N-1}} (question numbers
         * are 1-based, the array is 0-based); multiple distinct question numbers
         * collapse to {@code /questions}; transcript/passage mentions add
         * {@code /section/transcript} / {@code /section/passage_text}. Returns an
         * empty list when no anchor is recognizable.
         */
        static List<String> deriveAffectedPaths(String message) {
            List<String> paths = new ArrayList<>();
            if (message == null || message.isBlank()) {
                return paths;
            }
            String lower = message.toLowerCase();

            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(?:question(?:[ _]number)?|\\bq)\\s*#?(\\d+)",
                            java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(message);
            java.util.LinkedHashSet<Integer> qNums = new java.util.LinkedHashSet<>();
            while (m.find()) {
                try {
                    qNums.add(Integer.parseInt(m.group(1)));
                } catch (NumberFormatException ignored) {
                    // non-parseable capture; skip
                }
            }

            if (qNums.size() == 1) {
                int n = qNums.iterator().next();
                if (n > 0) {
                    paths.add("/questions/" + (n - 1));
                }
            } else if (qNums.size() > 1) {
                paths.add("/questions");
            }

            if (lower.contains("transcript")) {
                paths.add("/section/transcript");
            }
            if (lower.contains("passage")) {
                paths.add("/section/passage_text");
            }

            return paths;
        }

        private static String stableId(String category, String message) {
            String seed = (category == null ? "" : category) + "|" + (message == null ? "" : message);
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
                byte[] digest = md.digest(seed.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 6 && i < digest.length; i++) {
                    sb.append(String.format("%02x", digest[i]));
                }
                return "vi-" + sb;
            } catch (java.security.NoSuchAlgorithmException e) {
                // SHA-1 is always available on the JVM; fall back to hashCode just in case.
                return "vi-" + Integer.toHexString(seed.hashCode());
            }
        }

        public String getId() {
            return id;
        }

        public String getCategory() {
            return category;
        }

        public String getSeverity() {
            return severity;
        }

        public String getMessage() {
            return message;
        }

        public List<String> getAffectedPaths() {
            return affectedPaths;
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

            // Check explanation length (warning only) - handles both string and object
            // formats
            String explanation = extractExplanationText(question.path("explanation"));
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
                            if (ans.length() > 1 && !answerAppearsInPassage(ans, passageLower)) {
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
                    // Note: AI-generated content does not include images - image_url is optional
                    // Image can be uploaded later by human reviewer
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
                typeCounts.merge(type, 1, (a, b) -> Objects.requireNonNull(a) + Objects.requireNonNull(b));
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
        if (request.getExplanationLanguage() == GenerationRequestDTO.ExplanationLanguage.VI && questions != null) {
            for (JsonNode question : questions) {
                String explanation = extractExplanationText(question.path("explanation"));
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
        return listeningValidator.validateListeningContent(jsonContent, request);
    }

    // ==================== WRITING VALIDATION ====================

    /**
     * Validate Writing content from AI response.
     * Enhanced for Phase 4 with Task 1 and Task 2 specific validation.
     */
    public ValidationResult validateWritingContent(String jsonContent, GenerationRequestDTO request) {
        return writingValidator.validateWritingContent(jsonContent, request);
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

        // Note: We no longer validate question numbering start position because
        // ABTSService.generateReadingForPart calls renumberQuestionsForReadingPart()
        // to automatically fix this after parsing.
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
        JsonValidationSupport.validateMultipleChoice(num, qContent, question, expectedOptions, result);
    }

    private void validateMultipleChoiceMultiple(int num, JsonNode qContent, JsonNode question, int expectedOptions,
            ValidationResult result) {
        JsonValidationSupport.validateMultipleChoiceMultiple(num, qContent, question, expectedOptions, result);
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

    /**
     * Extract explanation text from explanation node.
     * Handles both string format (legacy) and object format (with detail, quote,
     * strategy).
     */
    private String extractExplanationText(JsonNode explanation) {
        if (explanation == null || explanation.isMissingNode() || explanation.isNull()) {
            return "";
        }
        // If it's a string, return directly
        if (explanation.isTextual()) {
            return explanation.asText("");
        }
        // If it's an object, concatenate detail + quote + strategy
        if (explanation.isObject()) {
            StringBuilder sb = new StringBuilder();
            if (explanation.has("detail")) {
                sb.append(explanation.get("detail").asText(""));
            }
            if (explanation.has("quote")) {
                sb.append(" ").append(explanation.get("quote").asText(""));
            }
            if (explanation.has("strategy")) {
                sb.append(" ").append(explanation.get("strategy").asText(""));
            }
            return sb.toString().trim();
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
        // Match a number in the text, including inside HTML tags like
        // <strong>1</strong>
        // Pattern matches: standalone numbers, or numbers inside <strong> tags
        return text.matches(".*\\b\\d+\\b.*") ||
                text.matches("(?s).*<strong>\\s*\\d+\\s*</strong>.*");
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
        return JsonValidationSupport.extractOptionLetters(options);
    }

    /**
     * Count words in text (handles HTML).
     */
    public int countWords(String text) {
        return JsonValidationSupport.countWords(text);
    }

    /**
     * Check if an answer appears in the passage using smart component matching.
     * For multi-word answers, checks if ALL significant words appear in the
     * passage.
     * This reduces false positives for answers like "electric motor" where both
     * words appear in the passage but not as a contiguous phrase.
     * 
     * @param answer       The answer to check
     * @param passageLower The passage text in lowercase
     * @return true if the answer (or all its components) appear in the passage
     */
    private boolean answerAppearsInPassage(String answer, String passageLower) {
        return JsonValidationSupport.answerAppearsInPassage(answer, passageLower);
    }

    /**
     * Check if text contains Vietnamese characters.
     */
    private boolean containsVietnamese(String text) {
        return JsonValidationSupport.containsVietnamese(text);
    }

    /**
     * Parse AI response content to GeneratedContentDTO.
     * Handles Reading, Listening, and Writing content structures.
     */
    public GeneratedContentDTO parseGeneratedContent(String jsonContent) throws Exception {
        return contentParser.parseGeneratedContent(jsonContent);
    }

    /**
     * Renumber questions for Reading Parts 2 and 3 if they start at 1.
     * This is a post-processing step to fix AI outputs that don't follow
     * IELTS numbering conventions.
     * 
     * Part 1: Q1-13 (no change needed)
     * Part 2: Q14-26 (add 13 if starting at 1)
     * Part 3: Q27-40 (add 26 if starting at 1)
     * 
     * @param content    The generated content to renumber
     * @param partNumber The Reading part number (1, 2, or 3)
     * @return The content with renumbered questions
     */
    public GeneratedContentDTO renumberQuestionsForReadingPart(GeneratedContentDTO content, int partNumber) {
        return contentParser.renumberQuestionsForReadingPart(content, partNumber);
    }
}
