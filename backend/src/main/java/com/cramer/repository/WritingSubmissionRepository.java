package com.cramer.repository;

import com.cramer.entity.WritingSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing WritingSubmission entities.
 */
@Repository
public interface WritingSubmissionRepository extends JpaRepository<WritingSubmission, Long> {

    /**
     * Find all submissions for a specific test attempt.
     */
    List<WritingSubmission> findByAttemptId(Long attemptId);

    /**
     * Find a specific submission by attempt ID and task number.
     */
    Optional<WritingSubmission> findByAttemptIdAndTaskNumber(Long attemptId, Integer taskNumber);

    /**
     * Find all submissions for a specific user.
     */
    List<WritingSubmission> findByUserId(UUID userId);

    /**
     * Find submissions by grading status.
     */
    List<WritingSubmission> findByGradingStatus(String gradingStatus);

    /**
     * Find pending submissions for background grading.
     */
    @Query("SELECT ws FROM WritingSubmission ws WHERE ws.gradingStatus = 'PENDING' ORDER BY ws.submittedAt ASC")
    List<WritingSubmission> findPendingSubmissions();

    /**
     * Delete all submissions for a specific attempt.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM WritingSubmission ws WHERE ws.attemptId = :attemptId")
    void deleteByAttemptId(@Param("attemptId") Long attemptId);

    /**
     * Check if a submission exists for an attempt and task.
     */
    boolean existsByAttemptIdAndTaskNumber(Long attemptId, Integer taskNumber);

    /**
     * Count completed graded submissions for a user.
     */
    @Query("SELECT COUNT(ws) FROM WritingSubmission ws WHERE ws.userId = :userId AND ws.gradingStatus = 'COMPLETED'")
    long countCompletedByUserId(@Param("userId") UUID userId);
}
