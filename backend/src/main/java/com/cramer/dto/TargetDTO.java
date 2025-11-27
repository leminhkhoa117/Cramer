package com.cramer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

public class TargetDTO {
    private UUID id;
    private UUID userId;
    private String examName;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate examDate;
    private Double listening;
    private Double reading;
    private Double writing;
    private Double speaking;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getExamName() {
        return examName;
    }

    public void setExamName(String examName) {
        this.examName = examName;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public void setExamDate(LocalDate examDate) {
        this.examDate = examDate;
    }

    public Double getListening() {
        return listening;
    }

    public void setListening(Double listening) {
        this.listening = listening;
    }

    public Double getReading() {
        return reading;
    }

    public void setReading(Double reading) {
        this.reading = reading;
    }

    public Double getWriting() {
        return writing;
    }

    public void setWriting(Double writing) {
        this.writing = writing;
    }

    public Double getSpeaking() {
        return speaking;
    }

    public void setSpeaking(Double speaking) {
        this.speaking = speaking;
    }
}
