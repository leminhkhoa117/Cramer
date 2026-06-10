package com.cramer.service.abts;

import com.cramer.dto.abts.GenerationRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

final class JsonWritingValidator {

    private final ObjectMapper objectMapper;

    JsonWritingValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    JsonValidatorService.ValidationResult validateWritingContent(String jsonContent, GenerationRequestDTO request) {
        JsonValidatorService.ValidationResult result = new JsonValidatorService.ValidationResult();

        try {
            JsonNode root = objectMapper.readTree(jsonContent);

            if (!root.has("task_prompt")) {
                result.addSchemaError("Missing required field: task_prompt");
                return result;
            }

            if (!root.has("task_type")) {
                result.addWarning("Missing task_type field");
            }

            if (!root.has("word_requirement")) {
                result.addSchemaError("Missing required field: word_requirement");
                return result;
            }

            Integer part = request.getPartNumber() != null ? request.getPartNumber() : 1;
            String testType = request.getTestType() != null ? request.getTestType().name() : "ACADEMIC";

            if (part == 1) {
                if ("GENERAL_TRAINING".equals(testType)) {
                    validateLetterContent(root, result);
                } else {
                    if (!root.has("chart_data")) {
                        result.addSchemaError("Academic Task 1 requires chart_data");
                    } else if (root.has("chart_data")) {
                        validateChartData(root.get("chart_data"), result);
                        JsonNode chartData = root.get("chart_data");
                        if (chartData.has("chart_type")) {
                            String chartType = chartData.get("chart_type").asText();
                            if (("process".equals(chartType) || "map".equals(chartType))
                                    && !root.has("figure_description")) {
                                result.addContentError("Process/Map chart requires figure_description");
                            }
                        }
                    }
                }
            } else {
                validateEssayContent(root, request, result);
            }

            if (root.has("sample_answer")) {
                JsonNode sample = root.get("sample_answer");
                if (!sample.has("content") || sample.get("content").asText().isEmpty()) {
                    result.addWarning("sample_answer is present but has no content");
                } else {
                    int wordCount = JsonValidationSupport.countWords(sample.get("content").asText());
                    int minWords = (part == 1) ? 150 : 250;
                    if (wordCount < minWords) {
                        result.addWarning(String.format(
                                "sample_answer word count (%d) below minimum (%d)", wordCount, minWords));
                    }
                }
            }

        } catch (Exception e) {
            result.addSchemaError("Invalid JSON: " + e.getMessage());
        }

        return result;
    }

    private void validateChartData(JsonNode chartData, JsonValidatorService.ValidationResult result) {
        List<String> requiredFields = List.of("chart_type", "title");

        for (String field : requiredFields) {
            if (!chartData.has(field)) {
                result.addContentError("Chart data missing required field: " + field);
            }
        }

        String chartType = chartData.has("chart_type") ? chartData.get("chart_type").asText() : "";

        if (chartType.isEmpty()) {
            result.addContentError("chart_type is required");
            return;
        }

        if (chartType.contains("bar") || chartType.contains("line")) {
            validateAxisChartData(chartData, result);
        } else if (chartType.equals("pie_standard")) {
            validatePieChartData(chartData, result);
        } else if (chartType.equals("table")) {
            validateTableData(chartData, result);
        } else if (chartType.equals("process") || chartType.equals("map")) {
            if (!chartData.has("elements") && !chartData.has("figure_description")) {
                result.addWarning("Process/Map charts should have elements or figure_description");
            }
        }

        if (!chartData.has("source")) {
            result.addWarning("Chart data should include source attribution");
        }
    }

    private void validateAxisChartData(JsonNode chartData, JsonValidatorService.ValidationResult result) {
        if (!chartData.has("x_axis")) {
            result.addContentError("Bar/Line chart requires x_axis");
        } else {
            JsonNode xAxis = chartData.get("x_axis");
            if (!xAxis.has("values") || !xAxis.get("values").isArray()) {
                result.addContentError("x_axis must have values array");
            } else {
                int xValues = xAxis.get("values").size();
                if (xValues < 3) {
                    result.addWarning("x_axis should have at least 3 data points for meaningful comparison");
                }
            }
        }

        if (!chartData.has("y_axis")) {
            result.addContentError("Bar/Line chart requires y_axis");
        } else {
            JsonNode yAxis = chartData.get("y_axis");
            if (!yAxis.has("label")) {
                result.addWarning("y_axis should have a label");
            }
        }

        if (!chartData.has("series") || !chartData.get("series").isArray()) {
            result.addContentError("Chart requires series array with data");
        } else {
            JsonNode series = chartData.get("series");
            if (series.isEmpty()) {
                result.addContentError("series array is empty");
            } else {
                validateSeriesData(series, chartData, result);
            }
        }
    }

    private void validateSeriesData(JsonNode series, JsonNode chartData, JsonValidatorService.ValidationResult result) {
        int expectedLength = 0;
        if (chartData.has("x_axis") && chartData.get("x_axis").has("values")) {
            expectedLength = chartData.get("x_axis").get("values").size();
        }

        for (int i = 0; i < series.size(); i++) {
            JsonNode s = series.get(i);

            if (!s.has("name")) {
                result.addContentError("Series " + i + " missing name");
            }

            if (!s.has("values") || !s.get("values").isArray()) {
                result.addContentError("Series " + i + " missing values array");
            } else if (expectedLength > 0 && s.get("values").size() != expectedLength) {
                result.addContentError("Series " + i + " has " + s.get("values").size() +
                        " values but x_axis has " + expectedLength + " values");
            }

            if (!s.has("color")) {
                result.addWarning("Series " + i + " should have a color for visualization");
            }
        }
    }

    private void validatePieChartData(JsonNode chartData, JsonValidatorService.ValidationResult result) {
        if (!chartData.has("series") || !chartData.get("series").isArray()) {
            result.addContentError("Pie chart requires series array");
            return;
        }

        JsonNode series = chartData.get("series");
        if (series.isEmpty()) {
            result.addContentError("Pie chart series is empty");
            return;
        }

        double total = 0;
        for (JsonNode category : series) {
            if (category.has("values") && category.get("values").isArray() && !category.get("values").isEmpty()) {
                total += category.get("values").get(0).asDouble();
            } else if (category.has("value")) {
                total += category.get("value").asDouble();
            }
        }

        if (total > 0 && (total < 95 || total > 105)) {
            result.addWarning("Pie chart values sum to " + total + "%, expected approximately 100%");
        }
    }

    private void validateTableData(JsonNode chartData, JsonValidatorService.ValidationResult result) {
        if (!chartData.has("rows") && !chartData.has("data")) {
            result.addContentError("Table chart requires rows or data field");
        }

        if (!chartData.has("headers") && !chartData.has("columns")) {
            result.addWarning("Table should have headers or columns defined");
        }
    }

    private void validateEssayContent(JsonNode root, GenerationRequestDTO request,
            JsonValidatorService.ValidationResult result) {
        if (!root.has("task_prompt")) {
            result.addSchemaError("Missing task_prompt for essay question");
            return;
        }

        String taskPrompt = root.get("task_prompt").asText();

        if (taskPrompt.length() < 50) {
            result.addContentError("Essay prompt too short (minimum 50 characters)");
        }

        boolean hasInstruction = false;
        String[] instructionPatterns = {
                "agree or disagree",
                "discuss both views",
                "advantages and disadvantages",
                "problems and solutions",
                "what are the",
                "give your opinion",
                "to what extent"
        };

        String lowerPrompt = taskPrompt.toLowerCase();
        for (String pattern : instructionPatterns) {
            if (lowerPrompt.contains(pattern)) {
                hasInstruction = true;
                break;
            }
        }

        if (!hasInstruction) {
            result.addWarning("Essay prompt may be missing clear task instruction (agree/disagree, discuss, etc.)");
        }

        if (!lowerPrompt.contains("250") && !lowerPrompt.contains("word")) {
            result.addWarning("Essay prompt should mention minimum word count (250 words)");
        }

        if (!root.has("essay_metadata")) {
            result.addContentError("Task 2 requires essay_metadata");
            return;
        }

        JsonNode meta = root.get("essay_metadata");
        List<String> validEssayTypes = List.of(
                "opinion", "discussion", "advantages_disadvantages",
                "problem_solution", "two_part");

        if (meta.has("essay_type")) {
            String essayType = meta.get("essay_type").asText().toLowerCase();
            if (!validEssayTypes.contains(essayType)) {
                result.addWarning("Unknown essay_type: " + essayType);
            }
        }
    }

    private void validateLetterContent(JsonNode root, JsonValidatorService.ValidationResult result) {
        if (!root.has("letter_context")) {
            result.addContentError("GT Task 1 requires letter_context");
            return;
        }

        JsonNode ctx = root.get("letter_context");

        if (!ctx.has("recipient")) {
            result.addContentError("letter_context should specify recipient");
        }

        if (!ctx.has("relationship")) {
            result.addWarning("letter_context should specify relationship (formal/informal/semi-formal)");
        } else {
            String relationship = ctx.get("relationship").asText().toLowerCase();
            if (!relationship.equals("formal") && !relationship.equals("informal")
                    && !relationship.equals("semi-formal")) {
                result.addWarning("letter_context relationship should be formal, informal, or semi-formal");
            }
        }

        if (!ctx.has("purpose")) {
            result.addWarning("letter_context should specify purpose");
        }
    }
}