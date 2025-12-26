package com.cramer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Entity representing an individual IELTS test within a test set.
 * Named "IeltsTest" to avoid conflicts with JUnit's "Test" annotation.
 */
@Entity
@Table(name = "tests", schema = "public",
        uniqueConstraints = @UniqueConstraint(columnNames = {"set_id", "test_number"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IeltsTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private TestSet testSet;

    @Column(name = "test_number", nullable = false)
    private Integer testNumber;

    @Column(name = "name_vi", length = 255)
    private String nameVi;

    @Column(name = "name_en", length = 255)
    private String nameEn;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "difficulty", length = 30)
    @Builder.Default
    private String difficulty = "INTERMEDIATE"; // BEGINNER, INTERMEDIATE, ADVANCED

    @Column(name = "estimated_time_minutes")
    @Builder.Default
    private Integer estimatedTimeMinutes = 170; // Full IELTS test duration

    @Column(name = "is_published")
    @Builder.Default
    private Boolean isPublished = false;

    @Column(name = "is_ai_generated")
    @Builder.Default
    private Boolean isAiGenerated = false;

    @Type(JsonType.class)
    @Column(name = "generation_metadata", columnDefinition = "jsonb")
    private JsonNode generationMetadata; // Stores AI generation parameters, model used, etc.

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // Relationships
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "test_hashtags",
            joinColumns = @JoinColumn(name = "test_id"),
            inverseJoinColumns = @JoinColumn(name = "hashtag_id")
    )
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Hashtag> hashtags = new HashSet<>();

    @OneToMany(mappedBy = "ieltsTest", fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Section> sections = new ArrayList<>();

    /**
     * Get the test set ID without loading the full entity.
     */
    public Long getSetId() {
        return testSet != null ? testSet.getId() : null;
    }

    /**
     * Get the test set code without loading the full entity.
     */
    public String getSetCode() {
        return testSet != null ? testSet.getCode() : null;
    }

    /**
     * Get count of sections in this test.
     */
    public int getSectionCount() {
        return sections != null ? sections.size() : 0;
    }

    /**
     * Get section counts by skill type.
     * @return Map with skill as key and count as value
     */
    public Map<String, Integer> getSkillSectionCounts() {
        if (sections == null || sections.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Integer> counts = new HashMap<>();
        for (Section section : sections) {
            String skill = section.getSkill();
            if (skill != null) {
                counts.merge(skill.toLowerCase(), 1, Integer::sum);
            }
        }
        return counts;
    }

    /**
     * Add a hashtag to this test.
     */
    public void addHashtag(Hashtag hashtag) {
        if (hashtags == null) {
            hashtags = new HashSet<>();
        }
        hashtags.add(hashtag);
        hashtag.getTests().add(this);
    }

    /**
     * Remove a hashtag from this test.
     */
    public void removeHashtag(Hashtag hashtag) {
        if (hashtags != null) {
            hashtags.remove(hashtag);
            hashtag.getTests().remove(this);
        }
    }
}
