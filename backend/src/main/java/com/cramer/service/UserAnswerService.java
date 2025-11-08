package com.cramer.service;

import com.cramer.entity.UserAnswer;
import com.cramer.repository.UserAnswerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service layer for UserAnswer entity.
 * Handles business logic for user answers and progress tracking.
 */
@Service
@Transactional
public class UserAnswerService {

    private static final Logger logger = LoggerFactory.getLogger(UserAnswerService.class);

    private final UserAnswerRepository userAnswerRepository;

    @Autowired
    public UserAnswerService(UserAnswerRepository userAnswerRepository) {
        this.userAnswerRepository = userAnswerRepository;
    }

    /**
     * Get all user answers.
     * 
     * @return list of all answers
     */
    @Transactional(readOnly = true)
    public List<UserAnswer> getAllUserAnswers() {
        logger.info("Fetching all user answers");
        return userAnswerRepository.findAll();
    }

    /**
     * Get user answer by ID.
     * 
     * @param id the answer ID
     * @return Optional containing the answer if found
     */
    @Transactional(readOnly = true)
    public Optional<UserAnswer> getUserAnswerById(Long id) {
        logger.info("Fetching user answer by ID: {}", id);
        return userAnswerRepository.findById(id);
    }

    /**
     * Get all answers by a user.
     * 
     * @param userId the user ID
     * @return list of user's answers
     */
    @Transactional(readOnly = true)
    public List<UserAnswer> getUserAnswersByUserId(UUID userId) {
        logger.info("Fetching answers for user: {}", userId);
        return userAnswerRepository.findByUserId(userId);
    }

    /**
     * Get all answers for a question.
     * 
     * @param questionId the question ID
     * @return list of answers
     */
    @Transactional(readOnly = true)
    public List<UserAnswer> getAnswersByQuestionId(Long questionId) {
        logger.info("Fetching answers for question: {}", questionId);
        return userAnswerRepository.findByQuestionId(questionId);
    }

    /**
     * Get a user's answer to a specific question.
     * 
     * @param userId the user ID
     * @param questionId the question ID
     * @return Optional containing the answer if found
     */
    @Transactional(readOnly = true)
    public Optional<UserAnswer> getUserAnswerForQuestion(UUID userId, Long questionId) {
        logger.info("Fetching answer for user {} on question {}", userId, questionId);
        return userAnswerRepository.findByUserIdAndQuestionId(userId, questionId);
    }

    /**
     * Get user's correct answers.
     * 
     * @param userId the user ID
     * @return list of correct answers
     */
    @Transactional(readOnly = true)
    public List<UserAnswer> getCorrectAnswers(UUID userId) {
        logger.info("Fetching correct answers for user: {}", userId);
        return userAnswerRepository.findCorrectAnswersByUserId(userId);
    }

    /**
     * Get user's incorrect answers.
     * 
     * @param userId the user ID
     * @return list of incorrect answers
     */
    @Transactional(readOnly = true)
    public List<UserAnswer> getIncorrectAnswers(UUID userId) {
        logger.info("Fetching incorrect answers for user: {}", userId);
        return userAnswerRepository.findIncorrectAnswersByUserId(userId);
    }

    /**
     * Get user's recent answers.
     * 
     * @param userId the user ID
     * @param limit maximum number of answers
     * @return list of recent answers
     */
    @Transactional(readOnly = true)
    public List<UserAnswer> getRecentAnswers(UUID userId, int limit) {
        logger.info("Fetching {} recent answers for user: {}", limit, userId);
        return userAnswerRepository.findRecentAnswersByUserId(userId, limit);
    }

    /**
     * Get answers within a time range.
     * 
     * @param userId the user ID
     * @param startTime start of range
     * @param endTime end of range
     * @return list of answers in range
     */
    @Transactional(readOnly = true)
    public List<UserAnswer> getAnswersInTimeRange(UUID userId, OffsetDateTime startTime, OffsetDateTime endTime) {
        logger.info("Fetching answers for user {} between {} and {}", userId, startTime, endTime);
        return userAnswerRepository.findUserAnswersInTimeRange(userId, startTime, endTime);
    }

    /**
     * Calculate user's accuracy percentage.
     * 
     * @param userId the user ID
     * @return accuracy percentage (0-100)
     */
    @Transactional(readOnly = true)
    public Double calculateUserAccuracy(UUID userId) {
        logger.info("Calculating accuracy for user: {}", userId);
        Double accuracy = userAnswerRepository.calculateUserAccuracy(userId);
        return accuracy != null ? accuracy : 0.0;
    }

    /**
     * Get user statistics.
     * 
     * @param userId the user ID
     * @return UserStats object with counts and accuracy
     */
    @Transactional(readOnly = true)
    public UserStats getUserStatistics(UUID userId) {
        logger.info("Getting statistics for user: {}", userId);
        
        long totalAnswers = userAnswerRepository.countByUserId(userId);

        // If the user has no answers, return zero stats to avoid division by zero.
        if (totalAnswers == 0) {
            return new UserStats(0, 0, 0, 0.0);
        }

        long correctAnswers = userAnswerRepository.countCorrectAnswersByUserId(userId);
        long incorrectAnswers = totalAnswers - correctAnswers;
        Double accuracy = calculateUserAccuracy(userId);
        
        return new UserStats(totalAnswers, correctAnswers, incorrectAnswers, accuracy);
    }

    /**
     * Submit a user answer.
     * 
     * @param userAnswer the answer to submit
     * @return the saved answer
     * @throws IllegalArgumentException if user already answered this question
     */
    public UserAnswer submitAnswer(UserAnswer userAnswer) {
        logger.info("Submitting answer for user {} on question {}", 
                userAnswer.getUserId(), userAnswer.getQuestionId());
        
        // Check if user already answered
        if (userAnswerRepository.existsByUserIdAndQuestionId(
                userAnswer.getUserId(), userAnswer.getQuestionId())) {
            logger.error("User {} already answered question {}", 
                    userAnswer.getUserId(), userAnswer.getQuestionId());
            throw new IllegalArgumentException("User already answered this question");
        }
        
        userAnswer.setSubmittedAt(OffsetDateTime.now());
        
        UserAnswer savedAnswer = userAnswerRepository.save(userAnswer);
        logger.info("Answer submitted successfully with ID: {}", savedAnswer.getId());
        return savedAnswer;
    }

    /**
     * Update an existing answer.
     * 
     * @param id the answer ID
     * @param updatedAnswer the updated answer data
     * @return the updated answer
     * @throws IllegalArgumentException if answer not found
     */
    public UserAnswer updateAnswer(Long id, UserAnswer updatedAnswer) {
        logger.info("Updating answer with ID: {}", id);
        
        UserAnswer existingAnswer = userAnswerRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Answer not found with ID: {}", id);
                    return new IllegalArgumentException("Answer not found with ID: " + id);
                });
        
        existingAnswer.setUserAnswer(updatedAnswer.getUserAnswer());
        existingAnswer.setIsCorrect(updatedAnswer.getIsCorrect());
        existingAnswer.setSubmittedAt(OffsetDateTime.now());
        
        UserAnswer savedAnswer = userAnswerRepository.save(existingAnswer);
        logger.info("Answer updated successfully: {}", id);
        return savedAnswer;
    }

    /**
     * Delete an answer by ID.
     * 
     * @param id the answer ID
     * @throws IllegalArgumentException if answer not found
     */
    public void deleteAnswer(Long id) {
        logger.info("Deleting answer with ID: {}", id);
        
        if (!userAnswerRepository.existsById(id)) {
            logger.error("Answer not found with ID: {}", id);
            throw new IllegalArgumentException("Answer not found with ID: " + id);
        }
        
        userAnswerRepository.deleteById(id);
        logger.info("Answer deleted successfully: {}", id);
    }

    /**
     * Delete all answers by a user.
     * 
     * @param userId the user ID
     */
    public void deleteUserAnswers(UUID userId) {
        logger.info("Deleting all answers for user: {}", userId);
        long count = userAnswerRepository.countByUserId(userId);
        userAnswerRepository.deleteByUserId(userId);
        logger.info("Deleted {} answers for user {}", count, userId);
    }

    /**
     * Helper class for user statistics.
     */
    public static class UserStats {
        private final long totalAnswers;
        private final long correctAnswers;
        private final long incorrectAnswers;
        private final double accuracy;

        public UserStats(long totalAnswers, long correctAnswers, long incorrectAnswers, double accuracy) {
            this.totalAnswers = totalAnswers;
            this.correctAnswers = correctAnswers;
            this.incorrectAnswers = incorrectAnswers;
            this.accuracy = accuracy;
        }

        public long getTotalAnswers() { return totalAnswers; }
        public long getCorrectAnswers() { return correctAnswers; }
        public long getIncorrectAnswers() { return incorrectAnswers; }
        public double getAccuracy() { return accuracy; }
    }
}
