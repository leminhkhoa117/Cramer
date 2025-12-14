package com.cramer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating new vocabulary entries.
 * Contains validation constraints for input data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VocabularyCreateDTO {

    @NotBlank(message = "Word is required")
    @Size(max = 200, message = "Word must be at most 200 characters")
    private String word;

    @Size(max = 5000, message = "Translation must be at most 5000 characters")
    private String translation;

    @Size(max = 100, message = "Phonetic must be at most 100 characters")
    private String phonetic;

    @Size(max = 50, message = "Part of speech must be at most 50 characters")
    private String partOfSpeech;

    @Size(max = 2000, message = "Definition must be at most 2000 characters")
    private String definition;

    @Size(max = 2000, message = "Example sentence must be at most 2000 characters")
    private String exampleSentence;

    @Size(max = 2000, message = "Source context must be at most 2000 characters")
    private String sourceContext;

    private Long sourceTestId;

    private Long sourceSectionId;

    @Size(max = 2000, message = "Notes must be at most 2000 characters")
    private String notes;

    /**
     * Whether to automatically translate the word using AI.
     * If true, the word will be sent to DeepSeek for translation.
     */
    private Boolean autoTranslate;
}
