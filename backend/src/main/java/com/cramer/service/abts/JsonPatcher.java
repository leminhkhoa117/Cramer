package com.cramer.service.abts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JsonPatcher - Comprehensive utility for applying JSON patches.
 * 
 * Supports operations:
 * - replace: Change existing value
 * - insert: Add element to array
 * - append: Add text to existing string field
 * 
 * @since 2026-01-06
 */
@Service
public class JsonPatcher {

    private static final Logger logger = LoggerFactory.getLogger(JsonPatcher.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern ARRAY_ACCESS = Pattern.compile("(\\w+)\\[(\\d+)\\]");

    /**
     * Patch operation types
     */
    public enum Operation {
        REPLACE, // Replace existing value
        INSERT, // Insert into array at index
        APPEND // Append to string field
    }

    /**
     * Patch data structure with operation type
     */
    public static class Patch {
        private String issueId;
        private Integer questionNumber;
        private Operation operation = Operation.REPLACE; // Default
        private String path;
        private Integer index; // For insert operations
        private Object oldValue;
        private Object newValue;
        private String reason;

        // Getters and setters
        public String getIssueId() {
            return issueId;
        }

        public void setIssueId(String issueId) {
            this.issueId = issueId;
        }

        public Integer getQuestionNumber() {
            return questionNumber;
        }

        public void setQuestionNumber(Integer questionNumber) {
            this.questionNumber = questionNumber;
        }

        public Operation getOperation() {
            return operation;
        }

        public void setOperation(Operation operation) {
            this.operation = operation;
        }

        public void setOperationFromString(String op) {
            if (op == null) {
                this.operation = Operation.REPLACE;
                return;
            }
            switch (op.toLowerCase()) {
                case "insert":
                    this.operation = Operation.INSERT;
                    break;
                case "append":
                    this.operation = Operation.APPEND;
                    break;
                default:
                    this.operation = Operation.REPLACE;
            }
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public Object getOldValue() {
            return oldValue;
        }

        public void setOldValue(Object oldValue) {
            this.oldValue = oldValue;
        }

        public Object getNewValue() {
            return newValue;
        }

        public void setNewValue(Object newValue) {
            this.newValue = newValue;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        @Override
        public String toString() {
            return String.format("Patch[%s Q%d: %s -> %s (%s)]",
                    operation, questionNumber, oldValue, newValue, reason);
        }
    }

    /**
     * Result of applying patches
     */
    public static class PatchResult {
        private String patchedJson;
        private int successCount;
        private int failCount;
        private List<String> errors;

        public String getPatchedJson() {
            return patchedJson;
        }

        public void setPatchedJson(String patchedJson) {
            this.patchedJson = patchedJson;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(int successCount) {
            this.successCount = successCount;
        }

        public int getFailCount() {
            return failCount;
        }

        public void setFailCount(int failCount) {
            this.failCount = failCount;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }
    }

    /**
     * Apply a list of patches to the original JSON content.
     */
    public PatchResult applyPatches(String originalJson, List<Patch> patches) {
        PatchResult result = new PatchResult();
        result.setErrors(new java.util.ArrayList<>());
        result.setSuccessCount(0);
        result.setFailCount(0);

        if (patches == null || patches.isEmpty()) {
            result.setPatchedJson(originalJson);
            return result;
        }

        try {
            JsonNode root = objectMapper.readTree(originalJson);

            for (Patch patch : patches) {
                try {
                    // Skip no-op patches
                    if (patch.getOperation() == Operation.REPLACE &&
                            patch.getOldValue() != null && patch.getNewValue() != null &&
                            patch.getOldValue().toString().equals(patch.getNewValue().toString())) {
                        logger.info("Skipping no-op patch: {}", patch);
                        result.setSuccessCount(result.getSuccessCount() + 1);
                        continue;
                    }

                    logger.info("Applying patch: {}", patch);
                    boolean applied = applySinglePatch(root, patch);

                    if (applied) {
                        result.setSuccessCount(result.getSuccessCount() + 1);
                        logger.info("Successfully applied: {}", patch);
                    } else {
                        result.setFailCount(result.getFailCount() + 1);
                        result.getErrors().add("Failed to apply: " + patch);
                        logger.warn("Failed to apply: {}", patch);
                    }
                } catch (Exception e) {
                    result.setFailCount(result.getFailCount() + 1);
                    result.getErrors().add("Error: " + patch.getPath() + " - " + e.getMessage());
                    logger.error("Error applying {}: {}", patch.getPath(), e.getMessage());
                }
            }

            result.setPatchedJson(objectMapper.writeValueAsString(root));

        } catch (Exception e) {
            logger.error("Failed to parse JSON: {}", e.getMessage());
            result.setFailCount(patches.size());
            result.getErrors().add("JSON parse error: " + e.getMessage());
            result.setPatchedJson(originalJson);
        }

        return result;
    }

    /**
     * Apply a single patch based on operation type.
     */
    private boolean applySinglePatch(JsonNode root, Patch patch) {
        if (patch.getPath() == null || patch.getPath().isBlank()) {
            logger.warn("Empty path");
            return false;
        }

        // Auto-derive path prefix from questionNumber if path doesn't have questions[N]
        String fixedPath = resolvePath(root, patch);
        if (!fixedPath.equals(patch.getPath())) {
            logger.info("Auto-fixed path: {} -> {}", patch.getPath(), fixedPath);
            patch.setPath(fixedPath);
        }

        switch (patch.getOperation()) {
            case INSERT:
                return applyInsert(root, patch);
            case APPEND:
                return applyAppend(root, patch);
            case REPLACE:
            default:
                return applyReplace(root, patch);
        }
    }

    /**
     * Smart path resolution:
     * 1. If path is already absolute (starts with "questions["), use it.
     * 2. If questionNumber is provided, find the index of that question in the
     * array.
     * 3. Construct "questions[INDEX].path".
     */
    private String resolvePath(JsonNode root, Patch patch) {
        String path = patch.getPath();
        Integer qNum = patch.getQuestionNumber();

        if (qNum == null) {
            return path;
        }

        // If path looks absolute, trust it (e.g. "questions[4].text")
        if (path.startsWith("questions[")) {
            return path;
        }

        // Otherwise, we need to find the index of this question number
        JsonNode questions = root.get("questions");
        if (questions == null || !questions.isArray()) {
            return path; // Can't resolve without questions array
        }

        // Find index where question_number == qNum
        int foundIndex = -1;
        for (int i = 0; i < questions.size(); i++) {
            JsonNode q = questions.get(i);
            if (q.has("question_number") && q.get("question_number").asInt() == qNum) {
                foundIndex = i;
                break;
            }
        }

        if (foundIndex != -1) {
            // Found it! Map "correct_answer" -> "questions[5].correct_answer"
            return "questions[" + foundIndex + "]." + path;
        }

        // If not found, fallback to simpler heuristic (assuming 1-based index match?
        // No, unsafe)
        // Just return original path and let it fail naturally if invalid
        logger.warn("Could not find question number {} in questions array", qNum);
        return path;
    }

    /**
     * REPLACE: Navigate to field and replace value.
     */
    private boolean applyReplace(JsonNode root, Patch patch) {
        String[] segments = patch.getPath().split("\\.");
        JsonNode current = root;

        // Navigate to parent
        for (int i = 0; i < segments.length - 1; i++) {
            current = navigateSegment(current, segments[i]);
            if (current == null) {
                logger.warn("Navigate failed at: {}", segments[i]);
                return false;
            }
        }

        // Apply to final segment
        String finalSegment = segments[segments.length - 1];
        return applyValueToNode(current, finalSegment, patch.getNewValue());
    }

    /**
     * INSERT: Insert element into array at specified index.
     */
    private boolean applyInsert(JsonNode root, Patch patch) {
        // Path should point to the array (e.g., "questions")
        JsonNode arrayNode = navigateToNode(root, patch.getPath());

        if (arrayNode == null || !arrayNode.isArray()) {
            logger.warn("INSERT target is not an array: {}", patch.getPath());
            return false;
        }

        ArrayNode arr = (ArrayNode) arrayNode;
        int index = patch.getIndex() != null ? patch.getIndex() : arr.size();

        // Clamp index
        if (index < 0)
            index = 0;
        if (index > arr.size())
            index = arr.size();

        JsonNode newElement = objectMapper.valueToTree(patch.getNewValue());
        arr.insert(index, newElement);

        logger.info("Inserted at {}[{}]", patch.getPath(), index);
        return true;
    }

    /**
     * APPEND: Append text to existing string field.
     */
    private boolean applyAppend(JsonNode root, Patch patch) {
        String[] segments = patch.getPath().split("\\.");
        JsonNode current = root;

        // Navigate to parent
        for (int i = 0; i < segments.length - 1; i++) {
            current = navigateSegment(current, segments[i]);
            if (current == null) {
                logger.warn("Navigate failed at: {}", segments[i]);
                return false;
            }
        }

        if (!(current instanceof ObjectNode)) {
            logger.warn("Parent is not ObjectNode for append");
            return false;
        }

        ObjectNode parent = (ObjectNode) current;
        String fieldName = segments[segments.length - 1];
        JsonNode existingNode = parent.get(fieldName);

        if (existingNode == null || !existingNode.isTextual()) {
            logger.warn("Cannot append to non-text field: {}", fieldName);
            return false;
        }

        String existingText = existingNode.asText();
        String appendText = patch.getNewValue() != null ? patch.getNewValue().toString() : "";
        parent.put(fieldName, existingText + appendText);

        logger.info("Appended {} chars to {}", appendText.length(), patch.getPath());
        return true;
    }

    /**
     * Navigate through a path segment.
     */
    private JsonNode navigateSegment(JsonNode node, String segment) {
        if (node == null)
            return null;

        Matcher m = ARRAY_ACCESS.matcher(segment);
        if (m.matches()) {
            String arrayName = m.group(1);
            int index = Integer.parseInt(m.group(2));
            JsonNode arrayNode = node.get(arrayName);
            if (arrayNode != null && arrayNode.isArray() && index < arrayNode.size()) {
                return arrayNode.get(index);
            }
            return null;
        }
        return node.get(segment);
    }

    /**
     * Navigate to a node by full path.
     */
    private JsonNode navigateToNode(JsonNode root, String path) {
        String[] segments = path.split("\\.");
        JsonNode current = root;
        for (String segment : segments) {
            current = navigateSegment(current, segment);
            if (current == null)
                return null;
        }
        return current;
    }

    /**
     * Apply value to a node (handles both simple fields and array elements).
     */
    private boolean applyValueToNode(JsonNode parent, String segment, Object newValue) {
        if (parent == null)
            return false;

        Matcher m = ARRAY_ACCESS.matcher(segment);
        if (m.matches()) {
            // Array element: correct_answer[0]
            String arrayName = m.group(1);
            int index = Integer.parseInt(m.group(2));
            JsonNode arrayNode = parent.get(arrayName);

            ArrayNode arr;

            if (arrayNode == null) {
                // Case 1: Array doesn't exist -> Create it
                arr = objectMapper.createArrayNode();
                if (parent instanceof ObjectNode) {
                    ((ObjectNode) parent).set(arrayName, arr);
                } else {
                    return false; // Cannot set field on non-object
                }
            } else if (arrayNode.isArray()) {
                // Case 2: It is already an array -> Use it
                arr = (ArrayNode) arrayNode;
            } else {
                // Case 3: It exists but is NOT an array (e.g. String "A") -> Promote to Array
                // ["A"]
                // "Anti-Fragile" logic: conform data to the patch's intent
                arr = objectMapper.createArrayNode();
                arr.add(arrayNode); // Add existing value at index 0
                if (parent instanceof ObjectNode) {
                    ((ObjectNode) parent).set(arrayName, arr);
                    logger.info("Promoted field '{}' from {} to Array to apply patch", arrayName,
                            arrayNode.getNodeType());
                } else {
                    return false;
                }
            }

            // Allow expanding array if index >= size (Jackson supports this)
            if (index >= 0) {
                // Fill gaps with nulls if expanding beyond immediate next index
                // (Jackson's set logic handles this, but explicit check ensures safety)
                while (arr.size() <= index) {
                    arr.addNull();
                }
                arr.set(index, objectMapper.valueToTree(newValue));
                return true;
            }
            return false;
        }

        // Simple field
        if (parent instanceof ObjectNode) {
            ObjectNode parentObj = (ObjectNode) parent;
            parentObj.set(segment, objectMapper.valueToTree(newValue));
            return true;
        }
        return false;
    }
}
