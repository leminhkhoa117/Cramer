package com.cramer.dto;

import com.cramer.entity.UserSubscription;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for user subscription information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscriptionDTO {

    private Long id;
    private UUID userId;
    private SubscriptionTierDTO tier;
    private OffsetDateTime startedAt;
    private OffsetDateTime expiresAt;
    private String status;
    private Integer aiGradingsUsed;
    private Integer aiGradingsRemaining;
    private Boolean autoRenew;
    private Boolean isActive;

    /**
     * Create a DTO from an entity.
     */
    public static UserSubscriptionDTO fromEntity(UserSubscription entity) {
        if (entity == null) return null;

        return UserSubscriptionDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .tier(SubscriptionTierDTO.fromEntity(entity.getTier()))
                .startedAt(entity.getStartedAt())
                .expiresAt(entity.getExpiresAt())
                .status(entity.getStatus().name())
                .aiGradingsUsed(entity.getAiGradingsUsed())
                .aiGradingsRemaining(entity.getRemainingAiGradings())
                .autoRenew(entity.getAutoRenew())
                .isActive(entity.isActive())
                .build();
    }
}
