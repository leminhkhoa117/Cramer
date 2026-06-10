package com.cramer.service.unit;

import com.cramer.dto.abts.GenerationRequestDTO;
import com.cramer.service.abts.PromptBuilderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PromptBuilderService characterization tests")
class PromptBuilderServiceTest {

    private PromptBuilderService promptBuilderService;
    private GenerationRequestDTO request;

    @BeforeEach
    void setUp() {
        promptBuilderService = new PromptBuilderService();
        request = new GenerationRequestDTO();
        request.setSkill(GenerationRequestDTO.SkillType.READING);
        request.setScope(GenerationRequestDTO.GenerationScope.SINGLE_PART);
        request.setTopic("Urban transport and sustainability");
        request.setDifficulty(GenerationRequestDTO.DifficultyLevel.INTERMEDIATE);
        request.setExplanationLanguage(GenerationRequestDTO.ExplanationLanguage.VI);
        request.setTestType(GenerationRequestDTO.TestType.ACADEMIC);
        request.setPartNumber(2);
        request.setFacts(List.of("Electric buses reduce local emissions", "Ticket integration improves access"));
        request.setQuestionTypes(List.of("MATCHING_INFORMATION", "FILL_IN_BLANK"));
        request.setQuestionTypeCounts(Map.of("MATCHING_INFORMATION", 6, "FILL_IN_BLANK", 7));
    }

    @Test
    @DisplayName("Reading prompt keeps critical IELTS contracts")
    void buildReadingPrompt_keepsCriticalContracts() {
        String prompt = promptBuilderService.buildReadingPrompt(request);

        assertThat(prompt)
                .contains("Generate IELTS Academic Reading Passage with Questions")
                .contains("Part 2 Specifications")
                .contains("Urban transport and sustainability")
                .contains("Explanation Language")
                .contains("EXPLANATION FORMAT")
                .contains("ONE WORD ONLY")
                .contains("NO MORE THAN THREE WORDS AND/OR A NUMBER")
                .contains("Numbering")
                .contains("Q14")
                .contains("MATCHING_INFORMATION: 6")
                .contains("FILL_IN_BLANK: 7");
    }

    @Test
    @DisplayName("Reading question prompt uses provided passage and numbering")
    void buildReadingQuestionsPrompt_keepsPassageAndNumbering() {
        String prompt = promptBuilderService.buildReadingQuestionsPrompt(request, "<strong>A.</strong> Passage text");

        assertThat(prompt)
                .contains("Generate IELTS Reading Questions")
                .contains("Source Passage")
                .contains("<strong>A.</strong> Passage text")
                .contains("Start at")
                .contains("Q14")
                .contains("End at")
                .contains("Q26")
                .contains("Requested Question Types");
    }

    @Test
    @DisplayName("Listening prompt keeps transcript and question layout contracts")
    void buildListeningPrompt_keepsCriticalContracts() {
        request.setSkill(GenerationRequestDTO.SkillType.LISTENING);
        request.setPartNumber(3);
        request.setQuestionTypes(List.of("MULTIPLE_CHOICE", "MATCHING"));
        request.setQuestionTypeCounts(Map.of("MULTIPLE_CHOICE", 5, "MATCHING", 5));

        String prompt = promptBuilderService.buildListeningPrompt(request);

        assertThat(prompt)
                .contains("Generate IELTS Listening Section Content")
                .contains("Part 3")
                .contains("transcript")
                .contains("question_numbers")
                .contains("word_limit")
                .contains("MULTIPLE_CHOICE")
                .contains("MATCHING");
    }

    @Test
    @DisplayName("Writing prompt keeps task and JSON contracts")
    void buildWritingPrompt_keepsCriticalContracts() {
        request.setSkill(GenerationRequestDTO.SkillType.WRITING);
        request.setPartNumber(2);
        request.setWritingEssayType("OPINION");

        String prompt = promptBuilderService.buildWritingPrompt(request);

        assertThat(prompt)
                .contains("Generate IELTS Writing")
                .contains("Task 2")
                .contains("OPINION")
                .contains("task_prompt")
                .contains("task_type")
                .contains("word_requirement");
    }

    @Test
    @DisplayName("Schemas expose required top-level fields")
    void jsonSchemas_keepTopLevelContracts() {
        assertThat(promptBuilderService.getReadingJsonSchema())
                .containsEntry("type", "object")
                .containsKey("properties")
                .containsKey("required");
        assertThat(promptBuilderService.getListeningJsonSchema())
                .containsEntry("type", "object")
                .containsKey("properties")
                .containsKey("required");
        assertThat(promptBuilderService.getWritingJsonSchema())
                .containsEntry("type", "object")
                .containsKey("properties")
                .containsKey("required");
    }

    @Test
    @DisplayName("Two-pass schemas keep phase-specific requirements")
    void twoPassSchemas_keepPhaseContracts() {
        assertThat(promptBuilderService.getReadingPassageSchema().get("required"))
                .isEqualTo(List.of("passage_text", "word_count"));
        assertThat(promptBuilderService.getReadingQuestionsSchema().get("required"))
                .isEqualTo(List.of("questions"));
        assertThat(promptBuilderService.getListeningTranscriptSchema().get("required"))
                .isEqualTo(List.of("transcript", "audio_placeholder"));
        assertThat(promptBuilderService.getListeningQuestionsSchema().get("required"))
                .isEqualTo(List.of("section_layout", "questions"));
    }

    @Test
    @DisplayName("Question type instructions are available for known types")
    void questionTypeInstructions_knownTypesHaveGuidance() {
        assertThat(promptBuilderService.getQuestionTypeInstructions("MULTIPLE_CHOICE"))
                .contains("Standard question format")
                .contains("MULTIPLE_CHOICE");
        assertThat(promptBuilderService.getListeningQuestionTypeInstructions("FORM_COMPLETION", 1))
                .contains("Fill in the Blank / Completion")
                .contains("word_limit");
    }
}