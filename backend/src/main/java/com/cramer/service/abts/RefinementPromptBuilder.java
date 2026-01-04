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
     */
    public String buildSystemPrompt() {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a JSON Refinement Specialist for IELTS test content.\n\n");
        prompt.append("Your role is to FIX SPECIFIC ISSUES in AI-generated IELTS test questions.\n\n");

        prompt.append("## Rules\n");
        prompt.append("1. **FIX ONLY the specified issues** - do not modify anything else\n");
        prompt.append("2. **Preserve all formatting** - maintain the exact JSON structure\n");
        prompt.append("3. **Use passage context** - answers must come from the original passage\n");
        prompt.append("4. **Follow IELTS standards** - word limits, question formats, etc.\n\n");

        prompt.append("## Output Format\n");
        prompt.append("Return ONLY the complete fixed JSON. Before the JSON, include a brief summary:\n");
        prompt.append("```\n");
        prompt.append("FIXES APPLIED:\n");
        prompt.append("- Q2: Changed answer from \"Urban Heat Island\" to \"heat island\" (word limit)\n");
        prompt.append("- Q8: Added <strong>8</strong> ____ placeholder\n");
        prompt.append("\n");
        prompt.append("```json\n");
        prompt.append("{ ... fixed JSON ... }\n");
        prompt.append("```\n");

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
        prompt.append("1. Fix EACH issue listed above\n");
        prompt.append("2. Do NOT change anything else in the JSON\n");
        prompt.append("3. Return the COMPLETE fixed JSON (not just the changed parts)\n");
        prompt.append("4. Include the FIXES APPLIED summary before the JSON\n");

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
                prompt.append("   → Find a shorter phrase from the passage with the same meaning\n");
                prompt.append("   → Valid limits: ONE WORD ONLY, NO MORE THAN TWO WORDS, NO MORE THAN THREE WORDS\n");
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
                prompt.append("   → Find the exact phrase in the passage that answers this question\n");
                break;

            case "DIAGRAM_NO_LABELS":
                prompt.append("   → Add labeled nodes (type: 'step', 'start', 'end') between blank nodes\n");
                prompt.append("   → Diagram should mix labeled steps with blank input nodes\n");
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
     * Extract passage text from the original prompt for context
     */
    private String extractPassageFromPrompt(String originalPrompt) {
        if (originalPrompt == null)
            return null;

        // Try to extract passage between markers
        int passageStart = originalPrompt.indexOf("## Passage");
        int passageEnd = originalPrompt.indexOf("##", passageStart + 10);

        if (passageStart != -1 && passageEnd != -1) {
            return originalPrompt.substring(passageStart, passageEnd).trim();
        }

        return null;
    }

    /**
     * Simple message wrapper for API calls
     */
    public record Message(String role, String content) {
    }
}
