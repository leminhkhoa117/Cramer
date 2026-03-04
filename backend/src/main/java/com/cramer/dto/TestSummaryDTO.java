package com.cramer.dto;

import com.cramer.dto.testhierarchy.HashtagDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DTO for IeltsTest entity - lightweight summary view.
 * Used when listing tests without full section details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestSummaryDTO {
    
    private Long id;
    private Long setId;
    private String setCode;
    private Integer testNumber;
    private String name;
    // removed duplicate nameEn
    private String difficulty;
    private Boolean isPublished;
    private Boolean isAiGenerated;
    
    // Computed fields
    private Integer sectionCount;
    private Map<String, Integer> skillSectionCounts; // {reading: 3, listening: 4, writing: 2}
    
    // Related hashtags
    private List<HashtagDTO> hashtags;
    
    private OffsetDateTime createdAt;

    /**
     * Create a summary DTO from an IeltsTest entity.
     */
    public static TestSummaryDTO fromEntity(com.cramer.entity.IeltsTest entity) {
        if (entity == null) return null;
        
        List<HashtagDTO> hashtagDTOs = null;
        if (entity.getHashtags() != null) {
            hashtagDTOs = entity.getHashtags().stream()
                    .map(HashtagDTO::fromEntity)
                    .collect(Collectors.toList());
        }
        
        return TestSummaryDTO.builder()
                .id(entity.getId())
                .setId(entity.getSetId())
                .setCode(entity.getSetCode())
                .testNumber(entity.getTestNumber())
                .name(entity.getName())
                .name(entity.getName())
                .difficulty(entity.getDifficulty())
                .isPublished(entity.getIsPublished())
                .isAiGenerated(entity.getIsAiGenerated())
                .sectionCount(entity.getSectionCount())
                .skillSectionCounts(entity.getSkillSectionCounts())
                .hashtags(hashtagDTOs)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
