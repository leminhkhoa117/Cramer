package com.cramer.dto.testhierarchy;

import com.cramer.dto.SectionDTO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for IeltsTest entity with full details including sections.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestDetailDTO {
    private Long id;
    private Long setId;
    private String setCode;
    private String setName;
    private Integer testNumber;
    private String name;
    private String description;
    private String difficulty;
    private Integer estimatedTimeMinutes;
    private Boolean isPublished;
    private Boolean isAiGenerated;
    private JsonNode generationMetadata;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Section counts by skill
    private Map<String, Long> skillSectionCounts;

    // Hashtags
    private List<HashtagDTO> hashtags;

    // Sections grouped by skill
    private Map<String, List<SectionDTO>> sectionsBySkill;
}
