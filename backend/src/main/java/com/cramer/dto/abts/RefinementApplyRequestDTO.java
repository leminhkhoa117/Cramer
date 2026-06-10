package com.cramer.dto.abts;

import com.cramer.dto.abts.RefinementResponseDTO.RefinementHunk;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RefinementApplyRequestDTO - request body for the partial-apply endpoint
 * ({@code POST /api/admin/abts/refine/apply}).
 *
 * <p>The frontend sends the original JSON, the full set of hunks produced by the
 * refinement, and the subset of hunk ids the user accepted. The backend applies
 * only the accepted hunks and returns the resulting JSON.</p>
 *
 * @since 2026 - ABTS refinement loop (PART D)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefinementApplyRequestDTO {

    /** The original (pre-refinement) JSON the hunks were diffed against. */
    @NotNull(message = "originalJson is required")
    private String originalJson;

    /** All hunks produced by the refinement (accepted and rejected). */
    @NotNull(message = "hunks is required")
    private List<RefinementHunk> hunks;

    /** Ids of the hunks the user accepted. Only these are applied. */
    private List<String> acceptedHunkIds;
}
