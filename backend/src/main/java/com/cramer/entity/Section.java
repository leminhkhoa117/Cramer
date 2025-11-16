package com.cramer.entity;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

/**
 * Entity representing exam sections (e.g., Reading passages, Listening parts).
 * Each section contains multiple questions.
 */
@Entity
@Table(name = "sections", schema = "public")
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "exam_source", nullable = false)
    private String examSource; // e.g., "cam17", "cam18"

    @Column(name = "test_number", nullable = false)
    private Integer testNumber; // e.g., 1, 2, 3, 4

    @Column(name = "skill", nullable = false)
    private String skill; // e.g., "reading", "listening"

    @Column(name = "part_number", nullable = false)
    private Integer partNumber; // e.g., 1, 2, 3

    @Column(name = "display_content_url")
    private String displayContentUrl; // Optional URL to image/PDF

    @Type(JsonType.class)
    @Column(name = "section_layout", columnDefinition = "jsonb")
    private JsonNode sectionLayout; // New field for flexible block-based layouts

    @Column(name = "passage_text", columnDefinition = "TEXT")
    private String passageText; // Full text content for Reading passages

    @Column(name = "audio_url")
    private String audioUrl; // URL for listening audio files

    // Constructors
    public Section() {
    }

    public Section(String examSource, Integer testNumber, String skill, Integer partNumber) {
        this.examSource = examSource;
        this.testNumber = testNumber;
        this.skill = skill;
        this.partNumber = partNumber;
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

    public JsonNode getSectionLayout() {
        return sectionLayout;
    }

    public void setSectionLayout(JsonNode sectionLayout) {
        this.sectionLayout = sectionLayout;
    }

    public String getPassageText() {
        return passageText;
    }

    public void setPassageText(String passageText) {
        this.passageText = passageText;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    @Override
    public String toString() {
        return "Section{" +
                "id=" + id +
                ", examSource='" + examSource + '\'' +
                ", testNumber=" + testNumber +
                ", skill='" + skill + '\'' +
                ", partNumber=" + partNumber +
                '}';
    }
}
