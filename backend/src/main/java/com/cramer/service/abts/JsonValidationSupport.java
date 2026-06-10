package com.cramer.service.abts;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

final class JsonValidationSupport {

    private JsonValidationSupport() {
    }

    static String extractQuestionText(JsonNode qContent) {
        if (qContent == null || qContent.isMissingNode() || qContent.isNull()) {
            return "";
        }
        if (qContent.has("text")) {
            return qContent.get("text").asText("");
        }
        if (qContent.has("statement")) {
            return qContent.get("statement").asText("");
        }
        if (qContent.has("question")) {
            return qContent.get("question").asText("");
        }
        if (qContent.has("sentence")) {
            return qContent.get("sentence").asText("");
        }
        if (qContent.has("prompt")) {
            return qContent.get("prompt").asText("");
        }
        if (qContent.has("item")) {
            return qContent.get("item").asText("");
        }
        if (qContent.has("paragraph")) {
            return qContent.get("paragraph").asText("");
        }
        if (qContent.has("heading")) {
            return qContent.get("heading").asText("");
        }
        return "";
    }

    static String extractExplanationText(JsonNode explanation) {
        if (explanation == null || explanation.isMissingNode() || explanation.isNull()) {
            return "";
        }
        if (explanation.isTextual()) {
            return explanation.asText("");
        }
        if (explanation.isObject()) {
            StringBuilder sb = new StringBuilder();
            if (explanation.has("detail")) {
                sb.append(explanation.get("detail").asText(""));
            }
            if (explanation.has("quote")) {
                sb.append(" ").append(explanation.get("quote").asText(""));
            }
            if (explanation.has("strategy")) {
                sb.append(" ").append(explanation.get("strategy").asText(""));
            }
            return sb.toString().trim();
        }
        return "";
    }

    static List<String> extractAnswers(JsonNode question) {
        List<String> answers = new ArrayList<>();
        JsonNode correct = question.get("correct_answer");
        if (correct != null && correct.isArray()) {
            for (JsonNode ans : correct) {
                if (!ans.isNull()) {
                    answers.add(ans.asText());
                }
            }
        }
        return answers;
    }

    static String extractWordLimit(JsonNode question) {
        if (question.has("word_limit") && !question.get("word_limit").isNull()) {
            return question.get("word_limit").asText();
        }
        return null;
    }

    static boolean hasInlineNumber(String text) {
        if (text == null) {
            return false;
        }
        return text.matches(".*\\b\\d+\\b.*") ||
                text.matches("(?s).*<strong>\\s*\\d+\\s*</strong>.*");
    }

    static void validateAnswerWordLimit(int num, List<String> answers, String wordLimit,
            JsonValidatorService.ValidationResult result) {
        if (wordLimit == null || wordLimit.isBlank()) {
            return;
        }
        Integer maxWords = deriveMaxWords(wordLimit);
        if (maxWords == null) {
            return;
        }
        boolean allowNumber = wordLimit.toUpperCase().contains("NUMBER");

        for (String answer : answers) {
            if (answer == null || answer.isBlank()) {
                continue;
            }
            int tokenCount = countAnswerTokens(answer, allowNumber);
            if (tokenCount > maxWords) {
                result.addContentError(String.format(
                        "Question %d answer '%s' exceeds word_limit (%s)",
                        num, answer, wordLimit));
            }
        }
    }

    static void validateMultipleChoice(int num, JsonNode qContent, JsonNode question, int expectedOptions,
            JsonValidatorService.ValidationResult result) {
        JsonNode options = qContent != null ? qContent.get("options") : null;
        if (options == null || !options.isArray()) {
            result.addContentError(String.format(
                    "Question %d: MULTIPLE_CHOICE requires options array",
                    num));
            return;
        }
        if (options.size() != expectedOptions) {
            result.addWarning(String.format(
                    "Question %d: expected %d options, found %d",
                    num, expectedOptions, options.size()));
        }
        Set<String> optionLetters = extractOptionLetters(options);
        List<String> answers = extractAnswers(question);
        if (answers.size() != 1) {
            result.addContentError(String.format(
                    "Question %d: MULTIPLE_CHOICE should have exactly one correct answer",
                    num));
            return;
        }
        if (!optionLetters.contains(answers.get(0).toUpperCase())) {
            result.addContentError(String.format(
                    "Question %d: correct_answer '%s' not found in options",
                    num, answers.get(0)));
        }
    }

    static void validateMultipleChoiceMultiple(int num, JsonNode qContent, JsonNode question, int expectedOptions,
            JsonValidatorService.ValidationResult result) {
        JsonNode options = qContent != null ? qContent.get("options") : null;
        if (options == null || !options.isArray()) {
            result.addContentError(String.format(
                    "Question %d: MULTIPLE_CHOICE_MULTIPLE_ANSWERS requires options array",
                    num));
            return;
        }
        if (options.size() < 4) {
            result.addWarning(String.format(
                    "Question %d: expected at least 4 options, found %d",
                    num, options.size()));
        }
        List<String> answers = extractAnswers(question);
        if (answers.size() != 2) {
            result.addContentError(String.format(
                    "Question %d: MULTIPLE_CHOICE_MULTIPLE_ANSWERS should have TWO correct answers",
                    num));
            return;
        }
        Set<String> optionLetters = extractOptionLetters(options);
        for (String ans : answers) {
            if (!optionLetters.contains(ans.toUpperCase())) {
                result.addContentError(String.format(
                        "Question %d: correct_answer '%s' not found in options",
                        num, ans));
            }
        }
    }

    static int countWords(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        String plainText = text.replaceAll("<[^>]+>", " ");
        plainText = plainText.replaceAll("\\s+", " ").trim();

        if (plainText.isEmpty()) {
            return 0;
        }

        return plainText.split("\\s+").length;
    }

    static boolean answerAppearsInPassage(String answer, String passageLower) {
        if (answer == null || answer.isBlank()) {
            return true;
        }

        String answerLower = answer.toLowerCase().trim();
        if (passageLower.contains(answerLower)) {
            return true;
        }

        String[] words = answerLower.split("\\s+");
        if (words.length > 1) {
            for (String word : words) {
                if (word.length() <= 2) {
                    continue;
                }
                if (!passageLower.contains(word)) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    static boolean containsVietnamese(String text) {
        if (text == null)
            return false;

        String vietnamese = "àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ";
        String upperVietnamese = vietnamese.toUpperCase();

        for (char c : text.toLowerCase().toCharArray()) {
            if (vietnamese.indexOf(c) >= 0 || upperVietnamese.indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }

    private static Integer deriveMaxWords(String wordLimit) {
        if (wordLimit == null) {
            return null;
        }
        String upper = wordLimit.toUpperCase();
        if (upper.contains("ONE WORD")) {
            return 1;
        }
        if (upper.contains("TWO WORDS")) {
            return 2;
        }
        if (upper.contains("THREE WORDS")) {
            return 3;
        }
        return null;
    }

    private static int countAnswerTokens(String answer, boolean allowNumber) {
        String normalized = answer == null ? "" : answer.trim();
        if (allowNumber) {
            for (int i = 0; i < 3; i++) {
                normalized = normalized.replaceAll("(\\d)\\s+(\\d)", "$1$2");
            }
        }
        normalized = normalized.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return 0;
        }
        return normalized.split("\\s+").length;
    }

    static Set<String> extractOptionLetters(JsonNode options) {
        Set<String> letters = new HashSet<>();
        if (options == null || !options.isArray()) {
            return letters;
        }
        for (JsonNode opt : options) {
            if (opt.isTextual()) {
                String text = opt.asText().trim();
                if (!text.isEmpty()) {
                    letters.add(text.substring(0, 1).toUpperCase());
                }
            } else if (opt.isObject()) {
                String letter = opt.path("letter").asText(null);
                if (letter != null && !letter.isBlank()) {
                    letters.add(letter.trim().toUpperCase());
                }
            }
        }
        return letters;
    }
}