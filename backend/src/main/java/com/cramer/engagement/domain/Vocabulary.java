package com.cramer.engagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A vocabulary notebook entry, table {@code vocabulary} (SPEC-16 §3). Unique per
 * {@code (user_id, word)} (enforced in the service).
 */
@Entity
@Table(name = "vocabulary", schema = "public")
@Getter
@Setter
public class Vocabulary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "word", nullable = false)
    private String word;

    @Column(name = "translation", columnDefinition = "TEXT")
    private String translation;

    @Column(name = "phonetic")
    private String phonetic;

    @Column(name = "part_of_speech")
    private String partOfSpeech;

    @Column(name = "definition", columnDefinition = "TEXT")
    private String definition;

    @Column(name = "example_sentence", columnDefinition = "TEXT")
    private String exampleSentence;

    @Column(name = "source_context", columnDefinition = "TEXT")
    private String sourceContext;

    @Column(name = "source_test_id")
    private Long sourceTestId;

    @Column(name = "source_section_id")
    private Long sourceSectionId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_mastered")
    private Boolean isMastered = false;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    @Column(name = "last_reviewed_at")
    private OffsetDateTime lastReviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
