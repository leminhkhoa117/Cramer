package com.cramer.repository;

import com.cramer.entity.UserQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserQuota entity.
 * Manages global monthly quota tracking.
 */
@Repository
public interface UserQuotaRepository extends JpaRepository<UserQuota, Long> {

    /**
     * Find quota by user ID and quota month.
     */
    Optional<UserQuota> findByUserIdAndQuotaMonth(UUID userId, LocalDate quotaMonth);

    /**
     * Check if quota exists for user and month.
     */
    boolean existsByUserIdAndQuotaMonth(UUID userId, LocalDate quotaMonth);

    /**
     * Atomic increment of attempt count.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UserQuota q SET q.attemptCount = q.attemptCount + 1 " +
           "WHERE q.userId = :userId AND q.quotaMonth = :quotaMonth")
    int incrementAttemptCount(@Param("userId") UUID userId, @Param("quotaMonth") LocalDate quotaMonth);

    /**
     * Atomic increment of AI attempt count.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UserQuota q SET q.attemptAiCount = q.attemptAiCount + 1 " +
           "WHERE q.userId = :userId AND q.quotaMonth = :quotaMonth")
    int incrementAttemptAiCount(@Param("userId") UUID userId, @Param("quotaMonth") LocalDate quotaMonth);

    /**
     * Get current attempt count for user and month.
     */
    @Query("SELECT COALESCE(q.attemptCount, 0) FROM UserQuota q WHERE q.userId = :userId AND q.quotaMonth = :quotaMonth")
    Optional<Integer> getAttemptCount(@Param("userId") UUID userId, @Param("quotaMonth") LocalDate quotaMonth);

    /**
     * Get current AI attempt count for user and month.
     */
    @Query("SELECT COALESCE(q.attemptAiCount, 0) FROM UserQuota q WHERE q.userId = :userId AND q.quotaMonth = :quotaMonth")
    Optional<Integer> getAttemptAiCount(@Param("userId") UUID userId, @Param("quotaMonth") LocalDate quotaMonth);
}
