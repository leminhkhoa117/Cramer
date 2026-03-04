package com.cramer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing hashtags for categorizing tests.
 * Categories include: 'topic', 'theme', 'difficulty', etc.
 */
@Entity
@Table(name = "hashtags", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code; // e.g., "environment", "technology", "education"

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "category", nullable = false, length = 50)
    private String category; // 'topic', 'theme', 'difficulty'

    @Column(name = "icon", length = 10)
    private String icon; // Emoji or icon code

    @Column(name = "color", length = 20)
    private String color; // Hex color code for UI display

    @Column(name = "use_count")
    @Builder.Default
    private Integer useCount = 0;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Relationships
    @ManyToMany(mappedBy = "hashtags", fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<IeltsTest> tests = new HashSet<>();

    // Note: TestSet -> Hashtag relationship was removed (test_set_hashtags table deleted)
    // Hashtags are now only associated at the IeltsTest level

    /**
     * Increment the use count when this hashtag is added to a test.
     */
    public void incrementUseCount() {
        this.useCount = (this.useCount != null ? this.useCount : 0) + 1;
    }

    /**
     * Decrement the use count when this hashtag is removed from a test.
     */
    public void decrementUseCount() {
        if (this.useCount != null && this.useCount > 0) {
            this.useCount--;
        }
    }

    /**
     * Get the number of tests using this hashtag.
     */
    public int getTestCount() {
        return tests != null ? tests.size() : 0;
    }
}
