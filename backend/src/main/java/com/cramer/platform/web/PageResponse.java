package com.cramer.platform.web;

import java.util.List;

/**
 * Standard pagination wrapper (SPEC-04 §3). Replaces the old ad-hoc {@code PageDTO}.
 *
 * @param content       the page items
 * @param page          0-based page index
 * @param size          requested page size
 * @param totalElements total matching elements
 * @param totalPages    total pages
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }
}
