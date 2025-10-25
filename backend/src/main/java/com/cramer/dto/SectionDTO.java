package com.cramer.dto;

/**
 * DTO for Section entity responses.
 */
public class SectionDTO {
    private Long id;
    private String examSource;
    private Integer testNumber;
    private String skill;
    private Integer partNumber;
    private String displayContentUrl;
    private String passageText;

    public SectionDTO() {
    }

    public SectionDTO(Long id, String examSource, Integer testNumber, String skill, 
                     Integer partNumber, String displayContentUrl, String passageText) {
        this.id = id;
        this.examSource = examSource;
        this.testNumber = testNumber;
        this.skill = skill;
        this.partNumber = partNumber;
        this.displayContentUrl = displayContentUrl;
        this.passageText = passageText;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getDisplayContentUrl() {
        return displayContentUrl;
    }

    public void setDisplayContentUrl(String displayContentUrl) {
        this.displayContentUrl = displayContentUrl;
    }

    public String getPassageText() {
        return passageText;
    }

    public void setPassageText(String passageText) {
        this.passageText = passageText;
    }
}
