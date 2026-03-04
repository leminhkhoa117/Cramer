package com.cramer.repository;

import com.cramer.entity.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserSubscription entity.
 */
@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    /**
     * Find the most recent active subscription for a user.
     * Uses JPQL with enum parameter for type-safe comparison.
     */
    @Query("SELECT s FROM UserSubscription s " +
           "JOIN FETCH s.tier " +
           "WHERE s.userId = :userId AND s.status = :status " +
           "AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP) " +
           "ORDER BY s.startedAt DESC")
    Optional<UserSubscription> findActiveByUserId(
            @Param("userId") UUID userId, 
            @Param("status") UserSubscription.Status status);
    
    /**
     * Convenience method to find active subscription.
     */
    default Optional<UserSubscription> findActiveByUserId(UUID userId) {
        return findActiveByUserId(userId, UserSubscription.Status.ACTIVE);
    }

    /**
     * Find all subscriptions for a user.
     */
    List<UserSubscription> findByUserIdOrderByStartedAtDesc(UUID userId);

    /**
     * Find subscriptions expiring soon (for renewal reminders).
     */
    @Query("SELECT s FROM UserSubscription s WHERE s.status = :status " +
           "AND s.expiresAt BETWEEN CURRENT_TIMESTAMP AND :expiryThreshold")
    List<UserSubscription> findExpiringSoon(
            @Param("expiryThreshold") OffsetDateTime expiryThreshold,
            @Param("status") UserSubscription.Status status);
    
    /**
     * Convenience method with default ACTIVE status.
     */
    default List<UserSubscription> findExpiringSoon(OffsetDateTime expiryThreshold) {
        return findExpiringSoon(expiryThreshold, UserSubscription.Status.ACTIVE);
    }

    /**
     * Find expired subscriptions that need status update.
     */
    @Query("SELECT s FROM UserSubscription s WHERE s.status = :status " +
           "AND s.expiresAt IS NOT NULL AND s.expiresAt < CURRENT_TIMESTAMP")
    List<UserSubscription> findExpiredActiveSubscriptions(@Param("status") UserSubscription.Status status);
    
    /**
     * Convenience method with default ACTIVE status.
     */
    default List<UserSubscription> findExpiredActiveSubscriptions() {
        return findExpiredActiveSubscriptions(UserSubscription.Status.ACTIVE);
    }

    // Note: incrementAiGradingsUsed and resetAiGradingsUsed removed - use attemptAisUsed instead

    /**
     * Increment ATTEMPT_AI usage count.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UserSubscription s SET s.attemptAisUsed = s.attemptAisUsed + 1 WHERE s.id = :id")
    void incrementAttemptAisUsed(@Param("id") Long id);

    /**
     * Check if user has any subscription (active or expired).
     */
    boolean existsByUserId(UUID userId);
}
