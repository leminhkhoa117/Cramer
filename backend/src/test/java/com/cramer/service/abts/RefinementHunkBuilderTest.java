package com.cramer.service.abts;

import com.cramer.dto.abts.RefinementResponseDTO.RefinementHunk;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RefinementHunkBuilder}. Verifies the structural diff
 * produces the expected hunks for the common shapes the refinement emits.
 */
class RefinementHunkBuilderTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private RefinementHunkBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new RefinementHunkBuilder();
    }

    private JsonNode json(String s) {
        try {
            return mapper.readTree(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void identicalTrees_produceNoHunks() {
        JsonNode node = json("{\"a\":1,\"b\":{\"c\":\"x\"}}");
        List<RefinementHunk> hunks = builder.buildHunks(node, node.deepCopy(), Collections.emptyMap());
        assertThat(hunks).isEmpty();
    }

    @Test
    void primitiveChange_producesSingleReplaceHunk() {
        JsonNode before = json("{\"answer\":\"cat\"}");
        JsonNode after = json("{\"answer\":\"dog\"}");

        List<RefinementHunk> hunks = builder.buildHunks(before, after, Collections.emptyMap());

        assertThat(hunks).hasSize(1);
        RefinementHunk h = hunks.get(0);
        assertThat(h.getOp()).isEqualTo("replace");
        assertThat(h.getPath()).isEqualTo("/answer");
        assertThat(h.getBefore().asText()).isEqualTo("cat");
        assertThat(h.getAfter().asText()).isEqualTo("dog");
        assertThat(h.getId()).isNotBlank();
    }

    @Test
    void arrayLengthChange_producesSingleReplaceAtArrayRoot() {
        JsonNode before = json("{\"items\":[1,2,3]}");
        JsonNode after = json("{\"items\":[1,2]}");

        List<RefinementHunk> hunks = builder.buildHunks(before, after, Collections.emptyMap());

        assertThat(hunks).hasSize(1);
        assertThat(hunks.get(0).getPath()).isEqualTo("/items");
        assertThat(hunks.get(0).getOp()).isEqualTo("replace");
    }

    @Test
    void addedAndRemovedKeys_produceAddAndRemoveHunks() {
        JsonNode before = json("{\"keep\":1,\"gone\":2}");
        JsonNode after = json("{\"keep\":1,\"added\":3}");

        List<RefinementHunk> hunks = builder.buildHunks(before, after, Collections.emptyMap());

        assertThat(hunks).hasSize(2);
        assertThat(hunks).anySatisfy(h -> {
            assertThat(h.getOp()).isEqualTo("add");
            assertThat(h.getPath()).isEqualTo("/added");
        });
        assertThat(hunks).anySatisfy(h -> {
            assertThat(h.getOp()).isEqualTo("remove");
            assertThat(h.getPath()).isEqualTo("/gone");
        });
    }

    @Test
    void nestedChange_tagsIssueIdsFromMatchingPath() {
        JsonNode before = json("{\"section\":{\"questions\":[{\"answer\":\"a\"}]}}");
        JsonNode after = json("{\"section\":{\"questions\":[{\"answer\":\"b\"}]}}");
        Map<String, List<String>> pathToIssueIds = Map.of(
                "/section/questions/0/answer", List.of("issue-1"));

        List<RefinementHunk> hunks = builder.buildHunks(before, after, pathToIssueIds);

        assertThat(hunks).hasSize(1);
        RefinementHunk h = hunks.get(0);
        assertThat(h.getPath()).isEqualTo("/section/questions/0/answer");
        assertThat(h.getIssueIds()).contains("issue-1");
    }
}
