package com.cramer.repository;

import com.cramer.entity.IeltsTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for IeltsTest entity.
 * Provides CRUD operations and custom query methods for individual tests.
 */
@Repository
public interface IeltsTestRepository extends JpaRepository<IeltsTest, Long> {

       /**
        * Find all tests belonging to a test set.
        * 
        * @param testSetId the test set ID
        * @return list of tests ordered by test number
        */
       List<IeltsTest> findByTestSetIdOrderByTestNumberAsc(Long testSetId);

       /**
        * Find all tests belonging to a test set with custom sorting.
        * 
        * @param testSetId the test set ID
        * @param sort      the sort specification
        * @return list of tests ordered as specified
        */
       List<IeltsTest> findByTestSetId(Long testSetId, org.springframework.data.domain.Sort sort);

       /**
        * Find a test by test set ID and test number.
        * 
        * @param testSetId  the test set ID
        * @param testNumber the test number
        * @return Optional containing the test if found
        */
       Optional<IeltsTest> findByTestSetIdAndTestNumber(Long testSetId, Integer testNumber);

       /**
        * Find a test by test set code and test number.
        * 
        * @param setCode    the test set code (e.g., "cam17")
        * @param testNumber the test number
        * @return Optional containing the test if found
        */
       @Query("SELECT t FROM IeltsTest t WHERE t.testSet.code = :setCode AND t.testNumber = :testNumber")
       Optional<IeltsTest> findBySetCodeAndTestNumber(@Param("setCode") String setCode,
                     @Param("testNumber") Integer testNumber);

       /**
        * Check if a test exists with the given test set ID and test number.
        * 
        * @param testSetId  the test set ID
        * @param testNumber the test number
        * @return true if exists, false otherwise
        */
       boolean existsByTestSetIdAndTestNumber(Long testSetId, Integer testNumber);

       /**
        * Find published tests in a test set.
        * 
        * @param testSetId the test set ID
        * @return list of published tests
        */
       List<IeltsTest> findByTestSetIdAndIsPublishedTrueOrderByTestNumberAsc(Long testSetId);

       /**
        * Get the maximum test number in a test set.
        * 
        * @param testSetId the test set ID
        * @return max test number or null if no tests exist
        */
       @Query("SELECT MAX(t.testNumber) FROM IeltsTest t WHERE t.testSet.id = :testSetId")
       Integer findMaxTestNumberByTestSetId(@Param("testSetId") Long testSetId);

       /**
        * Count sections by skill for a test.
        * 
        * @param testId the test ID
        * @param skill  the skill type
        * @return section count
        */
       @Query("SELECT COUNT(s) FROM Section s WHERE s.examSource = " +
                     "(SELECT ts.code FROM TestSet ts JOIN ts.tests t WHERE t.id = :testId) " +
                     "AND s.testNumber = (SELECT t.testNumber FROM IeltsTest t WHERE t.id = :testId) " +
                     "AND s.skill = :skill")
       long countSectionsByTestIdAndSkill(@Param("testId") Long testId, @Param("skill") String skill);

       /**
        * Find tests with specific hashtag.
        * 
        * @param hashtagId the hashtag ID
        * @return list of tests with that hashtag
        */
       @Query("SELECT t FROM IeltsTest t JOIN t.hashtags h WHERE h.id = :hashtagId ORDER BY t.testSet.displayOrder, t.testNumber")
       List<IeltsTest> findByHashtagId(@Param("hashtagId") Long hashtagId);

       /**
        * Find tests by difficulty level.
        * 
        * @param difficulty the difficulty level
        * @return list of tests
        */
       List<IeltsTest> findByDifficultyOrderByTestSetIdAscTestNumberAsc(String difficulty);

       /**
        * Search tests by name.
        * 
        * @param searchTerm the search term
        * @return list of matching tests
        */
       @Query("SELECT t FROM IeltsTest t WHERE " +
                     "LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                     "LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
                     "ORDER BY t.testSet.displayOrder, t.testNumber")
       List<IeltsTest> searchByName(@Param("searchTerm") String searchTerm);

       /**
        * Count tests by test set code.
        * 
        * @param setCode the test set code
        * @return number of tests
        */
       @Query("SELECT COUNT(t) FROM IeltsTest t WHERE t.testSet.code = :setCode")
       long countBySetCode(@Param("setCode") String setCode);
}
