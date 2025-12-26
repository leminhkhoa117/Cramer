package com.cramer.dto.testhierarchy;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

/**
 * Request DTO for creating/updating an IeltsTest.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTestRequest {
    
    @Min(value = 1, message = "Test number must be at least 1")
    private Integer testNumber; // Optional - auto-generated if not provided
    
    @Size(max = 255, message = "Vietnamese name must be at most 255 characters")
    private String nameVi;
    
    @Size(max = 255, message = "English name must be at most 255 characters")
    private String nameEn;
    
    @Size(max = 5000, message = "Description must be at most 5000 characters")
    private String description;
    
    @Size(max = 30, message = "Difficulty must be at most 30 characters")
    private String difficulty; // BEGINNER, INTERMEDIATE, ADVANCED
    
    private Integer estimatedTimeMinutes;
    
    private Boolean isPublished;
    
    private Boolean isAiGenerated;
    
    private JsonNode generationMetadata;
    
    // Hashtag codes to associate with this test
    private List<String> hashtagCodes;
}
