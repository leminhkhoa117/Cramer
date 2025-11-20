package com.cramer.repository;

import com.cramer.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
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
     * @param skill the skill type
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
     * @param skill the skill type
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
     * Check if a section exists with the given parameters.
     * 
     * @param examSource the exam source identifier
     * @param testNumber the test number
     * @param skill the skill type
     * @param partNumber the part number
     * @return true if section exists, false otherwise
     */
    boolean existsByExamSourceAndTestNumberAndSkillAndPartNumber(
            String examSource, Integer testNumber, String skill, Integer partNumber);

    @Query("SELECT DISTINCT s.examSource FROM Section s ORDER BY s.examSource ASC")
    List<String> findDistinctExamSources();

    @Query("SELECT DISTINCT s.examSource FROM Section s WHERE (:search IS NULL OR LOWER(s.examSource) LIKE LOWER(CONCAT('%', :search, '%'))) ORDER BY s.examSource ASC")
    org.springframework.data.domain.Page<String> findDistinctExamSources(org.springframework.data.domain.Pageable pageable, @Param("search") String search);

    @Query("SELECT DISTINCT s.testNumber FROM Section s WHERE s.examSource = :examSource ORDER BY s.testNumber ASC")
    List<Integer> findDistinctTestNumbersByExamSource(@Param("examSource") String examSource);
}
