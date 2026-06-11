package com.cramer.assessment.repository;

import com.cramer.assessment.domain.Attempt;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** Spring Data repository for {@link Attempt} (SPEC-12). */
public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    /**
     * Lock + fetch all attempts for a user/source/test/skill (most recent first), to serialize
     * concurrent start/resume requests (SPEC-12 §3).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Attempt a where a.userId = :userId and a.examSource = :examSource "
            + "and a.testNumber = :testNumber and a.skill = :skill order by a.startedAt desc")
    List<Attempt> lockByKey(@Param("userId") UUID userId,
                            @Param("examSource") String examSource,
                            @Param("testNumber") String testNumber,
                            @Param("skill") String skill);

    List<Attempt> findByUserIdOrderByStartedAtDesc(UUID userId);
}
