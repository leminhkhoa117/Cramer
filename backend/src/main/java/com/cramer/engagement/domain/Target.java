package com.cramer.engagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A user's IELTS goal, table {@code target} (SPEC-16 §5). One per user ({@code user_id} unique);
 * bands 0–9 (DB check). PK is a UUID assigned by the DB default; the service upserts by user.
 */
@Entity
@Table(name = "target", schema = "public")
@Getter
@Setter
public class Target {

    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "exam_name", nullable = false)
    private String examName;

    @Column(name = "exam_date")
    private LocalDate examDate;

    @Column(name = "listening")
    private Double listening;

    @Column(name = "reading")
    private Double reading;

    @Column(name = "writing")
    private Double writing;

    @Column(name = "speaking")
    private Double speaking;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
