package com.cramer.service.abts;

import com.cramer.dto.abts.GeneratedContentDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class GeneratedContentParser {

    private static final Logger logger = LoggerFactory.getLogger(GeneratedContentParser.class);

    private final ObjectMapper objectMapper;

    GeneratedContentParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    GeneratedContentDTO parseGeneratedContent(String jsonContent) throws Exception {
        JsonNode root = objectMapper.readTree(jsonContent);
        GeneratedContentDTO content = new GeneratedContentDTO();

        if (root.has("section")) {
            JsonNode sectionNode = root.get("section");
            GeneratedContentDTO.GeneratedSectionDTO section = new GeneratedContentDTO.GeneratedSectionDTO();

            if (sectionNode.has("passage_text")) {
                section.setPassageText(sectionNode.get("passage_text").asText());
                section.setWordCount(JsonValidationSupport.countWords(section.getPassageText()));
            }
            if (sectionNode.has("word_count_valid")) {
                section.setWordCountValid(sectionNode.get("word_count_valid").asBoolean());
            }

            content.setSection(section);
        } else if (root.has("transcript")) {
            GeneratedContentDTO.GeneratedSectionDTO section = new GeneratedContentDTO.GeneratedSectionDTO();
            section.setPassageText(root.get("transcript").asText());
            section.setWordCount(JsonValidationSupport.countWords(section.getPassageText()));

            if (root.has("section_layout")) {
                section.setSectionLayout(root.get("section_layout"));
            }

            content.setSection(section);
        }

        if (root.has("task_prompt")) {
            GeneratedContentDTO.GeneratedSectionDTO section = content.getSection();
            if (section == null) {
                section = new GeneratedContentDTO.GeneratedSectionDTO();
                content.setSection(section);
            }
            String taskPrompt = root.get("task_prompt").asText();
            section.setTaskText(taskPrompt);
            if (section.getPassageText() == null || section.getPassageText().isBlank()) {
                section.setPassageText(taskPrompt);
                section.setWordCount(JsonValidationSupport.countWords(taskPrompt));
            }
        }

        // FIX 1: Writing Phase 2 (sample answer) + Phase 3 (band breakdown) outputs.
        // The runner merges these onto the root; map them onto the section so they
        // are not silently discarded.
        if (content.getSection() != null) {
            GeneratedContentDTO.GeneratedSectionDTO writingSection = content.getSection();

            if (root.has("sample_answer") && root.get("sample_answer").isObject()) {
                JsonNode sampleNode = root.get("sample_answer");
                if (sampleNode.has("content")) {
                    String sampleContent = sampleNode.get("content").asText();
                    writingSection.setSampleAnswerContent(sampleContent);
                    if (sampleNode.has("word_count")) {
                        writingSection.setSampleAnswerWordCount(sampleNode.get("word_count").asInt());
                    } else if (sampleContent != null) {
                        writingSection.setSampleAnswerWordCount(JsonValidationSupport.countWords(sampleContent));
                    }
                }
                if (sampleNode.has("band_score")) {
                    writingSection.setSampleAnswerBandScore(sampleNode.get("band_score").asDouble());
                }
            }

            if (root.has("band_breakdown") && root.get("band_breakdown").isObject()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> breakdown = objectMapper.convertValue(root.get("band_breakdown"), Map.class);
                writingSection.setBandBreakdown(breakdown != null ? breakdown : new LinkedHashMap<>());
            }

            if (root.has("key_phrases") && root.get("key_phrases").isArray()) {
                List<String> keyPhrases = new ArrayList<>();
                for (JsonNode phrase : root.get("key_phrases")) {
                    keyPhrases.add(phrase.asText());
                }
                writingSection.setKeyPhrases(keyPhrases);
            }

            if (root.has("grading_notes")) {
                writingSection.setGradingNotes(root.get("grading_notes").asText());
            }
        }

        if (root.has("section_layout") && content.getSection() != null) {
            content.getSection().setSectionLayout(root.get("section_layout"));
        }

        if (root.has("questions") && root.get("questions").isArray()) {
            List<GeneratedContentDTO.GeneratedQuestionDTO> questions = new ArrayList<>();

            for (JsonNode qNode : root.get("questions")) {
                GeneratedContentDTO.GeneratedQuestionDTO question = new GeneratedContentDTO.GeneratedQuestionDTO();

                if (qNode.has("question_number")) {
                    question.setQuestionNumber(qNode.get("question_number").asInt());
                }
                if (qNode.has("question_type")) {
                    question.setQuestionType(qNode.get("question_type").asText());
                }
                if (qNode.has("question_content")) {
                    question.setQuestionContent(qNode.get("question_content"));
                }
                if (qNode.has("correct_answer") && qNode.get("correct_answer").isArray()) {
                    List<String> answers = new ArrayList<>();
                    for (JsonNode ans : qNode.get("correct_answer")) {
                        answers.add(ans.asText());
                    }
                    question.setCorrectAnswer(answers);
                }
                if (qNode.has("explanation")) {
                    question.setExplanation(qNode.get("explanation"));
                }
                if (qNode.has("word_limit") && !qNode.get("word_limit").isNull()) {
                    question.setWordLimit(qNode.get("word_limit").asText());
                }
                if (qNode.has("image_url") && !qNode.get("image_url").isNull()) {
                    question.setImageUrl(qNode.get("image_url").asText());
                } else if (qNode.has("imageUrl") && !qNode.get("imageUrl").isNull()) {
                    question.setImageUrl(qNode.get("imageUrl").asText());
                }

                questions.add(question);
            }

            content.setQuestions(questions);
        }

        if (root.has("chart_data")) {
            content.setChartData(root.get("chart_data"));
        }

        if (root.has("task_type")) {
            content.setTaskType(root.get("task_type").asText());
        }
        if (root.has("word_requirement")) {
            content.setWordRequirement(root.get("word_requirement").asInt());
        }
        if (root.has("letter_context")) {
            content.setLetterContext(root.get("letter_context"));
        }
        if (root.has("essay_metadata")) {
            content.setEssayMetadata(root.get("essay_metadata"));
        }

        if (root.has("figure_description")) {
            content.setFigureDescription(root.get("figure_description"));
        }

        if (root.has("audio_placeholder")) {
            JsonNode audioNode = root.get("audio_placeholder");
            GeneratedContentDTO.AudioPlaceholderDTO audio = new GeneratedContentDTO.AudioPlaceholderDTO();

            if (audioNode.has("duration_estimate")) {
                audio.setDurationEstimate(audioNode.get("duration_estimate").asText());
            }
            if (audioNode.has("speaker_count")) {
                audio.setSpeakerCount(audioNode.get("speaker_count").asInt());
            }
            if (audioNode.has("speaker_genders") && audioNode.get("speaker_genders").isArray()) {
                List<String> genders = new ArrayList<>();
                for (JsonNode gender : audioNode.get("speaker_genders")) {
                    genders.add(gender.asText());
                }
                audio.setSpeakerGenders(genders);
            }
            if (audioNode.has("accent_recommendation")) {
                audio.setAccentRecommendation(audioNode.get("accent_recommendation").asText());
            }
            if (audioNode.has("pacing_notes")) {
                audio.setPacingNotes(audioNode.get("pacing_notes").asText());
            }
            if (audioNode.has("background_ambient")) {
                audio.setBackgroundAmbient(audioNode.get("background_ambient").asText());
            }
            if (audioNode.has("tts_ready")) {
                audio.setTtsReady(audioNode.get("tts_ready").asBoolean());
            }

            content.setAudioPlaceholder(audio);
        }

        return content;
    }

    GeneratedContentDTO renumberQuestionsForReadingPart(GeneratedContentDTO content, int partNumber) {
        if (content == null || content.getQuestions() == null || content.getQuestions().isEmpty()) {
            return content;
        }

        List<GeneratedContentDTO.GeneratedQuestionDTO> questions = content.getQuestions();
        int firstQuestionNumber = questions.get(0).getQuestionNumber();

        int expectedStart;
        int offset;
        switch (partNumber) {
            case 2:
                expectedStart = 14;
                offset = 13;
                break;
            case 3:
                expectedStart = 27;
                offset = 26;
                break;
            default:
                return content;
        }

        if (firstQuestionNumber == 1) {
            logger.info("Renumbering questions for Part {} - adding offset {}", partNumber, offset);
            for (GeneratedContentDTO.GeneratedQuestionDTO question : questions) {
                question.setQuestionNumber(question.getQuestionNumber() + offset);
            }
        } else if (firstQuestionNumber != expectedStart) {
            logger.warn("Part {} questions start at {} (expected {}), not renumbering",
                    partNumber, firstQuestionNumber, expectedStart);
        }

        return content;
    }
}