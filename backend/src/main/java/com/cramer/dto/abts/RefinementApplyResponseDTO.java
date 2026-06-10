package com.cramer.dto.abts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RefinementApplyResponseDTO - result of the partial-apply endpoint
 * ({@code POST /api/admin/abts/refine/apply}).
 *
 * @since 2026 - ABTS refinement loop (PART D)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefinementApplyResponseDTO {

    /** The JSON after applying the accepted hunks. */
    private String patchedJson;

    /** Number of hunks successfully applied. */
    private int appliedCount;

    /** Number of hunks rejected (not accepted by the user). */
    private int rejectedCount;

    /**
     * Ids of accepted hunks that could not be applied (e.g. stale path).
     * Empty when everything applied cleanly.
     */
    private List<String> skippedHunkIds;

    /**
     * FIX 9: per-skipped-hunk reason ({@code id} + human-readable {@code reason})
     * so the Issue Rail can explain why an accepted hunk was dropped instead of
     * silently discarding it. Parallel to {@link #skippedHunkIds}.
     */
    private List<SkippedHunkDTO> skippedHunks;

    /** Whether the overall apply succeeded (no fatal error). */
    private boolean success;

    /** Populated with a human-readable message on failure. */
    private String errorMessage;

    /** FIX 9: a skipped hunk paired with the reason it could not be applied. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkippedHunkDTO {
        private String id;
        private String reason;
    }
}
