package com.cramer.dto;

import com.cramer.entity.UserCredit;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for user credit (Lúa) balance and statistics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreditDTO {

    private Long id;
    private UUID userId;
    private Integer balance;
    private Integer lifetimeEarned;
    private Integer lifetimeSpent;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /**
     * Create a DTO from an entity.
     */
    public static UserCreditDTO fromEntity(UserCredit entity) {
        if (entity == null) return null;

        return UserCreditDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .balance(entity.getBalance())
                .lifetimeEarned(entity.getLifetimeEarned())
                .lifetimeSpent(entity.getLifetimeSpent())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
