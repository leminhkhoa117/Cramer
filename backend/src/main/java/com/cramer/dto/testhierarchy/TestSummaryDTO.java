package com.cramer.dto.testhierarchy;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for IeltsTest entity - basic info for list views.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestSummaryDTO {
    private Long id;
    private Long setId;
    private String setCode;
    private String setNameVi;
    private String setNameEn;
    private Integer testNumber;
    private String nameVi;
    private String nameEn;
    private String description;
    private String difficulty;
    private Integer estimatedTimeMinutes;
    private Boolean isPublished;
    private Boolean isAiGenerated;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    // Section counts by skill
    private Map<String, Long> skillSectionCounts;
    
    // Hashtags
    private List<HashtagDTO> hashtags;
}
