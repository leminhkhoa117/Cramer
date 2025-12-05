package com.cramer.dto;

import java.util.Map;

/**
 * DTO for submitting writing essays.
 */
public class WritingSubmitDTO {
    
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
