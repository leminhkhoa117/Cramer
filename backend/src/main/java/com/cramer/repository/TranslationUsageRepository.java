package com.cramer.repository;

import com.cramer.entity.TranslationUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TranslationUsage entity.
 * Tracks monthly translation usage for Vocabulary Notebook.
 */
@Repository
public interface TranslationUsageRepository extends JpaRepository<TranslationUsage, Long> {

    /**
     * Find usage for a specific user and month.
     */
    Optional<TranslationUsage> findByUserIdAndUsageMonth(UUID userId, LocalDate usageMonth);

    /**
     * Find current month's usage for a user.
     */
    default Optional<TranslationUsage> findCurrentMonthUsage(UUID userId) {
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
        return findByUserIdAndUsageMonth(userId, firstOfMonth);
    }

    /**
     * Get translation count for current month.
     */
    @Query("SELECT COALESCE(u.translationsUsed, 0) FROM TranslationUsage u " +
           "WHERE u.userId = :userId AND u.usageMonth = :usageMonth")
    Integer getTranslationsUsed(@Param("userId") UUID userId, @Param("usageMonth") LocalDate usageMonth);

    /**
     * Increment translation usage atomically.
     */
    @Modifying
    @Query("UPDATE TranslationUsage u SET u.translationsUsed = u.translationsUsed + 1, " +
           "u.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE u.userId = :userId AND u.usageMonth = :usageMonth")
    int incrementTranslationsUsed(@Param("userId") UUID userId, @Param("usageMonth") LocalDate usageMonth);

    /**
     * Delete old usage records (for cleanup).
     */
    @Modifying
    @Query("DELETE FROM TranslationUsage u WHERE u.usageMonth < :beforeMonth")
    int deleteOldRecords(@Param("beforeMonth") LocalDate beforeMonth);
}
