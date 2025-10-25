package com.cramer.util;

import com.cramer.dto.*;
import com.cramer.entity.*;
import com.cramer.service.UserAnswerService;

/**
 * Utility class for mapping between Entity and DTO objects.
 */
public class EntityMapper {

    /**
     * Convert Profile entity to DTO.
     */
    public static ProfileDTO toDTO(Profile profile) {
        if (profile == null) return null;
        return new ProfileDTO(
                profile.getId(),
                profile.getUsername(),
                profile.getCreatedAt()
        );
    }

    /**
     * Convert ProfileDTO to entity.
     */
    public static Profile toEntity(ProfileDTO dto) {
        if (dto == null) return null;
        Profile profile = new Profile();
        profile.setId(dto.getId());
        profile.setUsername(dto.getUsername());
        profile.setCreatedAt(dto.getCreatedAt());
        return profile;
    }

    /**
     * Convert Section entity to DTO.
     */
    public static SectionDTO toDTO(Section section) {
        if (section == null) return null;
        return new SectionDTO(
                section.getId(),
                section.getExamSource(),
                section.getTestNumber(),
                section.getSkill(),
                section.getPartNumber(),
                section.getDisplayContentUrl(),
                section.getPassageText()
        );
    }

    /**
     * Convert SectionDTO to entity.
     */
    public static Section toEntity(SectionDTO dto) {
        if (dto == null) return null;
        Section section = new Section();
        section.setId(dto.getId());
        section.setExamSource(dto.getExamSource());
        section.setTestNumber(dto.getTestNumber());
        section.setSkill(dto.getSkill());
        section.setPartNumber(dto.getPartNumber());
        section.setDisplayContentUrl(dto.getDisplayContentUrl());
        section.setPassageText(dto.getPassageText());
        return section;
    }

    /**
     * Convert Question entity to DTO.
     */
    public static QuestionDTO toDTO(Question question) {
        if (question == null) return null;
        return new QuestionDTO(
                question.getId(),
                question.getSectionId(),
                question.getQuestionNumber(),
                question.getQuestionUid(),
                question.getQuestionType(),
                question.getQuestionContent(),
                question.getCorrectAnswer()
        );
    }

    /**
     * Convert QuestionDTO to entity.
     */
    public static Question toEntity(QuestionDTO dto) {
        if (dto == null) return null;
        Question question = new Question();
        question.setId(dto.getId());
        question.setSectionId(dto.getSectionId());
        question.setQuestionNumber(dto.getQuestionNumber());
        question.setQuestionUid(dto.getQuestionUid());
        question.setQuestionType(dto.getQuestionType());
        question.setQuestionContent(dto.getQuestionContent());
        question.setCorrectAnswer(dto.getCorrectAnswer());
        return question;
    }

    /**
     * Convert UserAnswer entity to DTO.
     */
    public static UserAnswerDTO toDTO(UserAnswer userAnswer) {
        if (userAnswer == null) return null;
        return new UserAnswerDTO(
                userAnswer.getId(),
                userAnswer.getUserId(),
                userAnswer.getQuestionId(),
                userAnswer.getUserAnswer(),
                userAnswer.getSubmittedAt(),
                userAnswer.getIsCorrect(),
                userAnswer.getCreatedAt()
        );
    }

    /**
     * Convert UserAnswerDTO to entity.
     */
    public static UserAnswer toEntity(UserAnswerDTO dto) {
        if (dto == null) return null;
        UserAnswer userAnswer = new UserAnswer();
        userAnswer.setId(dto.getId());
        userAnswer.setUserId(dto.getUserId());
        userAnswer.setQuestionId(dto.getQuestionId());
        userAnswer.setUserAnswer(dto.getUserAnswer());
        userAnswer.setSubmittedAt(dto.getSubmittedAt());
        userAnswer.setIsCorrect(dto.getIsCorrect());
        userAnswer.setCreatedAt(dto.getCreatedAt());
        return userAnswer;
    }

    /**
     * Convert UserStats to DTO.
     */
    public static UserStatsDTO toDTO(UserAnswerService.UserStats stats) {
        if (stats == null) return null;
        return new UserStatsDTO(
                stats.getTotalAnswers(),
                stats.getCorrectAnswers(),
                stats.getIncorrectAnswers(),
                stats.getAccuracy()
        );
    }
}
