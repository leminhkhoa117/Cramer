package com.cramer.service.abts.prompt;

import com.cramer.dto.abts.GenerationRequestDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.cramer.service.abts.prompt.PromptFragments.structuredExplanationFormat;
import static com.cramer.service.abts.prompt.PromptFragments.wordLimitFormat;

public class ReadingPromptBuilder {

        /**
         * Build complete prompt for Reading content generation.
         */
        public String buildReadingPrompt(GenerationRequestDTO request) {
                StringBuilder prompt = new StringBuilder();

                Integer partNumber = request.getPartNumber();
                if (partNumber == null) {
                        partNumber = 1;
                }

                prompt.append("## TASK: Generate IELTS Academic Reading Passage with Questions\n\n");

                // Part-specific context
                prompt.append("### Part ").append(partNumber).append(" Specifications\n");
                switch (partNumber) {
                        case 1:
                                prompt.append("- **Context**: General interest, social, or factual topic.\n");
                                prompt.append("- **Style**: Descriptive and factual.\n");
                                prompt.append("- **Word Count**: 900-1000 words.\n");
                                break;
                        case 2:
                                prompt.append("- **Context**: Workplace, training, or general interest topic.\n");
                                prompt.append("- **Style**: Discursive, logical argument or detailed description.\n");
                                prompt.append("- **Word Count**: 1000-1100 words.\n");
                                break;
                        case 3:
                                prompt.append("- **Context**: Complex academic topic.\n");
                                prompt.append("- **Style**: Argumentative, abstract, complex sentence structures.\n");
                                prompt.append("- **Word Count**: 1100-1200 words.\n");
                                break;
                }
                prompt.append("\n");

                // Topic and requirements
                prompt.append("### Topic Information\n");
                prompt.append("- **Topic**: ").append(request.getTopic()).append("\n");
                prompt.append("- **Difficulty**: ").append(request.getDifficulty().getDisplayName())
                                .append(" (Band ").append(request.getDifficulty().getBandRange()).append(")\n");
                prompt.append("- **Test Type**: ").append(request.getTestType()).append("\n");
                prompt.append("- **Explanation Language**: ").append(
                                request.getExplanationLanguage() == GenerationRequestDTO.ExplanationLanguage.VI
                                                ? "Vietnamese (tiếng Việt) - ONLY for explanations!"
                                                : "English")
                                .append("\n");
                prompt.append("⚠️ CRITICAL: The passage and ALL question text must ALWAYS be in ENGLISH. ");
                prompt.append("Only the 'explanation' field should be in the language specified above.\n\n");

                // STRUCTURED EXPLANATION FORMAT (3 fields only - correct_answer is stored
                // separately)
                prompt.append(structuredExplanationFormat());

                // Check if using existing passage
                if (request.getExistingPassageText() != null && !request.getExistingPassageText().isEmpty()) {
                        prompt.append("### PROVIDED PASSAGE (Do NOT rewrite this passage, generate questions based on it)\n");
                        prompt.append(request.getExistingPassageText()).append("\n\n");

                        prompt.append("### Task Requirement\n");
                        prompt.append("You must generate IELTS Reading questions based EXACTLY on the text provided above.\n");
                        prompt.append("Do NOT generate a new passage. Use the provided text as the source material.\n\n");
                } else {
                        // Facts handling (Auto vs Custom)
                        List<String> facts = request.getFacts();
                        boolean hasRichContext = facts != null && facts.size() >= 5;

                        if (hasRichContext) {
                                prompt.append("### Content Source (Strict Mode)\n");
                                prompt.append("You MUST base the passage content primarily on the following verified facts.\n");
                                prompt.append("You may connect them with logical transitions, but do not contradict them.\n\n");
                                for (int i = 0; i < Objects.requireNonNull(facts).size(); i++) {
                                        prompt.append(String.format("%d. %s\n", i + 1, facts.get(i)));
                                }
                        } else {
                                prompt.append("### Content Generation Mode (Research Mode)\n");
                                prompt.append("You are acting as a researcher and writer.\n");
                                prompt.append("1. Research the topic: '").append(request.getTopic()).append("'\n");
                                if (facts != null && !facts.isEmpty()) {
                                        prompt.append("2. Incorporate these key points: ")
                                                        .append(String.join("; ", facts)).append("\n");
                                } else {
                                        prompt.append("2. No facts provided. Use realistic, verifiable details.\n");
                                }
                                prompt.append("3. Create a comprehensive, academic article suitable for IELTS Reading.\n");
                                prompt.append(
                                                "4. Invent plausible academic details (names, dates, studies) if needed to ensure density and length, but keep them realistic.\n");
                        }
                        prompt.append("\n");

                        // Passage requirements
                        prompt.append("### Passage Requirements\n");
                        if (partNumber == 1) {
                                prompt.append("- **Word count**: 900-1000 words.\n");
                        } else if (partNumber == 2) {
                                prompt.append("- **Word count**: 1000-1100 words.\n");
                        } else {
                                prompt.append("- **Word count**: 1100-1200 words.\n");
                        }
                        if (request.getPassageLength() != null) {
                                prompt.append("- **Passage length preference**: ").append(request.getPassageLength())
                                                .append(" (aim for the lower end if SHORT, upper end if LONG).\n");
                        }
                        prompt.append("- **Structure**: Use paragraphs labeled A, B, C... with <strong> tags\n");
                        prompt.append("- **Paragraph Format**: Each paragraph must be separated by TWO newlines (blank line).\n");
                        prompt.append(
                                        "  Example: <strong>A.</strong> First paragraph text...\n\n<strong>B.</strong> Second paragraph...\n");
                        prompt.append(
                                        "- **Style**: Academic, formal, information-dense with specific names, dates, and statistics.\n");
                        prompt.append("- **Language**: ENGLISH ONLY for the passage text.\n");
                        prompt.append("- **Format**: HTML tags for formatting.\n\n");
                }

                // CRITICAL: Word Limit Format Section
                prompt.append(wordLimitFormat());

                // Question requirements
                prompt.append("### Question Requirements\n");
                // Set correct starting number based on part (IELTS Reading standard)
                // Part 1: Q1-13, Part 2: Q14-26, Part 3: Q27-40
                int startNumber;
                if (partNumber == 1) {
                        startNumber = 1;
                } else if (partNumber == 2) {
                        startNumber = 14;
                } else {
                        startNumber = 27; // Part 3
                }
                // Calculate effective total questions to avoid conflicts
                // Cambridge IELTS: Part 1 has 13, Part 2 has 13, Part 3 has 14 questions
                int effectiveTotalQuestions = (partNumber == 3) ? 14 : 13;
                boolean isDerivedFromCounts = false;

                if (request.getQuestionTypeCounts() != null && !request.getQuestionTypeCounts().isEmpty()) {
                        int sumCounts = request.getQuestionTypeCounts().values().stream().mapToInt(Integer::intValue)
                                        .sum();
                        if (sumCounts > 0) {
                                effectiveTotalQuestions = sumCounts;
                                isDerivedFromCounts = true;
                        }
                } else if (request.getTotalQuestions() != null) {
                        effectiveTotalQuestions = request.getTotalQuestions();
                }

                prompt.append("- **Total questions**: ").append(effectiveTotalQuestions).append(" questions");
                if (isDerivedFromCounts) {
                        prompt.append(" (Strictly sum of requested question types)\n");
                } else {
                        prompt.append("\n");
                }
                prompt.append("- **Numbering**: Use sequential numbering starting at ").append(startNumber)
                                .append(" (Part ").append(partNumber).append(" starts at Q").append(startNumber)
                                .append(").\n");
                prompt.append("- **⚠️ INLINE NUMBERS CRITICAL**: For completion questions, the bold number inside `<strong>X</strong> ____` MUST match the question_number. Example: Q")
                                .append(startNumber).append(" uses `<strong>").append(startNumber)
                                .append("</strong> ____`.\n");
                prompt.append("- **word_limit**: Include `word_limit` for every question (null if not applicable).\n");
                prompt.append("- **Question types to include**:\n");

                List<String> requestedTypes = request.getQuestionTypes();
                if ((requestedTypes == null || requestedTypes.isEmpty())
                                && request.getQuestionTypeCounts() != null
                                && !request.getQuestionTypeCounts().isEmpty()) {
                        requestedTypes = new ArrayList<>(request.getQuestionTypeCounts().keySet());
                }

                if (requestedTypes != null && !requestedTypes.isEmpty()) {
                        prompt.append("### Question Structure (Follow EXACTLY)\n");
                        if (request.getTotalQuestions() != null) {
                                prompt.append("You must organize the ")
                                                .append(request.getTotalQuestions())
                                                .append(" questions into DISTINCT GROUPS based on the requested types.\n");
                        } else {
                                prompt.append(
                                                "You must organize the 13-14 questions into 3 DISTINCT GROUPS based on the requested types.\n");
                        }

                        if (request.getQuestionTypeCounts() != null && !request.getQuestionTypeCounts().isEmpty()) {
                                prompt.append("### Exact Question Type Counts (STRICT)\n");
                                request.getQuestionTypeCounts().forEach((type, count) -> {
                                        prompt.append("- ").append(type).append(": ").append(count).append("\n");
                                });
                                prompt.append("Ensure the total count matches the required total.\n\n");
                        }

                        // === INLINE STRATEGY: SUMMARY_COMPLETION, FILL_IN_BLANK, NOTE_COMPLETION ===
                        prompt.append(
                                        "\n#### INLINE STRATEGY for SUMMARY_COMPLETION, FILL_IN_BLANK, NOTE_COMPLETION (CRITICAL):\n");
                        prompt.append(
                                        "- **Each question object MUST contain the specific sentence or text fragment containing its blank.**\n");
                        prompt.append("- **Do NOT leave the 'text' field empty for any question.**\n");
                        prompt.append("- **Format Example** (generating 2 questions, Q1 and Q2):\n");
                        prompt.append("```json\n");
                        prompt.append("[\n");
                        prompt.append("  {\n");
                        prompt.append("    \"question_number\": 1,\n");
                        prompt.append(
                                        "    \"question_content\": { \"text\": \"The <strong>1</strong> ____ of London increased rapidly...\" },\n");
                        prompt.append("    \"correct_answer\": [\"population\"]\n");
                        prompt.append("  },\n");
                        prompt.append("  {\n");
                        prompt.append("    \"question_number\": 2,\n");
                        prompt.append(
                                        "    \"question_content\": { \"text\": \"...to move people to better housing in the <strong>2</strong> ____\" },\n");
                        prompt.append("    \"correct_answer\": [\"suburbs\"]\n");
                        prompt.append("  }\n");
                        prompt.append("]\n");
                        prompt.append("```\n");
                        prompt.append(
                                        "- **Failure to include text in each question will cause the test to render broken/blank questions.**\n");

                        // === BLOCK STRATEGY: TABLE_COMPLETION, FLOW_CHART_COMPLETION ONLY ===
                        prompt.append(
                                        "\n#### BLOCK STRATEGY for TABLE_COMPLETION and FLOW_CHART_COMPLETION ONLY:\n");
                        prompt.append(
                                        "- For these types ONLY, the **FIRST question** holds the ENTIRE HTML table or flowchart.\n");
                        prompt.append("- Subsequent questions in the group MUST have `\"text\": \"\"` (empty string).\n");
                        prompt.append("- The frontend renders the table/chart once and creates input boxes below.\n");

                        prompt.append("\n#### IELTS Reading Question Type Rules (STRICT)\n");
                        prompt.append("- Always include `word_limit` on every question (use null for non-completion types).\n\n");

                        // INLINE COMPLETION TYPES
                        prompt.append(
                                        "##### Inline Completion Types (`FILL_IN_BLANK`, `SUMMARY_COMPLETION`, `NOTE_COMPLETION`):\n");
                        prompt.append(
                                        "- **BLANK FORMAT**: Use `<strong>{number}</strong> ____` (bold number followed by 4 underscores).\n");
                        prompt.append(
                                        "- **CRITICAL**: Each question's `text` field MUST contain its own sentence fragment with one blank.\n");
                        prompt.append(
                                        "- Add `word_limit`: `ONE WORD ONLY`, `NO MORE THAN TWO WORDS`, or `ONE WORD AND/OR A NUMBER`.\n\n");

                        // BLOCK COMPLETION TYPES
                        prompt.append("##### Block Completion Types (`TABLE_COMPLETION`, `FLOW_CHART_COMPLETION`):\n");
                        prompt.append("- Q1 contains full HTML structure with ALL numbered blanks. Q2+ have empty `text`.\n");
                        prompt.append("- **CRITICAL FORMAT**: Each blank must be: `<strong>N</strong> ____` where N is the question number.\n");
                        prompt.append("- Example TABLE_COMPLETION HTML for Q1:\n");
                        prompt.append("```html\n");
                        prompt.append("<table>\n");
                        prompt.append("  <thead><tr><th>Topic</th><th>Key Detail</th></tr></thead>\n");
                        prompt.append("  <tbody>\n");
                        prompt.append("    <tr><td>First item</td><td><strong>5</strong> ____</td></tr>\n");
                        prompt.append("    <tr><td>Second item</td><td><strong>6</strong> ____</td></tr>\n");
                        prompt.append("    <tr><td>Third item</td><td>Use of <strong>7</strong> ____ (e.g., examples)</td></tr>\n");
                        prompt.append("  </tbody>\n");
                        prompt.append("</table>\n");
                        prompt.append("```\n");
                        prompt.append("- Q2+ (subsequent questions) have `\"text\": \"\"` (empty string).\n\n");

                        // DIAGRAM LABEL COMPLETION
                        prompt.append("##### `DIAGRAM_LABEL_COMPLETION` (IELTS Diagram/Flowchart):\n\n");
                        prompt.append("**Structure Requirements:**\n");
                        prompt.append("- Include `diagram` JSON object with `direction` and `nodes` array.\n");
                        prompt.append("- **CRITICAL**: `nodes` MUST be an array of OBJECTS with specific fields.\n");
                        prompt.append("- Each node object MUST have: `id`, `type`, `label`.\n");
                        prompt.append("- Node types: `start`, `step`, `blank`, `end`.\n\n");
                        prompt.append("**Blank Node Format (IELTS-style):**\n");
                        prompt.append("- `blank` nodes MUST have:\n");
                        prompt.append("  - `question_number`: integer for user input\n");
                        prompt.append("  - `label`: text WITH `____` placeholder where answer goes\n");
                        prompt.append("- The `____` shows where the test-taker fills in their answer.\n");
                        prompt.append("- Example: `\"label\": \"Brain perceives ____ is unnecessary\"`\n\n");
                        prompt.append("**CORRECT Format (IELTS-style hybrid):**\n");
                        prompt.append("```json\n");
                        prompt.append("{\n");
                        prompt.append("  \"diagram\": {\n");
                        prompt.append("    \"direction\": \"vertical\",\n");
                        prompt.append("    \"nodes\": [\n");
                        prompt.append("      { \"id\": \"n1\", \"label\": \"Raw materials collected\", \"type\": \"start\" },\n");
                        prompt.append("      { \"id\": \"n2\", \"label\": \"Materials undergo ____\", \"type\": \"blank\", \"question_number\": 9 },\n");
                        prompt.append("      { \"id\": \"n3\", \"label\": \"Quality control check\", \"type\": \"step\" },\n");
                        prompt.append("      { \"id\": \"n4\", \"label\": \"Products stored in ____\", \"type\": \"blank\", \"question_number\": 10 },\n");
                        prompt.append("      { \"id\": \"n5\", \"label\": \"Distribution to retailers\", \"type\": \"end\" }\n");
                        prompt.append("    ]\n");
                        prompt.append("  }\n");
                        prompt.append("}\n");
                        prompt.append("```\n\n");
                        prompt.append("**WRONG Formats:**\n");
                        prompt.append("```json\n");
                        prompt.append("// ❌ Nodes as strings:\n");
                        prompt.append("{ \"nodes\": [\"Step 1\", \"Step 2\"] }\n\n");

                        prompt.append("// ❌ Blank node without label (just input box):\n");
                        prompt.append("{ \"id\": \"n2\", \"type\": \"blank\", \"question_number\": 9 }\n");
                        prompt.append("```\n\n");

                        // MULTIPLE CHOICE
                        prompt.append("##### `MULTIPLE_CHOICE`:\n");
                        prompt.append("- Options array must be strings like `\"A ...\", \"B ...\", \"C ...\", \"D ...\"`.\n");
                        prompt.append("- `correct_answer` is a single-element array: `[\"B\"]`.\n\n");

                        // MULTIPLE CHOICE MULTIPLE ANSWERS
                        prompt.append("##### `MULTIPLE_CHOICE_MULTIPLE_ANSWERS` (Choose TWO letters):\n");
                        prompt.append("- **CRITICAL**: Create TWO consecutive questions with IDENTICAL `text` and `options`.\n");
                        prompt.append("- BOTH questions have the SAME `correct_answer` array with TWO letters.\n");
                        prompt.append("- This matches Cambridge IELTS format where one stem tests two answers.\n");
                        prompt.append("- Example (Q5 and Q6 are a pair):\n");
                        prompt.append("```json\n");
                        prompt.append("[\n");
                        prompt.append("  {\n");
                        prompt.append("    \"question_number\": 5,\n");
                        prompt.append("    \"question_type\": \"MULTIPLE_CHOICE_MULTIPLE_ANSWERS\",\n");
                        prompt.append("    \"question_content\": {\n");
                        prompt.append("      \"text\": \"Which TWO advantages of electric cars are mentioned in the passage?\",\n");
                        prompt.append(
                                        "      \"options\": [\"A Lower running costs\", \"B Quieter operation\", \"C Faster acceleration\", \"D Reduced emissions\", \"E Longer range\"]\n");
                        prompt.append("    },\n");
                        prompt.append("    \"correct_answer\": [\"B\", \"D\"],\n");
                        prompt.append("    \"word_limit\": null\n");
                        prompt.append("  },\n");
                        prompt.append("  {\n");
                        prompt.append("    \"question_number\": 6,\n");
                        prompt.append("    \"question_type\": \"MULTIPLE_CHOICE_MULTIPLE_ANSWERS\",\n");
                        prompt.append("    \"question_content\": {\n");
                        prompt.append("      \"text\": \"Which TWO advantages of electric cars are mentioned in the passage?\",\n");
                        prompt.append(
                                        "      \"options\": [\"A Lower running costs\", \"B Quieter operation\", \"C Faster acceleration\", \"D Reduced emissions\", \"E Longer range\"]\n");
                        prompt.append("    },\n");
                        prompt.append("    \"correct_answer\": [\"B\", \"D\"],\n");
                        prompt.append("    \"word_limit\": null\n");
                        prompt.append("  }\n");
                        prompt.append("]\n");
                        prompt.append("```\n\n");

                        // MATCHING INFORMATION
                        prompt.append("##### `MATCHING_INFORMATION`:\n");
                        prompt.append("- Options array is simply the list of paragraph letters: `[\"A\", \"B\", \"C\", ...]`.\n");
                        prompt.append("- `text` contains the statement to match.\n\n");

                        // MATCHING HEADINGS
                        prompt.append("##### `MATCHING_HEADINGS`:\n");
                        prompt.append("- Options array of `{letter, text}` objects with roman numerals (i, ii, iii...).\n");
                        prompt.append("- Options MUST be IDENTICAL for all questions in the group.\n");
                        prompt.append("- `text` contains the paragraph reference (e.g., \"Paragraph A\").\n\n");

                        // MATCHING FEATURES
                        prompt.append("##### `MATCHING_FEATURES`:\n");
                        prompt.append("- Options array of `{letter, text}` objects (A, B, C...).\n");
                        prompt.append("- Options MUST be IDENTICAL for all questions in the group.\n");
                        prompt.append("- `text` contains the statement to match to a person/category.\n\n");

                        // MATCHING SENTENCE ENDINGS
                        prompt.append("##### `MATCHING_SENTENCE_ENDINGS` (CRITICAL FORMAT):\n");
                        prompt.append("- Each question's `text` is an INCOMPLETE sentence stem (no period at end).\n");
                        prompt.append("- Options array contains sentence endings with roman numerals (i, ii, iii...).\n");
                        prompt.append("- Options MUST be IDENTICAL for ALL questions in the group.\n");
                        prompt.append("- Example:\n");
                        prompt.append("```json\n");
                        prompt.append("[\n");
                        prompt.append("  {\n");
                        prompt.append("    \"question_number\": 1,\n");
                        prompt.append("    \"question_type\": \"MATCHING_SENTENCE_ENDINGS\",\n");
                        prompt.append("    \"question_content\": {\n");
                        prompt.append(
                                        "      \"text\": \"Electric vehicles are considered environmentally friendly because they\",\n");
                        prompt.append("      \"options\": [\n");
                        prompt.append("        {\"letter\": \"i\", \"text\": \"produce zero direct emissions while driving.\"},\n");
                        prompt.append(
                                        "        {\"letter\": \"ii\", \"text\": \"require less frequent maintenance than conventional cars.\"},\n");
                        prompt.append(
                                        "        {\"letter\": \"iii\", \"text\": \"can be powered by renewable energy sources.\"},\n");
                        prompt.append("        {\"letter\": \"iv\", \"text\": \"have regenerative braking systems.\"},\n");
                        prompt.append("        {\"letter\": \"v\", \"text\": \"are quieter than petrol-powered vehicles.\"}\n");
                        prompt.append("      ]\n");
                        prompt.append("    },\n");
                        prompt.append("    \"correct_answer\": [\"i\"],\n");
                        prompt.append("    \"word_limit\": null\n");
                        prompt.append("  },\n");
                        prompt.append("  {\n");
                        prompt.append("    \"question_number\": 2,\n");
                        prompt.append("    \"question_type\": \"MATCHING_SENTENCE_ENDINGS\",\n");
                        prompt.append("    \"question_content\": {\n");
                        prompt.append("      \"text\": \"The main barrier to widespread EV adoption remains\",\n");
                        prompt.append("      \"options\": [\n");
                        prompt.append("        {\"letter\": \"i\", \"text\": \"produce zero direct emissions while driving.\"},\n");
                        prompt.append(
                                        "        {\"letter\": \"ii\", \"text\": \"require less frequent maintenance than conventional cars.\"},\n");
                        prompt.append(
                                        "        {\"letter\": \"iii\", \"text\": \"can be powered by renewable energy sources.\"},\n");
                        prompt.append("        {\"letter\": \"iv\", \"text\": \"have regenerative braking systems.\"},\n");
                        prompt.append("        {\"letter\": \"v\", \"text\": \"are quieter than petrol-powered vehicles.\"}\n");
                        prompt.append("      ]\n");
                        prompt.append("    },\n");
                        prompt.append("    \"correct_answer\": [\"iv\"],\n");
                        prompt.append("    \"word_limit\": null\n");
                        prompt.append("  }\n");
                        prompt.append("]\n");
                        prompt.append("```\n\n");

                        // SUMMARY COMPLETION OPTIONS
                        prompt.append("##### `SUMMARY_COMPLETION_OPTIONS` (CRITICAL FORMAT):\n");
                        prompt.append("- **EACH question MUST include an `options` array** with `{letter, text}` objects.\n");
                        prompt.append("- 🚨 **CRITICAL: The `options` array MUST be IDENTICAL for ALL questions in the group.**\n");
                        prompt.append("- 🚨 **DO NOT split options between questions!** Every question gets ALL 10 options (A-J).\n");
                        prompt.append("- ❌ WRONG: Q36-37 have A-F, Q38-40 have G-J (splitting options)\n");
                        prompt.append("- ✅ CORRECT: ALL questions have the same complete A-J options array\n");
                        prompt.append("- Each question's `text` has ONE blank with `<strong>{number}</strong> ____`.\n");
                        prompt.append("- Example (ALL questions in group have IDENTICAL options):\n");
                        prompt.append("```json\n");
                        prompt.append("[\n");
                        prompt.append("  {\n");
                        prompt.append("    \"question_number\": 8,\n");
                        prompt.append("    \"question_type\": \"SUMMARY_COMPLETION_OPTIONS\",\n");
                        prompt.append("    \"question_content\": {\n");
                        prompt.append(
                                        "      \"text\": \"The <strong>8</strong> ____ of electric vehicles has grown significantly.\",\n");
                        prompt.append("      \"options\": [\n");
                        prompt.append("        {\"letter\": \"A\", \"text\": \"popularity\"},\n");
                        prompt.append("        {\"letter\": \"B\", \"text\": \"cost\"},\n");
                        prompt.append("        {\"letter\": \"C\", \"text\": \"efficiency\"},\n");
                        prompt.append("        {\"letter\": \"D\", \"text\": \"range\"},\n");
                        prompt.append("        {\"letter\": \"E\", \"text\": \"reliability\"}\n");
                        prompt.append("      ]\n");
                        prompt.append("    },\n");
                        prompt.append("    \"correct_answer\": [\"A\"],\n");
                        prompt.append("    \"word_limit\": null\n");
                        prompt.append("  },\n");
                        prompt.append("  {\n");
                        prompt.append("    \"question_number\": 9,\n");
                        prompt.append("    \"question_type\": \"SUMMARY_COMPLETION_OPTIONS\",\n");
                        prompt.append("    \"question_content\": {\n");
                        prompt.append(
                                        "      \"text\": \"However, their <strong>9</strong> ____ remains a concern for many buyers.\",\n");
                        prompt.append("      \"options\": [\n");
                        prompt.append("        {\"letter\": \"A\", \"text\": \"popularity\"},\n");
                        prompt.append("        {\"letter\": \"B\", \"text\": \"cost\"},\n");
                        prompt.append("        {\"letter\": \"C\", \"text\": \"efficiency\"},\n");
                        prompt.append("        {\"letter\": \"D\", \"text\": \"range\"},\n");
                        prompt.append("        {\"letter\": \"E\", \"text\": \"reliability\"}\n");
                        prompt.append("      ]\n");
                        prompt.append("    },\n");
                        prompt.append("    \"correct_answer\": [\"D\"],\n");
                        prompt.append("    \"word_limit\": null\n");
                        prompt.append("  }\n");
                        prompt.append("]\n");
                        prompt.append("```\n");
                        prompt.append("⚠️ The options arrays above are IDENTICAL - this is REQUIRED. Validation will FAIL if options differ between questions.\n");

                        int totalQuestions = request.getTotalQuestions() != null ? request.getTotalQuestions() : 13;
                        int groupSize = Math.max(2, totalQuestions / requestedTypes.size());
                        for (int i = 0; i < requestedTypes.size(); i++) {
                                String type = requestedTypes.get(i);
                                prompt.append(String.format("   - **Group %d**: %s (%d-%d questions)\n", i + 1, type,
                                                groupSize,
                                                groupSize + 1));
                        }
                } else {
                        // Default Strict Structure based on Part Number
                        prompt.append("### Question Structure (STRICT GROUPING)\n");
                        prompt.append("You must generate exactly 3 groups of questions:\n\n");

                        if (partNumber == 1) {
                                prompt.append("1. **Questions 1-5/6**: SUMMARY_COMPLETION or TABLE_COMPLETION (5-7 questions)\n");
                                prompt.append("   - Summary or table format with word limits.\n\n");
                                prompt.append("2. **Questions 7-13/14**: TRUE_FALSE_NOT_GIVEN (7-8 questions)\n");
                                prompt.append("   - Statements aligned to passage order.\n");
                        } else if (partNumber == 2) {
                                // Part 2 starts at Question 14 - Based on Cambridge IELTS 17 Test 1 Reading
                                prompt.append("⚠️ **CRITICAL: Part 2 question numbering starts at 14, NOT 1!**\n\n");
                                prompt.append("**Cambridge-Aligned Structure** (based on real Cambridge IELTS 17):\n\n");
                                prompt.append("1. **Questions 14-17**: MATCHING_INFORMATION (4 questions)\n");
                                prompt.append("   - 'Which paragraph contains the following information?'\n");
                                prompt.append("   - Each question is a statement to match to a paragraph\n");
                                prompt.append("   - `options` array: Simple paragraph letters `[\"A\",\"B\",\"C\",\"D\",\"E\",\"F\",\"G\"]`\n");
                                prompt.append("   - `question_content.text`: The statement (use `text` NOT `statement`)\n");
                                prompt.append("   - `correct_answer`: Single-element array `[\"A\"]`, `[\"G\"]` etc.\n\n");
                                prompt.append("2. **Questions 18-22**: SUMMARY_COMPLETION (5 questions)\n");
                                prompt.append("   - Inline text format with blanks\n");
                                prompt.append("   - Format: `<strong>18</strong> ____` in each question's text\n");
                                prompt.append("   - Include `word_limit`: \"ONE WORD ONLY\"\n\n");
                                prompt.append("3. **Questions 23-26**: MULTIPLE_CHOICE_MULTIPLE_ANSWERS (4 questions)\n");
                                prompt.append("   - 'Choose TWO letters A-E'\n");
                                prompt.append("   - Create PAIRED questions: Q23 & Q24 identical, Q25 & Q26 identical\n");
                                prompt.append("   - Both questions in pair have SAME `text`, `options`, and `correct_answer`\n");
                                prompt.append("   - `correct_answer` array: EXACTLY 2 items `[\"C\",\"D\"]`\n");
                                prompt.append("   - `options` array: 5 choices `[\"A. option\", \"B. option\", ...]`\n\n");
                                prompt.append("**Total: 13 questions (Q14-Q26)**\n\n");
                                prompt.append("🚨 **MANDATORY MATCHING_INFORMATION FORMAT** (Q14-17):\n");
                                prompt.append("```json\n");
                                prompt.append("{\n");
                                prompt.append("  \"question_number\": 14,\n");
                                prompt.append("  \"question_type\": \"MATCHING_INFORMATION\",\n");
                                prompt.append("  \"question_content\": {\n");
                                prompt.append("    \"text\": \"a mention of negative attitudes towards the project\",\n");
                                prompt.append("    \"options\": [\"A\",\"B\",\"C\",\"D\",\"E\",\"F\",\"G\"]\n");
                                prompt.append("  },\n");
                                prompt.append("  \"correct_answer\": [\"A\"],\n");
                                prompt.append("  \"word_limit\": null,\n");
                                prompt.append("  \"explanation\": \"...\"\n");
                                prompt.append("}\n");
                                prompt.append("```\n");
                                prompt.append("⚠️ WITHOUT `options` array, validation WILL FAIL!\n");
                        } else {
                                // Part 3 starts at Question 27 - Based on Cambridge IELTS 17 Test 1 Reading
                                prompt.append("⚠️ **CRITICAL: Part 3 question numbering starts at 27, NOT 1!**\n\n");

                                prompt.append("**Cambridge-Aligned Structure** (based on real Cambridge IELTS 17):\n\n");
                                prompt.append("1. **Questions 27-33**: SUMMARY_COMPLETION_OPTIONS (7 questions)\n");
                                prompt.append("   - Summary with blanks, select from word box\n");
                                prompt.append("   - `options` array: Objects with `{\"letter\": \"A\", \"text\": \"word or phrase\"}`\n");
                                prompt.append("   - Provide 10 options (A-J) for 7 questions\n");
                                prompt.append("   - 🚨 **CRITICAL: ALL 7 questions MUST have the EXACT SAME options array!**\n");
                                prompt.append("   - **Copy-paste the IDENTICAL options array to each question.**\n");
                                prompt.append("   - Format: `<strong>27</strong> ____` in each question's text\n");
                                prompt.append("   - `correct_answer`: Single letter `[\"H\"]`\n\n");
                                prompt.append("2. **Questions 34-36**: YES_NO_NOT_GIVEN (3 questions)\n");
                                prompt.append("   - Do statements agree with writer's claims/views?\n");
                                prompt.append("   - `question_content.text`: The statement\n");
                                prompt.append("   - `correct_answer`: `[\"YES\"]`, `[\"NO\"]`, or `[\"NOT GIVEN\"]`\n\n");
                                prompt.append("3. **Questions 37-40**: MULTIPLE_CHOICE (4 questions)\n");
                                prompt.append("   - Standard A/B/C/D format\n");
                                prompt.append("   - `options` array: 4 choices `[\"A. option\", \"B. option\", \"C. option\", \"D. option\"]`\n");
                                prompt.append("   - `correct_answer`: Single letter `[\"C\"]`\n\n");
                                prompt.append("**Total: 14 questions (Q27-Q40)**\n");
                        }

                        prompt.append("\n#### Question Format Details for MATCHING Types (CRITICAL):\n");
                        prompt.append("- **MATCHING_INFORMATION**: Match statements to paragraphs\n");
                        prompt.append("  ```json\n");
                        prompt.append("  { \"question_number\": 14, \"question_type\": \"MATCHING_INFORMATION\",\n");
                        prompt.append("    \"question_content\": { \"text\": \"A description of...\", \"options\": [\"A\",\"B\",\"C\",\"D\",\"E\",\"F\"] },\n");
                        prompt.append("    \"correct_answer\": [\"C\"], \"explanation\": \"...\" }\n");
                        prompt.append("  ```\n\n");

                        prompt.append("- **MATCHING_HEADINGS**: Match paragraphs to headings\n");
                        prompt.append("  ```json\n");
                        prompt.append("  { \"question_number\": 27, \"question_type\": \"MATCHING_HEADINGS\",\n");
                        prompt.append("    \"question_content\": { \"text\": \"Paragraph A\", \"options\": [\"i. Origins\", \"ii. Health\", \"iii. Trade\"] },\n");
                        prompt.append("    \"correct_answer\": [\"ii\"], \"explanation\": \"...\" }\n");
                        prompt.append("  ```\n\n");

                        prompt.append("- **MULTIPLE_CHOICE_MULTIPLE_ANSWERS**: Choose TWO correct answers\n");
                        prompt.append("  ```json\n");
                        prompt.append("  { \"question_number\": 23, \"question_type\": \"MULTIPLE_CHOICE_MULTIPLE_ANSWERS\",\n");
                        prompt.append("    \"question_content\": { \"text\": \"Which TWO of the following...\", \"options\": [\"A. opt1\", \"B. opt2\", \"C. opt3\", \"D. opt4\", \"E. opt5\"] },\n");
                        prompt.append("    \"correct_answer\": [\"A\", \"D\"], \"explanation\": \"...\" }\n");
                        prompt.append("  ```\n\n");

                        prompt.append("#### Standard Question Formats:\n");
                        prompt.append(
                                        "- TRUE_FALSE_NOT_GIVEN / YES_NO_NOT_GIVEN: use `question_content.statement` and correct_answer (e.g., \"TRUE\", \"NOT GIVEN\").\n");
                        prompt.append("- MATCHING_HEADINGS: `question_content.text` like \"Paragraph A\"; shared options list.\n");
                        prompt.append(
                                        "- Completion types: MUST use format `<strong>{number}</strong> ____` (bold number followed by 4 underscores) and include word_limit.\n\n");

                        // Add inline strategy for completion types (same as explicit mode)
                        prompt.append(
                                        "#### INLINE STRATEGY for SUMMARY_COMPLETION, FILL_IN_BLANK, TABLE_COMPLETION (CRITICAL):\n");
                        prompt.append(
                                        "- **Each question object MUST contain the specific sentence or text fragment containing its blank.**\n");
                        prompt.append("- **Do NOT leave the 'text' field empty for any question.**\n");
                        prompt.append("- **BLANK FORMAT**: Use `<strong>{number}</strong> ____` (bold number followed by 4 underscores).\n");
                        prompt.append("- Add `word_limit`: Use `ONE WORD ONLY`, `NO MORE THAN TWO WORDS`, or `ONE WORD AND/OR A NUMBER`.\n");
                        prompt.append("- **Format Example** (Part 2 style, starting at Q24):\n");
                        prompt.append("```json\n");
                        prompt.append("[\n");
                        prompt.append("  {\n");
                        prompt.append("    \"question_number\": 24,\n");
                        prompt.append(
                                        "    \"question_content\": { \"text\": \"The <strong>24</strong> ____ of London increased rapidly...\" },\n");
                        prompt.append("    \"correct_answer\": [\"population\"],\n");
                        prompt.append("    \"word_limit\": \"ONE WORD ONLY\"\n");
                        prompt.append("  }\n");
                        prompt.append("]\n");
                        prompt.append("```\n");
                        prompt.append(
                                        "- **Failure to include text in each question will cause the test to render broken/blank questions.**\n\n");

                        prompt.append("#### 🚨 ANSWER WORD LIMIT ENFORCEMENT (CRITICAL - VALIDATION WILL FAIL) 🚨\n");
                        prompt.append("The `correct_answer` MUST respect the `word_limit` constraint. This is NON-NEGOTIABLE.\n\n");
                        prompt.append("| word_limit | Max Words | ✅ Valid Examples | ❌ Invalid Examples |\n");
                        prompt.append("|------------|-----------|-------------------|---------------------|\n");
                        prompt.append("| ONE WORD ONLY | 1 | \"population\", \"technology\" | \"electric motor\", \"grid system\" |\n");
                        prompt.append("| NO MORE THAN TWO WORDS | 1-2 | \"solar panels\", \"efficiency\" | \"renewable energy sources\" |\n");
                        prompt.append("| NO MORE THAN THREE WORDS | 1-3 | \"carbon dioxide emissions\" | \"the main power source\" |\n\n");
                        prompt.append("⚠️ BEFORE FINALIZING EACH ANSWER, COUNT THE WORDS:\n");
                        prompt.append("- If word_limit is ONE WORD ONLY and answer has a space → WRONG, pick a single word\n");
                        prompt.append("- \"Vehicle-to-Grid technology\" = 2 words (hyphenated counts as 1) but still WRONG for ONE WORD ONLY\n");
                        prompt.append("- \"electric motor\" = 2 words → WRONG for ONE WORD ONLY, pick just \"motor\"\n");
                        prompt.append("- Answers violating word limits will be REJECTED by validation.\n\n");
                }
                prompt.append("IMPORTANT: Do NOT mix question types. Finish one group before starting the next.\n");
                prompt.append("\n");

                // Answer and explanation requirements
                prompt.append("### Answer & Explanation Requirements\n");
                prompt.append("⚠️ CRITICAL: Each question MUST have its OWN `correct_answer` array with EXACTLY ONE answer.\n");
                prompt.append("- Question 1's correct_answer = [\"answer1\"] (just this question's answer)\n");
                prompt.append("- Question 2's correct_answer = [\"answer2\"] (just this question's answer)\n");
                prompt.append("- DO NOT put all answers in one array and duplicate across questions!\n");
                prompt.append("- ⚠️ NEVER leave `correct_answer` empty or null - this causes validation failures!\n");
                prompt.append("- MCMA exception: correct_answer has exactly 2 items `[\"A\",\"D\"]`\n");
                prompt.append("- Each question MUST have its own INDIVIDUAL explanation in ")
                                .append(request.getExplanationLanguage() == GenerationRequestDTO.ExplanationLanguage.VI
                                                ? "Vietnamese"
                                                : "English")
                                .append("\n");
                prompt.append("- Explanations must reference specific paragraph letters.\n\n");

                // Final reminder
                prompt.append("### ⚠️ FINAL REMINDER - COMMON VALIDATION FAILURES ⚠️\n");
                prompt.append("Before responding, verify:\n");
                prompt.append("1. **Question numbering**: Part 1 starts at 1, Part 2 starts at 14, Part 3 starts at 27\n");
                prompt.append("2. **OPTIONS LOCATION**: ALL options arrays go INSIDE `question_content`, NOT at question top level!\n");
                prompt.append("   - Correct: `\"question_content\": { \"text\": \"...\", \"options\": [...] }`\n");
                prompt.append("   - Wrong: `\"question_content\": {...}, \"options\": [...]` (top level)\n");
                prompt.append("3. **MULTIPLE_CHOICE_MULTIPLE_ANSWERS**: correct_answer must have EXACTLY 2 items\n");
                prompt.append("4. **SUMMARY_COMPLETION_OPTIONS**: options must be [{\"letter\":\"A\",\"text\":\"word\"},...] format\n");
                prompt.append("5. **correct_answer field**: NEVER empty or null - every question needs an answer!\n");
                prompt.append("6. **Word count**: Follow part-specific requirements for Part ").append(partNumber)
                                .append("\n");
                prompt.append("7. **Completion types**: EVERY question has its own text with blank.\n");
                prompt.append("8. **JSON validity**: All required fields present, proper escaping.\n\n");

                prompt.append("### Mini Example Output (MATCHING_INFORMATION Style)\n");
                prompt.append("{\"section\":{\"passage_text\":\"<strong>A.</strong> ...\",\"word_count\":780},");
                prompt.append("\"questions\":[{\"question_number\":14,\"question_type\":\"MATCHING_INFORMATION\",");
                prompt.append("\"question_content\":{\"text\":\"A reference to...\",\"options\":[\"A\",\"B\",\"C\",\"D\",\"E\",\"F\"]},");
                prompt.append("\"correct_answer\":[\"C\"],\"word_limit\":null,\"explanation\":\"...\"}]}\n\n");

                if (request.getCustomInstructions() != null && !request.getCustomInstructions().isBlank()) {
                        prompt.append("### Custom Instructions (Highest Priority)\n");
                        prompt.append(request.getCustomInstructions()).append("\n\n");
                }

                return prompt.toString();
        }

        /**
         * Build system prompt for Reading generation.
         */
        public String buildReadingSystemPrompt() {
                StringBuilder system = new StringBuilder();

                system.append("You are an expert IELTS Academic Reading test creator with 15+ years of experience.\n\n");

                system.append("## Your Role\n");
                system.append("Create authentic IELTS Reading passages and questions that:\n");
                system.append("- Match the difficulty level of real Cambridge IELTS tests\n");
                system.append("- Follow official IELTS question type formats exactly\n");
                system.append("- Prioritize provided facts; if none, use realistic academic details\n");
                system.append("- Enforce completion word limits (no 3+ word answers unless allowed)\n\n");

                system.append("## CRITICAL: Word Count Requirements\n");
                system.append("- Follow the part-specific word count range provided in the user prompt.\n");
                system.append("- If provided facts are limited, expand with realistic academic knowledge.\n");
                system.append("- Stay within the range; target the upper end if possible.\n\n");

                system.append("## Critical Formatting Rules\n");
                system.append(
                                "1. FACTUAL ACCURACY: Prioritize provided facts. If gaps exist, use accurate general knowledge.\n");
                system.append(
                                "2. PARAGRAPH LABELS: Use <strong>A.</strong>, <strong>B.</strong>, etc. at the start of paragraphs.\n");
                system.append("3. PARAGRAPH SPACING: Separate each paragraph with a blank line (double newline).\n");
                system.append("4. COMPLETION LIMITS: Use word_limit and keep answers within it.\n");
                system.append("5. JSON FORMAT: Return valid JSON matching the schema exactly.\n");
                system.append("6. LANGUAGE: The passage and ALL question content must be in ENGLISH.\n");
                system.append("   The 'explanation' field MUST be in VIETNAMESE (Tiếng Việt). Use HTML formatting.\n\n");

                system.append("## CRITICAL: Answer Validation Rules\n");
                system.append("For FILL_IN_BLANK, SUMMARY_COMPLETION, TABLE_COMPLETION, FLOW_CHART_COMPLETION:\n");
                system.append("- The correct_answer MUST appear EXACTLY (verbatim) in the passage text.\n");
                system.append("- Use the EXACT wording, spelling, and casing from the passage.\n");
                system.append("- Before finalizing, verify each answer exists word-for-word in the passage.\n");
                system.append(
                                "- Example: If passage says 'battery technology', answer must be 'battery technology' (not 'Battery Technology').\n\n");

                system.append("## 🚨 CRITICAL: Answer Word Limit Compliance 🚨\n");
                system.append("Answers MUST NOT exceed the word_limit. COUNT THE WORDS before finalizing:\n");
                system.append("- ONE WORD ONLY = exactly 1 word, NO SPACES (e.g., \"motor\" not \"electric motor\")\n");
                system.append("- NO MORE THAN TWO WORDS = 1-2 words maximum\n");
                system.append("- NO MORE THAN THREE WORDS = 1-3 words maximum\n");
                system.append("If your answer exceeds the limit, PICK A SHORTER VERSION that still appears in the passage.\n\n");

                system.append("For MATCHING and SELECTION types (TFNG, MCQ, Matching, etc.):\n");
                system.append("- Answers are letter choices from the options, not text from passage.\n");
                system.append("- Ensure the correct letter corresponds to the right option.\n\n");

                // CRITICAL: Options Array Requirement
                system.append("## 🚨 MANDATORY: Options Array for MATCHING Types 🚨\n");
                system.append("The following question types MUST ALWAYS include an 'options' array:\n");
                system.append("- MATCHING_INFORMATION: options = [\"A\",\"B\",\"C\",\"D\",\"E\",\"F\",...] (paragraph letters)\n");
                system.append("- MATCHING_HEADINGS: options = [\"i. heading\", \"ii. heading\", ...] (roman numerals)\n");
                system.append("- MATCHING_FEATURES: options = [\"A. Person/Category\", \"B. Person/Category\", ...]\n");
                system.append("- MULTIPLE_CHOICE: options = [\"A. choice\", \"B. choice\", \"C. choice\", \"D. choice\"]\n");
                system.append("⚠️ WITHOUT the 'options' array, validation WILL FAIL. This is non-negotiable.\n\n");
                system.append("### SUMMARY_COMPLETION_OPTIONS Example (Part 3 Style):\n");
                system.append("```json\n");
                system.append("{ \"question_number\": 27, \"question_type\": \"SUMMARY_COMPLETION_OPTIONS\",\n");
                system.append("  \"question_content\": { \"text\": \"Charles II formed a <strong>27</strong> ____ with Scots\",\n");
                system.append("    \"options\": [{\"letter\":\"A\",\"text\":\"military innovation\"},{\"letter\":\"H\",\"text\":\"strategic alliance\"}]},\n");
                system.append("  \"correct_answer\": [\"H\"], \"word_limit\": null, \"explanation\": \"...\" }\n");
                system.append("```\n\n");

                system.append("## CRITICAL: Question Count Accuracy\n");
                system.append("- Generate EXACTLY the number of questions specified in the request.\n");
                system.append("- Question numbers must be sequential with no gaps or duplicates.\n");
                system.append("- If questionTypeCounts is provided, the sum must equal the total.\n\n");

                system.append("## Output Format\n");
                system.append("You MUST respond with valid JSON only. No markdown, no explanations outside JSON.\n");

                return system.toString();
        }

        /**
         * PASS 1: Build prompt for Reading Passage Generation ONLY.
         * strictly prioritizes user facts.
         */
        public String buildReadingPassagePrompt(GenerationRequestDTO request) {
                StringBuilder prompt = new StringBuilder();

                Integer partNumber = request.getPartNumber() != null ? request.getPartNumber() : 1;

                prompt.append("## TASK: Generate IELTS Academic Reading Passage (Phase 1/2)\n\n");
                prompt.append("Your goal is to write a high-quality, academic passage suitable for IELTS Reading Part ")
                                .append(partNumber).append(".\n");
                prompt.append("Do NOT generate questions yet. Focus ONLY on the text.\n\n");

                // Part-specific specs
                prompt.append("### Part ").append(partNumber).append(" Specifications\n");
                switch (partNumber) {
                        case 1:
                                prompt.append("- **Context**: General interest, social, or factual topic.\n");
                                prompt.append("- **Style**: Descriptive and factual.\n");
                                prompt.append("- **Word Count**: 900-1000 words.\n");
                                break;
                        case 2:
                                prompt.append("- **Context**: Workplace, training, or general interest topic.\n");
                                prompt.append("- **Style**: Discursive, logical argument or detailed description.\n");
                                prompt.append("- **Word Count**: 1000-1100 words.\n");
                                break;
                        case 3:
                                prompt.append("- **Context**: Complex academic topic.\n");
                                prompt.append("- **Style**: Argumentative, abstract, complex sentence structures.\n");
                                prompt.append("- **Word Count**: 1100-1200 words.\n");
                                break;
                }

                // Topic & Facts
                prompt.append("\n### Topic: ").append(request.getTopic()).append("\n");
                List<String> facts = request.getFacts();
                if (facts != null && !facts.isEmpty()) {
                        prompt.append("### Content Source (Strict Mode)\n");
                        prompt.append("You MUST base the passage content primarily on the following verified facts.\n");
                        prompt.append("You may connect them with logical transitions, but do not contradict them.\n");
                        for (int i = 0; i < facts.size(); i++) {
                                prompt.append(String.format("- %s\n", facts.get(i)));
                        }
                } else {
                        prompt.append("### Content Generation\n");
                        prompt.append("No specific facts provided. Research the topic and use realistic, verifiable academic details.\n");
                }

                // PARAGRAPH FORMATTING - CRITICAL
                prompt.append("\n### Paragraph Formatting (CRITICAL)\n");
                prompt.append("Each paragraph MUST follow this EXACT format:\n\n");
                prompt.append("```\n");
                prompt.append("<strong>A.</strong> First paragraph text here. This should be a complete paragraph with multiple sentences developing one main idea.\n");
                prompt.append("\n");
                prompt.append("<strong>B.</strong> Second paragraph text here. Continue developing the topic with a new aspect or argument.\n");
                prompt.append("```\n\n");
                prompt.append("**CRITICAL RULES:**\n");
                prompt.append("1. Start each paragraph with `<strong>X.</strong>` where X is A, B, C, D, etc.\n");
                prompt.append("2. **BLANK LINE between paragraphs** - Use `\\n\\n` (double newline) to separate paragraphs\n");
                prompt.append("3. Each paragraph should be 80-150 words of continuous prose\n");
                prompt.append("4. Do NOT use bullet points or numbered lists within paragraphs\n\n");

                prompt.append("\n### Output Requirements\n");
                prompt.append("Return valid JSON with exactly:\n");
                prompt.append("1. `passage_text`: The full academic text with paragraph labels. Use `\\n\\n` between paragraphs.\n");
                prompt.append("2. `word_count`: Integer count of words.\n");
                prompt.append("Do NOT include `questions`.\n");

                return prompt.toString();
        }

        /**
         * PASS 2: Build prompt for Reading Questions Generation ONLY.
         * ENHANCED: Includes ALL critical formatting rules from buildReadingPrompt.
         */
        public String buildReadingQuestionsPrompt(GenerationRequestDTO request, String passageText) {
                StringBuilder prompt = new StringBuilder();

                Integer partNumber = request.getPartNumber() != null ? request.getPartNumber() : 1;
                int startNumber = (partNumber == 1) ? 1 : (partNumber == 2 ? 14 : 27);
                int totalQuestions = (partNumber == 3) ? 14 : 13;

                prompt.append("## TASK: Generate IELTS Reading Questions (Phase 2/2)\n\n");
                prompt.append("Based on the provided passage, generate the exam questions.\n\n");

                // Provide the passage
                prompt.append("### Source Passage\n");
                prompt.append("```html\n").append(passageText).append("\n```\n\n");

                // Question Numbering
                prompt.append("### Question Numbering (STRICT)\n");
                prompt.append("- **Start at**: Q").append(startNumber).append("\n");
                prompt.append("- **End at**: Q").append(startNumber + totalQuestions - 1).append("\n");
                prompt.append("- **Total**: ").append(totalQuestions).append(" questions\n\n");

                // Requested Types
                List<String> requestedTypes = request.getQuestionTypes();
                if ((requestedTypes == null || requestedTypes.isEmpty())
                                && request.getQuestionTypeCounts() != null
                                && !request.getQuestionTypeCounts().isEmpty()) {
                        requestedTypes = new ArrayList<>(request.getQuestionTypeCounts().keySet());
                }

                if (requestedTypes != null && !requestedTypes.isEmpty()) {
                        prompt.append("### Requested Question Types\n");
                        if (request.getQuestionTypeCounts() != null && !request.getQuestionTypeCounts().isEmpty()) {
                                request.getQuestionTypeCounts().forEach((type, count) -> {
                                        prompt.append("- ").append(type).append(": ").append(count)
                                                        .append(" questions\n");
                                });
                        } else {
                                for (String type : requestedTypes) {
                                        prompt.append("- ").append(type).append("\n");
                                }
                        }
                        prompt.append("\n");
                }

                // ==========================================
                // QUESTION TYPE RULES (Critical Section)
                // ==========================================
                prompt.append("### Question Type Rules (CRITICAL - MUST FOLLOW EXACTLY)\n\n");

                // FILL_IN_BLANK / SUMMARY_COMPLETION
                prompt.append("#### FILL_IN_BLANK / SUMMARY_COMPLETION / NOTE_COMPLETION\n");
                prompt.append("- **INLINE STRATEGY**: Each question MUST have its own text with ONE blank.\n");
                prompt.append("- **BLANK FORMAT**: Use `<strong>N</strong> ____` (bold number + 4 underscores)\n");
                prompt.append("- **word_limit**: REQUIRED. Use EXACT strings:\n");
                prompt.append("  - `\"ONE WORD ONLY\"`\n");
                prompt.append("  - `\"NO MORE THAN TWO WORDS\"`\n");
                prompt.append("  - `\"NO MORE THAN THREE WORDS\"`\n");
                prompt.append("  - `\"ONE WORD AND/OR A NUMBER\"`\n");
                prompt.append("- Example:\n");
                prompt.append("```json\n");
                prompt.append("{\"question_number\": ").append(startNumber)
                                .append(", \"question_type\": \"FILL_IN_BLANK\",\n");
                prompt.append(" \"question_content\": {\"text\": \"The <strong>").append(startNumber)
                                .append("</strong> ____ of the city grew rapidly.\"},\n");
                prompt.append(" \"correct_answer\": [\"population\"], \"word_limit\": \"ONE WORD ONLY\"}\n");
                prompt.append("```\n\n");

                // TRUE_FALSE_NOT_GIVEN / YES_NO_NOT_GIVEN
                prompt.append("#### TRUE_FALSE_NOT_GIVEN / YES_NO_NOT_GIVEN\n");
                prompt.append("- `question_content.text`: The statement to evaluate\n");
                prompt.append("- `correct_answer`: EXACTLY `[\"TRUE\"]`, `[\"FALSE\"]`, or `[\"NOT GIVEN\"]`\n");
                prompt.append("  (or `[\"YES\"]`, `[\"NO\"]`, `[\"NOT GIVEN\"]` for YES_NO type)\n");
                prompt.append("- `word_limit`: null\n\n");

                // MATCHING_INFORMATION
                prompt.append("#### MATCHING_INFORMATION\n");
                prompt.append("- Match statements to paragraphs (A, B, C...)\n");
                prompt.append("- `question_content.text`: The statement\n");
                prompt.append("- `question_content.options`: Array of paragraph letters `[\"A\",\"B\",\"C\",\"D\",\"E\",\"F\"]`\n");
                prompt.append("- `correct_answer`: Single letter `[\"C\"]`\n\n");

                // MATCHING_HEADINGS
                prompt.append("#### MATCHING_HEADINGS\n");
                prompt.append("- Match paragraphs to headings\n");
                prompt.append("- `question_content.text`: Paragraph reference (e.g., \"Paragraph A\")\n");
                prompt.append("- `question_content.options`: Array of heading objects with roman numerals\n");
                prompt.append("  `[{\"letter\":\"i\",\"text\":\"Origins of...\"},{\"letter\":\"ii\",\"text\":\"Health benefits\"}...]`\n");
                prompt.append("- Options must be IDENTICAL for all questions in the group\n");
                prompt.append("- `correct_answer`: `[\"ii\"]`\n\n");

                // MULTIPLE_CHOICE
                prompt.append("#### MULTIPLE_CHOICE\n");
                prompt.append("- `question_content.text`: The question\n");
                prompt.append("- `question_content.options`: `[\"A. First option\", \"B. Second option\", \"C. Third option\", \"D. Fourth option\"]`\n");
                prompt.append("- `correct_answer`: `[\"B\"]`\n\n");

                // MULTIPLE_CHOICE_MULTIPLE_ANSWERS
                prompt.append("#### MULTIPLE_CHOICE_MULTIPLE_ANSWERS (Choose TWO)\n");
                prompt.append("- Create TWO consecutive questions with IDENTICAL text/options\n");
                prompt.append("- BOTH questions have the SAME `correct_answer` array with TWO letters\n");
                prompt.append("- Example (Q").append(startNumber + 5).append(" and Q").append(startNumber + 6)
                                .append(" are a pair):\n");
                prompt.append("```json\n");
                prompt.append("[{\"question_number\": ").append(startNumber + 5)
                                .append(", \"question_type\": \"MULTIPLE_CHOICE_MULTIPLE_ANSWERS\",\n");
                prompt.append("  \"question_content\": {\"text\": \"Which TWO advantages are mentioned?\",\n");
                prompt.append("   \"options\": [\"A Lower costs\",\"B Quieter\",\"C Faster\",\"D Cleaner\",\"E Safer\"]},\n");
                prompt.append("  \"correct_answer\": [\"B\",\"D\"]},\n");
                prompt.append(" {\"question_number\": ").append(startNumber + 6)
                                .append(", \"question_type\": \"MULTIPLE_CHOICE_MULTIPLE_ANSWERS\",\n");
                prompt.append("  \"question_content\": {\"text\": \"Which TWO advantages are mentioned?\",\n");
                prompt.append("   \"options\": [\"A Lower costs\",\"B Quieter\",\"C Faster\",\"D Cleaner\",\"E Safer\"]},\n");
                prompt.append("  \"correct_answer\": [\"B\",\"D\"]}]\n");
                prompt.append("```\n\n");

                // SUMMARY_COMPLETION_OPTIONS
                prompt.append("#### SUMMARY_COMPLETION_OPTIONS (Word Box)\n");
                prompt.append("- Each question has inline blank + selects from word box\n");
                prompt.append("- `question_content.options`: Array of `{letter, text}` objects\n");
                prompt.append("- **CRITICAL**: ALL questions in group MUST have IDENTICAL options array\n");
                prompt.append("- Provide 10 options (A-J) for 5-7 questions\n\n");

                // Explanation Format
                prompt.append("### Explanation Format (CRITICAL)\n");
                prompt.append("All explanations must be JSON objects in Vietnamese:\n");
                prompt.append("```json\n");
                prompt.append("{\n");
                prompt.append("  \"detail\": \"<Vietnamese explanation: why is this correct?>\",\n");
                prompt.append("  \"quote\": \"<EXACT English quote from passage that proves the answer>\",\n");
                prompt.append("  \"strategy\": \"<Vietnamese strategy tip for this question type>\"\n");
                prompt.append("}\n");
                prompt.append("```\n\n");

                // Answer Validation
                prompt.append("### Answer Validation Rules\n");
                prompt.append("- Each question MUST have its OWN `correct_answer` array\n");
                prompt.append("- For completion types: answer MUST appear EXACTLY in the passage\n");
                prompt.append("- Answer word count MUST respect `word_limit`:\n");
                prompt.append("  - ONE WORD ONLY = 1 word (no spaces)\n");
                prompt.append("  - NO MORE THAN TWO WORDS = 1-2 words max\n");
                prompt.append("  - NO MORE THAN THREE WORDS = 1-3 words max\n\n");

                // Validation Checklist
                prompt.append("### MANDATORY VALIDATION CHECKLIST\n");
                prompt.append("Before outputting, verify:\n");
                prompt.append("- [ ] Question numbers start at ").append(startNumber).append(" and are sequential\n");
                prompt.append("- [ ] OPTIONS array is INSIDE `question_content` for MATCHING/MCQ types\n");
                prompt.append("- [ ] Every FILL_IN_BLANK has `<strong>N</strong> ____` in text\n");
                prompt.append("- [ ] Every question has individual `correct_answer` array\n");
                prompt.append("- [ ] Every question has individual `explanation` object\n");
                prompt.append("- [ ] Word limits use EXACT format strings\n");
                prompt.append("- [ ] MCMA pairs have IDENTICAL text/options and correct_answer\n\n");

                // Output
                prompt.append("### Output Requirements\n");
                prompt.append("Return valid JSON with:\n");
                prompt.append("1. `questions`: Array of ").append(totalQuestions).append(" question objects.\n");
                prompt.append("Do NOT re-output the passage.\n");

                return prompt.toString();
        }
}