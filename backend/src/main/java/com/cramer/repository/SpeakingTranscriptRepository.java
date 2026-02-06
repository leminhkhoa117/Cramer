package com.cramer.repository;

import com.cramer.entity.SpeakingTranscript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SpeakingTranscript entity.
 */
@Repository
public interface SpeakingTranscriptRepository extends JpaRepository<SpeakingTranscript, Long> {

    /**
     * Find all transcripts for a session ordered by part and creation time.
     */
    List<SpeakingTranscript> findBySessionIdOrderByPartAscCreatedAtAsc(Long sessionId);

    /**
     * Find transcripts by session and part.
     */
    List<SpeakingTranscript> findBySessionIdAndPartOrderByCreatedAtAsc(Long sessionId, Integer part);

    /**
     * Find transcript by session and question.
     */
    Optional<SpeakingTranscript> findBySessionIdAndQuestionId(Long sessionId, Long questionId);

    /**
     * Count transcripts for a session.
     */
    long countBySessionId(Long sessionId);

    /**
     * Get total audio duration for a session.
     */
    @Query("SELECT COALESCE(SUM(t.audioDurationSeconds), 0) FROM SpeakingTranscript t WHERE t.sessionId = :sessionId")
    Integer getTotalAudioDuration(@Param("sessionId") Long sessionId);

    /**
     * Delete all transcripts for a session.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM SpeakingTranscript t WHERE t.sessionId = :sessionId")
    int deleteBySessionId(@Param("sessionId") Long sessionId);

    /**
     * Get all transcript texts for a session (for evaluation).
     */
    @Query("SELECT t.transcriptText FROM SpeakingTranscript t " +
            "WHERE t.sessionId = :sessionId AND t.transcriptText IS NOT NULL " +
            "ORDER BY t.part ASC, t.createdAt ASC")
    List<String> getTranscriptTextsForSession(@Param("sessionId") Long sessionId);
}
