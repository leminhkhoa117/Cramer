package com.cramer.dto;

import com.cramer.entity.CreditTransaction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Simplified DTO for credit transaction history display.
 * Used in the Lúa store transaction history list.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Credit transaction history item")
public class CreditHistoryDTO {

    @Schema(description = "Transaction ID")
    private Long id;

    @Schema(description = "Transaction date and time")
    private OffsetDateTime date;

    @Schema(description = "Human-readable description", example = "Mua gói Lúa Medium")
    private String description;

    @Schema(description = "Amount (+/- value)", example = "500")
    private Integer amount;

    @Schema(description = "Balance after transaction", example = "1500")
    private Integer balanceAfter;

    @Schema(description = "Transaction type: EARN or SPEND")
    private CreditTransaction.Type type;

    @Schema(description = "Transaction category")
    private CreditTransaction.Category category;

    @Schema(description = "Icon emoji based on category", example = "💰")
    private String icon;

    /**
     * Convert from CreditTransaction entity.
     */
    public static CreditHistoryDTO fromEntity(CreditTransaction tx) {
        return CreditHistoryDTO.builder()
                .id(tx.getId())
                .date(tx.getCreatedAt())
                .description(tx.getDescription())
                .amount(tx.getAmount())
                .balanceAfter(tx.getBalanceAfter())
                .type(tx.getType())
                .category(tx.getCategory())
                .icon(getCategoryIcon(tx.getCategory()))
                .build();
    }

    /**
     * Get emoji icon based on transaction category.
     */
    private static String getCategoryIcon(CreditTransaction.Category category) {
        return switch (category) {
            case INITIAL_BONUS -> "🎁";
            case TIER_BONUS -> "⭐";
            case STREAK_BONUS -> "🔥";
            case MILESTONE_REWARD -> "🏆";
            case PURCHASE -> "💰";
            case REFERRAL -> "👥";
            case PROMOTION -> "🎉";
            case signup -> "🎁";  // Account signup bonus
            case AI_GRADING -> "📝";
            case VOCABULARY_TRANSLATION -> "📚";
            case PREMIUM_CONTENT -> "🔓";
            case ESSAY_FEEDBACK -> "✍️";
            case CHAT_EXTENSION -> "💬";
            case SPEAKING_SESSION -> "🎤";  // Speaking practice session
            case OTHER -> "🌙";
        };
    }
}
