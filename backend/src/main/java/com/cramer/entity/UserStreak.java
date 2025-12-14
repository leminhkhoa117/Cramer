package com.cramer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing a user's login streak.
 * Tracks consecutive days of activity for gamification.
 */
@Entity
@Table(name = "user_streaks", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStreak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "current_streak", nullable = false)
    @Builder.Default
    private Integer currentStreak = 0;

    @Column(name = "longest_streak", nullable = false)
    @Builder.Default
    private Integer longestStreak = 0;

    @Column(name = "last_login_date")
    private LocalDate lastLoginDate;

    @Column(name = "last_streak_bonus_at")
    private OffsetDateTime lastStreakBonusAt; // When last streak bonus was awarded

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    /**
     * Update streak based on current login.
     * Call this when user logs in or performs activity.
     * @return true if streak was incremented, false if broken or already recorded today
     */
    public boolean updateStreak() {
        LocalDate today = LocalDate.now();
        
        if (lastLoginDate == null) {
            // First login ever
            currentStreak = 1;
            longestStreak = 1;
            lastLoginDate = today;
            return true;
        }
        
        if (lastLoginDate.equals(today)) {
            // Already logged in today
            return false;
        }
        
        if (lastLoginDate.plusDays(1).equals(today)) {
            // Consecutive day - increment streak
            currentStreak++;
            if (currentStreak > longestStreak) {
                longestStreak = currentStreak;
            }
            lastLoginDate = today;
            return true;
        }
        
        // Streak broken - reset to 1
        currentStreak = 1;
        lastLoginDate = today;
        return true;
    }

    /**
     * Check if user has maintained a streak for given days.
     */
    public boolean hasStreak(int days) {
        return currentStreak >= days;
    }

    /**
     * Check if user should receive 7-day streak bonus.
     */
    public boolean shouldReceive7DayBonus() {
        if (currentStreak < 7) return false;
        if (lastStreakBonusAt == null) return true;
        
        // Check if 7 days have passed since last bonus
        return lastStreakBonusAt.plusDays(7).isBefore(OffsetDateTime.now());
    }

    /**
     * Check if user should receive 30-day streak bonus.
     */
    public boolean shouldReceive30DayBonus() {
        if (currentStreak < 30) return false;
        if (lastStreakBonusAt == null) return true;
        
        // Check if 30 days have passed since last bonus
        return lastStreakBonusAt.plusDays(30).isBefore(OffsetDateTime.now());
    }
}
