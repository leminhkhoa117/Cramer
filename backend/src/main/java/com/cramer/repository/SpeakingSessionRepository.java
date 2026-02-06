package com.cramer.repository;

import com.cramer.entity.SpeakingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SpeakingSession entity.
 */
@Repository
public interface SpeakingSessionRepository extends JpaRepository<SpeakingSession, Long> {

    /**
     * Find sessions by user ID ordered by creation date (newest first).
     */
    List<SpeakingSession> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find sessions by user ID and status.
     */
    List<SpeakingSession> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status);

    /**
     * Find the most recent in-progress session for a user.
     */
    Optional<SpeakingSession> findFirstByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status);

    /**
     * Find session by ID and user ID (for authorization).
     */
    Optional<SpeakingSession> findByIdAndUserId(Long id, UUID userId);

    /**
     * Count completed sessions by user.
     */
    long countByUserIdAndStatus(UUID userId, String status);

    /**
     * Find completed sessions for user history (with pagination support).
     */
    @Query("SELECT s FROM SpeakingSession s WHERE s.userId = :userId AND s.status = 'completed' " +
            "ORDER BY s.completedAt DESC")
    List<SpeakingSession> findCompletedSessionsByUser(@Param("userId") UUID userId);

    /**
     * Find sessions by topic for analytics.
     */
    List<SpeakingSession> findByTopicIdAndStatusOrderByCompletedAtDesc(Long topicId, String status);

    /**
     * Update session status.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SpeakingSession s SET s.status = :status WHERE s.id = :sessionId")
    int updateStatus(@Param("sessionId") Long sessionId, @Param("status") String status);

    /**
     * Mark Lúa as deducted for a session.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SpeakingSession s SET s.luaDeducted = true WHERE s.id = :sessionId")
    int markLuaDeducted(@Param("sessionId") Long sessionId);

    /**
     * Find abandoned sessions older than specified hours for cleanup.
     */
    @Query(value = "SELECT * FROM speaking_sessions " +
            "WHERE status = 'in_progress' AND created_at < NOW() - INTERVAL ':hours hours'",
            nativeQuery = true)
    List<SpeakingSession> findAbandonedSessions(@Param("hours") int hours);
}
