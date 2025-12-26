package com.cramer.dto.abts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * DTO for saving AI-generated content to the database.
 * 
 * This maps the GeneratedContentDTO structure to database entities.
 * Updated to support the new test hierarchy (test_sets -> tests -> sections).
 * 
 * @since 2025-12-25 - ABTS Save System
 * @since 2025-12-26 - Phase 3.5/6: Test Hierarchy Support
 */
public class SaveContentRequestDTO {

    // ==================== LEGACY FIELDS (kept for backward compatibility) ====================

    /**
     * Exam source identifier (e.g., "AI-GEN", "custom").
     * Used for backward compatibility with existing sections.
     */
    private String examSource;

    /**
     * Test number as string (will be auto-generated if not provided).
     * Used for backward compatibility.
     */
    private String testNumber;

    // ==================== REQUIRED FIELDS ====================

    /**
     * Skill type: "reading", "listening", "writing".
     */
    @NotBlank(message = "Skill is required")
    private String skill;

    /**
     * Part number within the skill (1, 2, 3, etc.).
     */
    @NotNull(message = "Part number is required")
    private Integer partNumber;

    /**
     * The generated content to save.
     */
    @NotNull(message = "Content is required")
    private GeneratedContentDTO content;

    // ==================== NEW HIERARCHY FIELDS ====================

    /**
     * Optional: existing TestSet ID to add this test to.
     * If provided, takes precedence over setCode.
     */
    private Long setId;

    /**
     * Optional: TestSet code to find or create.
     * If setId is not provided, this is used to find or create a TestSet.
     * Defaults to "ai_generated" if neither setId nor setCode is provided.
     */
    private String setCode;

    /**
     * Optional: existing Test ID to add this section to.
     * If provided, the section will be added to this existing test.
     */
    private Long testId;

    /**
     * Optional: hashtag codes to associate with the test.
     * These will be found or created automatically.
     */
    private List<String> hashtagCodes;

    /**
     * Optional: hashtag IDs to associate with the test.
     * Alternative to hashtagCodes when IDs are already known.
     */
    private List<Long> hashtagIds;

    // ==================== METADATA FIELDS ====================

    /**
     * Optional topic name for reference and metadata.
     */
    private String topic;

    /**
     * Optional: generation configuration for reproducibility.
     * Stores the inputs used for AI generation (topic, facts, model, etc.).
     */
    private Map<String, Object> generationConfig;

    // ==================== GETTERS AND SETTERS ====================

    public String getExamSource() {
        return examSource;
    }

    public void setExamSource(String examSource) {
        this.examSource = examSource;
    }

    public String getTestNumber() {
        return testNumber;
    }

    public void setTestNumber(String testNumber) {
        this.testNumber = testNumber;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public Integer getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(Integer partNumber) {
        this.partNumber = partNumber;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public GeneratedContentDTO getContent() {
        return content;
    }

    public void setContent(GeneratedContentDTO content) {
        this.content = content;
    }

    public Long getSetId() {
        return setId;
    }

    public void setSetId(Long setId) {
        this.setId = setId;
    }

    public String getSetCode() {
        return setCode;
    }

    public void setSetCode(String setCode) {
        this.setCode = setCode;
    }

    public Long getTestId() {
        return testId;
    }

    public void setTestId(Long testId) {
        this.testId = testId;
    }

    public List<String> getHashtagCodes() {
        return hashtagCodes;
    }

    public void setHashtagCodes(List<String> hashtagCodes) {
        this.hashtagCodes = hashtagCodes;
    }

    public List<Long> getHashtagIds() {
        return hashtagIds;
    }

    public void setHashtagIds(List<Long> hashtagIds) {
        this.hashtagIds = hashtagIds;
    }

    public Map<String, Object> getGenerationConfig() {
        return generationConfig;
    }

    public void setGenerationConfig(Map<String, Object> generationConfig) {
        this.generationConfig = generationConfig;
    }
}
