package com.cramer.abts.validation;

/**
 * A structured validation finding (SPEC-23 §1.1) with a stable id and a best-effort JSON-pointer
 * path so the review UI can target it for refinement.
 *
 * @param id       stable identifier (e.g. {@code rd-q5-missing-answer})
 * @param severity ERROR or WARNING
 * @param path     JSON-pointer into the content (e.g. {@code /questions/4/correct_answer})
 * @param message  human-readable detail
 */
public record ValidationIssue(String id, Severity severity, String path, String message) {

    public static ValidationIssue error(String id, String path, String message) {
        return new ValidationIssue(id, Severity.ERROR, path, message);
    }

    public static ValidationIssue warning(String id, String path, String message) {
        return new ValidationIssue(id, Severity.WARNING, path, message);
    }
}
