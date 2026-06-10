package com.cramer.service.abts.prompt;

public final class PromptFragments {

    private PromptFragments() {
    }

    public static String structuredExplanationFormat() {
        return "### ⚠️ EXPLANATION FORMAT (CRITICAL - MUST FOLLOW EXACTLY)\n"
                + "Each question's `explanation` field must be a JSON object with this EXACT structure:\n"
                + "```json\n"
                + "{\n"
                + "  \"detail\": \"<detailed explanation in Vietnamese why this is correct>\",\n"
                + "  \"quote\": \"<EXACT quote from the passage in English that proves the answer>\",\n"
                + "  \"strategy\": \"<strategy tip in Vietnamese for this question type>\"\n"
                + "}\n"
                + "```\n\n"
                + "**Field Requirements:**\n"
                + "- `detail`: 2-4 sentences explaining the reasoning (in Vietnamese).\n"
                + "- `quote`: Direct quote from passage with quotation marks. Keep in English.\n"
                + "- `strategy`: Brief strategy tip for similar questions (in Vietnamese).\n\n";
    }

    public static String wordLimitFormat() {
        return "### ⚠️ WORD LIMIT FORMAT (CRITICAL - MUST USE EXACT VALUES)\n"
                + "For completion-type questions, `word_limit` MUST be one of these EXACT strings:\n"
                + "✅ VALID:\n"
                + "- `\"ONE WORD ONLY\"`\n"
                + "- `\"NO MORE THAN TWO WORDS\"`\n"
                + "- `\"NO MORE THAN THREE WORDS\"`\n"
                + "- `\"ONE WORD AND/OR A NUMBER\"`\n"
                + "- `\"NO MORE THAN TWO WORDS AND/OR A NUMBER\"`\n"
                + "- `\"NO MORE THAN THREE WORDS AND/OR A NUMBER\"`\n\n"
                + "❌ INVALID (do NOT use these):\n"
                + "- `\"ONE WORD\"` ← WRONG, use `\"ONE WORD ONLY\"`\n"
                + "- `\"TWO WORDS\"` ← WRONG, use `\"NO MORE THAN TWO WORDS\"`\n"
                + "- `\"THREE WORDS\"` ← WRONG, use `\"NO MORE THAN THREE WORDS\"`\n"
                + "- `\"FOUR WORDS\"` or `\"NO MORE THAN FOUR WORDS\"` ← WRONG, maximum is THREE WORDS\n\n";
    }
}