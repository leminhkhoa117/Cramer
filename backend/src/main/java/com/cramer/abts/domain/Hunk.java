package com.cramer.abts.domain;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * An RFC-6902-style diff segment proposed during refinement (SPEC-23 §5). The author
 * accepts/rejects hunks; accepted hunks are applied to the original content.
 *
 * @param id          stable hunk id (e.g. {@code hunk-3})
 * @param op          {@code replace} / {@code add} / {@code remove}
 * @param path        JSON-pointer into the content
 * @param before      value before (null for add)
 * @param after       value after (null for remove)
 * @param description human-readable summary
 */
public record Hunk(
        String id,
        String op,
        String path,
        JsonNode before,
        JsonNode after,
        String description) {
}
