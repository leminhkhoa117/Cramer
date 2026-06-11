package com.cramer.billing.repository;

import com.cramer.billing.domain.UserQuota;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Spring Data repository for {@link UserQuota} (SPEC-15 §5). */
public interface UserQuotaRepository extends JpaRepository<UserQuota, Long> {

    Optional<UserQuota> findByUserIdAndQuotaMonth(UUID userId, LocalDate quotaMonth);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from UserQuota q where q.userId = :userId and q.quotaMonth = :month")
    Optional<UserQuota> findForUpdate(@Param("userId") UUID userId, @Param("month") LocalDate month);
}
