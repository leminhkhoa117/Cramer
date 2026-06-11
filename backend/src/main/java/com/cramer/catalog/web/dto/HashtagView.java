package com.cramer.catalog.web.dto;

import com.cramer.catalog.domain.Hashtag;

/** Outbound hashtag projection (SPEC-11 §4). */
public record HashtagView(
        Long id,
        String code,
        String name,
        String category,
        String icon,
        String color,
        Integer useCount,
        Boolean isActive) {

    public static HashtagView of(Hashtag h) {
        return new HashtagView(
                h.getId(), h.getCode(), h.getName(), h.getCategory(), h.getIcon(), h.getColor(),
                h.getUseCount(), h.getIsActive());
    }
}
