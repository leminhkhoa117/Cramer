package com.cramer.abts.validation;

import java.util.List;

/**
 * Serializable projection of a {@link ValidationResult} for HTTP/SSE payloads (SPEC-23 §1.1).
 * {@code issues[]} is the structured, UI-targetable contract; {@code errors}/{@code warnings}
 * are the flat message lists for quick display.
 *
 * @param valid       no ERROR issues
 * @param issues      all structured findings (id, severity, path, message)
 * @param errors      ERROR messages
 * @param warnings    WARNING messages
 * @param errorCount  number of errors
 * @param warningCount number of warnings
 */
public record ValidationView(
        boolean valid,
        List<ValidationIssue> issues,
        List<String> errors,
        List<String> warnings,
        int errorCount,
        int warningCount) {
}
