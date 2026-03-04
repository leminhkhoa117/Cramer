package com.cramer.service.abts;

import com.cramer.dto.abts.RefinementRequestDTO;
import com.cramer.dto.abts.RefinementRequestDTO.ValidationIssue;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RefinementPromptBuilder - Builds prompts for Agent 2 (Refinement Agent)
 * 
 * This is kept separate from PromptBuilderService to maintain clean separation
 * between generation (Agent 1) and refinement (Agent 2) logic.
 * 
 * @since 2026-01-04
 */
@Service
public class RefinementPromptBuilder {

    /**
     * Build the system prompt for Agent 2 (Refinement Agent)
     * Comprehensive patch system with multiple operation types
     */
    public String buildSystemPrompt() {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a JSON Patch Specialist for IELTS test content.\n\n");
        prompt.append("Your role is to generate PATCHES to fix issues without regenerating content.\n\n");

        prompt.append("## Patch Operations\n");
        prompt.append("You have 3 operations available:\n\n");
        prompt.append("| Operation | Use Case |\n");
        prompt.append("|-----------|----------|\n");
        prompt.append("| `replace` | Change existing value (answer, quote, format) |\n");
        prompt.append("| `insert`  | Add new element to array (add missing question) |\n");
        prompt.append("| `append`  | Add text to string field (extend transcript) |\n\n");

        prompt.append("## Path Strategy (SIMPLIFIED)\n");
        prompt.append("You do NOT need to calculate array indices (e.g., `questions[6]`) yourself.\n");
        prompt.append("Instead, provide the **`questionNumber`** and the **`field`** name.\n\n");

        prompt.append("## Example Patches\n\n");

        prompt.append("### REPLACE - Fix answer value\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"operation\": \"replace\",\n");
        prompt.append("  \"questionNumber\": 7,\n");
        prompt.append("  \"path\": \"correct_answer[0]\",\n");
        prompt.append("  \"oldValue\": \"pears\",\n");
        prompt.append("  \"newValue\": \"peers\",\n");
        prompt.append("  \"reason\": \"Spelling error\"\n");
        prompt.append("}\n");
        prompt.append("```\n");
        prompt.append("*(Note: System will automatically map this to `questions[6].correct_answer[0]`)*\n\n");

        prompt.append("### INSERT - Add missing question\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"operation\": \"insert\",\n");
        prompt.append("  \"path\": \"questions\",\n");
        prompt.append("  \"index\": 5,\n");
        prompt.append("  \"newValue\": {\n");
        prompt.append("    \"question_number\": 6,\n");
        prompt.append("    \"question_type\": \"MULTIPLE_CHOICE\",\n");
        prompt.append("    \"correct_answer\": [\"B\"],\n");
        prompt.append("    \"options\": [\"A. ...\", \"B. ...\"],\n");
        prompt.append("    \"question_content\": { \"text\": \"...\" },\n");
        prompt.append("    \"explanation\": { \"detail\": \"...\", \"quote\": \"...\" }\n");
        prompt.append("  },\n");
        prompt.append("  \"reason\": \"Missing question 6\"\n");
        prompt.append("}\n");
        prompt.append("```\n\n");

        prompt.append("### APPEND - Extend transcript\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"operation\": \"append\",\n");
        prompt.append("  \"path\": \"transcript\",\n");
        prompt.append(
                "  \"newValue\": \"\\n\\nSPEAKER 1: Also...\",\n");
        prompt.append("  \"reason\": \"Low word count\"\n");
        prompt.append("}\n");
        prompt.append("```\n\n");

        prompt.append("## MANDATORY Fields\n");
        prompt.append("1. `operation`: replace, insert, or append\n");
        prompt.append(
                "2. `questionNumber`: The visual question number (1, 2, 14, etc.) - REQUIRED for question edits\n");
        prompt.append("3. `path`: The field name (e.g., `correct_answer`, `explanation.quote`) OR full path\n");
        prompt.append("4. `newValue`: The new value\n");
        prompt.append("5. `reason`: Brief explanation\n\n");

        prompt.append("## Rules\n");
        prompt.append("- NEVER modify passage_text - answers must match passage\n");
        prompt.append("- Output ONLY the JSON patches object, wrapped in {\"patches\": [...]}\n");

        return prompt.toString();
    }

    /**
     * Build the refinement prompt with specific issues to fix
     */
    public String buildRefinementPrompt(
            String originalJson,
            List<ValidationIssue> selectedIssues,
            String passageText) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("## Original Content\n");
        prompt.append("The following JSON was generated but has validation issues:\n\n");
        prompt.append("```json\n");
        prompt.append(originalJson);
        prompt.append("\n```\n\n");

        if (passageText != null && !passageText.isBlank()) {
            prompt.append("## Passage Context (for reference)\n");
            prompt.append("Use this passage to find correct answers:\n\n");
            prompt.append(passageText);
            prompt.append("\n\n");
        }

        prompt.append("## Issues to Fix\n");
        prompt.append("Fix ONLY the following issues:\n\n");

        for (int i = 0; i < selectedIssues.size(); i++) {
            ValidationIssue issue = selectedIssues.get(i);
            prompt.append(String.format("%d. **[%s]** %s\n",
                    i + 1,
                    issue.getType(),
                    issue.getMessage()));

            // Add specific fix instructions based on category
            appendFixInstructions(prompt, issue);
            prompt.append("\n");
        }

        prompt.append("\n## Instructions\n");
        prompt.append("Generate a JSON object with 'patches' array to fix each issue above.\n");
        prompt.append("For each issue, create a patch with:\n");
        prompt.append("- `issueId`: Use 'warning-N' where N is the issue number (1-based)\n");
        prompt.append("- `questionNumber`: The affected question number (e.g., 27)\n");
        prompt.append("- `path`: The specific field to change (e.g., 'correct_answer', 'explanation.quote')\n");
        prompt.append("- `oldValue`: The current incorrect value\n");
        prompt.append("- `newValue`: The corrected value\n");
        prompt.append("- `reason`: Brief explanation\n\n");
        prompt.append("Output ONLY the patches JSON. Do NOT output the full content.\n");

        return prompt.toString();
    }

    /**
     * Add specific fix instructions based on issue category
     */
    private void appendFixInstructions(StringBuilder prompt, ValidationIssue issue) {
        if (issue.getCategory() == null)
            return;

        switch (issue.getCategory()) {
            case "WORD_LIMIT":
                prompt.append("   → The current answer exceeds the word limit specified in the question\n");
                prompt.append("   → Find the EXACT word(s) from the passage, not a paraphrase\n");
                prompt.append("   → For 'ONE WORD ONLY': Use exactly 1 word from the passage\n");
                prompt.append("   → Hyphenated words count as 1 word (e.g., 'self-esteem')\n");
                prompt.append("   → Also update the 'quote' field in explanation to match the passage exactly\n");
                break;

            case "MISSING_PLACEHOLDER":
                prompt.append("   → Add `<strong>{N}</strong> ____` format where N is the question number\n");
                break;

            case "INCONSISTENT_OPTIONS":
                prompt.append("   → Ensure ALL questions in the group have the IDENTICAL options array\n");
                prompt.append("   → Copy the complete options array to each question\n");
                break;

            case "INVALID_WORD_LIMIT_FORMAT":
                prompt.append(
                        "   → Use exact format: \"ONE WORD ONLY\", \"NO MORE THAN TWO WORDS\", or \"NO MORE THAN THREE WORDS\"\n");
                prompt.append("   → Can add \"AND/OR A NUMBER\" suffix if needed\n");
                break;

            case "ANSWER_NOT_IN_PASSAGE":
                prompt.append("   → The answer does not appear in the passage text\n");
                prompt.append("   → Search the passage for the EXACT phrase that answers this question\n");
                prompt.append("   → Copy the word(s) exactly as written in the passage (same spelling, form)\n");
                prompt.append("   → Update both 'correct_answer' AND the 'quote' field in explanation\n");
                break;

            case "DIAGRAM_NO_LABELS":
                prompt.append("   → Add labeled nodes (type: 'step', 'start', 'end') between blank nodes\n");
                prompt.append("   → Diagram should mix labeled steps with blank input nodes\n");
                break;

            // Listening-specific categories
            case "TRANSCRIPT_ANSWER_MISMATCH":
                prompt.append(
                        "   → The answer must appear EXACTLY in the transcript (possibly with minor form changes)\n");
                prompt.append("   → Search the transcript for the closest matching phrase\n");
                prompt.append("   → If no exact match exists, adjust the question to match what's in the transcript\n");
                break;

            case "SPEAKER_LABEL_MISSING":
                prompt.append(
                        "   → Add speaker labels (e.g., 'Speaker 1:', 'Man:', 'Woman:', 'Tutor:') to dialogue sections\n");
                prompt.append("   → Each speaker turn should be clearly labeled\n");
                break;

            case "LISTENING_WORD_LIMIT":
                prompt.append("   → Find a shorter phrase from the transcript that means the same thing\n");
                prompt.append("   → Listening answers are typically 1-2 words, follow exact word limit\n");
                break;

            case "INVALID_SECTION_LAYOUT":
                prompt.append(
                        "   → The structure of the section_layout is invalid (missing blocks or question_numbers)\n");
                prompt.append("   → Ensure 'blocks' array exists and contains valid block objects\n");
                prompt.append("   → Add specific 'question_numbers' array to each block (e.g., [1, 2, 3, 4, 5])\n");
                prompt.append("   → Verify every question is assigned to exactly one block\n");
                break;

            case "JSON_VALIDATION_ERROR":
                prompt.append("   → The original output was not valid JSON\n");
                prompt.append("   → Fix syntax errors (missing commas, unclosed brackets/braces)\n");
                prompt.append("   → Ensure all keys are quoted and special characters are escaped\n");
                break;

            default:
                // No specific instructions for unknown categories
                break;
        }
    }

    /**
     * Build conversation messages for OpenRouter with context caching
     */
    public List<Object> buildConversationMessages(
            RefinementRequestDTO request,
            List<ValidationIssue> selectedIssues) {
        // This will be used by RefinementService to construct the API call
        // The original prompt is cached, and we add the refinement prompt
        return List.of(
                // System message
                new Message("system", buildSystemPrompt()),
                // Original prompt (cached)
                new Message("user", request.getOriginalPrompt()),
                // Original output from Agent 1
                new Message("assistant", request.getOriginalJson()),
                // Refinement request
                new Message("user", buildRefinementPrompt(
                        request.getOriginalJson(),
                        selectedIssues,
                        extractPassageFromPrompt(request.getOriginalPrompt()))));
    }

    /**
     * Extract passage/transcript text from the original prompt for context
     */
    private String extractPassageFromPrompt(String originalPrompt) {
        if (originalPrompt == null)
            return null;

        // Try to extract passage between markers (Reading)
        int passageStart = originalPrompt.indexOf("## Passage");
        int passageEnd = passageStart != -1 ? originalPrompt.indexOf("##", passageStart + 10) : -1;

        if (passageStart != -1 && passageEnd != -1) {
            return originalPrompt.substring(passageStart, passageEnd).trim();
        }

        // Try to extract transcript between markers (Listening)
        int transcriptStart = originalPrompt.indexOf("## Transcript");
        int transcriptEnd = transcriptStart != -1 ? originalPrompt.indexOf("##", transcriptStart + 12) : -1;

        if (transcriptStart != -1 && transcriptEnd != -1) {
            return originalPrompt.substring(transcriptStart, transcriptEnd).trim();
        }

        // Fallback: try to find any section with conversation or transcript content
        int contextStart = originalPrompt.indexOf("## Context");
        int contextEnd = contextStart != -1 ? originalPrompt.indexOf("##", contextStart + 10) : -1;

        if (contextStart != -1 && contextEnd != -1) {
            return originalPrompt.substring(contextStart, contextEnd).trim();
        }

        return null;
    }

    /**
     * Simple message wrapper for API calls
     */
    public record Message(String role, String content) {
    }
}
