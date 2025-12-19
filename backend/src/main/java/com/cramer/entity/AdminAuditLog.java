package com.cramer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "admin_audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_user_id", nullable = false)
    private UUID adminUserId;

    @Column(name = "admin_email")
    private String adminEmail;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private String targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value", columnDefinition = "jsonb")
    private Map<String, Object> oldValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value", columnDefinition = "jsonb")
    private Map<String, Object> newValue;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    // Action Types
    public static final String ACTION_STATUS_CHANGE = "STATUS_CHANGE";
    public static final String ACTION_CREDITS_ADD = "CREDITS_ADD";
    public static final String ACTION_CREDITS_SUBTRACT = "CREDITS_SUBTRACT";
    public static final String ACTION_SUBSCRIPTION_CHANGE = "SUBSCRIPTION_CHANGE";
    public static final String ACTION_PROFILE_UPDATE = "PROFILE_UPDATE";
    public static final String ACTION_BAN = "BAN";
    public static final String ACTION_UNBAN = "UNBAN";

    // Target Types
    public static final String TARGET_USER = "USER";
    public static final String TARGET_SUBSCRIPTION = "SUBSCRIPTION";
    public static final String TARGET_CREDITS = "CREDITS";
    public static final String TARGET_CONTENT = "CONTENT";
}