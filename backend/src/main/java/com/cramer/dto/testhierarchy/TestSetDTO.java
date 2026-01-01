package com.cramer.dto.testhierarchy;

import lombok.*;
import java.time.OffsetDateTime;

/**
 * DTO for TestSet entity - basic info for list views.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestSetDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String coverImageUrl;
    private String sourceType;
    private Boolean isPublished;
    private Integer displayOrder;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Computed counts
    private Long testCount;
    private Long publishedTestCount;
}
