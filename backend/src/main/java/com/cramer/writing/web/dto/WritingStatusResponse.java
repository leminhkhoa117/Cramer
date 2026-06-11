package com.cramer.writing.web.dto;

import java.util.List;

/**
 * Aggregate grading status for an attempt (SPEC-13 §5): overall status plus per-task statuses
 * and counts. {@code overall} ∈ COMPLETED, PARTIAL_FAILURE, GRADING, PENDING.
 */
public record WritingStatusResponse(
        Long attemptId,
        String overall,
        int total,
        int completed,
        int failed,
        List<TaskStatus> tasks) {

    /** Per-task status line. */
    public record TaskStatus(Integer taskNumber, String status) {
    }
}
