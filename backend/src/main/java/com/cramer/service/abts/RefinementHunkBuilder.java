package com.cramer.service.abts;

import com.cramer.dto.abts.RefinementResponseDTO.RefinementHunk;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * RefinementHunkBuilder - produces structured, independently-applyable diff
 * hunks between the original Agent-1 JSON and the refined Agent-2 JSON.
 *
 * <p>Hunks use RFC 6902 (JSON Patch) operation vocabulary ({@code add},
 * {@code remove}, {@code replace}) and RFC 6901 (JSON Pointer) paths. Each hunk
 * is deterministic and carries a stable id so the frontend Issue Rail can offer
 * per-hunk accept/reject and the partial-apply endpoint can reconcile.</p>
 *
 * <p>Diff algorithm (recursive parallel walk):</p>
 * <ul>
 *   <li>Objects: walk the union of field names; missing-in-refined =&gt; remove,
 *       new-in-refined =&gt; add, present-in-both =&gt; recurse.</li>
 *   <li>Arrays of equal length: align by index and recurse.</li>
 *   <li>Arrays of differing length: emit a single {@code replace} at the array
 *       path (whole-array swap).</li>
 *   <li>Primitives / type mismatches: emit a single {@code replace} when not
 *       equal.</li>
 * </ul>
 *
 * @since 2026 - ABTS refinement loop (PART D)
 */
@Service
public class RefinementHunkBuilder {

    private static final Logger logger = LoggerFactory.getLogger(RefinementHunkBuilder.class);
    private static final int SUMMARY_MAX = 120;

    /**
     * Build the ordered list of diff hunks between {@code original} and
     * {@code refined}.
     *
     * @param original       the JSON produced by Agent 1 (may be null)
     * @param refined        the JSON produced by Agent 2 (may be null)
     * @param pathToIssueIds optional map of JSON Pointer path -&gt; validation
     *                       issue ids, used to tag each hunk. May be null/empty.
     * @return ordered, deterministic list of hunks (empty when there is no diff)
     */
    public List<RefinementHunk> buildHunks(JsonNode original, JsonNode refined,
            Map<String, List<String>> pathToIssueIds) {
        List<RefinementHunk> hunks = new ArrayList<>();
        if (original == null && refined == null) {
            return hunks;
        }
        try {
            diff("", original, refined, pathToIssueIds, hunks);
        } catch (Exception e) {
            logger.warn("Hunk build failed, returning {} partial hunks: {}", hunks.size(), e.getMessage());
        }
        return hunks;
    }

    private void diff(String path, JsonNode before, JsonNode after,
            Map<String, List<String>> pathToIssueIds, List<RefinementHunk> out) {

        boolean beforeMissing = before == null || before.isMissingNode();
        boolean afterMissing = after == null || after.isMissingNode();

        if (beforeMissing && afterMissing) {
            return;
        }
        if (beforeMissing) {
            out.add(hunk("add", path, null, after, pathToIssueIds));
            return;
        }
        if (afterMissing) {
            out.add(hunk("remove", path, before, null, pathToIssueIds));
            return;
        }
        if (before.equals(after)) {
            return;
        }

        // Both present and different.
        if (before.isObject() && after.isObject()) {
            Set<String> keys = new TreeSet<>();
            before.fieldNames().forEachRemaining(keys::add);
            after.fieldNames().forEachRemaining(keys::add);
            for (String key : keys) {
                String childPath = path + "/" + escape(key);
                diff(childPath, before.get(key), after.get(key), pathToIssueIds, out);
            }
            return;
        }

        if (before.isArray() && after.isArray()) {
            if (before.size() == after.size()) {
                for (int i = 0; i < before.size(); i++) {
                    diff(path + "/" + i, before.get(i), after.get(i), pathToIssueIds, out);
                }
            } else {
                // Length change: whole-array replace at the array path.
                out.add(hunk("replace", path, before, after, pathToIssueIds));
            }
            return;
        }

        // Primitive change or structural type mismatch.
        out.add(hunk("replace", path, before, after, pathToIssueIds));
    }

    private RefinementHunk hunk(String op, String path, JsonNode before, JsonNode after,
            Map<String, List<String>> pathToIssueIds) {
        String id = stableId(path, before, after);
        List<String> issueIds = resolveIssueIds(path, pathToIssueIds);
        return RefinementHunk.builder()
                .id(id)
                .op(op)
                .path(path)
                .before(before)
                .after(after)
                .issueIds(issueIds)
                // FIX 8: hunks that address a known validation issue are flagged
                // "warning" (actionable), purely-cosmetic diffs default to "info".
                .severity(issueIds.isEmpty() ? "info" : "warning")
                .summary(summarize(op, path, before, after))
                .build();
    }

    /**
     * Stable id: {@code "hunk_" + sha1(path + "|" + before + "|" + after)} (first
     * 12 hex chars). Falls back to a hashCode-based id if SHA-1 is unavailable.
     */
    private String stableId(String path, JsonNode before, JsonNode after) {
        String material = path + "|" + nodeString(before) + "|" + nodeString(after);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(material.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
                if (hex.length() >= 12) {
                    break;
                }
            }
            return "hunk_" + hex.substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            return "hunk_" + String.format("%012x", (material.hashCode() & 0xFFFFFFFFL));
        }
    }

    /**
     * Collect issue ids whose registered path is an ancestor, equal to, or a
     * descendant of this hunk's path. Order-preserving and de-duplicated.
     */
    private List<String> resolveIssueIds(String path, Map<String, List<String>> pathToIssueIds) {
        if (pathToIssueIds == null || pathToIssueIds.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> matched = new LinkedHashSet<>();
        for (Map.Entry<String, List<String>> e : pathToIssueIds.entrySet()) {
            String key = e.getKey();
            if (key == null || e.getValue() == null) {
                continue;
            }
            boolean related = path.equals(key)
                    || path.startsWith(key + "/")
                    || key.startsWith(path + "/");
            if (related) {
                matched.addAll(e.getValue());
            }
        }
        return new ArrayList<>(matched);
    }

    private String summarize(String op, String path, JsonNode before, JsonNode after) {
        String text;
        boolean beforeContainer = before != null && before.isContainerNode();
        boolean afterContainer = after != null && after.isContainerNode();
        if (beforeContainer || afterContainer) {
            JsonNode container = "remove".equals(op) ? before : after;
            int n = container != null ? container.size() : 0;
            text = path + ": " + op + " (" + n + " " + (n == 1 ? "key" : "keys") + ")";
        } else {
            text = path + ": " + render(before) + " \u2192 " + render(after);
        }
        if (text.length() > SUMMARY_MAX) {
            text = text.substring(0, SUMMARY_MAX - 1) + "\u2026";
        }
        return text;
    }

    private String render(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return "null";
        }
        if (node.isValueNode()) {
            return node.asText();
        }
        return node.toString();
    }

    private String nodeString(JsonNode node) {
        return (node == null || node.isMissingNode()) ? "null" : node.toString();
    }

    /** RFC 6901 JSON Pointer token escaping: ~ -&gt; ~0, / -&gt; ~1. */
    private String escape(String token) {
        return token.replace("~", "~0").replace("/", "~1");
    }
}
