package com.cramer.service.abts;

import com.cramer.dto.abts.GenerationRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(PromptBuilderService.class);

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
                prompt.append("- **Word Count**: 700-800 words.\n");
                break;
            case 2:
                prompt.append("- **Context**: Workplace, training, or general interest topic.\n");
                prompt.append("- **Style**: Discursive, logical argument or detailed description.\n");
                prompt.append("- **Word Count**: 800-900 words.\n");
                break;
            case 3:
                prompt.append("- **Context**: Complex academic topic.\n");
                prompt.append("- **Style**: Argumentative, abstract, complex sentence structures.\n");
                prompt.append("- **Word Count**: 900-1000 words.\n");
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
        prompt.append("Only the 'explanation' field should be in the language specified above.\n");
        prompt.append("⚠️ EXPLANATION QUALITY: Explanations must be detailed and structured. Include:\n");
        prompt.append("   1. **Reasoning**: Why is this the correct answer?\n");
        prompt.append("   2. **Evidence**: A direct quote from the passage.\n");
        prompt.append("   3. **Strategy**: A brief tip on how to find such answers.\n\n");

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
                for (int i = 0; i < facts.size(); i++) {
                    prompt.append(String.format("%d. %s\n", i + 1, facts.get(i)));
                }
            } else {
                prompt.append("### Content Generation Mode (Research Mode)\n");
                prompt.append("You are acting as a researcher and writer.\n");
                prompt.append("1. Research the topic: '").append(request.getTopic()).append("'\n");
                if (facts != null && !facts.isEmpty()) {
                    prompt.append("2. Incorporate these key points: ").append(String.join("; ", facts)).append("\n");
                }
                prompt.append("3. Create a comprehensive, academic article suitable for IELTS Reading.\n");
                prompt.append(
                        "4. Invent plausible academic details (names, dates, studies) if needed to ensure density and length, but keep them realistic.\n");
            }
            prompt.append("\n");

            // Passage requirements
            prompt.append("### Passage Requirements\n");
            if (partNumber == 1) {
                prompt.append("- **Word count**: 700-800 words.\n");
            } else if (partNumber == 2) {
                prompt.append("- **Word count**: 800-900 words.\n");
            } else {
                prompt.append("- **Word count**: 900-1000 words.\n");
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

        // Question requirements
        prompt.append("### Question Requirements\n");
        prompt.append("- **Total questions**: 13-14 questions\n");
        prompt.append("- **Question types to include**:\n");

        if (request.getQuestionTypes() != null && !request.getQuestionTypes().isEmpty()) {
            prompt.append("### Question Structure (Follow EXACTLY)\n");
            prompt.append(
                    "You must organize the 13-14 questions into 3 DISTINCT GROUPS based on the requested types.\n");

            prompt.append("\n#### Special Rule for Block Completion Types (TABLE, FLOW_CHART, DIAGRAM):\n");
            prompt.append(
                    "- IF generating `TABLE_COMPLETION`, `FLOW_CHART_COMPLETION`, or `DIAGRAM_LABEL_COMPLETION`:\n");
            prompt.append(
                    "  - The **FIRST question** of the group MUST contain the MAIN HTML CONTENT definition (e.g., `<table class=\"question-table\">...</table>`) in its `text` field.\n");
            prompt.append(
                    "  - **ALL SUBSEQUENT QUESTIONS** in that group MUST have an empty string `\"\"` for their `text` field.\n");
            prompt.append("  - This is CRITICAL for correct rendering. Do not repeat the table in every question.\n");

            int groupSize = Math.max(4, 13 / request.getQuestionTypes().size());
            for (int i = 0; i < request.getQuestionTypes().size(); i++) {
                String type = request.getQuestionTypes().get(i);
                prompt.append(String.format("   - **Group %d**: %s (%d-%d questions)\n", i + 1, type, groupSize,
                        groupSize + 1));
            }
        } else {
            // Default Strict Structure based on Part Number
            prompt.append("### Question Structure (STRICT GROUPING)\n");
            prompt.append("You must generate exactly 3 groups of questions:\n\n");

            if (partNumber == 1) {
                prompt.append("1. **Questions 1-6**: TRUE_FALSE_NOT_GIVEN (6 questions)\n");
                prompt.append("   - Statements in chronological order.\n\n");
                prompt.append("2. **Questions 7-13**: FILL_IN_BLANK (7 questions)\n");
                prompt.append("   - Summary or Note completion format.\n");
            } else if (partNumber == 2) {
                prompt.append("1. **Questions 14-18**: MATCHING_INFORMATION (5 questions)\n");
                prompt.append("   - Which paragraph contains the following information?\n\n");
                prompt.append("2. **Questions 19-22**: MATCHING_FEATURES (4 questions)\n");
                prompt.append("   - Match statements to people/dates/categories.\n");
                prompt.append("   - Provide `options` array with the list of people/categories.\n\n");
                prompt.append("3. **Questions 23-26**: FILL_IN_BLANK (4 questions)\n");
                prompt.append("   - Summary completion.\n");
            } else {
                // Part 3 (Hardest)
                prompt.append("1. **Questions 27-32**: MATCHING_HEADINGS (6 questions)\n");
                prompt.append("   - Match headings (i-ix) to paragraphs (A-G).\n");
                prompt.append("   - Provide shared `options` array (List of Headings).\n\n");
                prompt.append("2. **Questions 33-36**: YES_NO_NOT_GIVEN (4 questions)\n");
                prompt.append("   - Do statements agree with writer's claims?\n\n");
                prompt.append("3. **Questions 37-40**: MULTIPLE_CHOICE (4 questions)\n");
                prompt.append("   - Standard A/B/C/D format.\n");
            }

            prompt.append("\n#### Standard Question Formats:\n");
            prompt.append(
                    "- **TRUE_FALSE_NOT_GIVEN / YES_NO_NOT_GIVEN**: Include `statement` in `question_content`.\n");
            prompt.append(
                    "- **MATCHING_HEADINGS**: Include `paragraph` (e.g., 'Paragraph A') in `question_content`. Provide `options` array.\n");
            prompt.append(
                    "- **FILL_IN_BLANK / SUMMARY_COMPLETION**: CRITICAL - Use EXACTLY 4 underscores `____` for blanks. NOT 3, NOT 5, NOT 6 - EXACTLY 4!\n");
            prompt.append("  Example: \"The researcher found that the main cause was ____.\"\n");
        }
        prompt.append("IMPORTANT: Do NOT mix question types. Finish one group before starting the next.\n");
        prompt.append("\n");

        // Answer and explanation requirements
        prompt.append("### Answer & Explanation Requirements\n");
        prompt.append("- Each question MUST have a proper `correct_answer` array.\n");
        prompt.append("- Each question MUST have an explanation in ")
                .append(request.getExplanationLanguage() == GenerationRequestDTO.ExplanationLanguage.VI
                        ? "Vietnamese"
                        : "English")
                .append("\n");
        prompt.append("- Explanations must reference specific paragraph letters.\n\n");

        // Final reminder
        prompt.append("### ⚠️ FINAL REMINDER ⚠️\n");
        prompt.append("Before responding, verify:\n");
        prompt.append("1. Word count logic for Part ").append(partNumber).append("\n");
        prompt.append("2. If generating TABLE/CHART completion, ONLY the first question has the HTML content.\n");
        prompt.append("3. Valid JSON format with all required fields.\n\n");

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
        system.append("- Are based ONLY on provided facts (no hallucination)\n\n");

        system.append("## CRITICAL: Word Count Requirements\n");
        system.append("- **Target length**: 900-1000 words. This is non-negotiable.\n");
        system.append(
                "- If provided facts are limited, you MUST expand with general academic knowledge related to the topic.\n");
        system.append(
                "- Passages under 850 words will be REJECTED. Aim for 950 words to be safe.\n\n");

        system.append("## Other Critical Rules\n");
        system.append(
                "1. FACTUAL ACCURACY: Prioritize provided facts. If gaps exist, use accurate general knowledge.\n");
        system.append(
                "2. PARAGRAPH LABELS: Use <strong>A.</strong>, <strong>B.</strong>, etc. at the start of paragraphs.\n");
        system.append(
                "3. PARAGRAPH SPACING: Separate each paragraph with a blank line (double newline).\n");
        system.append("4. ANSWER CONSISTENCY: Answers must be strictly contained within the text.\n");
        system.append("5. JSON FORMAT: Return valid JSON matching the schema exactly.\n");
        system.append(
                "6. LANGUAGE: The passage and ALL question content (statements, text, options) must be in ENGLISH.\n");
        system.append(
                "   ONLY the 'explanation' field for each question should be in the specified language (Vietnamese or English).\n\n");

        system.append("## Output Format\n");
        system.append("You MUST respond with valid JSON only. No markdown, no explanations outside JSON.\n");

        return system.toString();
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
        for (int i = 0; i < facts.size(); i++) {
            prompt.append(String.format("%d. %s\n", i + 1, facts.get(i)));
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
        if (request.getQuestionTypes() != null && !request.getQuestionTypes().isEmpty()) {
            prompt.append("### Required Question Types\n");
            for (String type : request.getQuestionTypes()) {
                prompt.append("- ").append(type).append("\n");
            }
            prompt.append("\n");
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
        prompt.append("### Question Content Format for FILL_IN_BLANK\n");
        prompt.append(
                "**CRITICAL: Use EXACTLY 4 underscores `____` for every blank. NOT 3, NOT 5, NOT 6+. EXACTLY 4.**\n");
        prompt.append("Include question number INLINE (no strong tag for listening):\n");
        prompt.append("```json\n");
        prompt.append(
                "{\"text\": \"• making sure the beach does not have 1 ____ on it\", \"section_title\": \"Beach\"}\n");
        prompt.append("{\"text\": \"– no 2 ____\"}\n");
        prompt.append("```\n");
        prompt.append("The `section_title` field is optional, add only when section changes.\n\n");

        // Figure descriptions if needed (Part 2)
        if (partNumber == 2) {
            prompt.append("### Figure Description (for Map/Plan)\n");
            prompt.append("If including map labelling, provide `figure_description` with:\n");
            prompt.append("- title: Description of the map/plan\n");
            prompt.append("- elements: Array of labeled locations\n");
            prompt.append("- answer_locations: Which letters correspond to which blanks\n\n");
        }

        // Explanation language
        prompt.append("### Explanation Language\n");
        prompt.append("All explanations must be in: ");
        prompt.append(request.getExplanationLanguage() == GenerationRequestDTO.ExplanationLanguage.VI
                ? "**Vietnamese (tiếng Việt)**"
                : "**English**");
        prompt.append("\n\n");

        return prompt.toString();
    }

    private void buildListeningPart1Prompt(StringBuilder prompt) {
        prompt.append("**Context**: Everyday social conversation between 2 speakers\n");
        prompt.append(
                "**Scenario Examples**: Booking accommodation, registering for service, asking about facilities\n");
        prompt.append("**Word count**: 450-550 words for transcript\n");
        prompt.append("**Questions**: 10 questions (Q1-10)\n");
        prompt.append("**Question Types**:\n");
        prompt.append("  - Form completion (names, dates, numbers, addresses)\n");
        prompt.append("  - Note completion (factual details)\n\n");
        prompt.append("**Speaker Guidelines**:\n");
        prompt.append("  - Use 2 named speakers (e.g., RECEPTIONIST:, CUSTOMER:)\n");
        prompt.append("  - Include natural greetings and closings\n");
        prompt.append("  - One speaker asks questions, the other provides information\n");
        prompt.append("  - Spell out names/addresses when necessary\n\n");
    }

    private void buildListeningPart2Prompt(StringBuilder prompt) {
        prompt.append("**Context**: Monologue in everyday social context\n");
        prompt.append("**Scenario Examples**: Tour guide speech, public announcement, facility orientation\n");
        prompt.append("**Word count**: 550-650 words for transcript\n");
        prompt.append("**Questions**: 10 questions (Q11-20)\n");
        prompt.append("**Question Types**:\n");
        prompt.append("  - Multiple choice (single answer)\n");
        prompt.append("  - Matching (features to categories)\n");
        prompt.append("  - Map/plan labelling (5-6 labels)\n\n");
        prompt.append("**Speaker Guidelines**:\n");
        prompt.append("  - Single speaker (named, e.g., GUIDE:, MANAGER:)\n");
        prompt.append("  - Organized sections with clear transitions\n");
        prompt.append("  - Reference to visual elements (map, diagram) where applicable\n\n");
    }

    private void buildListeningPart3Prompt(StringBuilder prompt) {
        prompt.append("**Context**: Academic discussion between 2-4 speakers\n");
        prompt.append("**Scenario Examples**: Tutorial discussion, project planning, seminar\n");
        prompt.append("**Word count**: 650-750 words for transcript\n");
        prompt.append("**Questions**: 10 questions (Q21-30)\n");
        prompt.append("**Question Types**:\n");
        prompt.append("  - Multiple choice (single or multiple answers)\n");
        prompt.append("  - Matching (opinions to speakers)\n");
        prompt.append("  - Sentence completion\n\n");
        prompt.append("**Speaker Guidelines**:\n");
        prompt.append("  - Use 2-4 named speakers (e.g., TUTOR:, SARAH:, MICHAEL:)\n");
        prompt.append("  - Include academic vocabulary\n");
        prompt.append("  - Show interaction: agreement, disagreement, building on ideas\n");
        prompt.append("  - Include hedging language and discourse markers\n\n");
    }

    private void buildListeningPart4Prompt(StringBuilder prompt) {
        prompt.append("**Context**: Academic lecture or monologue\n");
        prompt.append("**Scenario Examples**: University lecture, conference presentation, research briefing\n");
        prompt.append("**Word count**: 750-850 words for transcript\n");
        prompt.append("**Questions**: 10 questions (Q31-40)\n");
        prompt.append("**Question Types**:\n");
        prompt.append("  - Note completion (academic content)\n");
        prompt.append("  - Summary completion with word bank\n");
        prompt.append("  - Sentence completion\n\n");
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
        system.append("1. **WORD COUNT**: Transcript must match the specified word count range\n");
        system.append("2. **NATURAL SPEECH**: Dialogue must sound authentic when read aloud\n");
        system.append("3. **ANSWER CLARITY**: Each answer must be clearly spoken in the transcript\n");
        system.append("4. **DISTRACTION**: Include plausible distractors mentioned before the answer\n");
        system.append("5. **PARAPHRASING**: Questions should paraphrase information from transcript\n");
        system.append("6. **10 QUESTIONS**: Each part must have exactly 10 questions\n\n");

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
        system.append("- `section_layout`: Array describing visual question layout\n");
        system.append("- `questions`: Array of 10 question objects\n");
        system.append("- `audio_placeholder`: Metadata for future TTS generation\n");
        system.append("- `figure_description`: (If applicable) Map/diagram description\n\n");

        system.append("## JSON Format\n");
        system.append("You MUST respond with valid JSON only. No markdown, no explanations outside JSON.\n");

        return system.toString();
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
        for (int i = 0; i < Math.min(10, facts.size()); i++) {
            prompt.append(String.format("%d. %s\n", i + 1, facts.get(i)));
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

        prompt.append("### Topic\n");
        prompt.append("**Topic**: ").append(request.getTopic()).append("\n\n");

        prompt.append("### Background Facts (for context)\n");
        List<String> facts = request.getFacts();
        for (int i = 0; i < Math.min(8, facts.size()); i++) {
            prompt.append(String.format("%d. %s\n", i + 1, facts.get(i)));
        }
        prompt.append("\n");

        prompt.append("### Essay Question Requirements\n");
        prompt.append("- Clear, unambiguous wording\n");
        prompt.append("- Avoid overly specialized or culturally-specific topics\n");
        prompt.append("- The question should be answerable in 250+ words\n");
        prompt.append("- Include: 'Give reasons for your answer and include any relevant examples ");
        prompt.append("from your own knowledge or experience.'\n");
        prompt.append("- Word requirement reminder: 'Write at least 250 words.'\n\n");

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
        system.append("5. Use professional, formal English throughout\n\n");

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
        questionProps.put("question_type", Map.of("type", "string"));
        questionProps.put("question_content", Map.of("type", "object"));
        questionProps.put("correct_answer", Map.of("type", "array", "items", Map.of("type", "string")));
        questionProps.put("explanation", Map.of("type", "string"));
        questionItem.put("properties", questionProps);
        questionItem.put("required",
                List.of("question_number", "question_type", "question_content", "correct_answer", "explanation"));

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
        properties.put("transcript", Map.of("type", "string", "description", "Full transcript with speaker labels"));

        // Section layout array
        Map<String, Object> sectionLayout = new LinkedHashMap<>();
        sectionLayout.put("type", "array");
        sectionLayout.put("items", Map.of(
                "type", "object",
                "properties", Map.of(
                        "type", Map.of("type", "string"),
                        "title", Map.of("type", "string"),
                        "question_range", Map.of("type", "string"),
                        "fields", Map.of("type", "array"))));
        properties.put("section_layout", sectionLayout);

        // Questions array
        Map<String, Object> questions = new LinkedHashMap<>();
        questions.put("type", "array");
        Map<String, Object> questionItem = new LinkedHashMap<>();
        questionItem.put("type", "object");
        Map<String, Object> questionProps = new LinkedHashMap<>();
        questionProps.put("question_number", Map.of("type", "integer"));
        questionProps.put("question_type", Map.of("type", "string"));
        questionProps.put("question_content", Map.of("type", "object"));
        questionProps.put("correct_answer", Map.of("type", "array", "items", Map.of("type", "string")));
        questionProps.put("explanation", Map.of("type", "string"));
        questionItem.put("properties", questionProps);
        questionItem.put("required", List.of("question_number", "question_type", "correct_answer"));
        questions.put("items", questionItem);
        properties.put("questions", questions);

        // Audio placeholder
        Map<String, Object> audioPlaceholder = new LinkedHashMap<>();
        audioPlaceholder.put("type", "object");
        audioPlaceholder.put("properties", Map.of(
                "duration_estimate", Map.of("type", "string"),
                "speaker_count", Map.of("type", "integer"),
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
        schema.put("required", List.of("transcript", "questions"));

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
        properties.put("task_type", Map.of("type", "string"));
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
        schema.put("required", List.of("task_prompt", "task_type"));

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
            case "MULTIPLE_CHOICE_MULTIPLE" -> """
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
