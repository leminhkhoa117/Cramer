package com.cramer.abts.domain;

import com.cramer.platform.common.ielts.Skill;

/**
 * Canonical question-number range for a Reading/Listening part. Single source of truth for
 * numbering and multi-part renumbering (SPEC-20 §4.1, SPEC-21 §4). Writing has no numbering.
 *
 * @param first first question number (inclusive)
 * @param last  last question number (inclusive)
 */
public record QuestionRange(int first, int last) {

    public QuestionRange {
        if (first < 1 || last < first) {
            throw new IllegalArgumentException("invalid range " + first + ".." + last);
        }
    }

    public int count() {
        return last - first + 1;
    }

    public boolean contains(int questionNumber) {
        return questionNumber >= first && questionNumber <= last;
    }

    /**
     * @param skill READING or LISTENING (WRITING/SPEAKING are not number-ranged)
     * @param part  1-based part number
     */
    public static QuestionRange of(Skill skill, int part) {
        return switch (skill) {
            case READING -> switch (part) {
                case 1 -> new QuestionRange(1, 13);
                case 2 -> new QuestionRange(14, 26);
                case 3 -> new QuestionRange(27, 40);
                default -> throw new IllegalArgumentException("Reading has parts 1..3, got " + part);
            };
            case LISTENING -> switch (part) {
                case 1 -> new QuestionRange(1, 10);
                case 2 -> new QuestionRange(11, 20);
                case 3 -> new QuestionRange(21, 30);
                case 4 -> new QuestionRange(31, 40);
                default -> throw new IllegalArgumentException("Listening has parts 1..4, got " + part);
            };
            case WRITING, SPEAKING ->
                throw new IllegalArgumentException(skill + " is not question-number ranged");
        };
    }
}
