package com.cramer.service.abts.prompt;

public class QuestionTypeInstructionBuilder {

        /**
         * Get detailed instructions for a specific question type.
         */
        public String getQuestionTypeInstructions(String questionType) {
                return switch (questionType.toUpperCase()) {
                        case "TRUE_FALSE_NOT_GIVEN" -> """
                                        ### TRUE / FALSE / NOT GIVEN Questions
                                        - Statement that is either TRUE, FALSE, or NOT GIVEN based on passage
                                        - TRUE: Passage explicitly confirms the statement
                                        - FALSE: Passage explicitly contradicts the statement
                                        - NOT GIVEN: Passage does not provide enough information
                                        - Question format: { "statement": "..." }
                                        """;
                        case "YES_NO_NOT_GIVEN" -> """
                                        ### YES / NO / NOT GIVEN Questions
                                        - Statement about author's opinion/views
                                        - YES: Author agrees with or supports the statement
                                        - NO: Author disagrees with or opposes the statement
                                        - NOT GIVEN: Author's view is not expressed
                                        - Question format: { "statement": "..." }
                                        """;
                        case "MATCHING_HEADINGS" -> """
                                        ### Matching Headings Questions
                                        - Match paragraph letters to heading options
                                        - Provide list of headings (i-x or similar)
                                        - More headings than paragraphs (distractors)
                                        - Question format: { "paragraph": "A", "heading_options": [...] }
                                        """;
                        case "FILL_IN_BLANK", "FORM_COMPLETION", "NOTE_COMPLETION" -> """
                                        ### Fill in the Blank / Completion Questions
                                        - Complete sentence/form/notes with words from audio/passage
                                        - Specify word limit (e.g., "NO MORE THAN TWO WORDS AND/OR A NUMBER")
                                        - Answer must be exact words from source
                                        - Question format: { "sentence": "...", "word_limit": 2 }
                                        """;
                        case "SENTENCE_COMPLETION" -> """
                                        ### Sentence Completion Questions
                                        - Complete sentence with information from passage/audio
                                        - Specify word limit
                                        - Question format: { "incomplete_sentence": "...", "word_limit": 3 }
                                        """;
                        case "MULTIPLE_CHOICE_SINGLE" ->
                                """
                                                ### Multiple Choice (Single Answer) Questions
                                                - One correct answer from 4 options (A, B, C, D)
                                                - Question stem followed by options
                                                - Question format: { "question": "...", "options": {"A": "...", "B": "...", "C": "...", "D": "..."} }
                                                """;
                        case "MULTIPLE_CHOICE_MULTIPLE" ->
                                """
                                                ### Multiple Choice (Multiple Answers) Questions
                                                - Select 2-3 correct answers from 5-7 options
                                                - Question format: { "question": "...", "options": [...], "answers_required": 2 }
                                                """;
                        case "MAP_LABELLING" -> """
                                        ### Map/Plan Labelling Questions
                                        - Label locations on a map or plan
                                        - Provide options list (A-H or similar)
                                        - Question format: { "location": "...", "options": [...] }
                                        - Requires figure_description in output
                                        """;
                        case "MATCHING" -> """
                                        ### Matching Questions
                                        - Match items from one list to another
                                        - Categories in one list, items in another
                                        - Question format: { "item": "...", "categories": [...] }
                                        """;
                        default -> "Standard question format for " + questionType;
                };
        }

        /**
         * Get Listening-specific question type instructions.
         */
        public String getListeningQuestionTypeInstructions(String questionType, int partNumber) {
                StringBuilder instructions = new StringBuilder();
                instructions.append(getQuestionTypeInstructions(questionType));

                // Add Listening-specific guidance
                instructions.append("\n#### Listening-Specific Guidelines:\n");
                instructions.append("- Answer must appear verbatim in the transcript\n");
                instructions.append("- Include distractor before the correct answer\n");
                instructions.append("- Questions should follow the order of information in the transcript\n");

                if (partNumber == 1 || partNumber == 2) {
                        instructions.append("- Use concrete, factual answers (names, numbers, places)\n");
                } else {
                        instructions.append("- Can include abstract concepts and opinions\n");
                }

                return instructions.toString();
        }
}