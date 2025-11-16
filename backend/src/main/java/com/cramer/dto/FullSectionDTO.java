package com.cramer.dto;

import java.util.List;

/**
 * A DTO that represents a full section, including the section details
 * and all its associated questions.
 */
public class FullSectionDTO extends SectionDTO {

    private List<QuestionDTO> questions;

    public FullSectionDTO() {
        super();
    }

    public FullSectionDTO(SectionDTO sectionDTO, List<QuestionDTO> questions) {
        super(
            sectionDTO.getId(),
            sectionDTO.getExamSource(),
            sectionDTO.getTestNumber(),
            sectionDTO.getSkill(),
            sectionDTO.getPartNumber(),
            sectionDTO.getDisplayContentUrl(),
            sectionDTO.getSectionLayout(), // Pass the new field
            sectionDTO.getPassageText(),
            sectionDTO.getAudioUrl()
        );
        this.questions = questions;
    }

    public List<QuestionDTO> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionDTO> questions) {
        this.questions = questions;
    }
}
