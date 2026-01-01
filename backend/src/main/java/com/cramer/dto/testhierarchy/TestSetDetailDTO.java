package com.cramer.dto.testhierarchy;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO for TestSet entity with detailed info including tests list.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestSetDetailDTO {
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

    // Nested tests
    private List<TestSummaryDTO> tests;
}
