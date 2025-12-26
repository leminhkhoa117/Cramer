package com.cramer.dto.testhierarchy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for creating/updating a Hashtag.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateHashtagRequest {
    
    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code must be at most 50 characters")
    private String code;
    
    @NotBlank(message = "Vietnamese name is required")
    @Size(max = 100, message = "Vietnamese name must be at most 100 characters")
    private String nameVi;
    
    @Size(max = 100, message = "English name must be at most 100 characters")
    private String nameEn;
    
    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category must be at most 50 characters")
    private String category; // 'topic', 'theme', 'difficulty'
    
    @Size(max = 10, message = "Icon must be at most 10 characters")
    private String icon;
    
    @Size(max = 20, message = "Color must be at most 20 characters")
    private String color;
}
