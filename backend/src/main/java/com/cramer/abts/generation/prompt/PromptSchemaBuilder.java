package com.cramer.abts.generation.prompt;

import com.cramer.platform.common.json.Json;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

/**
 * Builds JSON schemas for OpenRouter {@code response_format: json_schema} (SPEC-22 §1, §4).
 * Schemas are <strong>strict-compatible</strong>: every object sets {@code additionalProperties:
 * false} and lists all properties in {@code required}; optional fields use nullable type arrays
 * ({@code ["string","null"]}); {@code correct_answer} is always an array of strings so the
 * single/multi answer split never needs a union type (set-graded downstream, SPEC-12).
 */
@Component
public class PromptSchemaBuilder {

    // ---- Reading ----

    public ObjectNode readingPassageSchema() {
        ObjectNode section = obj();
        section.set("properties", props("passage_text", str()));
        required(section, "passage_text");
        ObjectNode root = obj();
        ObjectNode p = props("section", section);
        root.set("properties", p);
        required(root, "section");
        return root;
    }

    public ObjectNode readingQuestionsSchema() {
        ObjectNode root = obj();
        root.set("properties", props("questions", arrayOf(questionSchema(true))));
        required(root, "questions");
        return root;
    }

    public ObjectNode readingFullSchema() {
        ObjectNode section = obj();
        section.set("properties", props("passage_text", str()));
        required(section, "passage_text");
        ObjectNode root = obj();
        ObjectNode p = Json.mapper().createObjectNode();
        p.set("section", section);
        p.set("questions", arrayOf(questionSchema(true)));
        root.set("properties", p);
        required(root, "section", "questions");
        return root;
    }

    // ---- Listening ----

    public ObjectNode listeningTranscriptSchema() {
        ObjectNode root = obj();
        ObjectNode p = Json.mapper().createObjectNode();
        p.set("transcript", str());
        p.set("audio_placeholder", str());
        root.set("properties", p);
        required(root, "transcript", "audio_placeholder");
        return root;
    }

    public ObjectNode listeningStemsSchema() {
        ObjectNode root = obj();
        ObjectNode p = Json.mapper().createObjectNode();
        p.set("questions", arrayOf(questionSchema(false)));
        p.set("section_layout", sectionLayoutSchema());
        root.set("properties", p);
        required(root, "questions", "section_layout");
        return root;
    }

    public ObjectNode listeningAnswersSchema() {
        ObjectNode answer = obj();
        ObjectNode ap = Json.mapper().createObjectNode();
        ap.set("question_number", intp());
        ap.set("correct_answer", arrStr());
        ap.set("explanation", explanationSchema());
        answer.set("properties", ap);
        required(answer, "question_number", "correct_answer", "explanation");
        ObjectNode root = obj();
        root.set("properties", props("answers", arrayOf(answer)));
        required(root, "answers");
        return root;
    }

    public ObjectNode listeningFullSchema() {
        ObjectNode root = obj();
        ObjectNode p = Json.mapper().createObjectNode();
        p.set("transcript", str());
        p.set("audio_placeholder", str());
        p.set("section_layout", sectionLayoutSchema());
        p.set("questions", arrayOf(questionSchema(true)));
        root.set("properties", p);
        required(root, "transcript", "audio_placeholder", "section_layout", "questions");
        return root;
    }

    // ---- Writing ----

    public ObjectNode writingTaskSchema() {
        ObjectNode root = obj();
        ObjectNode p = Json.mapper().createObjectNode();
        p.set("task_prompt", str());
        p.set("word_requirement", str());
        p.set("task_type", str());
        p.set("chart_data", nullableObject(chartDataProps()));
        p.set("letter_context", nullableStr());
        p.set("essay_metadata", nullableObject(essayMetaProps()));
        root.set("properties", p);
        required(root, "task_prompt", "word_requirement", "task_type", "chart_data", "letter_context", "essay_metadata");
        return root;
    }

    public ObjectNode writingSampleSchema() {
        ObjectNode root = obj();
        root.set("properties", props("sample_answer", str()));
        required(root, "sample_answer");
        return root;
    }

    public ObjectNode writingBandSchema() {
        ObjectNode root = obj();
        ObjectNode p = Json.mapper().createObjectNode();
        p.set("band_breakdown", str());
        p.set("key_phrases", arrStr());
        p.set("grading_notes", str());
        root.set("properties", p);
        required(root, "band_breakdown", "key_phrases", "grading_notes");
        return root;
    }

    public ObjectNode writingFullSchema() {
        ObjectNode root = obj();
        ObjectNode p = Json.mapper().createObjectNode();
        p.set("task_prompt", str());
        p.set("word_requirement", str());
        p.set("task_type", str());
        p.set("chart_data", nullableObject(chartDataProps()));
        p.set("letter_context", nullableStr());
        p.set("essay_metadata", nullableObject(essayMetaProps()));
        p.set("sample_answer", str());
        p.set("band_breakdown", str());
        p.set("key_phrases", arrStr());
        p.set("grading_notes", str());
        root.set("properties", p);
        required(root, "task_prompt", "word_requirement", "task_type", "chart_data", "letter_context",
                "essay_metadata", "sample_answer", "band_breakdown", "key_phrases", "grading_notes");
        return root;
    }

    // ---------------------------------------------------------------- shared pieces

    private ObjectNode questionSchema(boolean withAnswer) {
        ObjectNode content = obj();
        ObjectNode cp = Json.mapper().createObjectNode();
        cp.set("text", str());
        cp.set("options", nullableArrStr());
        content.set("properties", cp);
        required(content, "text", "options");

        ObjectNode q = obj();
        ObjectNode p = Json.mapper().createObjectNode();
        p.set("question_number", intp());
        p.set("question_type", str());
        p.set("question_content", content);
        p.set("word_limit", nullableStr());
        if (withAnswer) {
            p.set("correct_answer", arrStr());
            p.set("explanation", explanationSchema());
            q.set("properties", p);
            required(q, "question_number", "question_type", "question_content", "word_limit",
                    "correct_answer", "explanation");
        } else {
            q.set("properties", p);
            required(q, "question_number", "question_type", "question_content", "word_limit");
        }
        return q;
    }

    private ObjectNode explanationSchema() {
        ObjectNode e = obj();
        ObjectNode p = Json.mapper().createObjectNode();
        p.set("text", str());
        p.set("evidence", nullableStr());
        e.set("properties", p);
        required(e, "text", "evidence");
        return e;
    }

    private ObjectNode sectionLayoutSchema() {
        ObjectNode block = obj();
        ObjectNode bp = Json.mapper().createObjectNode();
        bp.set("type", str());
        bp.set("title", nullableStr());
        bp.set("instructions", nullableStr());
        bp.set("question_numbers", intArr());
        bp.set("image_url", nullableStr());
        bp.set("options", nullableArrStr());
        block.set("properties", bp);
        required(block, "type", "title", "instructions", "question_numbers", "image_url", "options");

        ObjectNode layout = obj();
        layout.set("properties", props("blocks", arrayOf(block)));
        required(layout, "blocks");
        return layout;
    }

    private ObjectNode chartDataProps() {
        ObjectNode p = Json.mapper().createObjectNode();
        p.set("chart_type", str());
        p.set("description", str());
        p.set("labels", arrStr());
        p.set("values", arrStr());
        return p;
    }

    private ObjectNode essayMetaProps() {
        ObjectNode p = Json.mapper().createObjectNode();
        p.set("prompt_type", str());
        p.set("key_points", arrStr());
        return p;
    }

    // ---------------------------------------------------------------- low-level helpers

    private ObjectNode obj() {
        ObjectNode o = Json.mapper().createObjectNode();
        o.put("type", "object");
        o.put("additionalProperties", false);
        return o;
    }

    private ObjectNode nullableObject(ObjectNode properties) {
        ObjectNode o = Json.mapper().createObjectNode();
        ArrayNode types = o.putArray("type");
        types.add("object");
        types.add("null");
        o.put("additionalProperties", false);
        o.set("properties", properties);
        ArrayNode req = o.putArray("required");
        properties.fieldNames().forEachRemaining(req::add);
        return o;
    }

    private ObjectNode props(String name, ObjectNode schema) {
        ObjectNode p = Json.mapper().createObjectNode();
        p.set(name, schema);
        return p;
    }

    private void required(ObjectNode schema, String... names) {
        ArrayNode req = schema.putArray("required");
        for (String n : names) {
            req.add(n);
        }
    }

    private ObjectNode str() {
        return Json.mapper().createObjectNode().put("type", "string");
    }

    private ObjectNode nullableStr() {
        ObjectNode o = Json.mapper().createObjectNode();
        ArrayNode t = o.putArray("type");
        t.add("string");
        t.add("null");
        return o;
    }

    private ObjectNode intp() {
        return Json.mapper().createObjectNode().put("type", "integer");
    }

    private ObjectNode arrStr() {
        ObjectNode a = Json.mapper().createObjectNode();
        a.put("type", "array");
        a.set("items", str());
        return a;
    }

    private ObjectNode nullableArrStr() {
        ObjectNode a = Json.mapper().createObjectNode();
        ArrayNode t = a.putArray("type");
        t.add("array");
        t.add("null");
        a.set("items", str());
        return a;
    }

    private ObjectNode intArr() {
        ObjectNode a = Json.mapper().createObjectNode();
        a.put("type", "array");
        a.set("items", intp());
        return a;
    }

    private ObjectNode arrayOf(ObjectNode itemSchema) {
        ObjectNode a = Json.mapper().createObjectNode();
        a.put("type", "array");
        a.set("items", itemSchema);
        return a;
    }
}
