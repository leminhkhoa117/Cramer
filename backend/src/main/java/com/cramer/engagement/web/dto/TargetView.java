package com.cramer.engagement.web.dto;

import com.cramer.engagement.domain.Target;

import java.time.LocalDate;

/** IELTS goal projection (SPEC-16 §5). */
public record TargetView(
        String examName,
        LocalDate examDate,
        Double listening,
        Double reading,
        Double writing,
        Double speaking) {

    public static TargetView of(Target t) {
        return new TargetView(t.getExamName(), t.getExamDate(), t.getListening(), t.getReading(),
                t.getWriting(), t.getSpeaking());
    }
}
