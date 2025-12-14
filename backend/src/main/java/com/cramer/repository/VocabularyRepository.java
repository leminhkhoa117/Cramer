package com.cramer.repository;

import com.cramer.entity.Vocabulary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Vocabulary entity.
 * Provides CRUD operations and custom query methods for vocabulary entries.
 */
@Repository
public interface VocabularyRepository extends JpaRepository<Vocabulary, Long> {

    /**
     * Find all vocabulary entries for a user, ordered by creation date (newest first).
     *
     * @param userId the user's UUID
     * @return list of vocabulary entries
     */
    List<Vocabulary> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find vocabulary entries for a user with pagination.
     *
     * @param userId   the user's UUID
     * @param pageable pagination parameters
     * @return page of vocabulary entries
     */
    Page<Vocabulary> findByUserId(UUID userId, Pageable pageable);

    /**
     * Find a specific vocabulary entry by ID and user ID.
     * Used for IDOR protection - ensures users can only access their own vocabulary.
     *
     * @param id     the vocabulary entry ID
     * @param userId the user's UUID
     * @return Optional containing the vocabulary entry if found
     */
    Optional<Vocabulary> findByIdAndUserId(Long id, UUID userId);

    /**
     * Find vocabulary entries by mastered status.
     *
     * @param userId     the user's UUID
     * @param isMastered the mastered status to filter by
     * @return list of vocabulary entries
     */
    List<Vocabulary> findByUserIdAndIsMastered(UUID userId, Boolean isMastered);

    /**
     * Check if a word already exists in user's vocabulary (case-insensitive).
     *
     * @param userId the user's UUID
     * @param word   the word to check
     * @return true if the word exists
     */
    boolean existsByUserIdAndWordIgnoreCase(UUID userId, String word);

    /**
     * Count total vocabulary entries for a user.
     *
     * @param userId the user's UUID
     * @return count of vocabulary entries
     */
    long countByUserId(UUID userId);

    /**
     * Count mastered vocabulary entries for a user.
     *
     * @param userId the user's UUID
     * @return count of mastered vocabulary entries
     */
    long countByUserIdAndIsMastered(UUID userId, Boolean isMastered);

    /**
     * Find vocabulary entries by source test ID.
     *
     * @param userId       the user's UUID
     * @param sourceTestId the source test ID
     * @return list of vocabulary entries from that test
     */
    List<Vocabulary> findByUserIdAndSourceTestId(UUID userId, Long sourceTestId);

    /**
     * Search vocabulary by word containing text (case-insensitive).
     *
     * @param userId     the user's UUID
     * @param searchTerm the search term
     * @param pageable   pagination parameters
     * @return page of matching vocabulary entries
     */
    @Query("SELECT v FROM Vocabulary v WHERE v.userId = :userId AND LOWER(v.word) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Vocabulary> searchByWord(@Param("userId") UUID userId, @Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Search vocabulary by word containing text (case-insensitive) AND mastered status.
     *
     * @param userId     the user's UUID
     * @param searchTerm the search term
     * @param isMastered the mastered status to filter by
     * @param pageable   pagination parameters
     * @return page of matching vocabulary entries
     */
    @Query("SELECT v FROM Vocabulary v WHERE v.userId = :userId AND LOWER(v.word) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND v.isMastered = :isMastered")
    Page<Vocabulary> searchByWordAndMastered(@Param("userId") UUID userId, @Param("searchTerm") String searchTerm, @Param("isMastered") Boolean isMastered, Pageable pageable);

    /**
     * Find vocabulary entries by mastered status with pagination.
     *
     * @param userId     the user's UUID
     * @param isMastered the mastered status to filter by
     * @param pageable   pagination parameters
     * @return page of vocabulary entries
     */
    Page<Vocabulary> findByUserIdAndIsMastered(UUID userId, Boolean isMastered, Pageable pageable);
}
