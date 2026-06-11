package com.cramer.speaking.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link SpeakingSessionStatus} as the <strong>lowercase</strong> string the DB expects
 * (SPEC-14 §2; verified live values {@code in_progress}, {@code abandoned}, …). A plain
 * {@code @Enumerated(STRING)} would write uppercase and violate the DB check constraint.
 */
@Converter
public class SpeakingSessionStatusConverter implements AttributeConverter<SpeakingSessionStatus, String> {

    @Override
    public String convertToDatabaseColumn(SpeakingSessionStatus status) {
        return status == null ? null : status.dbValue();
    }

    @Override
    public SpeakingSessionStatus convertToEntityAttribute(String dbValue) {
        return (dbValue == null || dbValue.isBlank()) ? null : SpeakingSessionStatus.from(dbValue);
    }
}
