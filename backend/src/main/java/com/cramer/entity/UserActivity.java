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
@Table(name = "user_activities")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "activity_type", nullable = false, length = 50)
    private String activityType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    
    }
    public static final String TYPE_TEST_COMPLETED = "TEST_COMPLETED";
    public static final String TYPE_VOCAB_SAVED = "VOCAB_SAVED";
    public static final String TYPE_SUBSCRIPTION_CHANGED = "SUBSCRIPTION_CHANGED";
    public static final String TYPE_LOGIN = "LOGIN";
    public static final String TYPE_ACHIEVEMENT_EARNED = "ACHIEVEMENT_EARNED";
    public static final String TYPE_PROFILE_UPDATED = "PROFILE_UPDATED";
    public static final String TYPE_CREDITS_CHANGED = "CREDITS_CHANGED";
}