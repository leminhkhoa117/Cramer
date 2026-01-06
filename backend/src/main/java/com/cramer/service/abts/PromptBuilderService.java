package com.cramer.service.abts;

import com.cramer.dto.abts.GenerationRequestDTO;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for building AI prompts for IELTS content generation.
 * 
 * Constructs system prompts with:
 * - Role & Context
 * - Task Specification
 * - Quality Requirements
 * - JSON Schema
 * - Examples
 * 
 * @since 2025-12-20 - ABTS v2.0
 * @updated Phase 3 - Enhanced Listening prompts
 */
@Service
public class PromptBuilderService {

        // ==================== READING PROMPTS ====================

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
                prompt.append("### ⚠️ EXPLANATION FORMAT (CRITICAL - MUST FOLLOW EXACTLY)\n");
                prompt.append("Each question's `explanation` field must be a JSON object with this EXACT structure:\n");
                prompt.append("```json\n");
                prompt.append("{\n");
                prompt.append("  \"detail\": \"<detailed explanation in Vietnamese why this is correct>\",\n");
                prompt.append("  \"quote\": \"<EXACT quote from the passage in English that proves the answer>\",\n");
                prompt.append("  \"strategy\": \"<strategy tip in Vietnamese for this question type>\"\n");
                prompt.append("}\n");
                prompt.append("```\n\n");
                prompt.append("**Field Requirements:**\n");
                prompt.append("- `detail`: 2-4 sentences explaining the reasoning (in Vietnamese).\n");
                prompt.append("- `quote`: Direct quote from passage with quotation marks. Keep in English.\n");
                prompt.append("- `strategy`: Brief strategy tip for similar questions (in Vietnamese).\n\n");

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
                prompt.append("### ⚠️ WORD LIMIT FORMAT (CRITICAL - MUST USE EXACT VALUES)\n");
                prompt.append("For completion-type questions, `word_limit` MUST be one of these EXACT strings:\n");
                prompt.append("✅ VALID:\n");
                prompt.append("- `\"ONE WORD ONLY\"`\n");
                prompt.append("- `\"NO MORE THAN TWO WORDS\"`\n");
                prompt.append("- `\"NO MORE THAN THREE WORDS\"`\n");
                prompt.append("- `\"ONE WORD AND/OR A NUMBER\"`\n");
                prompt.append("- `\"NO MORE THAN TWO WORDS AND/OR A NUMBER\"`\n");
                prompt.append("- `\"NO MORE THAN THREE WORDS AND/OR A NUMBER\"`\n\n");
                prompt.append("❌ INVALID (do NOT use these):\n");
                prompt.append("- `\"ONE WORD\"` ← WRONG, use `\"ONE WORD ONLY\"`\n");
                prompt.append("- `\"TWO WORDS\"` ← WRONG, use `\"NO MORE THAN TWO WORDS\"`\n");
                prompt.append("- `\"THREE WORDS\"` ← WRONG, use `\"NO MORE THAN THREE WORDS\"`\n");
                prompt.append("- `\"FOUR WORDS\"` or `\"NO MORE THAN FOUR WORDS\"` ← WRONG, maximum is THREE WORDS\n\n");

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

        // ==================== LISTENING PROMPTS (Phase 3 Enhanced)
        // ====================

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

                // Figure descriptions if needed (Part 2)
                if (partNumber == 2) {
                        prompt.append("### Figure Description (for Map/Plan)\n");
                        prompt.append("If including map labelling, provide `figure_description` with:\n");
                        prompt.append("- title: Description of the map/plan\n");
                        prompt.append("- elements: Array of labeled locations\n");
                        prompt.append("- answer_locations: Which letters correspond to which blanks\n\n");
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
                prompt.append("**Transcript Length**: 900-1000 words (approximately 6-7 minutes of natural dialogue)\n");
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
                prompt.append("**Transcript Length**: 1000-1100 words (approximately 7-8 minutes of natural speech)\n");
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
                prompt.append("**Transcript Length**: 1100-1200 words (approximately 7-8 minutes of academic discussion)\n");
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
                prompt.append("**Transcript Length**: 1100-1200 words (approximately 7-8 minutes of academic lecture)\n");
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
                                prompt.append("- Target: 700-900 words. Maximum: 1050 words.\n");
                                break;
                        case 2:
                                prompt.append("- Target: 800-1000 words. Maximum: 1150 words.\n");
                                break;
                        case 3:
                                prompt.append("- Target: 900-1100 words. Maximum: 1250 words.\n");
                                break;
                        case 4:
                                prompt.append("- Target: 1000-1200 words. Maximum: 1400 words.\n");
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

                prompt.append("## TASK: Generate IELTS Listening Questions (Phase 2/2)\n\n");
                prompt.append("Based on the provided transcript, generate the exam questions.\n\n");

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
                prompt.append("- `correct_answer`: single-element array `[\"B\"]`\n\n");
                prompt.append("**MULTIPLE_CHOICE_MULTIPLE_ANSWERS** (Choose TWO letters) - STRICT FORMAT:\n");
                prompt.append("- Create **TWO consecutive questions** (e.g., Q21 and Q22)\n");
                prompt.append("- **BOTH questions MUST have IDENTICAL** `text` and `options`\n");
                prompt.append("- **BOTH questions MUST have the SAME** `correct_answer` array with **EXACTLY 2 letters**\n");
                prompt.append("- Example:\n");
                prompt.append("```json\n");
                prompt.append("[\n");
                prompt.append("  {\"question_number\": 21, \"question_type\": \"MULTIPLE_CHOICE_MULTIPLE_ANSWERS\",\n");
                prompt.append("   \"question_content\": {\"text\": \"Which TWO benefits are mentioned?\",\n");
                prompt.append("    \"options\": [\"A Lower cost\", \"B Better quality\", \"C Faster delivery\", \"D More options\", \"E Eco-friendly\"]},\n");
                prompt.append("   \"correct_answer\": [\"B\", \"E\"]},\n");
                prompt.append("  {\"question_number\": 22, \"question_type\": \"MULTIPLE_CHOICE_MULTIPLE_ANSWERS\",\n");
                prompt.append("   \"question_content\": {\"text\": \"Which TWO benefits are mentioned?\",\n");
                prompt.append("    \"options\": [\"A Lower cost\", \"B Better quality\", \"C Faster delivery\", \"D More options\", \"E Eco-friendly\"]},\n");
                prompt.append("   \"correct_answer\": [\"B\", \"E\"]}\n");
                prompt.append("]\n");
                prompt.append("```\n");
                prompt.append("**FAILURE**: If correct_answer has only 1 letter, validation will fail!\n\n");

                // Matching Format
                prompt.append("### Matching Format\n");
                prompt.append("- MATCHING questions: `question_content` must include only `text`.\n");
                prompt.append("- All options belong in the block content (`section_layout.blocks[].content.options`).\n");
                prompt.append("- **CRITICAL**: Options MUST be an array of `{letter, text}` objects, NOT strings.\n\n");

                // Answer Validation - ENHANCED
                prompt.append("### CRITICAL: Answer Validation Rules\n");
                prompt.append("1. **Answers MUST appear in transcript**: For FILL_IN_BLANK questions, the correct_answer word(s) MUST appear EXACTLY (or nearly exactly) in the transcript you generated.\n");
                prompt.append("2. **Each question has OWN answer**: Each question MUST have its OWN `correct_answer` array.\n");
                prompt.append("3. **MCMA = TWO answers**: For MULTIPLE_CHOICE_MULTIPLE_ANSWERS, `correct_answer` MUST have EXACTLY 2 letters.\n");
                prompt.append("4. **DO NOT duplicate answers**: Question ").append(startNumber)
                                .append("'s correct_answer = [\"answer1\"] (just this question's answer)\n\n");

                // Explanation format
                prompt.append("### Explanation Format\n");
                prompt.append("All explanations must be JSON objects in Vietnamese:\n");
                prompt.append("```json\n");
                prompt.append("{\n");
                prompt.append("  \"detail\": \"<Vietnamese explanation: why is this correct?>\",\n");
                prompt.append("  \"quote\": \"<EXACT English quote from transcript that PROVES the answer>\",\n");
                prompt.append("  \"strategy\": \"<Vietnamese strategy tip for this question type>\"\n");
                prompt.append("}\n");
                prompt.append("```\n\n");

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
                prompt.append("  - [ ] Every FILL_IN_BLANK answer appears in the transcript\n");
                prompt.append("  - [ ] Every MCMA question has correct_answer with EXACTLY 2 letters\n");
                prompt.append("  - [ ] Every question has its own individual `correct_answer` (NOT duplicated)\n");
                prompt.append("  - [ ] Every question has its own individual `explanation` object\n\n");

                prompt.append("### Output Requirements\n");
                prompt.append("Return valid JSON with:\n");
                prompt.append("1. `section_layout`: The block layout.\n");
                prompt.append("2. `questions`: The array of 10 questions.\n");
                prompt.append("3. `figure_description`: If needed for Part 2 map/plan.\n");
                prompt.append("Do NOT re-output the transcript.\n");

                return prompt.toString();
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
         * Get JSON Schema for Pass 2 (Questions).
         */
        @SuppressWarnings("unchecked")
        public Map<String, Object> getListeningQuestionsSchema() {
                // Reuse the full schema but remove transcript requirement/property
                Map<String, Object> fullSchema = getListeningJsonSchema();
                Map<String, Object> properties = new LinkedHashMap<>(
                                (Map<String, Object>) fullSchema.get("properties"));
                properties.remove("transcript");
                properties.remove("audio_placeholder");

                Map<String, Object> schema = new LinkedHashMap<>();
                schema.put("type", "object");
                schema.put("properties", properties);
                schema.put("required", List.of("section_layout", "questions"));
                return schema;
        }

        // ==================== WRITING PROMPTS ====================

        /**
         * Build complete prompt for Writing content generation.
         * Enhanced for Phase 4 with detailed Task 1 and Task 2 requirements.
         */
        public String buildWritingPrompt(GenerationRequestDTO request) {
                StringBuilder prompt = new StringBuilder();

                Integer partNumber = request.getPartNumber();
                if (partNumber == null) {
                        partNumber = 1;
                }

                boolean isTask1 = partNumber == 1;
                String testType = request.getTestType() != null ? request.getTestType().name() : "ACADEMIC";

                if (isTask1) {
                        buildWritingTask1Prompt(prompt, request, testType);
                } else {
                        buildWritingTask2Prompt(prompt, request);
                }

                prompt.append("### Output Requirements (STRICT)\n");
                prompt.append("- Must include `task_prompt`, `task_type`, and `word_requirement`\n");
                prompt.append("- Must include full fields required by the task type\n");
                prompt.append("- Do NOT include `sample_answer` unless explicitly requested\n\n");

                prompt.append("### Mini Example Output (Structure Only)\n");
                prompt.append("{\"task_prompt\":\"The chart below shows...\",\"task_type\":\"TASK_1_ACADEMIC\",");
                prompt.append("\"word_requirement\":150,\"chart_data\":{\"chart_type\":\"bar_grouped\",\"title\":\"...\",");
                prompt.append("\"source\":\"...\",\"x_axis\":{\"label\":\"\",\"values\":[\"2019\",\"2020\"]},");
                prompt.append("\"y_axis\":{\"label\":\"\",\"unit\":\"%\"},\"series\":[{\"name\":\"A\",");
                prompt.append("\"values\":[10,20],\"color\":\"#4F46E5\"}]}}\n\n");

                if (request.getCustomInstructions() != null && !request.getCustomInstructions().isBlank()) {
                        prompt.append("### Custom Instructions (Highest Priority)\n");
                        prompt.append(request.getCustomInstructions()).append("\n\n");
                }

                return prompt.toString();
        }

        /**
         * Build Task 1 prompt (Charts/Graphs/Letters).
         */
        private void buildWritingTask1Prompt(StringBuilder prompt, GenerationRequestDTO request, String testType) {
                if ("GENERAL_TRAINING".equalsIgnoreCase(testType)) {
                        prompt.append("## TASK: Generate IELTS General Training Writing Task 1 (Letter)\n\n");
                        prompt.append("### Task Overview\n");
                        prompt.append("Create a letter-writing task that is realistic and clearly specifies:\n");
                        prompt.append("- The situation/context\n");
                        prompt.append("- Who to write to\n");
                        prompt.append("- What to include in the letter\n");
                        prompt.append("- The appropriate tone (formal/informal/semi-formal)\n\n");
                        prompt.append("You MUST include `letter_context` in the output.\n\n");

                        prompt.append("### Letter Type Options (choose one based on topic):\n");
                        prompt.append("- Complaint letter (about service, product, noise, etc.)\n");
                        prompt.append("- Request letter (for information, permission, refund)\n");
                        prompt.append("- Suggestion letter (to improve something)\n");
                        prompt.append("- Application letter (for job, course, membership)\n");
                        prompt.append("- Thank you/Apology letter\n\n");
                } else {
                        prompt.append("## TASK: Generate IELTS Academic Writing Task 1 (Data Visualization)\n\n");
                        prompt.append("### Task Overview\n");
                        prompt.append("Create a data description task with REALISTIC numerical data.\n");
                        prompt.append("The data should show clear trends, comparisons, or patterns.\n\n");

                        prompt.append("### Chart Type Selection (choose ONE type that best fits the topic):\n\n");
                        prompt.append("**Line Graph (line_multiple)**\n");
                        prompt.append("- Best for: Changes over time, trends\n");
                        prompt.append("- Example: Population growth, temperature changes, sales over years\n\n");

                        prompt.append("**Bar Chart (bar_grouped or bar_stacked)**\n");
                        prompt.append("- Best for: Comparisons between categories\n");
                        prompt.append("- Example: Spending by category, survey results by age group\n\n");

                        prompt.append("**Pie Chart (pie_standard)**\n");
                        prompt.append("- Best for: Proportions of a whole (should total ~100%)\n");
                        prompt.append("- Example: Budget allocation, market share, survey preferences\n\n");

                        prompt.append("**Table (table)**\n");
                        prompt.append("- Best for: Multiple data points across categories\n");
                        prompt.append("- Example: Statistics across countries and years\n\n");

                        prompt.append("**Process Diagram (process)**\n");
                        prompt.append("- Best for: Showing steps/stages in a process\n");
                        prompt.append("- Example: Manufacturing process, life cycle, how something works\n\n");

                        prompt.append("**Map/Floor Plan (map)**\n");
                        prompt.append("- Best for: Changes to a location over time\n");
                        prompt.append("- Example: Town development, building layout before/after\n\n");
                }

                prompt.append("### Topic\n");
                prompt.append("**Topic**: ").append(request.getTopic()).append("\n\n");

                prompt.append("### Background Facts (for realistic data creation)\n");
                List<String> facts = request.getFacts();
                if (facts != null && !facts.isEmpty()) {
                        for (int i = 0; i < Math.min(10, facts.size()); i++) {
                                prompt.append(String.format("%d. %s\n", i + 1, facts.get(i)));
                        }
                } else {
                        prompt.append("No facts provided. Use realistic, verifiable details.\n");
                }
                prompt.append("\n");

                if (!"GENERAL_TRAINING".equalsIgnoreCase(testType)) {
                        prompt.append("### Chart Data Requirements (CRITICAL)\n");
                        prompt.append("You MUST provide a `chart_data` object with:\n");
                        prompt.append("1. **chart_type**: Exactly one of: 'line_multiple', 'bar_grouped', 'bar_stacked',\n");
                        prompt.append("   'pie_standard', 'table', 'process', 'map'\n");
                        prompt.append("2. **title**: Clear, descriptive title\n");
                        prompt.append("3. **source**: Data source (e.g., 'World Bank, 2023')\n");
                        prompt.append("4. **x_axis**: { label: string, values: string[] }\n");
                        prompt.append("5. **y_axis**: { label: string, unit: string (e.g., '%', 'million', '$') }\n");
                        prompt.append("6. **series**: Array of { name: string, values: number[], color: string (hex) }\n");
                        prompt.append("7. **data_points**: Minimum 4 x_axis values, 2-5 series for comparison\n\n");
                        prompt.append("If chart_type is `process` or `map`, you MUST also include `figure_description`.\n\n");

                        prompt.append("### Data Realism Guidelines\n");
                        prompt.append("- Use plausible numbers that reflect real-world data\n");
                        prompt.append("- Ensure data shows patterns (trends, comparisons, peaks)\n");
                        prompt.append("- For percentages: ensure categories sum to 100% for pie charts\n");
                        prompt.append("- Include notable features: highest/lowest values, significant changes\n\n");
                }

                prompt.append("### Task Prompt Requirements\n");
                prompt.append("- Start with 'The [chart/graph/diagram] below shows...'\n");
                prompt.append("- Clearly state what data is presented\n");
                prompt.append("- Include time period if applicable\n");
                prompt.append("- End with: 'Summarise the information by selecting and reporting the main features, ");
                prompt.append("and make comparisons where relevant.'\n");
                prompt.append("- Word requirement reminder: 'Write at least 150 words.'\n\n");
        }

        /**
         * Build Task 2 prompt (Essay).
         */
        private void buildWritingTask2Prompt(StringBuilder prompt, GenerationRequestDTO request) {
                prompt.append("## TASK: Generate IELTS Writing Task 2 (Essay Question)\n\n");

                prompt.append("### Task Overview\n");
                prompt.append("Create a thought-provoking essay question that:\n");
                prompt.append("- Presents a clear, debatable issue\n");
                prompt.append("- Is relevant to modern society\n");
                prompt.append("- Allows for multiple perspectives\n");
                prompt.append("- Is suitable for international test-takers\n\n");

                String essayType = request.getWritingEssayType();
                if (essayType != null && !essayType.isBlank()) {
                        prompt.append("### Essay Type (FIXED)\n");
                        prompt.append("You MUST generate a ").append(essayType).append(" essay prompt.\n");
                        prompt.append("Do NOT choose another type.\n\n");
                } else {
                        prompt.append("### Essay Type Selection (choose ONE that fits the topic):\n\n");
                        prompt.append("**Opinion Essay (Agree/Disagree)**\n");
                        prompt.append("- Format: Statement + 'To what extent do you agree or disagree?'\n");
                        prompt.append("- Candidates give their opinion and support it\n\n");

                        prompt.append("**Discussion Essay (Discuss Both Views)**\n");
                        prompt.append("- Format: Two views + 'Discuss both views and give your own opinion.'\n");
                        prompt.append("- Candidates discuss multiple perspectives\n\n");

                        prompt.append("**Advantages/Disadvantages Essay**\n");
                        prompt.append("- Format: Topic + 'Discuss the advantages and disadvantages.'\n");
                        prompt.append("- May ask for opinion: 'Do the advantages outweigh the disadvantages?'\n\n");

                        prompt.append("**Problem/Solution Essay**\n");
                        prompt.append("- Format: Issue + 'What are the problems and how can they be solved?'\n");
                        prompt.append("- Focus on identifying issues and proposing solutions\n\n");

                        prompt.append("**Two-Part Question Essay**\n");
                        prompt.append("- Format: Statement + TWO related questions\n");
                        prompt.append("- Example: 'Why is this happening? What can be done about it?'\n\n");
                }

                prompt.append("### Topic\n");
                prompt.append("**Topic**: ").append(request.getTopic()).append("\n\n");

                prompt.append("### Background Facts (for context)\n");
                List<String> facts = request.getFacts();
                if (facts != null && !facts.isEmpty()) {
                        for (int i = 0; i < Math.min(8, facts.size()); i++) {
                                prompt.append(String.format("%d. %s\n", i + 1, facts.get(i)));
                        }
                } else {
                        prompt.append("No facts provided. Use realistic, verifiable details.\n");
                }
                prompt.append("\n");

                prompt.append("### Essay Question Requirements\n");
                prompt.append("- Clear, unambiguous wording\n");
                prompt.append("- Avoid overly specialized or culturally-specific topics\n");
                prompt.append("- The question should be answerable in 250+ words\n");
                prompt.append("- Include: 'Give reasons for your answer and include any relevant examples ");
                prompt.append("from your own knowledge or experience.'\n");
                prompt.append("- Word requirement reminder: 'Write at least 250 words.'\n\n");

                prompt.append("### Essay Metadata (REQUIRED)\n");
                prompt.append("- Provide `essay_metadata` with essay_type, topic_category, complexity\n\n");

                prompt.append("### Sample Answer Guidelines (optional, include if requested)\n");
                prompt.append("- Band 8.0+ style writing\n");
                prompt.append("- Clear thesis statement\n");
                prompt.append("- Well-developed paragraphs with examples\n");
                prompt.append("- Appropriate linking words\n");
                prompt.append("- Academic vocabulary\n\n");
        }

        /**
         * Build system prompt for Writing generation.
         * Enhanced for Phase 4 with comprehensive instructions.
         */
        public String buildWritingSystemPrompt() {
                StringBuilder system = new StringBuilder();

                system.append("You are an expert IELTS Writing task creator with 15+ years of experience.\n\n");

                system.append("## Your Role\n");
                system.append("Create authentic, challenging IELTS Writing tasks that:\n");
                system.append("- Match official Cambridge IELTS test standards\n");
                system.append("- Use realistic, verifiable data for Task 1\n");
                system.append("- Pose thought-provoking questions for Task 2\n");
                system.append("- Follow exact IELTS formatting conventions\n\n");

                system.append("## Critical Rules\n");
                system.append("1. For Task 1 Academic: ALWAYS provide complete chart_data with numerical values\n");
                system.append("2. For Task 1 GT: Create realistic, specific letter scenarios\n");
                system.append("3. For Task 2: Ensure questions are debatable (not yes/no answers)\n");
                system.append("4. Avoid culturally biased or politically sensitive topics\n");
                system.append("5. Use professional, formal English throughout\n");
                system.append("6. Do NOT include sample_answer unless explicitly requested\n\n");

                system.append("## Chart Data Generation (Task 1 Academic)\n");
                system.append("When generating chart_data:\n");
                system.append("- Use 4-6 x_axis data points (years, categories, etc.)\n");
                system.append("- Include 2-4 series for meaningful comparison\n");
                system.append("- Use realistic number ranges (research typical values)\n");
                system.append("- Ensure data tells a story (trends, peaks, anomalies)\n");
                system.append("- Colors should be distinct: ['#4F46E5','#10B981','#F59E0B','#EC4899']\n\n");

                system.append("## Essay Question Quality (Task 2)\n");
                system.append("- Questions must be clear and unambiguous\n");
                system.append("- Avoid leading questions that suggest an answer\n");
                system.append("- Include specific task instructions (agree/disagree, discuss, etc.)\n");
                system.append("- Topics should be globally relevant\n\n");

                system.append("## Output Format\n");
                system.append("Return valid JSON exactly matching this schema:\n");
                system.append(getWritingJsonSchemaAsString());

                return system.toString();
        }

        /**
         * Get JSON schema as formatted string for system prompt.
         */
        private String getWritingJsonSchemaAsString() {
                return """
                                {
                                  "task_prompt": "The complete task instruction text",
                                  "task_type": "TASK_1_ACADEMIC | TASK_1_GT_LETTER | TASK_2_OPINION | TASK_2_DISCUSSION | TASK_2_ADVANTAGES | TASK_2_PROBLEM_SOLUTION | TASK_2_TWO_PART",
                                  "word_requirement": 150 or 250,
                                  "chart_data": {  // Required for Task 1 Academic only
                                    "chart_type": "line_multiple | bar_grouped | bar_stacked | pie_standard | table | process | map",
                                    "title": "Chart title",
                                    "source": "Data source",
                                    "x_axis": { "label": "X-axis label", "values": ["2019", "2020", "2021", "2022"] },
                                    "y_axis": { "label": "Y-axis label", "unit": "%" },
                                    "series": [
                                      { "name": "Series 1", "values": [10, 25, 30, 45], "color": "#4F46E5" },
                                      { "name": "Series 2", "values": [20, 15, 35, 40], "color": "#10B981" }
                                    ]
                                  },
                                  "figure_description": {  // For process/map types
                                    "type": "process | map",
                                    "title": "Description title",
                                    "elements": ["Step 1", "Step 2", "Step 3"],
                                    "image_placeholder": "Describe what the diagram shows"
                                  },
                                  "letter_context": {  // For General Training Task 1
                                    "recipient": "Who to write to",
                                    "relationship": "formal | informal | semi-formal",
                                    "purpose": "complaint | request | application | thank_you | apology | suggestion"
                                  },
                                  "essay_metadata": {  // For Task 2
                                    "essay_type": "opinion | discussion | advantages_disadvantages | problem_solution | two_part",
                                    "topic_category": "technology | education | environment | health | society | work",
                                    "complexity": "standard | complex"
                                  },
                                  "sample_answer": {  // Optional
                                    "content": "Full sample essay/response (Band 8+)",
                                    "word_count": 280,
                                    "band_score": 8.0,
                                    "examiner_comments": "Brief feedback on why this scores well"
                                  }
                                }
                                """;
        }

        // ==================== JSON SCHEMAS ====================

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
                blockSchema.put("properties", Map.of(
                                "block_type", Map.of("type", "string"),
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

        // ==================== QUESTION TYPE INSTRUCTIONS ====================

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
