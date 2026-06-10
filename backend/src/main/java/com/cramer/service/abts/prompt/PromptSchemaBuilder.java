package com.cramer.service.abts.prompt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PromptSchemaBuilder {

        /**
         * Get JSON Schema for Pass 1 (Reading Passage).
         */
        public Map<String, Object> getReadingPassageSchema() {
                Map<String, Object> schema = new LinkedHashMap<>();
                schema.put("type", "object");
                schema.put("properties", Map.of(
                                "passage_text", Map.of("type", "string"),
                                "word_count", Map.of("type", "integer")));
                schema.put("required", List.of("passage_text", "word_count"));
                return schema;
        }

        /**
         * Get JSON Schema for Pass 2 (Reading Questions).
         */
        @SuppressWarnings("unchecked")
        public Map<String, Object> getReadingQuestionsSchema() {
                // Reuse full schema but remove section/passage requirement
                Map<String, Object> fullSchema = getReadingJsonSchema();
                Map<String, Object> properties = new LinkedHashMap<>(
                                (Map<String, Object>) fullSchema.get("properties"));
                properties.remove("section"); // Remove passage section

                Map<String, Object> schema = new LinkedHashMap<>();
                schema.put("type", "object");
                schema.put("properties", properties);
                schema.put("required", List.of("questions"));
                return schema;
        }

        /**
         * Get JSON Schema for Pass 1 (Transcript).
         */
        public Map<String, Object> getListeningTranscriptSchema() {
                Map<String, Object> schema = new LinkedHashMap<>();
                schema.put("type", "object");
                schema.put("properties", Map.of(
                                "transcript", Map.of("type", "string"),
                                "audio_placeholder", Map.of("type", "object")));
                schema.put("required", List.of("transcript", "audio_placeholder"));
                return schema;
        }

        /**
         * Get JSON Schema for Pass 2 (Questions ONLY - no answers/explanations).
         *
         * Phase 2 of the 3-phase Listening split produces question stems only.
         * correct_answer and explanation are deliberately excluded here; they are
         * generated separately in Phase 3 ({@link #getListeningAnswersSchema()}).
         */
        @SuppressWarnings("unchecked")
        public Map<String, Object> getListeningQuestionsSchema() {
                Map<String, Object> properties = new LinkedHashMap<>();

                // Reuse section_layout from the full schema unchanged.
                Map<String, Object> fullProps = (Map<String, Object>) getListeningJsonSchema().get("properties");
                properties.put("section_layout", fullProps.get("section_layout"));

                // Questions array WITHOUT correct_answer / explanation.
                Map<String, Object> questions = new LinkedHashMap<>();
                questions.put("type", "array");
                Map<String, Object> questionItem = new LinkedHashMap<>();
                questionItem.put("type", "object");
                Map<String, Object> questionProps = new LinkedHashMap<>();
                questionProps.put("question_number", Map.of("type", "integer"));
                questionProps.put("question_type", Map.of(
                                "type", "string",
                                "enum", List.of(
                                                "FILL_IN_BLANK",
                                                "MULTIPLE_CHOICE",
                                                "MULTIPLE_CHOICE_MULTIPLE_ANSWERS",
                                                "MATCHING")));
                questionProps.put("question_content", Map.of("type", "object"));
                questionProps.put("word_limit", Map.of("type", List.of("string", "null")));
                questionItem.put("properties", questionProps);
                questionItem.put("required",
                                List.of("question_number", "question_type", "question_content"));
                questionItem.put("additionalProperties", false);
                questions.put("items", questionItem);
                properties.put("questions", questions);

                Map<String, Object> schema = new LinkedHashMap<>();
                schema.put("type", "object");
                schema.put("properties", properties);
                schema.put("required", List.of("section_layout", "questions"));
                schema.put("additionalProperties", false);
                return schema;
        }

        /**
         * Get JSON Schema for Pass 3 (Listening Answers + Explanations).
         *
         * Phase 3 of the 3-phase Listening split. Produces an {@code answers} array
         * keyed by {@code question_number}, each carrying the correct answer,
         * a structured Vietnamese explanation, and supporting transcript evidence.
         */
        public Map<String, Object> getListeningAnswersSchema() {
                Map<String, Object> answerItem = new LinkedHashMap<>();
                answerItem.put("type", "object");
                Map<String, Object> answerProps = new LinkedHashMap<>();
                answerProps.put("question_number", Map.of("type", "integer"));
                answerProps.put("correct_answer", Map.of("type", "array", "items", Map.of("type", "string")));
                answerProps.put("explanation", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                                "detail", Map.of("type", "string",
                                                                "description",
                                                                "Detailed explanation why this is the correct answer (in Vietnamese)"),
                                                "quote", Map.of("type", "string",
                                                                "description",
                                                                "Direct quote from transcript (in English)"),
                                                "strategy", Map.of("type", "string",
                                                                "description",
                                                                "Strategy tip for this question type (in Vietnamese)")),
                                "required", List.of("detail", "quote", "strategy")));
                answerProps.put("evidence_from_transcript", Map.of("type", "string"));
                answerItem.put("properties", answerProps);
                answerItem.put("required", List.of("question_number", "correct_answer", "explanation", "evidence_from_transcript"));
                answerItem.put("additionalProperties", false);

                Map<String, Object> answers = new LinkedHashMap<>();
                answers.put("type", "array");
                answers.put("items", answerItem);

                Map<String, Object> schema = new LinkedHashMap<>();
                schema.put("type", "object");
                schema.put("properties", Map.of("answers", answers));
                schema.put("required", List.of("answers"));
                return schema;
        }

        /**
         * Get JSON Schema for Reading output validation.
         */
        public Map<String, Object> getReadingJsonSchema() {
                Map<String, Object> schema = new LinkedHashMap<>();
                schema.put("type", "object");

                Map<String, Object> properties = new LinkedHashMap<>();

                // Section properties
                Map<String, Object> section = new LinkedHashMap<>();
                section.put("type", "object");
                Map<String, Object> sectionProps = new LinkedHashMap<>();
                sectionProps.put("passage_text", Map.of("type", "string"));
                sectionProps.put("word_count", Map.of("type", "integer"));
                sectionProps.put("word_count_valid", Map.of("type", "boolean"));
                section.put("properties", sectionProps);
                section.put("required", List.of("passage_text", "word_count"));
                properties.put("section", section);

                // Questions array
                Map<String, Object> questions = new LinkedHashMap<>();
                questions.put("type", "array");

                Map<String, Object> questionItem = new LinkedHashMap<>();
                questionItem.put("type", "object");
                Map<String, Object> questionProps = new LinkedHashMap<>();
                questionProps.put("question_number", Map.of("type", "integer"));
                questionProps.put("question_type", Map.of(
                                "type", "string",
                                "enum", List.of(
                                                "FILL_IN_BLANK",
                                                "SUMMARY_COMPLETION",
                                                "TRUE_FALSE_NOT_GIVEN",
                                                "YES_NO_NOT_GIVEN",
                                                "MATCHING_INFORMATION",
                                                "MATCHING_HEADINGS",
                                                "MATCHING_FEATURES",
                                                "MATCHING_SENTENCE_ENDINGS",
                                                "MULTIPLE_CHOICE",
                                                "MULTIPLE_CHOICE_MULTIPLE_ANSWERS",
                                                "SUMMARY_COMPLETION_OPTIONS",
                                                "DIAGRAM_LABEL_COMPLETION",
                                                "TABLE_COMPLETION",
                                                "FLOW_CHART_COMPLETION")));
                questionProps.put("question_content", Map.of("type", "object"));
                questionProps.put("correct_answer", Map.of("type", "array", "items", Map.of("type", "string")));
                // Structured explanation in Vietnamese (3 fields - dapAn removed as it's
                // redundant with correct_answer)
                questionProps.put("explanation", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                                "detail", Map.of("type", "string",
                                                                "description",
                                                                "Detailed explanation why this is the correct answer (in Vietnamese)"),
                                                "quote", Map.of("type", "string",
                                                                "description",
                                                                "Direct quote from passage/transcript (in English)"),
                                                "strategy", Map.of("type", "string",
                                                                "description",
                                                                "Strategy tip for this question type (in Vietnamese)")),
                                "required", List.of("detail", "quote", "strategy")));
                questionProps.put("word_limit", Map.of("type", List.of("string", "null")));
                questionProps.put("image_url", Map.of("type", List.of("string", "null")));
                questionItem.put("properties", questionProps);
                questionItem.put("required",
                                List.of("question_number", "question_type", "question_content", "correct_answer",
                                                "explanation",
                                                "word_limit"));

                questions.put("items", questionItem);
                properties.put("questions", questions);

                schema.put("properties", properties);
                schema.put("required", List.of("section", "questions"));

                return schema;
        }

        /**
         * Get JSON Schema for Listening output validation.
         * Enhanced for Phase 3.
         */
        public Map<String, Object> getListeningJsonSchema() {
                Map<String, Object> schema = new LinkedHashMap<>();
                schema.put("type", "object");

                Map<String, Object> properties = new LinkedHashMap<>();

                // Transcript
                properties.put("transcript",
                                Map.of("type", "string", "description", "Full transcript with speaker labels"));

                // Section layout object with blocks
                Map<String, Object> blockSchema = new LinkedHashMap<>();
                blockSchema.put("type", "object");
                // CRITICAL: Add enum constraint to block_type to prevent AI hallucination
                Map<String, Object> blockTypeSchema = new LinkedHashMap<>();
                blockTypeSchema.put("type", "string");
                blockTypeSchema.put("enum", List.of(
                                "NOTE_COMPLETION",
                                "INSTRUCTIONS_ONLY",
                                "MATCHING_FEATURES",
                                "PLAN_MAP_DIAGRAM_LABELING"));
                blockSchema.put("properties", Map.of(
                                "block_type", blockTypeSchema,
                                "content", Map.of("type", "object"),
                                "question_numbers", Map.of("type", "array", "items", Map.of("type", "integer"))));

                Map<String, Object> sectionLayout = new LinkedHashMap<>();
                sectionLayout.put("type", "object");
                sectionLayout.put("properties", Map.of(
                                "blocks", Map.of("type", "array", "items", blockSchema)));
                properties.put("section_layout", sectionLayout);

                // Questions array
                Map<String, Object> questions = new LinkedHashMap<>();
                questions.put("type", "array");
                Map<String, Object> questionItem = new LinkedHashMap<>();
                questionItem.put("type", "object");
                Map<String, Object> questionProps = new LinkedHashMap<>();
                questionProps.put("question_number", Map.of("type", "integer"));
                questionProps.put("question_type", Map.of(
                                "type", "string",
                                "enum", List.of(
                                                "FILL_IN_BLANK",
                                                "MULTIPLE_CHOICE",
                                                "MULTIPLE_CHOICE_MULTIPLE_ANSWERS",
                                                "MATCHING")));
                questionProps.put("question_content", Map.of("type", "object"));
                questionProps.put("correct_answer", Map.of("type", "array", "items", Map.of("type", "string")));
                // Structured explanation in Vietnamese (3 fields - dapAn removed as it's
                // redundant with correct_answer)
                questionProps.put("explanation", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                                "detail", Map.of("type", "string",
                                                                "description",
                                                                "Detailed explanation why this is the correct answer (in Vietnamese)"),
                                                "quote", Map.of("type", "string",
                                                                "description",
                                                                "Direct quote from transcript (in English)"),
                                                "strategy", Map.of("type", "string",
                                                                "description",
                                                                "Strategy tip for this question type (in Vietnamese)")),
                                "required", List.of("detail", "quote", "strategy")));
                questionProps.put("word_limit", Map.of("type", List.of("string", "null")));
                questionItem.put("properties", questionProps);
                questionItem.put("required",
                                List.of("question_number", "question_type", "question_content", "correct_answer",
                                                "explanation"));
                questions.put("items", questionItem);
                properties.put("questions", questions);

                // Audio placeholder
                Map<String, Object> audioPlaceholder = new LinkedHashMap<>();
                audioPlaceholder.put("type", "object");
                audioPlaceholder.put("properties", Map.of(
                                "duration_estimate", Map.of("type", "string"),
                                "speaker_count", Map.of("type", "integer"),
                                "speaker_genders", Map.of("type", "array"),
                                "accent_recommendation", Map.of("type", "string"),
                                "pacing_notes", Map.of("type", "string"),
                                "background_ambient", Map.of("type", "string"),
                                "tts_ready", Map.of("type", "boolean")));
                properties.put("audio_placeholder", audioPlaceholder);

                // Figure description (for maps/plans)
                Map<String, Object> figureDescription = new LinkedHashMap<>();
                figureDescription.put("type", "object");
                figureDescription.put("properties", Map.of(
                                "title", Map.of("type", "string"),
                                "elements", Map.of("type", "array"),
                                "answer_locations", Map.of("type", "object")));
                properties.put("figure_description", figureDescription);

                schema.put("properties", properties);
                schema.put("required", List.of("transcript", "questions", "section_layout", "audio_placeholder"));

                return schema;
        }

        /**
         * Get JSON Schema for Writing output validation.
         * Enhanced for Phase 4 with complete schema.
         */
        public Map<String, Object> getWritingJsonSchema() {
                Map<String, Object> schema = new LinkedHashMap<>();
                schema.put("type", "object");

                Map<String, Object> properties = new LinkedHashMap<>();

                // Core task prompt
                properties.put("task_prompt", Map.of("type", "string"));
                properties.put("task_type", Map.of(
                                "type", "string",
                                "enum", List.of(
                                                "TASK_1_ACADEMIC",
                                                "TASK_1_GT_LETTER",
                                                "TASK_2_OPINION",
                                                "TASK_2_DISCUSSION",
                                                "TASK_2_ADVANTAGES",
                                                "TASK_2_PROBLEM_SOLUTION",
                                                "TASK_2_TWO_PART")));
                properties.put("word_requirement", Map.of("type", "integer"));

                // Chart data for Task 1 Academic
                Map<String, Object> chartData = new LinkedHashMap<>();
                chartData.put("type", "object");
                Map<String, Object> chartProps = new LinkedHashMap<>();
                chartProps.put("chart_type", Map.of("type", "string"));
                chartProps.put("title", Map.of("type", "string"));
                chartProps.put("source", Map.of("type", "string"));
                chartProps.put("x_axis", Map.of("type", "object"));
                chartProps.put("y_axis", Map.of("type", "object"));
                chartProps.put("series", Map.of("type", "array"));
                chartData.put("properties", chartProps);
                properties.put("chart_data", chartData);

                // Figure description for process/map
                Map<String, Object> figureDesc = new LinkedHashMap<>();
                figureDesc.put("type", "object");
                Map<String, Object> figureProps = new LinkedHashMap<>();
                figureProps.put("type", Map.of("type", "string"));
                figureProps.put("title", Map.of("type", "string"));
                figureProps.put("elements", Map.of("type", "array"));
                figureProps.put("image_placeholder", Map.of("type", "string"));
                figureDesc.put("properties", figureProps);
                properties.put("figure_description", figureDesc);

                // Letter context for GT Task 1
                Map<String, Object> letterContext = new LinkedHashMap<>();
                letterContext.put("type", "object");
                Map<String, Object> letterProps = new LinkedHashMap<>();
                letterProps.put("recipient", Map.of("type", "string"));
                letterProps.put("relationship", Map.of("type", "string"));
                letterProps.put("purpose", Map.of("type", "string"));
                letterContext.put("properties", letterProps);
                properties.put("letter_context", letterContext);

                // Essay metadata for Task 2
                Map<String, Object> essayMeta = new LinkedHashMap<>();
                essayMeta.put("type", "object");
                Map<String, Object> essayProps = new LinkedHashMap<>();
                essayProps.put("essay_type", Map.of("type", "string"));
                essayProps.put("topic_category", Map.of("type", "string"));
                essayProps.put("complexity", Map.of("type", "string"));
                essayMeta.put("properties", essayProps);
                properties.put("essay_metadata", essayMeta);

                // Optional sample answer
                Map<String, Object> sampleAnswer = new LinkedHashMap<>();
                sampleAnswer.put("type", "object");
                Map<String, Object> sampleProps = new LinkedHashMap<>();
                sampleProps.put("content", Map.of("type", "string"));
                sampleProps.put("word_count", Map.of("type", "integer"));
                sampleProps.put("band_score", Map.of("type", "number"));
                sampleProps.put("examiner_comments", Map.of("type", "string"));
                sampleAnswer.put("properties", sampleProps);
                properties.put("sample_answer", sampleAnswer);

                schema.put("properties", properties);
                schema.put("required", List.of("task_prompt", "task_type", "word_requirement"));

                // Conditional requirements based on task_type
                List<Map<String, Object>> oneOf = new ArrayList<>();
                oneOf.add(Map.of(
                                "properties", Map.of("task_type", Map.of("enum", List.of("TASK_1_ACADEMIC"))),
                                "required", List.of("chart_data")));
                oneOf.add(Map.of(
                                "properties", Map.of("task_type", Map.of("enum", List.of("TASK_1_GT_LETTER"))),
                                "required", List.of("letter_context")));
                oneOf.add(Map.of(
                                "properties", Map.of("task_type", Map.of("enum", List.of(
                                                "TASK_2_OPINION",
                                                "TASK_2_DISCUSSION",
                                                "TASK_2_ADVANTAGES",
                                                "TASK_2_PROBLEM_SOLUTION",
                                                "TASK_2_TWO_PART"))),
                                "required", List.of("essay_metadata")));
                schema.put("oneOf", oneOf);

                return schema;
        }

        /**
         * Get JSON Schema for Writing Phase 1 (Task only).
         *
         * Derived from the full Writing schema with {@code sample_answer} removed.
         * Retains the conditional {@code oneOf} so the produced task object remains
         * compatible with {@code JsonWritingValidator} (task_prompt, task_type,
         * word_requirement, and the task-type-specific structure field).
         */
        @SuppressWarnings("unchecked")
        public Map<String, Object> getWritingTaskSchema() {
                Map<String, Object> fullSchema = getWritingJsonSchema();
                Map<String, Object> properties = new LinkedHashMap<>(
                                (Map<String, Object>) fullSchema.get("properties"));
                properties.remove("sample_answer");

                Map<String, Object> schema = new LinkedHashMap<>();
                schema.put("type", "object");
                schema.put("properties", properties);
                schema.put("required", List.of("task_prompt", "task_type", "word_requirement"));
                if (fullSchema.containsKey("oneOf")) {
                        schema.put("oneOf", fullSchema.get("oneOf"));
                }
                return schema;
        }

        /**
         * Get JSON Schema for Writing Phase 2 (Sample answer).
         *
         * Produces a {@code sample_answer} object ({content, word_count, band_score})
         * that merges at the root so {@code JsonWritingValidator} can validate
         * {@code sample_answer.content}.
         */
        public Map<String, Object> getWritingSampleSchema() {
                Map<String, Object> sampleAnswer = new LinkedHashMap<>();
                sampleAnswer.put("type", "object");
                Map<String, Object> sampleProps = new LinkedHashMap<>();
                sampleProps.put("content", Map.of("type", "string"));
                sampleProps.put("word_count", Map.of("type", "integer"));
                sampleProps.put("band_score", Map.of("type", "number"));
                sampleAnswer.put("properties", sampleProps);
                sampleAnswer.put("required", List.of("content", "word_count"));

                Map<String, Object> schema = new LinkedHashMap<>();
                schema.put("type", "object");
                schema.put("properties", Map.of("sample_answer", sampleAnswer));
                schema.put("required", List.of("sample_answer"));
                return schema;
        }

        /**
         * Get JSON Schema for Writing Phase 3 (Band breakdown + grading notes).
         *
         * Extra grading metadata that merges at the root. Not enforced by
         * {@code JsonWritingValidator} and ignored by the content parser, but kept
         * in the final JSON for downstream consumers.
         */
        public Map<String, Object> getWritingBandSchema() {
                Map<String, Object> bandBreakdown = new LinkedHashMap<>();
                bandBreakdown.put("type", "object");
                bandBreakdown.put("properties", Map.of(
                                "TR", Map.of("type", "number"),
                                "CC", Map.of("type", "number"),
                                "LR", Map.of("type", "number"),
                                "GRA", Map.of("type", "number")));
                bandBreakdown.put("required", List.of("TR", "CC", "LR", "GRA"));

                Map<String, Object> schema = new LinkedHashMap<>();
                schema.put("type", "object");
                schema.put("properties", Map.of(
                                "band_breakdown", bandBreakdown,
                                "key_phrases", Map.of("type", "array", "items", Map.of("type", "string")),
                                "grading_notes", Map.of("type", "string")));
                schema.put("required", List.of("band_breakdown"));
                return schema;
        }
}