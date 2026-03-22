package com.cramer.repository;

import com.cramer.entity.SpeakingTranscript;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpeakingTranscriptRepository extends JpaRepository<SpeakingTranscript, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO public.speaking_transcripts (
                session_id,
                source_question_id,
                part_number,
                turn_index,
                question_snapshot,
                audio_storage_path,
                audio_duration_seconds,
                transcript_text,
                transcript_confidence,
                question_evaluation,
                recorded_at
            )
            VALUES (
                :sessionId,
                :sourceQuestionId,
                :partNumber,
                :turnIndex,
                CAST(:questionSnapshot AS jsonb),
                :audioStoragePath,
                :audioDurationSeconds,
                :transcriptText,
                :transcriptConfidence,
                CAST(:questionEvaluation AS jsonb),
                :recordedAt
            )
            ON CONFLICT (session_id, turn_index) DO UPDATE SET
                source_question_id = EXCLUDED.source_question_id,
                part_number = EXCLUDED.part_number,
                question_snapshot = EXCLUDED.question_snapshot,
                audio_storage_path = EXCLUDED.audio_storage_path,
                audio_duration_seconds = EXCLUDED.audio_duration_seconds,
                transcript_text = EXCLUDED.transcript_text,
                transcript_confidence = EXCLUDED.transcript_confidence,
                question_evaluation = EXCLUDED.question_evaluation,
                recorded_at = EXCLUDED.recorded_at,
                updated_at = NOW()
            """, nativeQuery = true)
    void upsertTranscript(
            @Param("sessionId") Long sessionId,
            @Param("sourceQuestionId") Long sourceQuestionId,
            @Param("partNumber") Integer partNumber,
            @Param("turnIndex") Integer turnIndex,
            @Param("questionSnapshot") String questionSnapshot,
            @Param("audioStoragePath") String audioStoragePath,
            @Param("audioDurationSeconds") Integer audioDurationSeconds,
            @Param("transcriptText") String transcriptText,
            @Param("transcriptConfidence") BigDecimal transcriptConfidence,
            @Param("questionEvaluation") String questionEvaluation,
            @Param("recordedAt") OffsetDateTime recordedAt);

    Optional<SpeakingTranscript> findBySessionIdAndTurnIndex(Long sessionId, Integer turnIndex);

    List<SpeakingTranscript> findBySessionIdOrderByTurnIndexAsc(Long sessionId);

    long countBySessionId(Long sessionId);
}
