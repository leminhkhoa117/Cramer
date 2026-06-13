package com.cramer.assessment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A test-taking session, table {@code test_attempts} (SPEC-12 §1). Keys on the legacy
 * {@code exam_source}/{@code test_number}/{@code skill}; {@code test_number} is varchar and
 * {@code skill} is stored lowercase (verified live DB). {@code status} is uppercase.
 */
@Entity
@Table(name = "test_attempts", schema = "public")
@Getter
@Setter
public class Attempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "exam_source", nullable = false)
    private String examSource;

    @Column(name = "test_number", nullable = false)
    private String testNumber;

    /** Stored lowercase in DB (reading/listening/writing/speaking). */
    @Column(name = "skill", nullable = false)
    private String skill;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @Column(name = "score")
    private Integer score;

    @Column(name = "current_part")
    private Integer currentPart;

    @Column(name = "time_left")
    private Integer timeLeft;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
}
