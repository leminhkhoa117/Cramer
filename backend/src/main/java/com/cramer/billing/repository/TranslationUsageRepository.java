package com.cramer.billing.repository;

import com.cramer.billing.domain.TranslationUsage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Spring Data repository for {@link TranslationUsage} (SPEC-15). */
public interface TranslationUsageRepository extends JpaRepository<TranslationUsage, Long> {

    Optional<TranslationUsage> findByUserIdAndUsageMonth(UUID userId, LocalDate usageMonth);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TranslationUsage t where t.userId = :userId and t.usageMonth = :month")
    Optional<TranslationUsage> findForUpdate(@Param("userId") UUID userId, @Param("month") LocalDate month);
}
