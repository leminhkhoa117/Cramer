package com.cramer.abts.generation;

import com.cramer.platform.common.json.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonPointerUtilTest {

    @Test
    void setsReplacesAndRemovesByPointer() {
        ObjectNode root = Json.mapper().createObjectNode();
        root.putObject("questions");
        ObjectNode questions = (ObjectNode) root.get("questions");
        questions.putArray("list").add(Json.mapper().createObjectNode().put("n", 1));

        JsonPointerUtil.set(root, "/title", Json.mapper().getNodeFactory().textNode("hello"));
        assertThat(root.path("title").asText()).isEqualTo("hello");

        JsonNode got = JsonPointerUtil.get(root, "/questions/list/0/n");
        assertThat(got.asInt()).isEqualTo(1);

        JsonPointerUtil.set(root, "/questions/list/0/n", Json.mapper().getNodeFactory().numberNode(42));
        assertThat(JsonPointerUtil.get(root, "/questions/list/0/n").asInt()).isEqualTo(42);

        JsonPointerUtil.remove(root, "/title");
        assertThat(root.has("title")).isFalse();
    }

    @Test
    void appendsToArrayWithDashToken() {
        ObjectNode root = Json.mapper().createObjectNode();
        root.putArray("items");
        JsonPointerUtil.set(root, "/items/-", Json.mapper().getNodeFactory().textNode("a"));
        JsonPointerUtil.set(root, "/items/-", Json.mapper().getNodeFactory().textNode("b"));
        assertThat(root.path("items")).hasSize(2);
        assertThat(root.path("items").get(1).asText()).isEqualTo("b");
    }

    @Test
    void rejectsOutOfRangeIndexAndRootMutation() {
        ObjectNode root = Json.mapper().createObjectNode();
        root.putArray("items");
        assertThatThrownBy(() -> JsonPointerUtil.remove(root, "/items/3"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JsonPointerUtil.set(root, "", Json.mapper().nullNode()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
