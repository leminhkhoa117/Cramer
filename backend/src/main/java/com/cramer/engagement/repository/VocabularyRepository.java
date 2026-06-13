package com.cramer.engagement.repository;

import com.cramer.engagement.domain.Vocabulary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data repository for {@link Vocabulary} (SPEC-16 §3). */
public interface VocabularyRepository extends JpaRepository<Vocabulary, Long> {

    boolean existsByUserIdAndWordIgnoreCase(UUID userId, String word);

    Optional<Vocabulary> findByIdAndUserId(Long id, UUID userId);

    Page<Vocabulary> findByUserId(UUID userId, Pageable pageable);

    Page<Vocabulary> findByUserIdAndWordContainingIgnoreCase(UUID userId, String word, Pageable pageable);

    Page<Vocabulary> findByUserIdAndIsMastered(UUID userId, boolean isMastered, Pageable pageable);

    long countByUserId(UUID userId);

    long countByUserIdAndIsMastered(UUID userId, boolean isMastered);
}
