package com.cramer.repository;

import com.cramer.entity.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for UserAnswer entity.
 * Provides CRUD operations and custom query methods for tracking user answers and progress.
 */
@Repository
public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {

    /**
     * Find all answers by a specific user.
     * 
     * @param userId the user ID
     * @return list of user's answers ordered by submission time
     */
    @Query("SELECT ua FROM UserAnswer ua WHERE ua.userId = :userId ORDER BY ua.submittedAt DESC")
    List<UserAnswer> findByUserId(@Param("userId") UUID userId);

    /**
     * Find all answers for a specific question.
     * 
     * @param questionId the question ID
     * @return list of answers for that question
     */
    List<UserAnswer> findByQuestionId(Long questionId);

    /**
     * Find a user's answer to a specific question.
     * 
     * @param userId the user ID
     * @param questionId the question ID
     * @return Optional containing the user's answer if found
     */
    Optional<UserAnswer> findByUserIdAndQuestionId(UUID userId, Long questionId);

    /**
     * Find all correct answers by a user.
     * 
     * @param userId the user ID
     * @return list of correct answers
     */
    @Query("SELECT ua FROM UserAnswer ua WHERE ua.userId = :userId AND ua.isCorrect = true")
    List<UserAnswer> findCorrectAnswersByUserId(@Param("userId") UUID userId);

    /**
     * Find all incorrect answers by a user.
     * 
     * @param userId the user ID
     * @return list of incorrect answers
     */
    @Query("SELECT ua FROM UserAnswer ua WHERE ua.userId = :userId AND ua.isCorrect = false")
    List<UserAnswer> findIncorrectAnswersByUserId(@Param("userId") UUID userId);

    /**
     * Count total answers submitted by a user.
     * 
     * @param userId the user ID
     * @return total number of answers
     */
    long countByUserId(UUID userId);

    /**
     * Count correct answers by a user.
     * 
     * @param userId the user ID
     * @return number of correct answers
     */
    @Query("SELECT COUNT(ua) FROM UserAnswer ua WHERE ua.userId = :userId AND ua.isCorrect = true")
    long countCorrectAnswersByUserId(@Param("userId") UUID userId);

    /**
     * Count incorrect answers by a user.
     * 
     * @param userId the user ID
     * @return number of incorrect answers
     */
    @Query("SELECT COUNT(ua) FROM UserAnswer ua WHERE ua.userId = :userId AND ua.isCorrect = false")
    long countIncorrectAnswersByUserId(@Param("userId") UUID userId);

    /**
     * Calculate accuracy percentage for a user.
     * 
     * @param userId the user ID
     * @return accuracy as a percentage (0-100)
     */
    @Query("SELECT (COUNT(CASE WHEN ua.isCorrect = true THEN 1 END) * 100.0 / COUNT(ua)) " +
           "FROM UserAnswer ua WHERE ua.userId = :userId")
    Double calculateUserAccuracy(@Param("userId") UUID userId);

    /**
     * Find user's answers within a time range.
     * 
     * @param userId the user ID
     * @param startTime start of time range
     * @param endTime end of time range
     * @return list of answers within that time range
     */
    @Query("SELECT ua FROM UserAnswer ua WHERE ua.userId = :userId " +
           "AND ua.submittedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY ua.submittedAt DESC")
    List<UserAnswer> findUserAnswersInTimeRange(@Param("userId") UUID userId,
                                                 @Param("startTime") OffsetDateTime startTime,
                                                 @Param("endTime") OffsetDateTime endTime);

    /**
     * Find user's most recent answers.
     * 
     * @param userId the user ID
     * @param limit maximum number of answers to return
     * @return list of most recent answers
     */
    @Query(value = "SELECT * FROM user_answers WHERE user_id = :userId " +
                   "ORDER BY submitted_at DESC LIMIT :limit", nativeQuery = true)
    List<UserAnswer> findRecentAnswersByUserId(@Param("userId") UUID userId,
                                                @Param("limit") int limit);

    /**
     * Check if user has already answered a specific question.
     * 
     * @param userId the user ID
     * @param questionId the question ID
     * @return true if user has answered this question, false otherwise
     */
    boolean existsByUserIdAndQuestionId(UUID userId, Long questionId);

    /**
     * Delete all answers by a user.
     * 
     * @param userId the user ID
     */
    void deleteByUserId(UUID userId);

    /**
     * Delete all answers for a specific question.
     * 
     * @param questionId the question ID
     */
    void deleteByQuestionId(Long questionId);
}
