package com.cramer.repository;

import com.cramer.entity.ChatbotUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ChatbotUsage entity.
 */
@Repository
public interface ChatbotUsageRepository extends JpaRepository<ChatbotUsage, Long> {

    /**
     * Find usage for a user on a specific date.
     */
    Optional<ChatbotUsage> findByUserIdAndUsageDate(UUID userId, LocalDate usageDate);

    /**
     * Find today's usage for a user.
     */
    default Optional<ChatbotUsage> findTodayUsage(UUID userId) {
        return findByUserIdAndUsageDate(userId, LocalDate.now());
    }

    /**
     * Get message count for today.
     */
    @Query("SELECT COALESCE(u.messagesUsed, 0) FROM ChatbotUsage u " +
           "WHERE u.userId = :userId AND u.usageDate = :date")
    Optional<Integer> getMessagesUsed(@Param("userId") UUID userId, @Param("date") LocalDate date);

    /**
     * Increment message count (atomic).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ChatbotUsage u SET u.messagesUsed = u.messagesUsed + 1 " +
           "WHERE u.userId = :userId AND u.usageDate = :date")
    int incrementMessagesUsed(@Param("userId") UUID userId, @Param("date") LocalDate date);

    /**
     * Check if user exists for today.
     */
    boolean existsByUserIdAndUsageDate(UUID userId, LocalDate usageDate);

    /**
     * Delete old usage records (cleanup).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ChatbotUsage u WHERE u.usageDate < :beforeDate")
    int deleteOldRecords(@Param("beforeDate") LocalDate beforeDate);
}
