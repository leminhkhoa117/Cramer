package com.cramer.repository;

import com.cramer.entity.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {

    /**
     * Finds all answers submitted by a specific user by joining through the TestAttempt entity.
     * @param userId The UUID of the user.
     * @return A list of all UserAnswer entities for that user.
     */
    List<UserAnswer> findByAttempt_UserId(UUID userId);

    /**
     * Finds all answers for a specific test attempt.
     * @param testAttemptId The ID of the test attempt.
     * @return A list of UserAnswer entities for that attempt.
     */
    List<UserAnswer> findByAttemptId(Long testAttemptId);

    /**
     * Deletes all answers associated with a specific test attempt.
     * This is useful for allowing users to re-submit a test.
     * @param testAttemptId The ID of the test attempt whose answers should be deleted.
     */
    @Modifying
    void deleteByAttemptId(Long testAttemptId);

    /**
     * Finds the 5 most recent answers for a specific user.
     * @param userId The UUID of the user.
     * @return A list of the 5 most recent UserAnswer entities.
     */
    List<UserAnswer> findTop5ByAttempt_UserIdOrderBySubmittedAtDesc(UUID userId);
}
