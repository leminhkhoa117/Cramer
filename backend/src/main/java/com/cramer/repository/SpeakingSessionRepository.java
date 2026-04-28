package com.cramer.repository;

import com.cramer.entity.SpeakingSession;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpeakingSessionRepository extends JpaRepository<SpeakingSession, Long> {

    Optional<SpeakingSession> findByIdAndUserId(Long id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SpeakingSession s WHERE s.id = :id AND s.userId = :userId")
    Optional<SpeakingSession> findAndLockByIdAndUserId(@Param("id") Long id, @Param("userId") UUID userId);

    @Query("SELECT s FROM SpeakingSession s WHERE s.userId = :userId AND (:status IS NULL OR s.status = :status)")
    Page<SpeakingSession> findHistoryByUserId(
            @Param("userId") UUID userId,
            @Param("status") String status,
            Pageable pageable);

    List<SpeakingSession> findByStatusAndUpdatedAtBefore(String status, OffsetDateTime threshold);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SpeakingSession s SET s.status = :newStatus WHERE s.status = :currentStatus AND s.updatedAt < :threshold")
    int batchUpdateStatus(@Param("currentStatus") String currentStatus, @Param("newStatus") String newStatus, @Param("threshold") OffsetDateTime threshold);
}
