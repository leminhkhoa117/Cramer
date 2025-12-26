package com.cramer.repository;

import com.cramer.entity.TestSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TestSet entity.
 * Provides CRUD operations and custom query methods for test sets.
 */
@Repository
public interface TestSetRepository extends JpaRepository<TestSet, Long> {

    /**
     * Find a test set by its unique code.
     * @param code the test set code (e.g., "cam17")
     * @return Optional containing the test set if found
     */
    Optional<TestSet> findByCode(String code);

    /**
     * Check if a test set exists with the given code.
     * @param code the test set code
     * @return true if exists, false otherwise
     */
    boolean existsByCode(String code);

    /**
     * Find all test sets ordered by display order.
     * @return list of test sets ordered by displayOrder ASC
     */
    List<TestSet> findAllByOrderByDisplayOrderAsc();

    /**
     * Find all published test sets ordered by display order.
     * @return list of published test sets
     */
    List<TestSet> findByIsPublishedTrueOrderByDisplayOrderAsc();

    /**
     * Find test sets by source type.
     * @param sourceType the source type (e.g., "cambridge", "custom", "ai_generated")
     * @return list of test sets with that source type
     */
    List<TestSet> findBySourceTypeOrderByDisplayOrderAsc(String sourceType);

    /**
     * Find all published test sets.
     * @return list of published test sets
     */
    List<TestSet> findByIsPublishedTrue();

    /**
     * Find test sets by source type.
     * @param sourceType the source type
     * @return list of test sets
     */
    List<TestSet> findBySourceType(String sourceType);

    /**
     * Count the number of tests in a test set.
     * @param testSetId the test set ID
     * @return number of tests
     */
    @Query("SELECT COUNT(t) FROM IeltsTest t WHERE t.testSet.id = :testSetId")
    long countTestsByTestSetId(@Param("testSetId") Long testSetId);

    /**
     * Count the number of published tests in a test set.
     * @param testSetId the test set ID
     * @return number of published tests
     */
    @Query("SELECT COUNT(t) FROM IeltsTest t WHERE t.testSet.id = :testSetId AND t.isPublished = true")
    long countPublishedTestsByTestSetId(@Param("testSetId") Long testSetId);

    /**
     * Get the maximum display order value.
     * @return max display order or 0 if no test sets exist
     */
    @Query("SELECT COALESCE(MAX(ts.displayOrder), 0) FROM TestSet ts")
    int findMaxDisplayOrder();

    /**
     * Update display orders for reordering.
     * @param id the test set ID
     * @param displayOrder the new display order
     */
    @Modifying
    @Query("UPDATE TestSet ts SET ts.displayOrder = :displayOrder WHERE ts.id = :id")
    void updateDisplayOrder(@Param("id") Long id, @Param("displayOrder") Integer displayOrder);

    /**
     * Find test sets by search term (matches code, nameVi, or nameEn).
     * @param searchTerm the search term
     * @return list of matching test sets
     */
    @Query("SELECT ts FROM TestSet ts WHERE " +
           "LOWER(ts.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(ts.nameVi) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(ts.nameEn) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY ts.displayOrder ASC")
    List<TestSet> searchByCodeOrName(@Param("searchTerm") String searchTerm);
}
