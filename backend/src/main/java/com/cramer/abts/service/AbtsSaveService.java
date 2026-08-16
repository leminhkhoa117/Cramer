package com.cramer.abts.service;

import com.cramer.abts.web.dto.SaveContentRequest;
import com.cramer.abts.web.dto.SaveContentResponse;
import com.cramer.catalog.service.ContentDraftPort;
import com.cramer.platform.common.ielts.QuestionType;
import com.cramer.platform.common.ielts.Skill;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates a generated-content save request into the catalog's {@link ContentDraftPort} draft
 * contract (SPEC-24 §4). ABTS never touches catalog repositories; the port resolves/creates the
 * set + test and upserts sections/questions as <strong>draft</strong>.
 */
@Service
public class AbtsSaveService {

    private final ContentDraftPort draftPort;

    public AbtsSaveService(ContentDraftPort draftPort) {
        this.draftPort = draftPort;
    }

    public SaveContentResponse save(SaveContentRequest request) {
        List<ContentDraftPort.DraftSection> sections = new ArrayList<>();
        for (SaveContentRequest.SaveSectionInput in : request.safeSections()) {
            sections.add(toSection(in));
        }
        if (sections.isEmpty()) {
            throw new IllegalArgumentException("At least one section is required");
        }
        ContentDraftPort.SaveDraftCommand command = new ContentDraftPort.SaveDraftCommand(
                request.setCode(), request.setId(), request.testNumber(), request.testId(),
                request.generationMetadata(), sections);
        ContentDraftPort.SaveDraftResult result = draftPort.saveDraft(command);
        return new SaveContentResponse(true, result.setId(), result.setCode(), result.testId(),
                result.testNumber(), result.sectionIds(), result.questionCount(), "Saved as draft");
    }

    private ContentDraftPort.DraftSection toSection(SaveContentRequest.SaveSectionInput in) {
        Skill skill = parseSkill(in.skill());
        List<ContentDraftPort.DraftQuestion> questions = new ArrayList<>();
        if (in.questions() != null && in.questions().isArray()) {
            for (JsonNode q : in.questions()) {
                questions.add(toQuestion(q));
            }
        }
        if (skill != Skill.WRITING && questions.isEmpty()) {
            throw new IllegalArgumentException("Sections for " + skill.name().toLowerCase()
                    + " must contain at least one question");
        }
        return new ContentDraftPort.DraftSection(skill, in.partNumber(), in.passageText(), in.audioUrl(),
                in.sectionLayout(), in.imageDescription(), null, questions);
    }

    private ContentDraftPort.DraftQuestion toQuestion(JsonNode q) {
        int number = q.path("question_number").asInt(-1);
        if (number <= 0) {
            throw new IllegalArgumentException("Every question needs a positive question_number");
        }
        String type = q.path("question_type").asText("");
        try {
            QuestionType.from(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown question_type for question " + number + ": " + type);
        }
        return new ContentDraftPort.DraftQuestion(
                number,
                type,
                q.path("question_content"),
                q.path("correct_answer"),
                q.path("explanation"),
                textOrNull(q, "word_limit"),
                textOrNull(q, "image_url"));
    }

    private Skill parseSkill(String raw) {
        try {
            return Skill.valueOf(raw == null ? "" : raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown skill: " + raw);
        }
    }

    private String textOrNull(JsonNode parent, String field) {
        JsonNode n = parent.path(field);
        return (n.isMissingNode() || n.isNull() || n.asText().isBlank()) ? null : n.asText();
    }
}
