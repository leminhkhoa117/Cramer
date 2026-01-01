package com.cramer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a collection/folder of tests.
 * Examples: "Cambridge 17", "Cambridge 18", "AI Generated Tests", etc.
 */
@Entity
@Table(name = "test_sets", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code; // e.g., "cam17", "cam18", "ai_001"

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "source_type", length = 50)
    @Builder.Default
    private String sourceType = "custom"; // 'cambridge', 'custom', 'ai_generated'

    @Column(name = "is_published")
    @Builder.Default
    private Boolean isPublished = false;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "testSet", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("testNumber ASC")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<IeltsTest> tests = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "test_set_hashtags", joinColumns = @JoinColumn(name = "test_set_id"), inverseJoinColumns = @JoinColumn(name = "hashtag_id"))
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Hashtag> hashtags = new ArrayList<>();

    /**
     * Get the count of all tests in this set.
     */
    public int getTestCount() {
        return tests != null ? tests.size() : 0;
    }

    /**
     * Get the count of published tests in this set.
     */
    public int getPublishedTestCount() {
        if (tests == null)
            return 0;
        return (int) tests.stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsPublished()))
                .count();
    }

    /**
     * Add a test to this set.
     */
    public void addTest(IeltsTest test) {
        if (tests == null) {
            tests = new ArrayList<>();
        }
        tests.add(test);
        test.setTestSet(this);
    }

    /**
     * Remove a test from this set.
     */
    public void removeTest(IeltsTest test) {
        if (tests != null) {
            tests.remove(test);
            test.setTestSet(null);
        }
    }
}
