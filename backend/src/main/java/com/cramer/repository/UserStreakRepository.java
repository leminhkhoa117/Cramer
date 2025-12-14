package com.cramer.repository;

import com.cramer.entity.UserStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserStreak entity.
 */
@Repository
public interface UserStreakRepository extends JpaRepository<UserStreak, Long> {

    /**
     * Find streak record for a user.
     */
    Optional<UserStreak> findByUserId(UUID userId);

    /**
     * Check if user has streak record.
     */
    boolean existsByUserId(UUID userId);

    /**
     * Find users with streaks that need checking (not logged in today).
     */
    @Query("SELECT s FROM UserStreak s WHERE s.lastLoginDate < :today")
    List<UserStreak> findStreaksNeedingUpdate(@Param("today") LocalDate today);

    /**
     * Find top streaks (leaderboard).
     */
    List<UserStreak> findTop10ByOrderByCurrentStreakDesc();

    /**
     * Find users eligible for 7-day streak bonus.
     */
    @Query("SELECT s FROM UserStreak s WHERE s.currentStreak >= 7 " +
           "AND (s.lastStreakBonusAt IS NULL OR s.lastStreakBonusAt < :threshold)")
    List<UserStreak> findEligibleFor7DayBonus(@Param("threshold") java.time.OffsetDateTime threshold);

    /**
     * Find users eligible for 30-day streak bonus.
     */
    @Query("SELECT s FROM UserStreak s WHERE s.currentStreak >= 30 " +
           "AND (s.lastStreakBonusAt IS NULL OR s.lastStreakBonusAt < :threshold)")
    List<UserStreak> findEligibleFor30DayBonus(@Param("threshold") java.time.OffsetDateTime threshold);
}
