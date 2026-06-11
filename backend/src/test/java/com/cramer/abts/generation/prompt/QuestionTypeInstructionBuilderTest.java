package com.cramer.abts.generation.prompt;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionTypeInstructionBuilderTest {

    private final QuestionTypeInstructionBuilder builder = new QuestionTypeInstructionBuilder();

    @Test
    void emitsSpecificRulesForKnownTypes() {
        String out = builder.instructionsFor(List.of("MULTIPLE_CHOICE_MULTIPLE_ANSWERS", "TRUE_FALSE_NOT_GIVEN"));
        assertThat(out).contains("array of the correct letters");
        assertThat(out).contains("TRUE, FALSE, NOT GIVEN");
    }

    @Test
    void tableCompletionPutsHtmlOnFirstQuestion() {
        assertThat(builder.ruleFor("TABLE_COMPLETION")).contains("FIRST question");
    }

    @Test
    void unknownTypeFallsBackToGenericRule() {
        assertThat(builder.ruleFor("SOMETHING_NEW")).contains("authentic IELTS conventions");
    }

    @Test
    void emptyListProducesNoInstructions() {
        assertThat(builder.instructionsFor(List.of())).isEmpty();
    }
}
