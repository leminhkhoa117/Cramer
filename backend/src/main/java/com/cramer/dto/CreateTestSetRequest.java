package com.cramer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new TestSet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTestSetRequest {
    
    @NotBlank(message = "Test set code is required")
    @Size(max = 50, message = "Code must be at most 50 characters")
    private String code;
    
    @NotBlank(message = "Vietnamese name is required")
    @Size(max = 255, message = "Vietnamese name must be at most 255 characters")
    private String name;
    
    @Size(max = 255, message = "English name must be at most 255 characters")
    // removed duplicate nameEn
    
    private String description;
    
    @Size(max = 500, message = "Cover image URL must be at most 500 characters")
    private String coverImageUrl;
    
    @Builder.Default
    private String sourceType = "custom";
    
    @Builder.Default
    private Boolean isPublished = false;
    
    @Builder.Default
    private Integer displayOrder = 0;
}
