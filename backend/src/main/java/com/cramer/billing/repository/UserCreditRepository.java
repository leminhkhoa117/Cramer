package com.cramer.billing.repository;

import com.cramer.billing.domain.UserCredit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** Spring Data repository for {@link UserCredit} (SPEC-15 §3). */
public interface UserCreditRepository extends JpaRepository<UserCredit, Long> {

    Optional<UserCredit> findByUserId(UUID userId);

    /**
     * Lock the user's credit row to serialize concurrent balance mutations — the basis for
     * atomic spend/earn/refund and reference idempotency (SPEC-15 §3).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from UserCredit c where c.userId = :userId")
    Optional<UserCredit> findByUserIdForUpdate(@Param("userId") UUID userId);
}
