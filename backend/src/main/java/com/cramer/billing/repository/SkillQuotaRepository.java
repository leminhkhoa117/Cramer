package com.cramer.billing.repository;

import com.cramer.billing.domain.SkillQuota;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data repository for {@link SkillQuota} (SPEC-15 §5). */
public interface SkillQuotaRepository extends JpaRepository<SkillQuota, Long> {

    Optional<SkillQuota> findByUserIdAndSkillAndQuotaMonth(UUID userId, String skill, LocalDate quotaMonth);

    List<SkillQuota> findByUserIdAndQuotaMonth(UUID userId, LocalDate quotaMonth);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from SkillQuota q where q.userId = :userId and q.skill = :skill and q.quotaMonth = :month")
    Optional<SkillQuota> findForUpdate(@Param("userId") UUID userId, @Param("skill") String skill,
                                       @Param("month") LocalDate month);
}
