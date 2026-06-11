package com.cramer.speaking.repository;

import com.cramer.speaking.domain.SpeakingTranscript;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Spring Data repository for {@link SpeakingTranscript} (SPEC-14 §5). */
public interface SpeakingTranscriptRepository extends JpaRepository<SpeakingTranscript, Long> {

    List<SpeakingTranscript> findBySessionIdOrderByTurnIndexAsc(Long sessionId);

    Optional<SpeakingTranscript> findBySessionIdAndTurnIndex(Long sessionId, Integer turnIndex);

    long countBySessionId(Long sessionId);
}
