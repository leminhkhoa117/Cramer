package com.cramer.dto.abts;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * DTO for ABTS generation requests.
 * Contains all parameters needed to generate IELTS test content.
 * 
 * @since 2025-12-20 - ABTS v2.0
 */
public class GenerationRequestDTO {

    /**
     * Skill type to generate content for.
     */
    public enum SkillType {
        READING, LISTENING, WRITING, SPEAKING
    }

    /**
     * Generation scope - how much content to generate.
     */
    public enum GenerationScope {
        FULL_SKILL, // All parts for the skill (e.g., all 3 Reading passages)
        SINGLE_PART, // One part/passage only
        MULTI_PART, // Multiple selected parts (user picks which parts)
        QUESTION_GROUP // Specific question range only
    }

    /**
     * Difficulty level mapped to IELTS band ranges.
     */
    public enum DifficultyLevel {
        BEGINNER("4.0-5.0", "Beginner"),
        LOWER_INTERMEDIATE("5.0-6.0", "Lower-Intermediate"),
        INTERMEDIATE("6.0-7.0", "Intermediate"),
        UPPER_INTERMEDIATE("7.0-8.0", "Upper-Intermediate"),
        ADVANCED("8.0-9.0", "Advanced/IELTS-like");

        private final String bandRange;
        private final String displayName;

        DifficultyLevel(String bandRange, String displayName) {
            this.bandRange = bandRange;
            this.displayName = displayName;
        }

        public String getBandRange() {
            return bandRange;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Language for explanations in generated content.
     */
    public enum ExplanationLanguage {
        VI, EN
    }

    /**
     * Test type (Academic or General Training).
     */
    public enum TestType {
        ACADEMIC, GENERAL_TRAINING
    }

    // ==================== REQUIRED FIELDS ====================

    @NotNull(message = "Loại kỹ năng là bắt buộc")
    private SkillType skill;

    @NotNull(message = "Phạm vi tạo là bắt buộc")
    private GenerationScope scope;

    @NotNull(message = "Chủ đề là bắt buộc")
    private String topic;

    @NotNull(message = "Độ khó là bắt buộc")
    private DifficultyLevel difficulty;

    // Facts are optional in Auto Mode
    private List<String> facts;

    @NotNull(message = "Ngôn ngữ giải thích là bắt buộc")
    private ExplanationLanguage explanationLanguage = ExplanationLanguage.VI;

    // ==================== OPTIONAL FIELDS ====================

    /**
     * Part number (required if scope is SINGLE_PART).
     * For Reading: 1, 2, or 3
     * For Listening: 1, 2, 3, or 4
     */
    private Integer partNumber;

    /**
     * Topic hashtags for categorization.
     */
    private List<String> hashtags;

    /**
     * Question types to include in generation.
     * If null, system will auto-select appropriate types.
     */
    private List<String> questionTypes;

    /**
     * Target word count range for passages.
     */
    private WordCountRange wordCountRange;

    /**
     * Test type (Academic by default).
     */
    private TestType testType = TestType.ACADEMIC;

    /**
     * Existing passage text (for question-only regeneration).
     */
    private String existingPassageText;

    /**
     * Specific question numbers to regenerate.
     */
    private List<Integer> questionsToRegenerate;

    /**
     * Selected AI model (optional - uses config default if not specified).
     */
    private String model;

    /**
     * Model variant suffix (e.g., ":thinking", ":free", ":nitro").
     */
    private String modelVariant;

    /**
     * Enable reasoning tokens for Chain-of-Thought visibility.
     */
    private Boolean enableReasoning = true;

    /**
     * Reasoning effort level.
     */
    private String reasoningEffort = "high";

    /**
     * Temperature for AI generation (0.0 to 2.0).
     * Lower values = more deterministic, higher = more creative.
     * Default: 1.0
     */
    private Double temperature = 1.0;

    /**
     * Custom question counts per type (power-user override).
     * Example: { "TRUE_FALSE_NOT_GIVEN": 6, "FILL_IN_BLANK": 7 }
     */
    private Map<String, Integer> questionTypeCounts;

    /**
     * Target total questions (power-user override).
     */
    private Integer totalQuestions;

    /**
     * Passage length preference: SHORT | MEDIUM | LONG.
     */
    private String passageLength;

    /**
     * Custom prompt instructions appended to user prompt.
     */
    private String customInstructions;

    /**
     * Max output tokens for AI generation.
     */
    private Integer maxTokens;

    /**
     * Enable web search for auto-generated facts mode.
     * When true, uses OpenRouter's web plugin to fetch real-time information.
     */
    private Boolean enableWebSearch = false;

    /**
     * Enable context caching to reduce response delay and cost.
     * Uses OpenRouter's cache_prompt feature.
     */
    private Boolean enableContextCaching = false;

    /**
     * Writing Task 2 essay type.
     * Options: OPINION, DISCUSSION, PROBLEM_SOLUTION, TWO_PART
     */
    private String writingEssayType;

    // ==================== MULTI-PART GENERATION ====================

    /**
     * Parts to generate (multi-selection).
     * For Reading: [1], [2], [3], [1,2], [2,3], [1,3], [1,2,3]
     * For Listening: [1], [2], [3], [4], any combination
     * For Writing: [1] (Task 1), [2] (Task 2), [1,2] (Both)
     * If null or empty, uses partNumber for single-part (backward compatible).
     */
    private List<Integer> partsToGenerate;

    /**
     * Per-part configuration map.
     * Key: part number (1, 2, 3, or 4)
     * Value: PartConfigDTO with optional overrides for topic, facts, name,
     * questionTypes.
     * If a part is not in this map, global values are used.
     */
    private Map<Integer, PartConfigDTO> partConfigs;

    // ==================== INNER CLASSES ====================

    /**
     * Word count range for passages.
     */
    public static class WordCountRange {
        private int min;
        private int max;

        public WordCountRange() {
        }

        public WordCountRange(int min, int max) {
            this.min = min;
            this.max = max;
        }

        public int getMin() {
            return min;
        }

        public void setMin(int min) {
            this.min = min;
        }

        public int getMax() {
            return max;
        }

        public void setMax(int max) {
            this.max = max;
        }
    }

    /**
     * Per-part configuration for multi-part generation.
     * All fields are optional - null values inherit from global config.
     */
    public static class PartConfigDTO {
        /**
         * Custom topic for this part. If null, uses global topic.
         */
        private String topic;

        /**
         * Custom facts for this part. If null, uses auto-mode (AI generates facts).
         * If empty list, no facts are provided. If non-empty, uses these facts.
         */
        private List<String> facts;

        /**
         * Custom name/title for this part. If null, auto-generated.
         */
        private String name;

        /**
         * Custom question types for this part. If null, uses auto-selection.
         */
        private List<String> questionTypes;

        /**
         * Question count per type. Key: question type ID, Value: count.
         * Example: { "FILL_IN_BLANK": 7, "SUMMARY_COMPLETION": 6 }
         */
        private Map<String, Integer> questionTypeCounts;

        public PartConfigDTO() {
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public List<String> getFacts() {
            return facts;
        }

        public void setFacts(List<String> facts) {
            this.facts = facts;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getQuestionTypes() {
            return questionTypes;
        }

        public void setQuestionTypes(List<String> questionTypes) {
            this.questionTypes = questionTypes;
        }

        public Map<String, Integer> getQuestionTypeCounts() {
            return questionTypeCounts;
        }

        public void setQuestionTypeCounts(Map<String, Integer> questionTypeCounts) {
            this.questionTypeCounts = questionTypeCounts;
        }
    }

    // ==================== GETTERS AND SETTERS ====================

    public SkillType getSkill() {
        return skill;
    }

    public void setSkill(SkillType skill) {
        this.skill = skill;
    }

    public GenerationScope getScope() {
        return scope;
    }

    public void setScope(GenerationScope scope) {
        this.scope = scope;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public DifficultyLevel getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }

    public List<String> getFacts() {
        return facts;
    }

    public void setFacts(List<String> facts) {
        this.facts = facts;
    }

    public ExplanationLanguage getExplanationLanguage() {
        return explanationLanguage;
    }

    public void setExplanationLanguage(ExplanationLanguage explanationLanguage) {
        this.explanationLanguage = explanationLanguage;
    }

    public Integer getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(Integer partNumber) {
        this.partNumber = partNumber;
    }

    public List<String> getHashtags() {
        return hashtags;
    }

    public void setHashtags(List<String> hashtags) {
        this.hashtags = hashtags;
    }

    public List<String> getQuestionTypes() {
        return questionTypes;
    }

    public void setQuestionTypes(List<String> questionTypes) {
        this.questionTypes = questionTypes;
    }

    public WordCountRange getWordCountRange() {
        return wordCountRange;
    }

    public void setWordCountRange(WordCountRange wordCountRange) {
        this.wordCountRange = wordCountRange;
    }

    public TestType getTestType() {
        return testType;
    }

    public void setTestType(TestType testType) {
        this.testType = testType;
    }

    public String getExistingPassageText() {
        return existingPassageText;
    }

    public void setExistingPassageText(String existingPassageText) {
        this.existingPassageText = existingPassageText;
    }

    public List<Integer> getQuestionsToRegenerate() {
        return questionsToRegenerate;
    }

    public void setQuestionsToRegenerate(List<Integer> questionsToRegenerate) {
        this.questionsToRegenerate = questionsToRegenerate;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getModelVariant() {
        return modelVariant;
    }

    public void setModelVariant(String modelVariant) {
        this.modelVariant = modelVariant;
    }

    public Boolean getEnableReasoning() {
        return enableReasoning;
    }

    public void setEnableReasoning(Boolean enableReasoning) {
        this.enableReasoning = enableReasoning;
    }

    public String getReasoningEffort() {
        return reasoningEffort;
    }

    public void setReasoningEffort(String reasoningEffort) {
        this.reasoningEffort = reasoningEffort;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Map<String, Integer> getQuestionTypeCounts() {
        return questionTypeCounts;
    }

    public void setQuestionTypeCounts(Map<String, Integer> questionTypeCounts) {
        this.questionTypeCounts = questionTypeCounts;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public String getPassageLength() {
        return passageLength;
    }

    public void setPassageLength(String passageLength) {
        this.passageLength = passageLength;
    }

    public String getCustomInstructions() {
        return customInstructions;
    }

    public void setCustomInstructions(String customInstructions) {
        this.customInstructions = customInstructions;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Boolean getEnableWebSearch() {
        return enableWebSearch;
    }

    public void setEnableWebSearch(Boolean enableWebSearch) {
        this.enableWebSearch = enableWebSearch;
    }

    public Boolean getEnableContextCaching() {
        return enableContextCaching;
    }

    public void setEnableContextCaching(Boolean enableContextCaching) {
        this.enableContextCaching = enableContextCaching;
    }

    public String getWritingEssayType() {
        return writingEssayType;
    }

    public void setWritingEssayType(String writingEssayType) {
        this.writingEssayType = writingEssayType;
    }

    public List<Integer> getPartsToGenerate() {
        return partsToGenerate;
    }

    public void setPartsToGenerate(List<Integer> partsToGenerate) {
        this.partsToGenerate = partsToGenerate;
    }

    public Map<Integer, PartConfigDTO> getPartConfigs() {
        return partConfigs;
    }

    public void setPartConfigs(Map<Integer, PartConfigDTO> partConfigs) {
        this.partConfigs = partConfigs;
    }
}
