package com.cramer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

/**
 * Entity representing exam sections (e.g., Reading passages, Listening parts).
 * Each section contains multiple questions.
 */
@Entity
@Table(name = "sections", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    // New: Link to IeltsTest entity (test_id column)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id")
    @JsonIgnore
    private IeltsTest ieltsTest;

    @Column(name = "exam_source")
    private String examSource; // e.g., "cam17", "cam18" (kept for backward compatibility)

    @Column(name = "test_number")
    private Integer testNumber; // e.g., 1, 2, 3, 4 (kept for backward compatibility)

    @Column(name = "skill")
    private String skill; // e.g., "reading", "listening"

    @Column(name = "part_number")
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

    @Column(name = "image_description", columnDefinition = "TEXT")
    private String imageDescription; // Detailed text description for Task 1 charts/maps when AI doesn't support
                                     // images

    @Builder.Default
    @Column(name = "status", length = 20)
    private String status = "PUBLISHED"; // 'PUBLISHED', 'DRAFT', 'ARCHIVED'

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

    public String getImageDescription() {
        return imageDescription;
    }

    public void setImageDescription(String imageDescription) {
        this.imageDescription = imageDescription;
    }

    public IeltsTest getIeltsTest() {
        return ieltsTest;
    }

    public void setIeltsTest(IeltsTest ieltsTest) {
        this.ieltsTest = ieltsTest;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Get the test ID if linked to an IeltsTest.
     */
    public Long getTestId() {
        return ieltsTest != null ? ieltsTest.getId() : null;
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
