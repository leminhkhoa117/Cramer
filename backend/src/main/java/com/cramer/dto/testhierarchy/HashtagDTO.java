package com.cramer.dto.testhierarchy;

import com.cramer.entity.Hashtag;
import lombok.*;
import java.time.OffsetDateTime;

/**
 * DTO for Hashtag entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HashtagDTO {
    private Long id;
    private String code;
    private String name;
    private String category;
    private String icon;
    private String color;
    private Integer useCount;
    private Boolean isActive;
    private OffsetDateTime createdAt;

    // Number of tests using this hashtag
    private Integer testCount;

    /**
     * Create DTO from Hashtag entity.
     */
    public static HashtagDTO fromEntity(Hashtag entity) {
        if (entity == null)
            return null;

        return HashtagDTO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .category(entity.getCategory())
                .icon(entity.getIcon())
                .color(entity.getColor())
                .useCount(entity.getUseCount())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .testCount(entity.getTestCount())
                .build();
    }
}
