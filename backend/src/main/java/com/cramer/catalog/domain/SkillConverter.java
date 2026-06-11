package com.cramer.catalog.domain;

import com.cramer.platform.common.ielts.Skill;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps {@link Skill} to the <strong>lowercase</strong> string stored in {@code sections.skill}
 * (verified live DB values: reading/listening/writing/speaking — SPEC-11 §1.2). A plain
 * {@code @Enumerated(STRING)} would persist uppercase and corrupt existing rows, so this
 * converter is required.
 */
@Converter
public class SkillConverter implements AttributeConverter<Skill, String> {

    @Override
    public String convertToDatabaseColumn(Skill skill) {
        return skill == null ? null : skill.dbValue();
    }

    @Override
    public Skill convertToEntityAttribute(String dbValue) {
        return (dbValue == null || dbValue.isBlank()) ? null : Skill.from(dbValue);
    }
}
