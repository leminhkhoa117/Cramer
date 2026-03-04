package com.cramer.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for aggregated user statistics.
 * Includes subscription, credits, and streak overview.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFullStatsDTO {

    private UUID userId;
    
    // Subscription info
    private SubscriptionTierDTO currentTier;
    private Integer attemptAisRemaining;
    private Integer dailyChatRemaining;
    private Boolean isSubscriptionActive;
    
    // Credit info
    private Integer luaBalance;
    private Integer lifetimeEarned;
    private Integer lifetimeSpent;
    
    // Streak info
    private Integer currentStreak;
    private Integer longestStreak;
    private LocalDate lastLoginDate;
    
    // Vocabulary summary (if available)
    private Long totalVocabulary;
    private Long masteredVocabulary;
}
