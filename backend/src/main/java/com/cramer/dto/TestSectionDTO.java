package com.cramer.dto;

import java.util.List;

/**
 * A DTO that represents a safe test section for test taking.
 * Includes the section details and safe questions (without answers).
 */
public class TestSectionDTO extends SectionDTO {

    private List<TestQuestionDTO> questions;

    public TestSectionDTO() {
        super();
    }

    public TestSectionDTO(SectionDTO sectionDTO, List<TestQuestionDTO> questions) {
        super(
            sectionDTO.getId(),
            sectionDTO.getExamSource(),
            sectionDTO.getTestNumber(),
            sectionDTO.getSkill(),
            sectionDTO.getPartNumber(),
            sectionDTO.getDisplayContentUrl(),
            sectionDTO.getSectionLayout(),
            sectionDTO.getPassageText(),
            sectionDTO.getAudioUrl()
        );
        this.questions = questions;
    }

    public List<TestQuestionDTO> getQuestions() {
        return questions;
    }

    public void setQuestions(List<TestQuestionDTO> questions) {
        this.questions = questions;
    }
}
