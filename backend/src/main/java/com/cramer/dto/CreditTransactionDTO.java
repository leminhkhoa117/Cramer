package com.cramer.dto;

import com.cramer.entity.CreditTransaction;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for credit transaction history.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditTransactionDTO {

    private Long id;
    private UUID userId;
    private Integer amount;
    private Integer balanceAfter;
    private String type;
    private String category;
    private String description;
    private String referenceId;
    private OffsetDateTime createdAt;

    /**
     * Create a DTO from an entity.
     */
    public static CreditTransactionDTO fromEntity(CreditTransaction entity) {
        if (entity == null) return null;

        return CreditTransactionDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .amount(entity.getAmount())
                .balanceAfter(entity.getBalanceAfter())
                .type(entity.getType().name())
                .category(entity.getCategory().name())
                .description(entity.getDescription())
                .referenceId(entity.getReferenceId())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
