package com.cramer.abts.generation.prompt;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Single source of per-question-type authoring rules (SPEC-22 §2, §5.2). All skill builders
 * reference this instead of embedding inline instructions (the old code's duplication bug).
 */
@Component
public class QuestionTypeInstructionBuilder {

    /** Authoring rules for the requested types, concatenated. Unknown types get a generic note. */
    public String instructionsFor(List<String> questionTypes) {
        if (questionTypes == null || questionTypes.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("Question-type authoring rules:\n");
        for (String type : questionTypes) {
            sb.append("- ").append(type.toUpperCase()).append(": ").append(ruleFor(type)).append('\n');
        }
        return sb.toString();
    }

    public String ruleFor(String type) {
        return switch (type == null ? "" : type.trim().toUpperCase()) {
            case "FILL_IN_BLANK" ->
                    "`question_content.text` holds the sentence with a `____` blank; `correct_answer` lists acceptable fillers; set `word_limit`.";
            case "SUMMARY_COMPLETION" ->
                    "A summary paragraph with `____` blanks; one question per blank; `correct_answer` is the exact word(s) from the text; set `word_limit`.";
            case "SUMMARY_COMPLETION_OPTIONS" ->
                    "Summary blanks filled from a provided option bank; `question_content.options` lists lettered choices; `correct_answer` is the chosen letter.";
            case "TRUE_FALSE_NOT_GIVEN" ->
                    "Statement to assess; `correct_answer` is exactly one of TRUE, FALSE, NOT GIVEN.";
            case "YES_NO_NOT_GIVEN" ->
                    "Claim about the writer's views; `correct_answer` is exactly one of YES, NO, NOT GIVEN.";
            case "MATCHING_INFORMATION" ->
                    "Match a statement to the paragraph (A, B, C, ...) containing it; `correct_answer` is the paragraph letter.";
            case "MATCHING_HEADINGS" ->
                    "Match a heading (roman numeral i, ii, iii, ...) to a paragraph; `question_content.options` lists headings; `correct_answer` is the numeral.";
            case "MATCHING_FEATURES" ->
                    "Match items to features from a shared option list; `question_content.options` lists features; `correct_answer` is the letter.";
            case "MATCHING_SENTENCE_ENDINGS" ->
                    "Complete a sentence with an ending from a shared list; `question_content.options` lists endings; `correct_answer` is the letter.";
            case "MULTIPLE_CHOICE" ->
                    "`question_content.options` lists choices (A, B, C, D); `correct_answer` is exactly one letter.";
            case "MULTIPLE_CHOICE_MULTIPLE_ANSWERS" ->
                    "`question_content.options` lists choices; `correct_answer` is an array of the correct letters (graded as a set).";
            case "TABLE_COMPLETION" ->
                    "The FIRST question of the group carries the full table HTML in `question_content.text`; later group questions use empty text; each is a blank; set `word_limit`.";
            case "FLOW_CHART_COMPLETION" ->
                    "The FIRST question of the group carries the full flow-chart HTML in `question_content.text`; later group questions use empty text; set `word_limit`.";
            case "DIAGRAM_LABEL_COMPLETION" ->
                    "Label a diagram; `image_url`/figure description provides the figure; `correct_answer` lists the label text; set `word_limit`.";
            case "MATCHING" ->
                    "Listening matching: match a prompt to a shared option set; `question_content.options` lists choices; `correct_answer` is the letter.";
            default -> "Author this type using authentic IELTS conventions; provide options where applicable.";
        };
    }
}
