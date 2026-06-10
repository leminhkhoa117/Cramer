package com.cramer.service.abts.prompt;

import com.cramer.dto.abts.GenerationRequestDTO;

import java.util.List;

public class WritingPromptBuilder {

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

        /**
         * Build the prompt for Writing Phase 1/3: the task only (no sample answer).
         *
         * <p>Reuses the existing Task 1 / Task 2 builders, then constrains the output
         * to exclude {@code sample_answer} (produced in Phase 2).</p>
         */
        public String buildWritingTaskPrompt(GenerationRequestDTO request) {
                StringBuilder prompt = new StringBuilder();

                Integer partNumber = request.getPartNumber();
                if (partNumber == null) {
                        partNumber = 1;
                }
                boolean isTask1 = partNumber == 1;
                String testType = request.getTestType() != null ? request.getTestType().name() : "ACADEMIC";

                prompt.append("## PHASE 1/3: Generate the IELTS Writing TASK only\n\n");

                if (isTask1) {
                        buildWritingTask1Prompt(prompt, request, testType);
                } else {
                        buildWritingTask2Prompt(prompt, request);
                }

                prompt.append("### Output Requirements (STRICT)\n");
                prompt.append("- Must include `task_prompt`, `task_type`, and `word_requirement`\n");
                prompt.append("- Must include the full structure field required by the task type ");
                prompt.append("(`chart_data`/`figure_description` for Task 1 Academic, `letter_context` for GT, `essay_metadata` for Task 2)\n");
                prompt.append("- **DO NOT include `sample_answer`** - it is generated in a later phase\n\n");

                if (request.getCustomInstructions() != null && !request.getCustomInstructions().isBlank()) {
                        prompt.append("### Custom Instructions (Highest Priority)\n");
                        prompt.append(request.getCustomInstructions()).append("\n\n");
                }

                return prompt.toString();
        }

        /**
         * Build the prompt for Writing Phase 2/3: a Band 8+ sample answer for the task.
         */
        public String buildWritingSamplePrompt(GenerationRequestDTO request, String taskJson) {
                StringBuilder prompt = new StringBuilder();

                Integer partNumber = request.getPartNumber();
                if (partNumber == null) {
                        partNumber = 1;
                }
                int minWords = partNumber == 1 ? 150 : 250;

                prompt.append("## PHASE 2/3: Write a model (Band 8+) sample answer\n\n");
                prompt.append("You are given the writing task produced in Phase 1.\n");
                prompt.append("Write a high-quality model answer that fully addresses it.\n\n");

                prompt.append("### Task (JSON from Phase 1)\n");
                prompt.append("```json\n").append(taskJson).append("\n```\n\n");

                prompt.append("### Sample Answer Requirements\n");
                prompt.append("- Band 8.0+ quality (clear position, cohesive paragraphs, precise vocabulary, varied grammar)\n");
                prompt.append("- At least ").append(minWords).append(" words\n");
                if (partNumber == 1) {
                        prompt.append("- Task 1: accurately describe the data/figure or fulfil the letter purpose; no personal opinion for Academic Task 1\n");
                } else {
                        prompt.append("- Task 2: clear thesis, developed arguments with examples, logical conclusion\n");
                }
                prompt.append("\n");

                prompt.append("### Output Requirements (STRICT)\n");
                prompt.append("Return valid JSON with a single top-level key `sample_answer`:\n");
                prompt.append("```json\n");
                prompt.append("{\n");
                prompt.append("  \"sample_answer\": {\n");
                prompt.append("    \"content\": \"<full model answer text>\",\n");
                prompt.append("    \"word_count\": <integer>,\n");
                prompt.append("    \"band_score\": 8.0\n");
                prompt.append("  }\n");
                prompt.append("}\n");
                prompt.append("```\n");
                prompt.append("Do NOT re-output the task.\n");

                return prompt.toString();
        }

        /**
         * Build the prompt for Writing Phase 3/3: band breakdown + grading notes for the sample.
         */
        public String buildWritingBandPrompt(GenerationRequestDTO request, String taskJson, String sampleJson) {
                StringBuilder prompt = new StringBuilder();

                prompt.append("## PHASE 3/3: Grade the sample answer (IELTS band breakdown)\n\n");
                prompt.append("Assess the Phase 2 sample answer against the four IELTS Writing criteria.\n\n");

                prompt.append("### Task (JSON from Phase 1)\n");
                prompt.append("```json\n").append(taskJson).append("\n```\n\n");

                prompt.append("### Sample Answer (JSON from Phase 2)\n");
                prompt.append("```json\n").append(sampleJson).append("\n```\n\n");

                prompt.append("### Grading Criteria (score each 0-9, half-bands allowed)\n");
                prompt.append("- **TR** - Task Response/Achievement\n");
                prompt.append("- **CC** - Coherence & Cohesion\n");
                prompt.append("- **LR** - Lexical Resource\n");
                prompt.append("- **GRA** - Grammatical Range & Accuracy\n\n");

                prompt.append("### Output Requirements (STRICT)\n");
                prompt.append("Return valid JSON:\n");
                prompt.append("```json\n");
                prompt.append("{\n");
                prompt.append("  \"band_breakdown\": { \"TR\": 8.0, \"CC\": 8.0, \"LR\": 7.5, \"GRA\": 8.0 },\n");
                prompt.append("  \"key_phrases\": [\"<notable phrase 1>\", \"<notable phrase 2>\"],\n");
                prompt.append("  \"grading_notes\": \"<concise Vietnamese rationale for the scores>\"\n");
                prompt.append("}\n");
                prompt.append("```\n");
                prompt.append("Do NOT re-output the task or the sample answer.\n");

                return prompt.toString();
        }
}