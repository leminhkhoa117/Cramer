package com.cramer.speaking.repository;

import com.cramer.speaking.domain.SpeakingSession;
import com.cramer.speaking.domain.SpeakingSessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data repository for {@link SpeakingSession} (SPEC-14). */
public interface SpeakingSessionRepository extends JpaRepository<SpeakingSession, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SpeakingSession s where s.id = :id")
    Optional<SpeakingSession> findByIdForUpdate(@Param("id") Long id);

    Page<SpeakingSession> findByUserIdOrderByStartedAtDesc(UUID userId, Pageable pageable);

    Page<SpeakingSession> findByUserIdAndStatusOrderByStartedAtDesc(UUID userId, SpeakingSessionStatus status, Pageable pageable);

    /** Sessions stuck in a status since before {@code threshold} (watchdog, SPEC-14 §6). */
    @Query("select s from SpeakingSession s where s.status = :status and s.updatedAt < :threshold")
    List<SpeakingSession> findStuck(@Param("status") SpeakingSessionStatus status,
                                    @Param("threshold") OffsetDateTime threshold);
}
