package com.cramer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TargetDTO(
    @NotBlank(message = "Exam name is required")
    @Size(max = 100, message = "Exam name must be at most 100 characters")
    String examName,
    
    LocalDate examDate,
    
    @Min(value = 0, message = "Band score must be at least 0")
    @Max(value = 9, message = "Band score must be at most 9")
    Double listening,
    
    @Min(value = 0, message = "Band score must be at least 0")
    @Max(value = 9, message = "Band score must be at most 9")
    Double reading,
    
    @Min(value = 0, message = "Band score must be at least 0")
    @Max(value = 9, message = "Band score must be at most 9")
    Double writing,
    
    @Min(value = 0, message = "Band score must be at least 0")
    @Max(value = 9, message = "Band score must be at most 9")
    Double speaking
) {}