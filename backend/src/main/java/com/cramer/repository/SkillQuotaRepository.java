package com.cramer.repository;

import com.cramer.entity.SkillQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SkillQuota entity.
 * Manages per-skill monthly quota tracking.
 */
@Repository
public interface SkillQuotaRepository extends JpaRepository<SkillQuota, Long> {

    /**
     * Find quota by user ID, skill, and quota month.
     */
    Optional<SkillQuota> findByUserIdAndSkillAndQuotaMonth(
            UUID userId, SkillQuota.Skill skill, LocalDate quotaMonth);

    /**
     * Find all quotas for user and month (all skills).
     */
    List<SkillQuota> findAllByUserIdAndQuotaMonth(UUID userId, LocalDate quotaMonth);

    /**
     * Check if quota exists for user, skill, and month.
     */
    boolean existsByUserIdAndSkillAndQuotaMonth(UUID userId, SkillQuota.Skill skill, LocalDate quotaMonth);

    /**
     * Atomic increment of attempt count for a skill.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SkillQuota q SET q.attemptCount = q.attemptCount + 1 " +
           "WHERE q.userId = :userId AND q.skill = :skill AND q.quotaMonth = :quotaMonth")
    int incrementAttemptCount(@Param("userId") UUID userId, 
                              @Param("skill") SkillQuota.Skill skill, 
                              @Param("quotaMonth") LocalDate quotaMonth);

    /**
     * Atomic increment of AI attempt count for a skill.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SkillQuota q SET q.attemptAiCount = q.attemptAiCount + 1 " +
           "WHERE q.userId = :userId AND q.skill = :skill AND q.quotaMonth = :quotaMonth")
    int incrementAttemptAiCount(@Param("userId") UUID userId, 
                                 @Param("skill") SkillQuota.Skill skill, 
                                 @Param("quotaMonth") LocalDate quotaMonth);

    /**
     * Get current attempt count for user, skill, and month.
     */
    @Query("SELECT COALESCE(q.attemptCount, 0) FROM SkillQuota q " +
           "WHERE q.userId = :userId AND q.skill = :skill AND q.quotaMonth = :quotaMonth")
    Optional<Integer> getAttemptCount(@Param("userId") UUID userId, 
                                      @Param("skill") SkillQuota.Skill skill, 
                                      @Param("quotaMonth") LocalDate quotaMonth);

    /**
     * Get current AI attempt count for user, skill, and month.
     */
    @Query("SELECT COALESCE(q.attemptAiCount, 0) FROM SkillQuota q " +
           "WHERE q.userId = :userId AND q.skill = :skill AND q.quotaMonth = :quotaMonth")
    Optional<Integer> getAttemptAiCount(@Param("userId") UUID userId, 
                                        @Param("skill") SkillQuota.Skill skill, 
                                        @Param("quotaMonth") LocalDate quotaMonth);
}
