package com.cramer.abts.generation.prompt;

import com.cramer.platform.common.json.Json;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * Prompt assembly for content refinement (SPEC-23 §5). Keeps the refinement prompt in the
 * prompt package like every generation prompt (SPEC-22 §1) instead of inline in a service.
 */
@Component
public class RefinementPromptBuilder {

    public String systemPrompt() {
        return "You are an IELTS content editor. Fix ONLY the selected issues with minimal, "
                + "targeted edits. Return strictly JSON: { \"patches\": [ { \"op\": \"replace|insert|append\", "
                + "\"questionNumber\": <n|null>, \"path\": \"/json/pointer\", \"value\": <new value> } ] }.";
    }

    public String userPrompt(java.util.List<String> issueIds, JsonNode validation, JsonNode originalJson) {
        return String.join("\n\n",
                "Selected issue ids to fix: " + String.join(", ", issueIds),
                "Current validation:\n" + (validation == null ? "{}" : Json.toJson(validation)),
                "Content to refine:\n" + Json.toJson(originalJson),
                "Each patch targets either a question (set questionNumber + a path relative to the question, "
                        + "e.g. \"/correct_answer\") or an absolute document path (questionNumber null).");
    }
}
