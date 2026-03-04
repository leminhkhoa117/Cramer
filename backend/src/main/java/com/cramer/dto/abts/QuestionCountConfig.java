package com.cramer.dto.abts;

import com.cramer.dto.abts.GenerationRequestDTO.SkillType;

/**
 * Configuration for question counts and ranges per skill/part.
 * Centralizes IELTS question numbering rules.
 * 
 * @since 2026-01-03 - Multi-part generation enhancement
 */
public class QuestionCountConfig {

    /**
     * Reading: 40 total (13 + 13 + 14)
     */
    private static final int[] READING_COUNTS = { 13, 13, 14 };

    /**
     * Listening: 40 total (10 + 10 + 10 + 10)
     */
    private static final int[] LISTENING_COUNTS = { 10, 10, 10, 10 };

    /**
     * Writing: 2 tasks (no question numbering)
     */
    private static final int[] WRITING_COUNTS = { 1, 1 };

    /**
     * Get the number of questions for a specific part of a skill.
     * 
     * @param skill      the skill type (READING, LISTENING, WRITING)
     * @param partNumber 1-based part number
     * @return number of questions for that part
     */
    public static int getQuestionCount(SkillType skill, int partNumber) {
        int index = partNumber - 1;
        return switch (skill) {
            case READING -> (index >= 0 && index < READING_COUNTS.length)
                    ? READING_COUNTS[index]
                    : 0;
            case LISTENING -> (index >= 0 && index < LISTENING_COUNTS.length)
                    ? LISTENING_COUNTS[index]
                    : 0;
            case WRITING -> (index >= 0 && index < WRITING_COUNTS.length)
                    ? WRITING_COUNTS[index]
                    : 0;
            default -> 0;
        };
    }

    /**
     * Get the starting question number for a specific part.
     * 
     * @param skill      the skill type
     * @param partNumber 1-based part number
     * @return 1-based starting question number
     */
    public static int getStartQuestionNumber(SkillType skill, int partNumber) {
        return switch (skill) {
            case READING -> switch (partNumber) {
                case 1 -> 1; // Q1-13
                case 2 -> 14; // Q14-26
                case 3 -> 27; // Q27-40
                default -> 1;
            };
            case LISTENING -> (partNumber - 1) * 10 + 1; // Q1, Q11, Q21, Q31
            case WRITING -> 1; // Writing uses task numbers, not question numbers
            default -> 1;
        };
    }

    /**
     * Get the ending question number for a specific part.
     * 
     * @param skill      the skill type
     * @param partNumber 1-based part number
     * @return 1-based ending question number
     */
    public static int getEndQuestionNumber(SkillType skill, int partNumber) {
        return getStartQuestionNumber(skill, partNumber)
                + getQuestionCount(skill, partNumber) - 1;
    }

    /**
     * Get the number of parts for a skill.
     * 
     * @param skill the skill type
     * @return number of parts (3 for Reading, 4 for Listening, 2 for Writing)
     */
    public static int getPartCount(SkillType skill) {
        return switch (skill) {
            case READING -> 3;
            case LISTENING -> 4;
            case WRITING -> 2;
            default -> 1;
        };
    }

    /**
     * Get total questions for a skill.
     * 
     * @param skill the skill type
     * @return total questions (40 for Reading/Listening, 2 for Writing)
     */
    public static int getTotalQuestions(SkillType skill) {
        return switch (skill) {
            case READING, LISTENING -> 40;
            case WRITING -> 2;
            default -> 0;
        };
    }
}
