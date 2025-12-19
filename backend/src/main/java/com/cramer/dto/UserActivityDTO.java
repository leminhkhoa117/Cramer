package com.cramer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityDTO {
    private Long id;
    private UUID userId;
    private String activityType;
    private String title;
    private String description;
    private Map<String, Object> metadata;
    private OffsetDateTime createdAt;
    
    // Helper để format icon dựa trên type
    public String getIcon() {
        return switch (activityType) {
            case "TEST_COMPLETED" -> "📝";
            case "VOCAB_SAVED" -> "📚";
            case "SUBSCRIPTION_CHANGED" -> "💎";
            case "LOGIN" -> "🔐";
            case "ACHIEVEMENT_EARNED" -> "🏆";
            case "CREDITS_CHANGED" -> "🌾";
            default -> "📌";
        };
    }
}