package com.cramer.dto;

import java.time.LocalDate;

/**
 * Represents a high-level target or milestone the user is working toward.
 */
public class DashboardGoalDTO {

    private String label;
    private String value;
    private LocalDate targetDate;

    public DashboardGoalDTO() {
    }

    public DashboardGoalDTO(String label, String value, LocalDate targetDate) {
        this.label = label;
        this.value = value;
        this.targetDate = targetDate;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }
}
