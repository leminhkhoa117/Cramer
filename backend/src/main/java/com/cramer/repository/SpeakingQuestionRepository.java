package com.cramer.repository;

import com.cramer.entity.SpeakingQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for SpeakingQuestion entity.
 */
@Repository
public interface SpeakingQuestionRepository extends JpaRepository<SpeakingQuestion, Long> {

    /**
     * Find active questions by topic and part.
     */
    List<SpeakingQuestion> findByTopicIdAndPartAndIsActiveTrueOrderById(Long topicId, Integer part);

    /**
     * Find active questions by part only (for generic Part 1 questions).
     */
    List<SpeakingQuestion> findByPartAndIsActiveTrueOrderById(Integer part);

    /**
     * Find questions by topic, part and difficulty.
     */
    List<SpeakingQuestion> findByTopicIdAndPartAndDifficultyAndIsActiveTrue(
            Long topicId, Integer part, String difficulty);

    /**
     * Get random questions for a topic and part.
     */
    @Query(value = "SELECT * FROM speaking_questions " +
            "WHERE topic_id = :topicId AND part = :part AND is_active = true " +
            "ORDER BY RANDOM() LIMIT :count", nativeQuery = true)
    List<SpeakingQuestion> findRandomQuestions(
            @Param("topicId") Long topicId,
            @Param("part") Integer part,
            @Param("count") int count);

    /**
     * Get random generic questions (no topic) for Part 1.
     */
    @Query(value = "SELECT * FROM speaking_questions " +
            "WHERE topic_id IS NULL AND part = :part AND is_active = true " +
            "ORDER BY RANDOM() LIMIT :count", nativeQuery = true)
    List<SpeakingQuestion> findRandomGenericQuestions(
            @Param("part") Integer part,
            @Param("count") int count);

    /**
     * Count active questions by topic and part.
     */
    long countByTopicIdAndPartAndIsActiveTrue(Long topicId, Integer part);

    /**
     * Find all questions for a topic (for admin).
     */
    List<SpeakingQuestion> findByTopicIdOrderByPartAscIdAsc(Long topicId);

    /**
     * Find questions by multiple IDs.
     */
    @Query("SELECT q FROM SpeakingQuestion q WHERE q.id IN :ids AND q.isActive = true")
    List<SpeakingQuestion> findByIdInAndIsActiveTrue(@Param("ids") List<Long> ids);

    /**
     * Find all active questions that don't have TTS audio yet (for batch generation).
     */
    @Query("SELECT q FROM SpeakingQuestion q WHERE q.isActive = true AND q.examinerAudioUrl IS NULL")
    List<SpeakingQuestion> findQuestionsWithoutTTS();

    /**
     * Find active questions by topic without TTS audio.
     */
    @Query("SELECT q FROM SpeakingQuestion q WHERE q.topicId = :topicId AND q.isActive = true AND q.examinerAudioUrl IS NULL")
    List<SpeakingQuestion> findQuestionsWithoutTTSByTopic(@Param("topicId") Long topicId);

    /**
     * Find all active questions.
     */
    List<SpeakingQuestion> findByIsActiveTrueOrderByTopicIdAscPartAscIdAsc();
}
