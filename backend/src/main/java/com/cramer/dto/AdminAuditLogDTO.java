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
public class AdminAuditLogDTO {
    private Long id;
    private UUID adminUserId;
    private String adminEmail;
    private String action;
    private String targetType;
    private String targetId;
    private Map<String, Object> oldValue;
    private Map<String, Object> newValue;
    private String description;
    private OffsetDateTime createdAt;
    
    // Helper để format action readable
    public String getFormattedAction() {
        return switch (action) {
            case "STATUS_CHANGE" -> "Đổi trạng thái";
            case "CREDITS_ADD" -> "Thêm Lúa";
            case "CREDITS_SUBTRACT" -> "Trừ Lúa";
            case "SUBSCRIPTION_CHANGE" -> "Đổi gói";
            case "BAN" -> "Ban người dùng";
            case "UNBAN" -> "Unban người dùng";
            default -> action;
        };
    }
}