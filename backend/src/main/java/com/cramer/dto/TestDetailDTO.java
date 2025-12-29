package com.cramer.dto;

import com.cramer.dto.testhierarchy.HashtagDTO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO for IeltsTest entity - full detail view.
 * Used when viewing a single test with all its sections.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestDetailDTO {

    private Long id;
    private Long setId;
    private String setCode;
    private String setName; // The test set's name for display
    private Integer testNumber;
    private String name;
    private String description;
    private String difficulty;
    private Integer estimatedTimeMinutes;
    private Boolean isPublished;
    private Boolean isAiGenerated;
    private JsonNode generationMetadata;

    // Related entities
    private List<HashtagDTO> hashtags;
    private List<SectionDTO> sections;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /**
     * Create a detail DTO from an IeltsTest entity.
     */
    public static TestDetailDTO fromEntity(com.cramer.entity.IeltsTest entity) {
        if (entity == null)
            return null;

        List<HashtagDTO> hashtagDTOs = null;
        if (entity.getHashtags() != null) {
            hashtagDTOs = entity.getHashtags().stream()
                    .map(HashtagDTO::fromEntity)
                    .collect(Collectors.toList());
        }

        List<SectionDTO> sectionDTOs = null;
        if (entity.getSections() != null) {
            sectionDTOs = entity.getSections().stream()
                    .map(s -> new SectionDTO(
                            s.getId(),
                            s.getExamSource(),
                            s.getTestNumber(),
                            s.getSkill(),
                            s.getPartNumber(),
                            s.getDisplayContentUrl(),
                            s.getSectionLayout(),
                            s.getPassageText(),
                            s.getAudioUrl(),
                            s.getImageDescription()))
                    .collect(Collectors.toList());
        }

        String setName = null;
        if (entity.getTestSet() != null) {
            setName = entity.getTestSet().getName();
        }

        return TestDetailDTO.builder()
                .id(entity.getId())
                .setId(entity.getSetId())
                .setCode(entity.getSetCode())
                .setName(setName)
                .testNumber(entity.getTestNumber())
                .name(entity.getName())
                .description(entity.getDescription())
                .difficulty(entity.getDifficulty())
                .estimatedTimeMinutes(entity.getEstimatedTimeMinutes())
                .isPublished(entity.getIsPublished())
                .isAiGenerated(entity.getIsAiGenerated())
                .generationMetadata(entity.getGenerationMetadata())
                .hashtags(hashtagDTOs)
                .sections(sectionDTOs)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
