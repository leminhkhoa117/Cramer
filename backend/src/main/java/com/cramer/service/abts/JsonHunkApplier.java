package com.cramer.service.abts;

import com.cramer.dto.abts.RefinementResponseDTO.RefinementHunk;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JsonHunkApplier - applies a user-accepted subset of {@link RefinementHunk}s to
 * the original JSON, producing the partially-refined document.
 *
 * <p>Hunks use RFC 6902 op vocabulary ({@code add}/{@code remove}/{@code replace})
 * and RFC 6901 JSON Pointer paths. Application is deterministic: accepted hunks
 * are sorted by path then id before being applied to a deep copy of the original
 * tree, so the same inputs always yield the same output.</p>
 *
 * <p>The {@link RefinementHunkBuilder} only ever emits add/remove on object keys
 * (never on array elements) plus whole-array and primitive replaces, so applying
 * a subset never causes array-index drift between hunks.</p>
 *
 * @since 2026 - ABTS refinement loop (PART D)
 */
@Service
public class JsonHunkApplier {

    private static final Logger logger = LoggerFactory.getLogger(JsonHunkApplier.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Outcome of a partial apply. */
    public static class ApplyResult {
        public String patchedJson;
        public int appliedCount;
        public int rejectedCount;
        public final List<String> skippedHunkIds = new ArrayList<>();
        // FIX 9: per-hunk skip reason so the UI can explain WHY a hunk was
        // dropped (parent missing, bad op, etc.) instead of silently losing it.
        public final List<SkippedHunk> skippedHunks = new ArrayList<>();
    }

    /** FIX 9: a skipped hunk paired with the reason it could not be applied. */
    public static class SkippedHunk {
        public final String id;
        public final String reason;

        public SkippedHunk(String id, String reason) {
            this.id = id;
            this.reason = reason;
        }
    }

    /**
     * Apply the accepted hunks to {@code originalJson}.
     *
     * @param originalJson    the original (pre-refinement) JSON string
     * @param hunks           all hunks from the refinement
     * @param acceptedHunkIds ids the user accepted; null/empty means apply none
     * @return an {@link ApplyResult} with the patched JSON and counts
     * @throws IllegalArgumentException if originalJson is null/blank or unparseable
     */
    public ApplyResult apply(String originalJson, List<RefinementHunk> hunks,
            List<String> acceptedHunkIds) {
        if (originalJson == null || originalJson.isBlank()) {
            throw new IllegalArgumentException("originalJson is required");
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(originalJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("originalJson is not valid JSON: " + e.getMessage());
        }

        ApplyResult result = new ApplyResult();
        List<RefinementHunk> all = hunks != null ? hunks : new ArrayList<>();
        Set<String> accepted = acceptedHunkIds != null ? new HashSet<>(acceptedHunkIds) : new HashSet<>();

        // Partition into accepted (to apply) and rejected.
        List<RefinementHunk> toApply = all.stream()
                .filter(h -> h != null && h.getId() != null && accepted.contains(h.getId()))
                .sorted(Comparator
                        .comparing((RefinementHunk h) -> h.getPath() == null ? "" : h.getPath())
                        .thenComparing(RefinementHunk::getId))
                .collect(Collectors.toList());
        result.rejectedCount = (int) all.stream()
                .filter(h -> h != null && (h.getId() == null || !accepted.contains(h.getId())))
                .count();

        for (RefinementHunk hunk : toApply) {
            try {
                JsonNode next = applyOne(root, hunk);
                if (next != null) {
                    root = next; // root-replace path
                }
                result.appliedCount++;
            } catch (Exception e) {
                logger.warn("Skipping hunk {} ({} {}): {}", hunk.getId(), hunk.getOp(), hunk.getPath(),
                        e.getMessage());
                result.skippedHunkIds.add(hunk.getId());
                result.skippedHunks.add(new SkippedHunk(hunk.getId(),
                        e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())); // FIX 9
            }
        }

        try {
            result.patchedJson = objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize patched JSON: " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * Apply a single hunk in place. Returns a non-null node only when the root
     * itself is replaced (path == "" with a replace op); otherwise mutates
     * {@code root} and returns null.
     */
    private JsonNode applyOne(JsonNode root, RefinementHunk hunk) {
        String path = hunk.getPath() == null ? "" : hunk.getPath();
        String op = hunk.getOp() == null ? "" : hunk.getOp();

        // Whole-document replace.
        if (path.isEmpty()) {
            if (!"replace".equals(op) && !"add".equals(op)) {
                throw new IllegalArgumentException("op '" + op + "' not valid at root");
            }
            if (hunk.getAfter() == null) {
                throw new IllegalArgumentException("missing 'after' for root replace");
            }
            return hunk.getAfter().deepCopy();
        }

        JsonPointer pointer = JsonPointer.compile(path);
        JsonPointer parentPointer = pointer.head();
        JsonNode parent = root.at(parentPointer);
        if (parent.isMissingNode()) {
            throw new IllegalArgumentException("parent path not found: " + parentPointer);
        }

        String property = pointer.last().getMatchingProperty();
        int index = pointer.last().getMatchingIndex();

        switch (op) {
            case "remove":
                removeAt(parent, property, index);
                break;
            case "add":
            case "replace":
                if (hunk.getAfter() == null) {
                    throw new IllegalArgumentException("missing 'after' for " + op);
                }
                setAt(parent, property, index, hunk.getAfter().deepCopy());
                break;
            default:
                throw new IllegalArgumentException("unsupported op: " + op);
        }
        return null;
    }

    private void setAt(JsonNode parent, String property, int index, JsonNode value) {
        if (parent.isObject()) {
            if (property == null) {
                throw new IllegalArgumentException("object parent requires a property token");
            }
            ((ObjectNode) parent).set(property, value);
        } else if (parent.isArray()) {
            ArrayNode arr = (ArrayNode) parent;
            if (index < 0) {
                arr.add(value); // "-" or non-index token => append
            } else if (index < arr.size()) {
                arr.set(index, value);
            } else {
                arr.add(value);
            }
        } else {
            throw new IllegalArgumentException("parent is not a container");
        }
    }

    private void removeAt(JsonNode parent, String property, int index) {
        if (parent.isObject()) {
            if (property == null) {
                throw new IllegalArgumentException("object parent requires a property token");
            }
            ((ObjectNode) parent).remove(property);
        } else if (parent.isArray()) {
            ArrayNode arr = (ArrayNode) parent;
            if (index < 0 || index >= arr.size()) {
                throw new IllegalArgumentException("array index out of range: " + index);
            }
            arr.remove(index);
        } else {
            throw new IllegalArgumentException("parent is not a container");
        }
    }
}
