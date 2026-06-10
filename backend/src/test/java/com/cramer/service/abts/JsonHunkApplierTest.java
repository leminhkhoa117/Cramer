package com.cramer.service.abts;

import com.cramer.dto.abts.RefinementResponseDTO.RefinementHunk;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JsonHunkApplier}. Verifies the partial-apply semantics
 * (FIX 6): only accepted hunks are applied, application is deterministic and
 * idempotent, and unapplyable hunks are skipped with a captured reason.
 */
class JsonHunkApplierTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private JsonHunkApplier applier;

    @BeforeEach
    void setUp() {
        applier = new JsonHunkApplier();
    }

    private JsonNode json(String s) {
        try {
            return mapper.readTree(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private RefinementHunk hunk(String id, String op, String path, JsonNode before, JsonNode after) {
        return RefinementHunk.builder()
                .id(id)
                .op(op)
                .path(path)
                .before(before)
                .after(after)
                .build();
    }

    @Test
    void emptyAcceptedIds_returnsOriginal() {
        String original = "{\"a\":1,\"b\":{\"c\":\"x\"}}";
        RefinementHunk h = hunk("h1", "replace", "/a", json("1"), json("2"));

        JsonHunkApplier.ApplyResult result = applier.apply(original, List.of(h), List.of());

        assertThat(result.appliedCount).isZero();
        assertThat(result.rejectedCount).isEqualTo(1);
        assertThat(result.skippedHunks).isEmpty();
        assertThat(json(result.patchedJson)).isEqualTo(json(original));
    }

    @Test
    void replaceAtNestedPath_appliesCorrectly() {
        String original = "{\"a\":1,\"b\":{\"c\":\"x\"}}";
        RefinementHunk h = hunk("h1", "replace", "/b/c", json("\"x\""), json("\"y\""));

        JsonHunkApplier.ApplyResult result = applier.apply(original, List.of(h), List.of("h1"));

        assertThat(result.appliedCount).isEqualTo(1);
        assertThat(result.rejectedCount).isZero();
        assertThat(result.skippedHunks).isEmpty();
        assertThat(json(result.patchedJson).at("/b/c").asText()).isEqualTo("y");
    }

    @Test
    void addAtNewKey_appliesAndDeepCopiesAfter() {
        String original = "{\"a\":1}";
        ObjectNode after = (ObjectNode) json("{\"nested\":1}");
        RefinementHunk h = hunk("h1", "add", "/b", null, after);

        JsonHunkApplier.ApplyResult result = applier.apply(original, List.of(h), List.of("h1"));

        assertThat(result.appliedCount).isEqualTo(1);
        assertThat(json(result.patchedJson).at("/b/nested").asInt()).isEqualTo(1);

        // Mutating the source 'after' node after apply must NOT affect the patched
        // output -> proves the applier deep-copied the value.
        after.put("nested", 999);
        assertThat(json(result.patchedJson).at("/b/nested").asInt()).isEqualTo(1);
    }

    @Test
    void removeAtPath_dropsKey() {
        String original = "{\"a\":1,\"b\":2}";
        RefinementHunk h = hunk("h1", "remove", "/b", json("2"), null);

        JsonHunkApplier.ApplyResult result = applier.apply(original, List.of(h), List.of("h1"));

        assertThat(result.appliedCount).isEqualTo(1);
        JsonNode patched = json(result.patchedJson);
        assertThat(patched.has("b")).isFalse();
        assertThat(patched.at("/a").asInt()).isEqualTo(1);
    }

    @Test
    void addOnMissingParent_skippedWithReason() {
        String original = "{\"a\":1}";
        // Parent /missing does not exist -> cannot add /missing/child.
        RefinementHunk h = hunk("h1", "add", "/missing/child", null, json("\"v\""));

        JsonHunkApplier.ApplyResult result = applier.apply(original, List.of(h), List.of("h1"));

        assertThat(result.appliedCount).isZero();
        assertThat(result.skippedHunkIds).containsExactly("h1");
        assertThat(result.skippedHunks).hasSize(1);
        assertThat(result.skippedHunks.get(0).id).isEqualTo("h1");
        assertThat(result.skippedHunks.get(0).reason).isNotBlank();
        // Original is unchanged.
        assertThat(json(result.patchedJson)).isEqualTo(json(original));
    }

    @Test
    void parentReplaceFollowedByChildReplace_childWins() {
        String original = "{\"obj\":{\"c\":\"orig\",\"d\":1}}";
        // Whole-object replace at /obj, then a child replace at /obj/c. Sorting by
        // path ("/obj" < "/obj/c") guarantees the parent applies first, so the
        // child replace lands on the new object and wins.
        RefinementHunk parent = hunk("h2", "replace", "/obj",
                json("{\"c\":\"orig\",\"d\":1}"), json("{\"c\":\"parent\",\"d\":2}"));
        RefinementHunk child = hunk("h1", "replace", "/obj/c",
                json("\"parent\""), json("\"child\""));

        JsonHunkApplier.ApplyResult result = applier.apply(
                original, List.of(child, parent), List.of("h1", "h2"));

        assertThat(result.appliedCount).isEqualTo(2);
        assertThat(result.skippedHunks).isEmpty();
        JsonNode patched = json(result.patchedJson);
        assertThat(patched.at("/obj/c").asText()).isEqualTo("child");
        assertThat(patched.at("/obj/d").asInt()).isEqualTo(2);
    }

    @Test
    void multipleHunksDeterministicOrder_idempotent() {
        String original = "{\"a\":1,\"b\":2,\"c\":3}";
        RefinementHunk h1 = hunk("h1", "replace", "/a", json("1"), json("10"));
        RefinementHunk h2 = hunk("h2", "replace", "/b", json("2"), json("20"));
        RefinementHunk h3 = hunk("h3", "add", "/d", null, json("40"));
        List<RefinementHunk> hunks = List.of(h1, h2, h3);
        List<String> accepted = List.of("h1", "h2", "h3");

        JsonHunkApplier.ApplyResult first = applier.apply(original, hunks, accepted);
        JsonHunkApplier.ApplyResult second = applier.apply(original, hunks, accepted);

        assertThat(first.appliedCount).isEqualTo(3);
        // Same inputs -> byte-identical output (deterministic + idempotent).
        assertThat(second.patchedJson).isEqualTo(first.patchedJson);
        JsonNode patched = json(first.patchedJson);
        assertThat(patched.at("/a").asInt()).isEqualTo(10);
        assertThat(patched.at("/b").asInt()).isEqualTo(20);
        assertThat(patched.at("/c").asInt()).isEqualTo(3);
        assertThat(patched.at("/d").asInt()).isEqualTo(40);
    }
}
