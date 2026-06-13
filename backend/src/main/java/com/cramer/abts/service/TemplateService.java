package com.cramer.abts.service;

import com.cramer.platform.common.json.Json;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

/**
 * Curated generation templates (SPEC-25 §1 {@code /templates}). The schema is frozen and ABTS adds
 * no template tables, so categories/templates are served from a small curated, in-memory set the
 * admin studio can use as starting points.
 */
@Service
public class TemplateService {

    /** Template categories ({@code GET /templates}). */
    public ArrayNode categories() {
        ArrayNode arr = Json.mapper().createArrayNode();
        arr.add(category("reading", "Reading", "Academic reading passage templates"));
        arr.add(category("listening", "Listening", "Listening transcript + question templates"));
        arr.add(category("writing", "Writing", "Task 1 / Task 2 templates"));
        return arr;
    }

    /** Active templates for a category ({@code GET /templates/{categoryId}}). */
    public ArrayNode templates(String categoryId) {
        ArrayNode arr = Json.mapper().createArrayNode();
        switch (categoryId == null ? "" : categoryId.toLowerCase()) {
            case "reading" -> {
                arr.add(template("reading-academic-balanced", "Academic — balanced types", "reading",
                        "TRUE_FALSE_NOT_GIVEN, MATCHING_HEADINGS, FILL_IN_BLANK"));
                arr.add(template("reading-mcq-heavy", "MCQ focus", "reading",
                        "MULTIPLE_CHOICE, MULTIPLE_CHOICE_MULTIPLE_ANSWERS, MATCHING_INFORMATION"));
            }
            case "listening" -> {
                arr.add(template("listening-form-completion", "Form/Note completion", "listening",
                        "FILL_IN_BLANK"));
                arr.add(template("listening-map-matching", "Map + matching", "listening",
                        "MATCHING, MULTIPLE_CHOICE"));
            }
            case "writing" -> {
                arr.add(template("writing-academic-t1", "Academic Task 1 (chart)", "writing", "ACADEMIC_TASK_1"));
                arr.add(template("writing-t2-opinion", "Task 2 opinion essay", "writing", "TASK_2"));
            }
            default -> { /* unknown category → empty */ }
        }
        return arr;
    }

    private ObjectNode category(String id, String name, String description) {
        ObjectNode o = Json.mapper().createObjectNode();
        o.put("id", id);
        o.put("name", name);
        o.put("description", description);
        return o;
    }

    private ObjectNode template(String id, String name, String skill, String questionTypes) {
        ObjectNode o = Json.mapper().createObjectNode();
        o.put("id", id);
        o.put("name", name);
        o.put("skill", skill);
        o.put("questionTypes", questionTypes);
        return o;
    }
}
