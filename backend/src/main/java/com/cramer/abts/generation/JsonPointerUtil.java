package com.cramer.abts.generation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Minimal RFC-6901 JSON-pointer mutation helper for refinement patch/hunk application
 * (SPEC-23 §5). {@code get} delegates to Jackson; {@code set}/{@code remove} navigate to the
 * parent node and mutate the final token. Throws {@link IllegalArgumentException} on an
 * unresolvable path so the caller can skip the hunk (SPEC-23 §5.2).
 */
public final class JsonPointerUtil {

    private JsonPointerUtil() {
    }

    public static JsonNode get(JsonNode root, String pointer) {
        return root.at(normalize(pointer));
    }

    /** Set {@code value} at {@code pointer}, creating object keys as needed (array indices must exist). */
    public static void set(JsonNode root, String pointer, JsonNode value) {
        String p = normalize(pointer);
        if (p.isEmpty()) {
            throw new IllegalArgumentException("Cannot replace the document root");
        }
        int slash = p.lastIndexOf('/');
        String parentPtr = p.substring(0, slash);
        String token = unescape(p.substring(slash + 1));
        JsonNode parent = parentPtr.isEmpty() ? root : root.at(parentPtr);
        if (parent.isObject()) {
            ((ObjectNode) parent).set(token, value);
        } else if (parent.isArray()) {
            ArrayNode arr = (ArrayNode) parent;
            if ("-".equals(token)) {
                arr.add(value);
            } else {
                int idx = parseIndex(token);
                if (idx < 0 || idx > arr.size()) {
                    throw new IllegalArgumentException("Array index out of range: " + pointer);
                }
                if (idx == arr.size()) {
                    arr.add(value);
                } else {
                    arr.set(idx, value);
                }
            }
        } else {
            throw new IllegalArgumentException("Parent of " + pointer + " is not a container");
        }
    }

    public static void remove(JsonNode root, String pointer) {
        String p = normalize(pointer);
        if (p.isEmpty()) {
            throw new IllegalArgumentException("Cannot remove the document root");
        }
        int slash = p.lastIndexOf('/');
        String parentPtr = p.substring(0, slash);
        String token = unescape(p.substring(slash + 1));
        JsonNode parent = parentPtr.isEmpty() ? root : root.at(parentPtr);
        if (parent.isObject()) {
            ((ObjectNode) parent).remove(token);
        } else if (parent.isArray()) {
            int idx = parseIndex(token);
            ArrayNode arr = (ArrayNode) parent;
            if (idx < 0 || idx >= arr.size()) {
                throw new IllegalArgumentException("Array index out of range: " + pointer);
            }
            arr.remove(idx);
        } else {
            throw new IllegalArgumentException("Parent of " + pointer + " is not a container");
        }
    }

    private static String normalize(String pointer) {
        if (pointer == null || pointer.isBlank() || "/".equals(pointer)) {
            return "";
        }
        return pointer.startsWith("/") ? pointer : "/" + pointer;
    }

    private static String unescape(String token) {
        return token.replace("~1", "/").replace("~0", "~");
    }

    private static int parseIndex(String token) {
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid array index token: " + token);
        }
    }
}
