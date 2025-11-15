package com.cramer.mapper;

import com.cramer.dto.FullSectionDTO;
import com.cramer.dto.QuestionDTO;
import com.cramer.dto.SectionDTO;
import com.cramer.entity.Question;
import com.cramer.entity.Section;

import java.util.List;
import java.util.stream.Collectors;

public class TestMapper {

    public static QuestionDTO toQuestionDTO(Question question, boolean includeAnswer) {
        if (question == null) {
            return null;
        }
        return new QuestionDTO(
                question.getId(),
                question.getSectionId(),
                question.getQuestionNumber(),
                question.getQuestionUid(),
                question.getQuestionType(),
                question.getQuestionContent(),
                includeAnswer ? question.getCorrectAnswer() : null, // Strip out correct answer
                question.getWordLimit(),
                question.getImageUrl()
        );
    }

    public static SectionDTO toSectionDTO(Section section) {
        if (section == null) {
            return null;
        }
        return new SectionDTO(
                section.getId(),
                section.getExamSource(),
                section.getTestNumber(),
                section.getSkill(),
                section.getPartNumber(),
                section.getDisplayContentUrl(),
                section.getPassageText(),
                section.getAudioUrl()
        );
    }

    public static FullSectionDTO toFullSectionDTO(Section section, List<Question> questions) {
        if (section == null) {
            return null;
        }
        SectionDTO sectionDTO = toSectionDTO(section);
        List<QuestionDTO> questionDTOs = questions.stream()
                .map(q -> toQuestionDTO(q, false)) // Ensure answers are NOT included
                .collect(Collectors.toList());

        return new FullSectionDTO(sectionDTO, questionDTOs);
    }
}
