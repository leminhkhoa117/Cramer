package com.cramer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * DTO for submitting writing essays.
 */
public class WritingSubmitDTO {
    
    @NotNull(message = "Essays map is required")
    @Size(min = 1, max = 2, message = "Must have 1 or 2 essays")
    private Map<Integer, String> essays; // Map<taskNumber, essayText>

    public WritingSubmitDTO() {}

    public WritingSubmitDTO(Map<Integer, String> essays) {
        this.essays = essays;
    }

    public Map<Integer, String> getEssays() {
        return essays;
    }

    public void setEssays(Map<Integer, String> essays) {
        this.essays = essays;
    }
}
