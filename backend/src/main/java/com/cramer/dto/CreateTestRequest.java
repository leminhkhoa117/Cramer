package com.cramer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for creating a new IeltsTest within a TestSet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTestRequest {
    
    @NotNull(message = "Test number is required")
    @Min(value = 1, message = "Test number must be at least 1")
    @Max(value = 999, message = "Test number must be at most 999")
    private Integer testNumber;
    
    @Size(max = 255, message = "Vietnamese name must be at most 255 characters")
    private String nameVi;
    
    @Size(max = 255, message = "English name must be at most 255 characters")
    private String nameEn;
    
    private String description;
    
    @Builder.Default
    private String difficulty = "INTERMEDIATE";
    
    @Builder.Default
    private Integer estimatedTimeMinutes = 170;
    
    @Builder.Default
    private Boolean isPublished = false;
    
    // List of hashtag IDs to associate with this test
    private List<Long> hashtagIds;
}
