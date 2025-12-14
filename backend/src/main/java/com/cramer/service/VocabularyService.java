package com.cramer.service;

import com.cramer.dto.VocabularyCreateDTO;
import com.cramer.dto.VocabularyDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for Vocabulary operations.
 * Provides CRUD operations and AI-powered translation.
 */
public interface VocabularyService {

    /**
     * Get all vocabulary entries for a user.
     *
     * @param userId the user's UUID
     * @return list of vocabulary DTOs
     */
    List<VocabularyDTO> getAllByUserId(UUID userId);

    /**
     * Get vocabulary entries for a user with pagination.
     *
     * @param userId   the user's UUID
     * @param pageable pagination parameters
     * @return page of vocabulary DTOs
     */
    Page<VocabularyDTO> getByUserId(UUID userId, Pageable pageable);

    /**
     * Get a specific vocabulary entry by ID (with IDOR protection).
     *
     * @param id     the vocabulary entry ID
     * @param userId the user's UUID
     * @return the vocabulary DTO
     * @throws RuntimeException if not found or access denied
     */
    VocabularyDTO getById(Long id, UUID userId);

    /**
     * Create a new vocabulary entry.
     * Optionally translates the word using AI if autoTranslate is true.
     *
     * @param userId    the user's UUID
     * @param createDTO the creation DTO
     * @return the created vocabulary DTO
     */
    VocabularyDTO create(UUID userId, VocabularyCreateDTO createDTO);

    /**
     * Update an existing vocabulary entry.
     *
     * @param id        the vocabulary entry ID
     * @param userId    the user's UUID
     * @param updateDTO the update data
     * @return the updated vocabulary DTO
     */
    VocabularyDTO update(Long id, UUID userId, VocabularyDTO updateDTO);

    /**
     * Delete a vocabulary entry.
     *
     * @param id     the vocabulary entry ID
     * @param userId the user's UUID
     */
    void delete(Long id, UUID userId);

    /**
     * Toggle the mastered status of a vocabulary entry.
     *
     * @param id     the vocabulary entry ID
     * @param userId the user's UUID
     * @return the updated vocabulary DTO
     */
    VocabularyDTO toggleMastered(Long id, UUID userId);

    /**
     * Translate a word using DeepSeek AI.
     *
     * @param word    the word to translate
     * @param context optional context for better translation
     * @param userId  the user's UUID (to retrieve their API key if set)
     * @return translation result containing translation, phonetic, definition, etc.
     */
    Map<String, String> translateWord(String word, String context, UUID userId);

    /**
     * Get vocabulary statistics for a user.
     *
     * @param userId the user's UUID
     * @return map containing statistics (total, mastered, learning, etc.)
     */
    Map<String, Object> getStats(UUID userId);

    /**
     * Search vocabulary by word.
     *
     * @param userId     the user's UUID
     * @param searchTerm the search term
     * @param pageable   pagination parameters
     * @return page of matching vocabulary DTOs
     */
    Page<VocabularyDTO> search(UUID userId, String searchTerm, Pageable pageable);

    /**
     * Search vocabulary by word with mastered status filter.
     *
     * @param userId     the user's UUID
     * @param searchTerm the search term
     * @param isMastered the mastered status to filter by
     * @param pageable   pagination parameters
     * @return page of matching vocabulary DTOs
     */
    Page<VocabularyDTO> searchWithFilter(UUID userId, String searchTerm, Boolean isMastered, Pageable pageable);

    /**
     * Get vocabulary entries filtered by mastered status with pagination.
     *
     * @param userId     the user's UUID
     * @param isMastered the mastered status to filter by
     * @param pageable   pagination parameters
     * @return page of vocabulary DTOs
     */
    Page<VocabularyDTO> getByUserIdAndMastered(UUID userId, Boolean isMastered, Pageable pageable);
}
