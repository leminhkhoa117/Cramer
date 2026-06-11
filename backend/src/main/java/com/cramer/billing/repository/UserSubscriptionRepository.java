package com.cramer.billing.repository;

import com.cramer.billing.domain.UserSubscription;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data repository for {@link UserSubscription} (SPEC-15 §2). */
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    Optional<UserSubscription> findFirstByUserIdOrderByStartedAtDesc(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from UserSubscription s where s.id = :id")
    Optional<UserSubscription> findByIdForUpdate(@Param("id") Long id);

    /** Active subscriptions that have passed their expiry (for the daily expiry scheduler). */
    @Query("select s from UserSubscription s where s.status = 'ACTIVE' and s.expiresAt is not null and s.expiresAt < :now")
    List<UserSubscription> findExpired(@Param("now") OffsetDateTime now);

    /** All subscriptions in a status (for the monthly reset scheduler). */
    List<UserSubscription> findByStatus(String status);
}
