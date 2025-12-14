package com.cramer.dto;

import com.cramer.entity.Vocabulary;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for Vocabulary entity responses.
 * Contains all vocabulary information for API responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VocabularyDTO {

    private Long id;
    private UUID userId;
    private String word;
    private String translation;
    private String phonetic;
    private String partOfSpeech;
    private String definition;
    private String exampleSentence;
    private String sourceContext;
    private Long sourceTestId;
    private Long sourceSectionId;
    private String notes;
    
    @JsonProperty("mastered")
    private Boolean isMastered;
    
    private Integer reviewCount;
    private OffsetDateTime lastReviewedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /**
     * Create a VocabularyDTO from a Vocabulary entity.
     *
     * @param entity the Vocabulary entity
     * @return the corresponding VocabularyDTO
     */
    public static VocabularyDTO fromEntity(Vocabulary entity) {
        if (entity == null) {
            return null;
        }

        return VocabularyDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .word(entity.getWord())
                .translation(entity.getTranslation())
                .phonetic(entity.getPhonetic())
                .partOfSpeech(entity.getPartOfSpeech())
                .definition(entity.getDefinition())
                .exampleSentence(entity.getExampleSentence())
                .sourceContext(entity.getSourceContext())
                .sourceTestId(entity.getSourceTestId())
                .sourceSectionId(entity.getSourceSectionId())
                .notes(entity.getNotes())
                .isMastered(entity.getIsMastered())
                .reviewCount(entity.getReviewCount())
                .lastReviewedAt(entity.getLastReviewedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Convert this DTO to a Vocabulary entity.
     *
     * @return the corresponding Vocabulary entity
     */
    public Vocabulary toEntity() {
        return Vocabulary.builder()
                .id(this.id)
                .userId(this.userId)
                .word(this.word)
                .translation(this.translation)
                .phonetic(this.phonetic)
                .partOfSpeech(this.partOfSpeech)
                .definition(this.definition)
                .exampleSentence(this.exampleSentence)
                .sourceContext(this.sourceContext)
                .sourceTestId(this.sourceTestId)
                .sourceSectionId(this.sourceSectionId)
                .notes(this.notes)
                .isMastered(this.isMastered != null ? this.isMastered : false)
                .reviewCount(this.reviewCount != null ? this.reviewCount : 0)
                .lastReviewedAt(this.lastReviewedAt)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}
