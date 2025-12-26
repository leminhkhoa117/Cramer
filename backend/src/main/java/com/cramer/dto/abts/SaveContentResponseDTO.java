package com.cramer.dto.abts;

import java.util.List;

/**
 * DTO for save operation response.
 * 
 * Returns the IDs of created entities and any warnings.
 * Updated to include test hierarchy IDs.
 * 
 * @since 2025-12-25 - ABTS Save System
 * @since 2025-12-26 - Phase 3.5/6: Test Hierarchy Support
 */
public class SaveContentResponseDTO {

    private boolean success;
    private String message;
    private Long sectionId;
    private Long testId;       // NEW: IeltsTest ID
    private Long setId;        // NEW: TestSet ID
    private String examSource;
    private Integer testNumber;
    private String skill;
    private Integer partNumber;
    private Integer questionsCreated;
    private List<String> warnings;

    // Static factory methods

    public static SaveContentResponseDTO success(Long sectionId, Long testId, Long setId,
            String examSource, Integer testNumber, String skill, Integer partNumber, Integer questionsCreated) {
        SaveContentResponseDTO response = new SaveContentResponseDTO();
        response.success = true;
        response.message = "Content saved successfully";
        response.sectionId = sectionId;
        response.testId = testId;
        response.setId = setId;
        response.examSource = examSource;
        response.testNumber = testNumber;
        response.skill = skill;
        response.partNumber = partNumber;
        response.questionsCreated = questionsCreated;
        return response;
    }

    /**
     * Legacy success factory for backward compatibility.
     */
    public static SaveContentResponseDTO success(Long sectionId, String examSource, Integer testNumber,
            String skill, Integer partNumber, Integer questionsCreated) {
        return success(sectionId, null, null, examSource, testNumber, skill, partNumber, questionsCreated);
    }

    public static SaveContentResponseDTO error(String message) {
        SaveContentResponseDTO response = new SaveContentResponseDTO();
        response.success = false;
        response.message = message;
        return response;
    }

    // Getters and Setters

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public Long getTestId() {
        return testId;
    }

    public void setTestId(Long testId) {
        this.testId = testId;
    }

    public Long getSetId() {
        return setId;
    }

    public void setSetId(Long setId) {
        this.setId = setId;
    }

    public String getExamSource() {
        return examSource;
    }

    public void setExamSource(String examSource) {
        this.examSource = examSource;
    }

    public Integer getTestNumber() {
        return testNumber;
    }

    public void setTestNumber(Integer testNumber) {
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

    public Integer getQuestionsCreated() {
        return questionsCreated;
    }

    public void setQuestionsCreated(Integer questionsCreated) {
        this.questionsCreated = questionsCreated;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
