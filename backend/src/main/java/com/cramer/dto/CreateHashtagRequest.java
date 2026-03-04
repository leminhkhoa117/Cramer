package com.cramer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new Hashtag.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateHashtagRequest {
    
    @NotBlank(message = "Hashtag code is required")
    @Size(max = 50, message = "Code must be at most 50 characters")
    private String code;
    
    @NotBlank(message = "Vietnamese name is required")
    @Size(max = 100, message = "Vietnamese name must be at most 100 characters")
    private String name;
    
    @Size(max = 100, message = "English name must be at most 100 characters")
    // removed duplicate nameEn
    
    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category must be at most 50 characters")
    private String category;
    
    @Size(max = 10, message = "Icon must be at most 10 characters")
    private String icon;
    
    @Size(max = 20, message = "Color must be at most 20 characters")
    private String color;
}
