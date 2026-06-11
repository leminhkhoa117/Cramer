package com.cramer.abts.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated validation outcome (SPEC-23 §1.1). {@code valid} is true when there are no ERROR
 * issues; warnings never block. Issues are structured for UI targeting.
 */
public final class ValidationResult {

    private final List<ValidationIssue> issues = new ArrayList<>();

    public ValidationResult addError(String id, String path, String message) {
        issues.add(ValidationIssue.error(id, path, message));
        return this;
    }

    public ValidationResult addWarning(String id, String path, String message) {
        issues.add(ValidationIssue.warning(id, path, message));
        return this;
    }

    public ValidationResult addAll(ValidationResult other) {
        issues.addAll(other.issues);
        return this;
    }

    public List<ValidationIssue> issues() {
        return List.copyOf(issues);
    }

    public List<ValidationIssue> errors() {
        return issues.stream().filter(i -> i.severity() == Severity.ERROR).toList();
    }

    public List<ValidationIssue> warnings() {
        return issues.stream().filter(i -> i.severity() == Severity.WARNING).toList();
    }

    /** Valid = no ERROR issues (SPEC-23 §1.1). */
    public boolean isValid() {
        return errors().isEmpty();
    }
}
