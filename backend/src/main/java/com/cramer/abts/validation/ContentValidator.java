package com.cramer.abts.validation;

import com.cramer.platform.common.ielts.Skill;
import com.cramer.platform.error.OperationNotAllowedException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

/**
 * Facade dispatching validation to the per-skill validator (SPEC-23 §1). Speaking is out of scope
 * for ABTS (SPEC-20 §1) and is rejected.
 */
@Service
public class ContentValidator {

    private final ReadingValidator reading;
    private final ListeningValidator listening;
    private final WritingValidator writing;

    public ContentValidator(ReadingValidator reading, ListeningValidator listening, WritingValidator writing) {
        this.reading = reading;
        this.listening = listening;
        this.writing = writing;
    }

    public ValidationResult validate(Skill skill, int part, String taskType, JsonNode content) {
        return switch (skill) {
            case READING -> reading.validate(content, part);
            case LISTENING -> listening.validate(content, part);
            case WRITING -> writing.validate(content, taskType);
            case SPEAKING -> throw new OperationNotAllowedException("Speaking generation is not supported by ABTS");
        };
    }
}
