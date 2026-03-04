package com.cramer.util;

import com.cramer.dto.*;
import com.cramer.entity.*;

/**
 * Utility class for mapping between Entity and DTO objects.
 */
public class EntityMapper {

    /**
     * Convert Profile entity to DTO.
     */
    public static ProfileDTO toDTO(Profile profile) {
        if (profile == null) return null;
        ProfileDTO dto = new ProfileDTO();
        dto.setId(profile.getId());
        dto.setUsername(profile.getUsername());
        dto.setFullName(profile.getFullName());
        dto.setPhoneNumber(profile.getPhoneNumber());
        dto.setAddress(profile.getAddress());
        dto.setAvatarUrl(profile.getAvatarUrl());
        dto.setCreatedAt(profile.getCreatedAt());
        return dto;
    }

    /**
     * Convert ProfileDTO to entity.
     */
    public static Profile toEntity(ProfileDTO dto) {
        if (dto == null) return null;
        Profile profile = new Profile();
        profile.setId(dto.getId());
        profile.setUsername(dto.getUsername());
        profile.setFullName(dto.getFullName());
        profile.setPhoneNumber(dto.getPhoneNumber());
        profile.setAddress(dto.getAddress());
        profile.setAvatarUrl(dto.getAvatarUrl());
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
                section.getSectionLayout(),
                section.getPassageText(),
                section.getAudioUrl(),
                section.getImageDescription()
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
        section.setSectionLayout(dto.getSectionLayout());
        section.setPassageText(dto.getPassageText());
        section.setAudioUrl(dto.getAudioUrl());
        section.setImageDescription(dto.getImageDescription());
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
                question.getCorrectAnswer(),
                question.getWordLimit(),
                question.getImageUrl()
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
        question.setWordLimit(dto.getWordLimit());
        question.setImageUrl(dto.getImageUrl());
        return question;
    }

    /**
     * Convert UserAnswer entity to DTO.
     */
    public static UserAnswerDTO toDTO(UserAnswer userAnswer) {
        if (userAnswer == null) return null;
        UserAnswerDTO dto = new UserAnswerDTO(
                userAnswer.getId(),
                userAnswer.getAttempt() != null ? userAnswer.getAttempt().getUserId() : null,
                userAnswer.getQuestion() != null ? userAnswer.getQuestion().getId() : null,
                userAnswer.getAnswerContent(),
                userAnswer.getSubmittedAt(),
                userAnswer.getCorrect()
        );
        dto.setUserAnswer(userAnswer.getUserAnswer()); // Also set the plain text answer
        return dto;
    }

    /**
     * Convert UserAnswerDTO to entity.
     */
    public static UserAnswer toEntity(UserAnswerDTO dto) {
        if (dto == null) return null;
        UserAnswer userAnswer = new UserAnswer();
        userAnswer.setId(dto.getId());
        // Note: We do not map attempt and question here as they should be set by the service
        userAnswer.setAnswerContent(dto.getAnswerContent());
        userAnswer.setSubmittedAt(dto.getSubmittedAt());
        userAnswer.setCorrect(dto.isCorrect());
        return userAnswer;
    }

    // Mappings for Target
    public static TargetDTO toDTO(Target target) {
        if (target == null) {
            return null;
        }
        return new TargetDTO(
            target.getExamName(),
            target.getExamDate(),
            target.getListening(),
            target.getReading(),
            target.getWriting(),
            target.getSpeaking()
        );
    }
}
