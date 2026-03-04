package com.cramer.dto.testhierarchy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for creating/updating a TestSet.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTestSetRequest {

    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code must be at most 50 characters")
    private String code;

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    private String name;

    @Size(max = 5000, message = "Description must be at most 5000 characters")
    private String description;

    @Size(max = 500, message = "Cover image URL must be at most 500 characters")
    private String coverImageUrl;

    @Size(max = 50, message = "Source type must be at most 50 characters")
    @Builder.Default
    private String sourceType = "custom";

    private Boolean isPublished;

    private Integer displayOrder;
}
