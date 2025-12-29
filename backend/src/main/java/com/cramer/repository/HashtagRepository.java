package com.cramer.repository;

import com.cramer.entity.Hashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for Hashtag entity.
 * Provides CRUD operations and custom query methods for hashtags.
 */
@Repository
public interface HashtagRepository extends JpaRepository<Hashtag, Long> {

    /**
     * Find a hashtag by its unique code.
     * 
     * @param code the hashtag code (e.g., "environment")
     * @return Optional containing the hashtag if found
     */
    Optional<Hashtag> findByCode(String code);

    /**
     * Check if a hashtag exists with the given code.
     * 
     * @param code the hashtag code
     * @return true if exists, false otherwise
     */
    boolean existsByCode(String code);

    /**
     * Find all active hashtags.
     * 
     * @return list of active hashtags
     */
    List<Hashtag> findByIsActiveTrueOrderByUseCountDesc();

    /**
     * Find hashtags by category.
     * 
     * @param category the category (e.g., "topic", "theme")
     * @return list of hashtags in that category
     */
    List<Hashtag> findByCategoryAndIsActiveTrueOrderByUseCountDesc(String category);

    /**
     * Find hashtags by codes (for bulk lookup).
     * 
     * @param codes set of hashtag codes
     * @return set of matching hashtags
     */
    Set<Hashtag> findByCodeIn(Set<String> codes);

    /**
     * Find hashtags by codes list.
     * 
     * @param codes list of hashtag codes
     * @return list of matching hashtags
     */
    List<Hashtag> findByCodeIn(List<String> codes);

    /**
     * Search hashtags by name or code.
     * 
     * @param searchTerm the search term
     * @return list of matching hashtags
     */
    @Query("SELECT h FROM Hashtag h WHERE h.isActive = true AND (" +
            "LOWER(h.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(h.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "ORDER BY h.useCount DESC")
    List<Hashtag> searchByNameOrCode(@Param("searchTerm") String searchTerm);

    /**
     * Get popular hashtags (most used).
     * 
     * @param limit the maximum number of hashtags to return
     * @return list of popular hashtags
     */
    @Query("SELECT h FROM Hashtag h WHERE h.isActive = true ORDER BY h.useCount DESC")
    List<Hashtag> findPopularHashtags(@Param("limit") int limit);

    /**
     * Increment the use count of a hashtag.
     * 
     * @param hashtagId the hashtag ID
     */
    @Modifying
    @Query("UPDATE Hashtag h SET h.useCount = h.useCount + 1 WHERE h.id = :hashtagId")
    void incrementUseCount(@Param("hashtagId") Long hashtagId);

    /**
     * Decrement the use count of a hashtag.
     * 
     * @param hashtagId the hashtag ID
     */
    @Modifying
    @Query("UPDATE Hashtag h SET h.useCount = CASE WHEN h.useCount > 0 THEN h.useCount - 1 ELSE 0 END WHERE h.id = :hashtagId")
    void decrementUseCount(@Param("hashtagId") Long hashtagId);

    /**
     * Get all distinct categories.
     * 
     * @return list of category names
     */
    @Query("SELECT DISTINCT h.category FROM Hashtag h WHERE h.isActive = true ORDER BY h.category")
    List<String> findDistinctCategories();

    /**
     * Count hashtags by category.
     * 
     * @param category the category
     * @return number of hashtags
     */
    long countByCategoryAndIsActiveTrue(String category);
}
