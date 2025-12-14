package com.cramer.repository;

import com.cramer.entity.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {
    Optional<TestAttempt> findByUserIdAndExamSourceAndTestNumberAndSkill(
            UUID userId, String examSource, String testNumber, String skill
    );

    /**
     * Find all attempts with pessimistic write lock to prevent race conditions.
     * Returns a List to handle cases where multiple attempts exist.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ta FROM TestAttempt ta WHERE ta.userId = :userId AND ta.examSource = :examSource AND ta.testNumber = :testNumber AND ta.skill = :skill ORDER BY ta.startedAt DESC")
    List<TestAttempt> findAndLockByUserIdAndExamSourceAndTestNumberAndSkill(
            @Param("userId") UUID userId, 
            @Param("examSource") String examSource, 
            @Param("testNumber") String testNumber, 
            @Param("skill") String skill
    );

    List<TestAttempt> findByUserId(UUID userId);

    Optional<TestAttempt> findTopByUserIdAndExamSourceAndTestNumberAndSkillOrderByStartedAtDesc(
            UUID userId, String examSource, String testNumber, String skill
    );

    List<TestAttempt> findByUserIdAndExamSourceAndTestNumberAndSkillOrderByStartedAtDesc(
            UUID userId, String examSource, String testNumber, String skill
    );

    List<TestAttempt> findByUserIdAndExamSourceAndTestNumberAndSkillAndStatus(
            UUID userId, String examSource, String testNumber, String skill, String status
    );

    /**
     * Delete a test attempt by ID using explicit JPQL query to ensure proper execution.
     * @param attemptId The ID of the test attempt to delete.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM TestAttempt ta WHERE ta.id = :attemptId")
    void deleteAttemptById(@Param("attemptId") Long attemptId);
}
