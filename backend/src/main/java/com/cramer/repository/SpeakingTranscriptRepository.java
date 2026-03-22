package com.cramer.repository;

import com.cramer.entity.SpeakingTranscript;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpeakingTranscriptRepository extends JpaRepository<SpeakingTranscript, Long> {

    Optional<SpeakingTranscript> findBySessionIdAndTurnIndex(Long sessionId, Integer turnIndex);

    List<SpeakingTranscript> findBySessionIdOrderByTurnIndexAsc(Long sessionId);

    long countBySessionId(Long sessionId);
}
