package com.cramer.catalog.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Replace the hashtag set on a test (SPEC-11 §4: {@code PUT …/tests/{id}/hashtags}). Codes are
 * resolved/created via find-or-create; max 20; each must match {@code ^[a-z0-9_-]+$}.
 */
public record UpdateTestHashtagsRequest(
        @NotNull List<String> codes) {
}
