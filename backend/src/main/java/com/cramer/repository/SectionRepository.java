package com.cramer.repository;

import com.cramer.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Section entity.
 * Provides CRUD operations and custom query methods for exam sections.
 */
@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {

        /**
         * Find all sections by exam source (e.g., "cam17", "cam18").
         * 
         * @param examSource the exam source identifier
         * @return list of sections from that exam source
         */
        List<Section> findByExamSource(String examSource);

        /**
         * Find all sections by exam source and test number.
         * 
         * @param examSource the exam source identifier
         * @param testNumber the test number
         * @return list of sections for that specific test
         */
        List<Section> findByExamSourceAndTestNumber(String examSource, Integer testNumber);

        /**
         * Find all sections by skill type (e.g., "reading", "listening").
         * 
         * @param skill the skill type
         * @return list of sections for that skill
         */
        List<Section> findBySkill(String skill);

        /**
         * Find a specific section by exam source, test number, skill, and part number.
         * 
         * @param examSource the exam source identifier
         * @param testNumber the test number
         * @param skill      the skill type
         * @param partNumber the part number
         * @return Optional containing the section if found
         */
        Optional<Section> findByExamSourceAndTestNumberAndSkillAndPartNumber(
                        String examSource, Integer testNumber, String skill, Integer partNumber);

        /**
         * Find all sections for a specific test and skill.
         * 
         * @param examSource the exam source identifier
         * @param testNumber the test number
         * @param skill      the skill type
         * @return list of sections ordered by part number
         */
        @Query("SELECT s FROM Section s WHERE s.examSource = :examSource " +
                        "AND s.testNumber = :testNumber AND s.skill = :skill " +
                        "ORDER BY s.partNumber ASC")
        List<Section> findSectionsForTest(@Param("examSource") String examSource,
                        @Param("testNumber") Integer testNumber,
                        @Param("skill") String skill);

        /**
         * Count sections by exam source.
         * 
         * @param examSource the exam source identifier
         * @return number of sections from that exam source
         */
        long countByExamSource(String examSource);

        /**
         * Find all sections by exam source, test number, and skill.
         * 
         * @param examSource the exam source identifier
         * @param testNumber the test number
         * @param skill      the skill type
         * @return list of sections
         */
        List<Section> findByExamSourceAndTestNumberAndSkill(String examSource, Integer testNumber, String skill);

        /**
         * Check if a section exists with the given parameters.
         * 
         * @param examSource the exam source identifier
         * @param testNumber the test number
         * @param skill      the skill type
         * @param partNumber the part number
         * @return true if section exists, false otherwise
         */
        boolean existsByExamSourceAndTestNumberAndSkillAndPartNumber(
                        String examSource, Integer testNumber, String skill, Integer partNumber);

        @Query("SELECT DISTINCT s.examSource FROM Section s ORDER BY s.examSource ASC")
        List<String> findDistinctExamSources();

        @Query("SELECT DISTINCT s.examSource FROM Section s WHERE (:search IS NULL OR LOWER(s.examSource) LIKE LOWER(CONCAT('%', :search, '%'))) ORDER BY s.examSource ASC")
        org.springframework.data.domain.Page<String> findDistinctExamSources(
                        org.springframework.data.domain.Pageable pageable, @Param("search") String search);

        @Query("SELECT DISTINCT s.testNumber FROM Section s WHERE s.examSource = :examSource ORDER BY s.testNumber ASC")
        List<Integer> findDistinctTestNumbersByExamSource(@Param("examSource") String examSource);

        // -------------------------------------------------------------------------
        // New methods for Test Hierarchy (using test_id)
        // -------------------------------------------------------------------------

        /**
         * Find all sections by Test ID.
         */
        List<Section> findByIeltsTestId(Long testId);

        /**
         * Find sections by Test ID and Skill.
         */
        List<Section> findByIeltsTestIdAndSkill(Long testId, String skill);

        /**
         * Find sections for a test and skill, ordered by part number.
         */
        @Query("SELECT s FROM Section s WHERE s.ieltsTest.id = :testId " +
                        "AND s.skill = :skill " +
                        "ORDER BY s.partNumber ASC")
        List<Section> findSectionsForTestId(@Param("testId") Long testId,
                        @Param("skill") String skill);

        /**
         * Count sections by Test ID.
         */
        long countByIeltsTestId(Long testId);

        /**
         * Find section by Test ID, Skill, and Part Number.
         * Used to check if a section already exists before creating.
         */
        Optional<Section> findByIeltsTestIdAndSkillAndPartNumber(Long testId, String skill, Integer partNumber);

        /**
         * Find legacy sections without test link.
         */
        List<Section> findByExamSourceAndTestNumberAndIeltsTestIsNull(String examSource, Integer testNumber);

        /**
         * Update status for all sections belonging to a test.
         */
        @Modifying
        @Query("UPDATE Section s SET s.status = :status WHERE s.ieltsTest.id = :testId")
        int updateStatusByTestId(@Param("testId") Long testId, @Param("status") String status);

        /**
         * Count sections grouped by skill for a test.
         * Returns a list of Object[] where [0] = skill (String), [1] = count (Long).
         */
        @Query("SELECT s.skill, COUNT(s) FROM Section s WHERE s.ieltsTest.id = :testId GROUP BY s.skill")
        List<Object[]> countSectionsByTestIdGroupBySkill(@Param("testId") Long testId);

        /**
         * Update status for legacy sections (linked via examSource/testNumber but not
         * FK).
         * This complements updateStatusByTestId for complete coverage.
         */
        @Modifying
        @Query("UPDATE Section s SET s.status = :status WHERE s.examSource = :examSource AND s.testNumber = :testNumber AND s.ieltsTest IS NULL")
        int updateStatusByExamSourceAndTestNumber(
                        @Param("examSource") String examSource,
                        @Param("testNumber") Integer testNumber,
                        @Param("status") String status);
}
