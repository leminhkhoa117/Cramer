package com.cramer.service.abts.prompt;

import com.cramer.dto.abts.GenerationRequestDTO;

import java.util.ArrayList;
import java.util.List;

public class ListeningPromptBuilder {

        /**
         * Build complete prompt for Listening content generation.
         * Enhanced for Phase 3 with detailed part-specific requirements.
         */
        public String buildListeningPrompt(GenerationRequestDTO request) {
                StringBuilder prompt = new StringBuilder();

                prompt.append("## TASK: Generate IELTS Listening Section Content\n\n");

                Integer partNumber = request.getPartNumber();
                if (partNumber == null) {
                        partNumber = 1;
                }

                prompt.append("### Part ").append(partNumber).append(" Specifications\n\n");

                switch (partNumber) {
                        case 1:
                                buildListeningPart1Prompt(prompt);
                                break;
                        case 2:
                                buildListeningPart2Prompt(prompt);
                                break;
                        case 3:
                                buildListeningPart3Prompt(prompt);
                                break;
                        case 4:
                                buildListeningPart4Prompt(prompt);
                                break;
                }

                // Difficulty based on request
                prompt.append("### Difficulty Level\n");
                prompt.append("- **Level**: ").append(request.getDifficulty().getDisplayName()).append("\n");
                prompt.append("- **Band Range**: ").append(request.getDifficulty().getBandRange()).append("\n\n");

                // Topic and facts
                prompt.append("### Topic: ").append(request.getTopic()).append("\n\n");

                prompt.append("### Facts to Use (IMPORTANT: Base content on these)\n");
                List<String> facts = request.getFacts();
                if (facts != null && !facts.isEmpty()) {
                        for (int i = 0; i < facts.size(); i++) {
                                prompt.append(String.format("%d. %s\n", i + 1, facts.get(i)));
                        }
                } else {
                        prompt.append("No facts provided. Use realistic, verifiable details.\n");
                }
                prompt.append("\n");

                // Transcript format requirements
                prompt.append("### Transcript Format Requirements\n");
                prompt.append("1. **Speaker Labels**: Use format `SPEAKER_NAME:` followed by their dialogue\n");
                prompt.append("2. **Natural Speech Elements**:\n");
                prompt.append("   - Hesitations: \"um\", \"uh\", \"well...\"\n");
                prompt.append("   - Backchanneling: \"Right\", \"I see\", \"Mm-hmm\"\n");
                prompt.append("   - Self-corrections: \"I mean...\", \"Sorry, let me rephrase that...\"\n");
                prompt.append("3. **Answer Markers**: Where answers appear, ensure clarity\n");
                prompt.append("4. **Spelling**: For names/addresses, include \"That's spelled...\"\n\n");

                // Question types based on request
                List<String> requestedTypes = request.getQuestionTypes();
                if ((requestedTypes == null || requestedTypes.isEmpty())
                                && request.getQuestionTypeCounts() != null
                                && !request.getQuestionTypeCounts().isEmpty()) {
                        requestedTypes = new ArrayList<>(request.getQuestionTypeCounts().keySet());
                }

                prompt.append("### Allowed Listening Question Types (STRICT ENUMS)\n");
                prompt.append("- FILL_IN_BLANK\n");
                prompt.append("- MULTIPLE_CHOICE\n");
                prompt.append("- MULTIPLE_CHOICE_MULTIPLE_ANSWERS\n");
                prompt.append("- MATCHING\n\n");
                prompt.append("Do NOT use other enums (e.g., FORM_COMPLETION, SENTENCE_COMPLETION, SHORT_ANSWER).\n\n");

                int startNumber = (partNumber - 1) * 10 + 1;
                prompt.append("### Question Numbering (STRICT)\n");
                prompt.append("- Use sequential numbers from ").append(startNumber)
                                .append(" to ").append(startNumber + 9).append(" for Part ").append(partNumber)
                                .append(".\n\n");

                // NOTE: "Required Question Types" section removed - per-part specs already
                // define allowed types
                // Adding both was confusing AI into thinking there was a conflict

                if (request.getQuestionTypeCounts() != null && !request.getQuestionTypeCounts().isEmpty()) {
                        prompt.append("### Exact Question Type Counts (STRICT)\n");
                        request.getQuestionTypeCounts().forEach((type, count) -> {
                                prompt.append("- ").append(type).append(": ").append(count).append("\n");
                        });
                        prompt.append("Ensure the total count equals 10 questions for this part.\n\n");
                }

                // Section layout requirements - CRITICAL for frontend compatibility
                prompt.append("### Section Layout (CRITICAL - EXACT FORMAT REQUIRED)\n");
                prompt.append("The `section_layout` MUST be an object with a `blocks` array:\n");
                prompt.append("```json\n");
                prompt.append("{\n");
                prompt.append("  \"blocks\": [\n");
                prompt.append("    {\n");
                prompt.append("      \"block_type\": \"NOTE_COMPLETION\",\n");
                prompt.append("      \"content\": {\n");
                prompt.append("        \"title\": \"Questions 1-10\",\n");
                prompt.append("        \"main_title\": \"Topic Title Here\",\n");
                prompt.append("        \"instructions_text\": \"<b>Instructions</b><br/>Complete the notes below.\"\n");
                prompt.append("      },\n");
                prompt.append("      \"question_numbers\": [1,2,3,4,5,6,7,8,9,10]\n");
                prompt.append("    }\n");
                prompt.append("  ]\n");
                prompt.append("}\n");
                prompt.append("```\n");
                prompt.append(
                                "Block types: NOTE_COMPLETION, INSTRUCTIONS_ONLY, MATCHING_FEATURES, PLAN_MAP_DIAGRAM_LABELING\n\n");

                // CRITICAL: Mandatory question_numbers requirement
                prompt.append("### ⚠️ MANDATORY: question_numbers Array (EVERY BLOCK MUST HAVE THIS)\n");
                prompt.append("**CRITICAL REQUIREMENT**: Every block in section_layout.blocks[] MUST include:\n");
                prompt.append("```json\n");
                prompt.append("\"question_numbers\": [list of question numbers this block contains]\n");
                prompt.append("```\n");
                prompt.append("**Examples:**\n");
                prompt.append("- Block for Q1-5: `\"question_numbers\": [1, 2, 3, 4, 5]`\n");
                prompt.append("- Block for Q6-10: `\"question_numbers\": [6, 7, 8, 9, 10]`\n");
                prompt.append("- Block for Q11-15: `\"question_numbers\": [11, 12, 13, 14, 15]`\n\n");
                prompt.append("**FAILURE TO INCLUDE question_numbers WILL BREAK THE UI - QUESTIONS WILL NOT DISPLAY!**\n");
                prompt.append("**EVERY question must be assigned to exactly ONE block via its question_numbers array.**\n\n");

                prompt.append("Rendering rules:\n");
                prompt.append("- `content.title` is plain text only (no HTML)\n");
                prompt.append("- `content.instructions_text` is HTML\n");
                prompt.append("- `content.main_title` (NOTE_COMPLETION) is plain text\n");
                prompt.append("- `question_content.section_title` is plain text\n");
                prompt.append("- `question_content.text` is HTML (use <br/> for line breaks)\n\n");
                prompt.append("Block rules:\n");
                prompt.append("- NOTE_COMPLETION uses question_type FILL_IN_BLANK\n");
                prompt.append("- PLAN_MAP_DIAGRAM_LABELING uses question_type MATCHING\n");
                prompt.append("- MATCHING_FEATURES uses question_type MATCHING\n");
                prompt.append("- INSTRUCTIONS_ONLY can wrap MULTIPLE_CHOICE or MATCHING groups\n\n");
                prompt.append("### Question Content Format for FILL_IN_BLANK / NOTE_COMPLETION\n");
                prompt.append(
                                "**⚠️ CRITICAL: INLINE STRATEGY (Each question has its own text)**\n");
                prompt.append(
                                "- **Each question object MUST contain its specific bullet point or sentence fragment.**\n");
                prompt.append(
                                "- **Do NOT leave the 'text' field empty for any question.**\n");
                prompt.append(
                                "- **BLANK FORMAT**: Use `<strong>{number}</strong> ____` (bold number followed by 4 underscores).\n");
                prompt.append(
                                "- **section_title**: Optional, use to create section headers (e.g., \"Beach\", \"Equipment\").\n\n");

                prompt.append("Example (generating Q1, Q2, Q3):\n");
                prompt.append("```json\n");
                prompt.append("// Question 1\n");
                prompt.append(
                                "{\"text\": \"• making sure the beach does not have <strong>1</strong> ____ on it\", \"section_title\": \"Beach\"}\n");
                prompt.append("// Question 2\n");
                prompt.append(
                                "{\"text\": \"• no <strong>2</strong> ____ allowed\"}\n");
                prompt.append("// Question 3 (new section)\n");
                prompt.append(
                                "{\"text\": \"• check <strong>3</strong> ____ for damage\", \"section_title\": \"Equipment\"}\n");
                prompt.append("```\n");
                prompt.append(
                                "**Failure to include text in each question will cause the test to render blank/broken questions.**\n");
                prompt.append("Always include `word_limit` for ALL questions (e.g., \"ONE WORD ONLY\").\n\n");

                prompt.append("### Multiple Choice Format\n");
                prompt.append("- MULTIPLE_CHOICE: options array of strings like \"A ...\", \"B ...\", \"C ...\".\n");
                prompt.append("- MULTIPLE_CHOICE_MULTIPLE_ANSWERS: options A-E, correct_answer has TWO letters.\n");
                prompt.append(
                                "- For \"Choose TWO letters\" tasks, create TWO questions with identical text/options and the same correct_answer array.\n\n");

                prompt.append("### Matching Format\n");
                prompt.append("- MATCHING questions: `question_content` must include only `text`.\n");
                prompt.append("- All options belong in the block content (`section_layout.blocks[].content.options`).\n");
                prompt.append("- **CRITICAL**: Options MUST be an array of `{letter, text}` objects, NOT strings.\n");
                prompt.append("- ❌ WRONG: `\"options\": [\"A. North Wing\", \"B. South Wing\"]`\n");
                prompt.append("- ✅ CORRECT: `\"options\": [{\"letter\": \"A\", \"text\": \"North Wing\"}, {\"letter\": \"B\", \"text\": \"South Wing\"}]`\n\n");
                prompt.append("Example MATCHING_FEATURES block:\n");
                prompt.append("```json\n");
                prompt.append("{\n");
                prompt.append("  \"block_type\": \"MATCHING_FEATURES\",\n");
                prompt.append("  \"content\": {\n");
                prompt.append("    \"title\": \"Questions 8-10\",\n");
                prompt.append("    \"instructions_text\": \"<b>Instructions</b><br/>Where are the following offices located?<br/>Choose the correct letter, <b>A, B, or C</b>.\",\n");
                prompt.append("    \"options\": [\n");
                prompt.append("      {\"letter\": \"A\", \"text\": \"North Wing\"},\n");
                prompt.append("      {\"letter\": \"B\", \"text\": \"South Wing\"},\n");
                prompt.append("      {\"letter\": \"C\", \"text\": \"Central Plaza\"}\n");
                prompt.append("    ]\n");
                prompt.append("  },\n");
                prompt.append("  \"question_numbers\": [8, 9, 10]\n");
                prompt.append("}\n");
                prompt.append("```\n\n");

                // Figure descriptions REQUIRED for Part 2 map labeling
                if (partNumber == 2) {
                        prompt.append("### FIGURE DESCRIPTION (REQUIRED for Map/Plan Labeling)\n");
                        prompt.append("When using PLAN_MAP_DIAGRAM_LABELING block_type, you MUST provide a DETAILED `figure_description`.\n");
                        prompt.append("This description allows manual recreation of the map/diagram.\n\n");
                        prompt.append("**Required structure:**\n");
                        prompt.append("```json\n");
                        prompt.append("\"figure_description\": {\n");
                        prompt.append("  \"title\": \"Map of Newtown Community Center\",\n");
                        prompt.append("  \"type\": \"floor_plan\",  // floor_plan, campus_map, town_map, building_layout\n");
                        prompt.append("  \"description\": \"A floor plan showing the main building of the community center with labeled rooms and facilities.\",\n");
                        prompt.append("  \"elements\": [\n");
                        prompt.append("    { \"label\": \"A\", \"name\": \"Main Entrance\", \"position\": \"south side, center\" },\n");
                        prompt.append("    { \"label\": \"B\", \"name\": \"Reception Desk\", \"position\": \"immediately inside entrance\" },\n");
                        prompt.append("    { \"label\": \"C\", \"name\": \"Gymnasium\", \"position\": \"east wing, large rectangular room\" },\n");
                        prompt.append("    { \"label\": \"D\", \"name\": \"Cafe\", \"position\": \"west wing, near windows\" },\n");
                        prompt.append("    { \"label\": \"E\", \"name\": \"Library\", \"position\": \"second floor, northwest corner\" }\n");
                        prompt.append("  ],\n");
                        prompt.append("  \"answer_locations\": { \"11\": \"B\", \"12\": \"C\", \"13\": \"D\", \"14\": \"E\" },\n");
                        prompt.append("  \"orientation\": \"North is at the top\",\n");
                        prompt.append("  \"scale\": \"Each grid square represents approximately 10 meters\"\n");
                        prompt.append("}\n");
                        prompt.append("```\n\n");
                        prompt.append("**CRITICAL:** Without a detailed figure_description, the map cannot be created!\n\n");
                }

                // Explanation language and format
                prompt.append("### Explanation Format (CRITICAL)\n");
                prompt.append("All explanations must be JSON objects in Vietnamese:\n");
                prompt.append("```json\n");
                prompt.append("{\n");
                prompt.append("  \"detail\": \"<Vietnamese explanation: why is this correct?>\",\n");
                prompt.append("  \"quote\": \"<EXACT English quote from transcript that proves the answer>\",\n");
                prompt.append("  \"strategy\": \"<Vietnamese strategy tip for this question type>\"\n");
                prompt.append("}\n");
                prompt.append("```\n\n");

                prompt.append("### ⚠️ CRITICAL: Answer Format\n");
                prompt.append("- Each question MUST have its OWN `correct_answer` array with exactly ONE answer.\n");
                prompt.append("- Question 1's correct_answer = [\"answer1\"] (just this question's answer)\n");
                prompt.append("- Question 2's correct_answer = [\"answer2\"] (just this question's answer)\n");
                prompt.append("- DO NOT put all answers in one array and duplicate across questions!\n");
                prompt.append("- Each question MUST have its own INDIVIDUAL explanation JSON object.\n\n");

                prompt.append("### Audio Placeholder (REQUIRED)\n");
                prompt.append("Include `audio_placeholder` with:\n");
                prompt.append("- duration_estimate (e.g., \"6:30\")\n");
                prompt.append("- speaker_count (int)\n");
                prompt.append("- speaker_genders (array, e.g., [\"male\",\"female\"])\n");
                prompt.append("- accent_recommendation (string)\n");
                prompt.append("- pacing_notes (string)\n");
                prompt.append("- background_ambient (string)\n");
                prompt.append("- tts_ready (boolean)\n\n");

                prompt.append("### Mini Example Output (Structure Only)\n");
                prompt.append(
                                "{\"transcript\":\"SPEAKER: ...\",\"section_layout\":{\"blocks\":[{\"block_type\":\"NOTE_COMPLETION\",");
                prompt.append("\"content\":{\"title\":\"Questions 1-10\",\"instructions_text\":\"...\"},");
                prompt.append(
                                "\"question_numbers\":[1,2,3]}]},\"questions\":[{\"question_number\":1,\"question_type\":\"FILL_IN_BLANK\",");
                prompt.append(
                                "\"question_content\":{\"text\":\"...\"},\"correct_answer\":[\"...\"],\"word_limit\":\"ONE WORD ONLY\",\"explanation\":\"...\"}],");
                prompt.append(
                                "\"audio_placeholder\":{\"duration_estimate\":\"6:30\",\"speaker_count\":2,\"tts_ready\":false}}\n\n");

                // CRITICAL: Self-validation checklist
                prompt.append("### ⚠️ MANDATORY VALIDATION CHECKLIST (Verify before output)\n");
                prompt.append("Before generating your response, VERIFY each of these requirements:\n\n");
                prompt.append("**□ Block Requirements:**\n");
                prompt.append("  - [ ] Every block has `question_numbers` array with ALL question numbers it contains\n");
                prompt.append("  - [ ] Every question is assigned to exactly ONE block\n");
                prompt.append("  - [ ] Block types are ONLY: NOTE_COMPLETION, INSTRUCTIONS_ONLY, MATCHING_FEATURES, or PLAN_MAP_DIAGRAM_LABELING\n");
                prompt.append("  - [ ] NO other block types like MULTIPLE_CHOICE (wrap MC questions in INSTRUCTIONS_ONLY block)\n\n");

                prompt.append("**□ Question Requirements:**\n");
                prompt.append("  - [ ] Every FILL_IN_BLANK question has `word_limit` field (e.g., \"ONE WORD ONLY\")\n");
                prompt.append("  - [ ] Every question has its own individual `correct_answer` array (NOT duplicated)\n");
                prompt.append("  - [ ] Every question has its own individual `explanation` object (NOT duplicated)\n");
                prompt.append("  - [ ] All question numbers are sequential and complete for this part\n\n");

                prompt.append("**□ audio_placeholder Requirements:**\n");
                prompt.append("  - [ ] duration_estimate (string, e.g., \"6:30\")\n");
                prompt.append("  - [ ] speaker_count (integer)\n");
                prompt.append("  - [ ] speaker_genders (array of strings)\n");
                prompt.append("  - [ ] accent_recommendation (string)\n");
                prompt.append("  - [ ] pacing_notes (string)\n");
                prompt.append("  - [ ] background_ambient (string)\n\n");
                prompt.append("**FAILURE TO MEET ANY CHECKPOINT = INVALID OUTPUT**\n\n");

                if (request.getCustomInstructions() != null && !request.getCustomInstructions().isBlank()) {
                        prompt.append("### Custom Instructions (Highest Priority)\n");
                        prompt.append(request.getCustomInstructions()).append("\n\n");
                }

                return prompt.toString();
        }

        private void buildListeningPart1Prompt(StringBuilder prompt) {
                prompt.append("**Context**: Everyday social conversation between 2 speakers\n");
                prompt.append(
                                "**Scenario Examples**: Booking accommodation, registering for service, asking about facilities\n");
                prompt.append("**Transcript Length**: 850-1050 words (approximately 6-7 minutes of natural dialogue)\n"); // MUST MATCH JsonListeningValidator.LISTENING_WORD_COUNTS
                prompt.append("**Important**: Do NOT count words yourself - just write a complete, natural conversation.\n");
                prompt.append("**Questions**: 10 questions (Q1-10)\n");
                prompt.append("**Typical Block Structure**:\n");
                prompt.append("  - NOTE_COMPLETION block for Q1-10\n");
                prompt.append(
                                "  - You may split into two NOTE_COMPLETION blocks if instructions change (e.g., notes then table)\n");
                prompt.append("**Question Types**:\n");
                prompt.append("  - FILL_IN_BLANK only (note/form completion)\n");
                prompt.append("**Word Limit**: Use ONE WORD ONLY or ONE WORD AND/OR A NUMBER\n\n");
                prompt.append("**Speaker Guidelines**:\n");
                prompt.append("  - Use 2 named speakers (e.g., RECEPTIONIST:, CUSTOMER:)\n");
                prompt.append("  - Include natural greetings and closings\n");
                prompt.append("  - One speaker asks questions, the other provides information\n");
                prompt.append("  - Spell out names/addresses when necessary\n\n");
        }

        private void buildListeningPart2Prompt(StringBuilder prompt) {
                prompt.append("**Context**: Monologue in everyday social context\n");
                prompt.append("**Scenario Examples**: Tour guide speech, public announcement, facility orientation\n");
                prompt.append("**Transcript Length**: 950-1150 words (approximately 7-8 minutes of natural speech)\n"); // MUST MATCH JsonListeningValidator.LISTENING_WORD_COUNTS
                prompt.append("**Important**: Do NOT count words yourself - just write a complete, detailed monologue.\n");
                prompt.append("**Questions**: 10 questions (Q11-20)\n");
                prompt.append("**Typical Block Structure (choose one realistic pattern)**:\n");
                prompt.append("  - 4 MULTIPLE_CHOICE (Q11-14) + 6 MATCHING (Q15-20)\n");
                prompt.append("  - 4 MULTIPLE_CHOICE (Q11-14) + 6 MULTIPLE_CHOICE_MULTIPLE_ANSWERS (Q15-20)\n");
                prompt.append("  - 4 MULTIPLE_CHOICE (Q11-14) + 6 PLAN_MAP_DIAGRAM_LABELING (Q15-20)\n");
                prompt.append("**Block Types**:\n");
                prompt.append("  - INSTRUCTIONS_ONLY for MCQ/MCMA groups\n");
                prompt.append("  - MATCHING_FEATURES for matching lists\n");
                prompt.append("  - PLAN_MAP_DIAGRAM_LABELING for maps/plans (include image_url + options)\n\n");

                prompt.append("**Speaker Guidelines**:\n");
                prompt.append("  - Single speaker (named, e.g., GUIDE:, MANAGER:)\n");
                prompt.append("  - Organized sections with clear transitions\n");
                prompt.append("  - Reference to visual elements (map, diagram) where applicable\n\n");
        }

        private void buildListeningPart3Prompt(StringBuilder prompt) {
                prompt.append("**Context**: Conversation between 2-4 speakers in academic context\n");
                prompt.append("**Scenario Examples**: Student-tutor discussion, group project planning\n");
                prompt.append("**Transcript Length**: 1050-1250 words (approximately 7-8 minutes of academic discussion)\n"); // MUST MATCH JsonListeningValidator.LISTENING_WORD_COUNTS
                prompt.append("**Important**: Do NOT count words yourself - just write a complete, substantial conversation.\n");
                prompt.append("**Questions**: 10 questions (Q21-30)\n");
                prompt.append("**Typical Block Structure**:\n");
                prompt.append("  - 2 MULTIPLE_CHOICE_MULTIPLE_ANSWERS (Q21-22) OR 4 MULTIPLE_CHOICE (Q21-24)\n");
                prompt.append("  - 5 MATCHING (Q23-27 or Q25-29) with shared options\n");
                prompt.append("  - 3-4 MULTIPLE_CHOICE (Q28-30)\n");
                prompt.append(
                                "**Note**: For MULTIPLE_CHOICE_MULTIPLE_ANSWERS, duplicate the same stem/options for each question number.\n\n");
                prompt.append("**Speaker Guidelines**:\n");
                prompt.append("  - Use 2-4 named speakers (e.g., TUTOR:, SARAH:, MICHAEL:)\n");
                prompt.append("  - Include academic vocabulary\n");
                prompt.append("  - Show interaction: agreement, disagreement, building on ideas\n");
                prompt.append("  - Include hedging language and discourse markers\n\n");
        }

        private void buildListeningPart4Prompt(StringBuilder prompt) {
                prompt.append("**Context**: Monologue on academic subject\n");
                prompt.append("**Scenario Examples**: University lecture, research presentation\n");
                prompt.append("**Transcript Length**: 1050-1250 words (approximately 7-8 minutes of academic lecture)\n"); // MUST MATCH JsonListeningValidator.LISTENING_WORD_COUNTS
                prompt.append("**Important**: Do NOT count words yourself - just write a complete, detailed lecture.\n");
                prompt.append("**Questions**: 10 questions (Q31-40)\n");
                prompt.append("**Typical Block Structure**:\n");
                prompt.append("  - NOTE_COMPLETION block for Q31-40\n");
                prompt.append("**Question Types**:\n");
                prompt.append("  - FILL_IN_BLANK only (note completion)\n");
                prompt.append("**Word Limit**: Use ONE WORD ONLY (or ONE WORD AND/OR A NUMBER if numeric data)\n\n");

                prompt.append("**Speaker Guidelines**:\n");
                prompt.append("  - Single academic speaker (e.g., PROFESSOR:, LECTURER:)\n");
                prompt.append("  - Formal academic register\n");
                prompt.append("  - Clear topic sentences and signposting\n");
                prompt.append("  - Technical terminology with some explanation\n\n");
        }

        /**
         * Build system prompt for Listening generation.
         * Enhanced for Phase 3 with comprehensive instructions.
         */
        public String buildListeningSystemPrompt() {
                StringBuilder system = new StringBuilder();

                system.append("You are an expert IELTS Listening test creator with extensive experience ");
                system.append("creating official Cambridge IELTS materials.\n\n");

                system.append("## Your Expertise\n");
                system.append("- Deep understanding of IELTS Listening test format and difficulty progression\n");
                system.append("- Ability to create natural-sounding transcripts suitable for audio recording\n");
                system.append("- Knowledge of authentic distractor patterns and paraphrasing techniques\n\n");

                system.append("## Critical Rules\n");
                system.append("1. **COMPLETENESS**: Write a full, natural transcript matching the time estimate. Do NOT count words yourself.\\n");
                system.append("2. **NATURAL SPEECH**: Dialogue must sound authentic when read aloud\n");
                system.append("3. **ANSWER CLARITY**: Each answer must be clearly spoken in the transcript\n");
                system.append("4. **DISTRACTION**: Include plausible distractors mentioned before the answer\n");
                system.append("5. **PARAPHRASING**: Questions should paraphrase information from transcript\n");
                system.append("6. **10 QUESTIONS**: Each part must have exactly 10 questions\n");
                system.append("7. **EXPLANATIONS**: Must be a JSON object with: detail, quote, strategy (3 fields)\n");
                system.append(
                                "8. **WORD LIMITS**: FILL_IN_BLANK answers must respect word_limit and use ORIGINAL CASING (e.g. 'Park Street', not 'PARK STREET')\n");
                system.append(
                                "9. **INLINE STRATEGY**: For NOTE_COMPLETION, each question MUST have its own text (bullet point). Do NOT leave text empty.\n");
                system.append("10. **JSON FORMAT**: Output must match schema exactly\n\n");

                // Add structured explanation format (3 fields - correct_answer stored
                // separately)
                system.append("## Explanation JSON Format (CRITICAL)\n");
                system.append("Each question's `explanation` must be a JSON object:\n");
                system.append("```json\n");
                system.append("{\n");
                system.append("  \"detail\": \"<detailed Vietnamese explanation>\",\n");
                system.append("  \"quote\": \"<EXACT English quote from transcript>\",\n");
                system.append("  \"strategy\": \"<Vietnamese strategy tip>\"\n");
                system.append("}\n");
                system.append("```\n\n");

                system.append("## Speaker Naming Convention\n");
                system.append("- Part 1: Two named roles (e.g., AGENT:, CALLER:)\n");
                system.append("- Part 2: Single informative role (e.g., CURATOR:, COORDINATOR:)\n");
                system.append("- Part 3: Academic names (e.g., TUTOR:, ANNA:, BEN:)\n");
                system.append("- Part 4: Academic title (e.g., PROFESSOR:, DR. SMITH:)\n\n");

                system.append("## Distractor Patterns\n");
                system.append("- Mention wrong answer first, then correct with actual answer\n");
                system.append("- Include negations: \"It's not X, it's actually Y\"\n");
                system.append("- Use similar-sounding words as distractors\n");
                system.append("- Reference changed plans: \"We were going to X, but now we'll Y\"\n\n");

                system.append("## Output Structure\n");
                system.append("Return valid JSON with:\n");
                system.append("- `transcript`: Full dialogue/monologue with speaker labels\n");
                system.append("- `section_layout`: Object with `blocks` array describing visual layout\n");
                system.append("- `questions`: Array of 10 question objects\n");
                system.append("- `audio_placeholder`: REQUIRED metadata for future TTS generation\n");
                system.append("- `figure_description`: REQUIRED for Part 2 map/plan labeling\n\n");

                system.append("## JSON Format\n");
                system.append("You MUST respond with valid JSON only. No markdown, no explanations outside JSON.\n");

                return system.toString();
        }

        /**
         * PASS 1: Build prompt for Transcript Generation ONLY.
         * ENHANCED: Includes audio_placeholder field requirements, speaker labels, word
         * counts.
         */
        public String buildListeningTranscriptPrompt(GenerationRequestDTO request) {
                StringBuilder prompt = new StringBuilder();

                prompt.append("## TASK: Generate IELTS Listening Transcript (Phase 1/2)\n\n");
                prompt.append("Your goal is to write a high-quality, authentic IELTS Listening transcript.\n");
                prompt.append("Do NOT generate questions yet. Focus ONLY on the dialogue/monologue.\n\n");

                Integer partNumber = request.getPartNumber() != null ? request.getPartNumber() : 1;
                prompt.append("### Part ").append(partNumber).append(" Specifications\n");
                switch (partNumber) {
                        case 1:
                                buildListeningPart1Prompt(prompt);
                                break;
                        case 2:
                                buildListeningPart2Prompt(prompt);
                                break;
                        case 3:
                                buildListeningPart3Prompt(prompt);
                                break;
                        case 4:
                                buildListeningPart4Prompt(prompt);
                                break;
                }

                // Speaker Label Requirements
                prompt.append("\n### Speaker Labels (MANDATORY)\n");
                switch (partNumber) {
                        case 1:
                                prompt.append("- Two named speakers with clear conversational roles (e.g., AGENT:, CALLER:)\n");
                                prompt.append("- Each line MUST start with the speaker label followed by a colon.\n");
                                break;
                        case 2:
                                prompt.append("- Single speaker monologue (e.g., CURATOR:, COORDINATOR:, TOUR GUIDE:)\n");
                                prompt.append("- Each paragraph MUST start with the speaker label.\n");
                                break;
                        case 3:
                                prompt.append("- 2-4 speakers in academic discussion (e.g., TUTOR:, ANNA:, BEN:, CARLOS:)\n");
                                prompt.append("- Each line MUST start with the speaker label followed by a colon.\n");
                                break;
                        case 4:
                                prompt.append("- Single speaker academic lecture (e.g., PROFESSOR:, DR. SMITH:, LECTURER:)\n");
                                prompt.append("- Each paragraph MUST start with the speaker label.\n");
                                break;
                }

                // Word Count Limits
                prompt.append("\n### Transcript Word Count (STRICT LIMITS)\n");
                switch (partNumber) {
                        case 1:
                                prompt.append("- Target: 850-1050 words. Maximum: 1050 words.\n"); // MUST MATCH JsonListeningValidator.LISTENING_WORD_COUNTS
                                break;
                        case 2:
                                prompt.append("- Target: 950-1150 words. Maximum: 1150 words.\n"); // MUST MATCH JsonListeningValidator.LISTENING_WORD_COUNTS
                                break;
                        case 3:
                                prompt.append("- Target: 1050-1250 words. Maximum: 1250 words.\n"); // MUST MATCH JsonListeningValidator.LISTENING_WORD_COUNTS
                                break;
                        case 4:
                                prompt.append("- Target: 1050-1250 words. Maximum: 1250 words.\n"); // MUST MATCH JsonListeningValidator.LISTENING_WORD_COUNTS
                                break;
                }
                prompt.append("- Do NOT exceed maximum. The transcript will be validated for word count.\n");

                // Topic & Facts
                prompt.append("\n### Topic: ").append(request.getTopic()).append("\n");
                prompt.append("### Facts to incorporate:\n");
                List<String> facts = request.getFacts();
                if (facts != null && !facts.isEmpty()) {
                        for (int i = 0; i < facts.size(); i++) {
                                prompt.append("- ").append(facts.get(i)).append("\n");
                        }
                } else {
                        prompt.append("- Use realistic details.\n");
                }

                // Audio Placeholder Requirements
                prompt.append("\n### audio_placeholder Object (ALL FIELDS MANDATORY)\n");
                prompt.append("You MUST include an `audio_placeholder` object with the following fields:\n");
                prompt.append("```json\n");
                prompt.append("{\n");
                prompt.append("  \"duration_estimate\": \"4-5 minutes\",\n");
                prompt.append("  \"speaker_count\": 2,\n");
                prompt.append("  \"speaker_genders\": [\"male\", \"female\"],\n");
                prompt.append("  \"accent_recommendation\": \"British RP\",\n");
                prompt.append("  \"pacing_notes\": \"Moderate pace with natural pauses\",\n");
                prompt.append("  \"background_ambient\": \"Office environment sounds\",\n");
                prompt.append("  \"tts_ready\": true\n");
                prompt.append("}\n");
                prompt.append("```\n");
                prompt.append("**Required fields:** duration_estimate, speaker_count, accent_recommendation, pacing_notes, background_ambient\n\n");

                // TRANSCRIPT FORMATTING - CRITICAL
                prompt.append("### Transcript Formatting (CRITICAL)\n");
                prompt.append("The transcript MUST follow this EXACT format with BLANK LINES between speaker turns:\n\n");
                prompt.append("```\n");
                prompt.append("SPEAKER_NAME: First speaker's complete turn. This should be a full sentence or paragraph of natural dialogue.\n");
                prompt.append("\n");
                prompt.append("SECOND_SPEAKER: Response from second speaker. Continue the conversation naturally.\n");
                prompt.append("\n");
                prompt.append("SPEAKER_NAME: Next turn from first speaker...\n");
                prompt.append("```\n\n");
                prompt.append("**CRITICAL RULES:**\n");
                prompt.append("1. Each speaker turn starts with `SPEAKER_NAME:` followed by their dialogue\n");
                prompt.append("2. **BLANK LINE between turns** - Use `\\n\\n` (double newline) to separate speaker turns\n");
                prompt.append("3. Use ALL CAPS for speaker labels (e.g., DR. RICHARDS:, MAYA:, SAM:)\n");
                prompt.append("4. Each turn should be a complete thought (1-4 sentences typically)\n");
                prompt.append("5. For monologues (Parts 2, 4), break into logical paragraphs with blank lines between\n\n");

                // Output Requirements
                prompt.append("### Output Requirements\n");
                prompt.append("Return valid JSON with exactly:\n");
                prompt.append("1. `transcript`: The full dialogue/monologue with speaker labels. Use `\\n\\n` between turns.\n");
                prompt.append("2. `audio_placeholder`: Metadata object with all required fields.\n");
                prompt.append("Do NOT include `questions` or `section_layout`.\n");

                return prompt.toString();
        }

        /**
         * PASS 2: Build prompt for Question Generation ONLY (using existing
         * transcript).
         * ENHANCED: Includes all critical formatting rules from buildListeningPrompt.
         */
        public String buildListeningQuestionsPrompt(GenerationRequestDTO request, String transcript) {
                StringBuilder prompt = new StringBuilder();

                prompt.append("## TASK: Generate IELTS Listening Questions (Phase 2/3)\n\n");
                prompt.append("Based on the provided transcript, generate the exam QUESTION STEMS only.\n");
                prompt.append("**DO NOT produce `correct_answer` or `explanation` here** - those are generated in a separate Phase 3.\n\n");

                // CRITICAL: block_type constraint at the TOP
                prompt.append("### CRITICAL: Valid block_type Values (EXACT STRINGS ONLY)\n");
                prompt.append("You MUST use EXACTLY one of these strings for block_type:\n");
                prompt.append("- `NOTE_COMPLETION` - for fill-in-blank/note completion\n");
                prompt.append("- `INSTRUCTIONS_ONLY` - for multiple choice questions\n");
                prompt.append("- `MATCHING_FEATURES` - for matching questions\n");
                prompt.append("- `PLAN_MAP_DIAGRAM_LABELING` - for map/diagram labeling\n\n");
                prompt.append("**ANY OTHER VALUE WILL CAUSE FAILURE.**\n\n");

                // Provide the transcript
                prompt.append("### Source Transcript\n");
                prompt.append("```\n").append(transcript).append("\n```\n\n");

                Integer partNumber = request.getPartNumber() != null ? request.getPartNumber() : 1;
                int startNumber = (partNumber - 1) * 10 + 1;

                // Part-specific requirements
                prompt.append("### Part ").append(partNumber).append(" Requirements\n");
                switch (partNumber) {
                        case 1:
                                buildListeningPart1Prompt(prompt);
                                break;
                        case 2:
                                buildListeningPart2Prompt(prompt);
                                break;
                        case 3:
                                buildListeningPart3Prompt(prompt);
                                break;
                        case 4:
                                buildListeningPart4Prompt(prompt);
                                break;
                }

                // === REQUESTED QUESTION TYPES (CRITICAL - USER CONFIG) ===
                List<String> requestedTypes = request.getQuestionTypes();
                if ((requestedTypes == null || requestedTypes.isEmpty())
                                && request.getQuestionTypeCounts() != null
                                && !request.getQuestionTypeCounts().isEmpty()) {
                        requestedTypes = new ArrayList<>(request.getQuestionTypeCounts().keySet());
                }

                if (requestedTypes != null && !requestedTypes.isEmpty()) {
                        prompt.append("\n### Requested Question Types (MUST INCLUDE ALL)\n");
                        prompt.append("The user has specifically requested these question types. You MUST include ALL of them:\n");
                        if (request.getQuestionTypeCounts() != null && !request.getQuestionTypeCounts().isEmpty()) {
                                request.getQuestionTypeCounts().forEach((type, count) -> {
                                        prompt.append("- **").append(type).append("**: ").append(count)
                                                        .append(" questions\n");
                                });
                        } else {
                                for (String type : requestedTypes) {
                                        prompt.append("- **").append(type).append("**\n");
                                }
                        }
                        prompt.append("\n");
                }

                // === CRITICAL FORMATTING RULES ===
                prompt.append("\n### Question Numbering (STRICT)\n");
                prompt.append("- Use sequential numbers from ").append(startNumber)
                                .append(" to ").append(startNumber + 9).append(" for Part ").append(partNumber)
                                .append(".\n\n");

                // Section layout requirements
                prompt.append("### Section Layout (CRITICAL - EXACT FORMAT REQUIRED)\n");
                prompt.append("The `section_layout` MUST be an object with a `blocks` array:\n");
                prompt.append("```json\n");
                prompt.append("{\n");
                prompt.append("  \"blocks\": [\n");
                prompt.append("    {\n");
                prompt.append("      \"block_type\": \"NOTE_COMPLETION\",\n");
                prompt.append("      \"content\": {\n");
                prompt.append("        \"title\": \"Questions ").append(startNumber).append("-").append(startNumber + 9)
                                .append("\",\n");
                prompt.append("        \"main_title\": \"Topic Title Here\",\n");
                prompt.append("        \"instructions_text\": \"<b>Instructions</b><br/>Complete the notes below.\"\n");
                prompt.append("      },\n");
                prompt.append("      \"question_numbers\": [").append(startNumber);
                for (int i = 1; i < 10; i++)
                        prompt.append(",").append(startNumber + i);
                prompt.append("]\n");
                prompt.append("    }\n");
                prompt.append("  ]\n");
                prompt.append("}\n");
                prompt.append("```\n");
                prompt.append("Block types: NOTE_COMPLETION, INSTRUCTIONS_ONLY, MATCHING_FEATURES, PLAN_MAP_DIAGRAM_LABELING\n\n");

                // CRITICAL: question_numbers requirement
                prompt.append("### MANDATORY: question_numbers Array (EVERY BLOCK MUST HAVE THIS)\n");
                prompt.append("Every block in section_layout.blocks[] MUST include:\n");
                prompt.append("`\"question_numbers\": [list of question numbers this block contains]`\n\n");

                // Question Content Format for FILL_IN_BLANK / NOTE_COMPLETION
                prompt.append("### Question Content Format for FILL_IN_BLANK / NOTE_COMPLETION\n");
                prompt.append("**CRITICAL: INLINE STRATEGY (Each question has its own text)**\n");
                prompt.append("- **Each question object MUST contain its specific bullet point or sentence fragment.**\n");
                prompt.append("- **Do NOT leave the 'text' field empty for any question.**\n");
                prompt.append("- **BLANK FORMAT**: Use `<strong>{number}</strong> ____` (bold number followed by 4 underscores).\n");
                prompt.append("- **section_title**: Optional, use to create section headers.\n\n");

                prompt.append("Example (generating Q").append(startNumber).append(", Q").append(startNumber + 1)
                                .append("):\n");
                prompt.append("```json\n");
                prompt.append("// Question ").append(startNumber).append("\n");
                prompt.append("{\"text\": \"• making sure the beach does not have <strong>").append(startNumber)
                                .append("</strong> ____ on it\", \"section_title\": \"Beach\"}\n");
                prompt.append("// Question ").append(startNumber + 1).append("\n");
                prompt.append("{\"text\": \"• no <strong>").append(startNumber + 1)
                                .append("</strong> ____ allowed\"}\n");
                prompt.append("```\n");
                prompt.append("**Failure to include text in each question will cause the test to render blank/broken questions.**\n");
                prompt.append("Always include `word_limit` for ALL questions (e.g., \"ONE WORD ONLY\").\n\n");

                // Multiple Choice Format - ENHANCED
                prompt.append("### Multiple Choice Format (CRITICAL)\n");
                prompt.append("**block_type for MC questions**: Use `INSTRUCTIONS_ONLY` (NOT `MULTIPLE_CHOICE` - that is invalid!)\n\n");
                prompt.append("**MULTIPLE_CHOICE** (single answer):\n");
                prompt.append("- `question_content.options`: array of strings `[\"A ...\", \"B ...\", \"C ...\"]`\n");
                prompt.append("- Do NOT include `correct_answer` (added in Phase 3)\n\n");
                prompt.append("**MULTIPLE_CHOICE_MULTIPLE_ANSWERS** (Choose TWO letters) - STRICT FORMAT:\n");
                prompt.append("- Create **TWO consecutive questions** (e.g., Q21 and Q22)\n");
                prompt.append("- **BOTH questions MUST have IDENTICAL** `text` and `options`\n");
                prompt.append("- Example (stems only - no correct_answer):\n");
                prompt.append("```json\n");
                prompt.append("[\n");
                prompt.append("  {\"question_number\": 21, \"question_type\": \"MULTIPLE_CHOICE_MULTIPLE_ANSWERS\",\n");
                prompt.append("   \"question_content\": {\"text\": \"Which TWO benefits are mentioned?\",\n");
                prompt.append("    \"options\": [\"A Lower cost\", \"B Better quality\", \"C Faster delivery\", \"D More options\", \"E Eco-friendly\"]}},\n");
                prompt.append("  {\"question_number\": 22, \"question_type\": \"MULTIPLE_CHOICE_MULTIPLE_ANSWERS\",\n");
                prompt.append("   \"question_content\": {\"text\": \"Which TWO benefits are mentioned?\",\n");
                prompt.append("    \"options\": [\"A Lower cost\", \"B Better quality\", \"C Faster delivery\", \"D More options\", \"E Eco-friendly\"]}}\n");
                prompt.append("]\n");
                prompt.append("```\n\n");

                // Matching Format
                prompt.append("### Matching Format\n");
                prompt.append("- MATCHING questions: `question_content` must include only `text`.\n");
                prompt.append("- All options belong in the block content (`section_layout.blocks[].content.options`).\n");
                prompt.append("- **CRITICAL**: Options MUST be an array of `{letter, text}` objects, NOT strings.\n\n");

                // FILL_IN_BLANK word_limit requirement
                prompt.append("### FILL_IN_BLANK: word_limit Requirement (MANDATORY)\n");
                prompt.append("Every FILL_IN_BLANK question MUST have a `word_limit` field with one of these values:\n");
                prompt.append("- `\"ONE WORD ONLY\"`\n");
                prompt.append("- `\"ONE WORD AND/OR A NUMBER\"`\n");
                prompt.append("- `\"NO MORE THAN TWO WORDS\"`\n");
                prompt.append("- `\"NO MORE THAN THREE WORDS\"`\n\n");

                // Validation Checklist - ENHANCED
                prompt.append("### MANDATORY VALIDATION CHECKLIST (Verify before output)\n\n");
                prompt.append("**Block Requirements (CRITICAL):**\n");
                prompt.append("  - [ ] `block_type` is ONLY one of: `NOTE_COMPLETION`, `INSTRUCTIONS_ONLY`, `MATCHING_FEATURES`, `PLAN_MAP_DIAGRAM_LABELING`\n");
                prompt.append("  - [ ] **DO NOT USE** `MULTIPLE_CHOICE` as block_type (use `INSTRUCTIONS_ONLY` for MC questions)\n");
                prompt.append("  - [ ] Every block has `question_numbers` array\n");
                prompt.append("  - [ ] **Each question number appears in ONLY ONE block** (no duplicates across blocks)\n");
                prompt.append("  - [ ] Every block has `content.instructions_text` (HTML string, even if brief)\n\n");

                prompt.append("**Question Requirements:**\n");
                prompt.append("  - [ ] Every FILL_IN_BLANK question has `word_limit` field\n");
                prompt.append("  - [ ] MCMA questions appear as TWO consecutive questions with identical text/options\n");
                prompt.append("  - [ ] **NO `correct_answer` and NO `explanation`** on any question (Phase 3 adds them)\n\n");

                prompt.append("### Output Requirements\n");
                prompt.append("Return valid JSON with:\n");
                prompt.append("1. `section_layout`: The block layout.\n");
                prompt.append("2. `questions`: The array of 10 question STEMS (no answers/explanations).\n");
                prompt.append("3. `figure_description`: If needed for Part 2 map/plan.\n");
                prompt.append("Do NOT re-output the transcript. Do NOT include correct_answer or explanation.\n");

                return prompt.toString();
        }

        /**
         * Build the prompt for Phase 3/3 of Listening generation: answers + explanations.
         *
         * <p>Given the previously generated transcript and question stems, the model
         * produces an {@code answers} array keyed by {@code question_number}. Each entry
         * carries the correct answer, a structured Vietnamese explanation, and the
         * supporting transcript evidence.</p>
         */
        public String buildListeningAnswersPrompt(GenerationRequestDTO request, String transcript,
                        String questionsJson) {
                StringBuilder prompt = new StringBuilder();

                prompt.append("## TASK: Generate IELTS Listening Answers & Explanations (Phase 3/3)\n\n");
                prompt.append("You are given the transcript and the question stems already produced.\n");
                prompt.append("Produce the correct answers and explanations for EVERY question.\n\n");

                prompt.append("### Source Transcript\n");
                prompt.append("```\n").append(transcript).append("\n```\n\n");

                prompt.append("### Question Stems (JSON)\n");
                prompt.append("```json\n").append(questionsJson).append("\n```\n\n");

                prompt.append("### Answer Rules (STRICT)\n");
                prompt.append("1. Output ONE entry in `answers[]` for EVERY `question_number` present in the stems.\n");
                prompt.append("2. **FILL_IN_BLANK / NOTE_COMPLETION**: `correct_answer` words MUST appear VERBATIM in the transcript above.\n");
                prompt.append("3. **MULTIPLE_CHOICE**: `correct_answer` is a single-element array, e.g. `[\"B\"]`.\n");
                prompt.append("4. **MULTIPLE_CHOICE_MULTIPLE_ANSWERS**: BOTH paired questions share the SAME `correct_answer` array with EXACTLY 2 letters.\n");
                prompt.append("5. Each entry MUST include `evidence_from_transcript`: the EXACT English sentence(s) proving the answer.\n\n");

                prompt.append("### Explanation Format (Vietnamese)\n");
                prompt.append("Each `explanation` is a JSON object:\n");
                prompt.append("```json\n");
                prompt.append("{\n");
                prompt.append("  \"detail\": \"<Vietnamese: why is this correct?>\",\n");
                prompt.append("  \"quote\": \"<EXACT English quote from transcript that PROVES the answer>\",\n");
                prompt.append("  \"strategy\": \"<Vietnamese strategy tip for this question type>\"\n");
                prompt.append("}\n");
                prompt.append("```\n\n");

                prompt.append("### Output Requirements\n");
                prompt.append("Return valid JSON with a single top-level key `answers` (array).\n");
                prompt.append("Each item: `question_number` (int), `correct_answer` (array of string), `explanation` (object), `evidence_from_transcript` (string).\n");
                prompt.append("Do NOT re-output the transcript or the question stems.\n");

                return prompt.toString();
        }
}