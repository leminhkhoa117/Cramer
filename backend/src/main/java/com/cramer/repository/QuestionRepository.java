package com.cramer.repository;

import com.cramer.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Question entity.
 * Provides CRUD operations and custom query methods for exam questions.
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    /**
     * Find all questions belonging to a specific section.
     * 
     * @param sectionId the section ID
     * @return list of questions ordered by question number
     */
    @Query("SELECT q FROM Question q WHERE q.sectionId = :sectionId ORDER BY q.questionNumber ASC")
    List<Question> findBySectionId(@Param("sectionId") Long sectionId);

    /**
     * Find a specific question by its unique identifier.
     * 
     * @param questionUid the unique question identifier (e.g., "cam17-t1-r-q1")
     * @return Optional containing the question if found
     */
    Optional<Question> findByQuestionUid(String questionUid);

    /**
     * Find all questions of a specific type.
     * 
     * @param questionType the question type (e.g., "FILL_IN_BLANK", "TRUE_FALSE_NOT_GIVEN")
     * @return list of questions of that type
     */
    List<Question> findByQuestionType(String questionType);

    /**
     * Find a specific question within a section by its number.
     * 
     * @param sectionId the section ID
     * @param questionNumber the question number within the section
     * @return Optional containing the question if found
     */
    Optional<Question> findBySectionIdAndQuestionNumber(Long sectionId, Integer questionNumber);

    /**
     * Count total questions in a section.
     * 
     * @param sectionId the section ID
     * @return number of questions in that section
     */
    long countBySectionId(Long sectionId);

    /**
     * Find questions by section ID and type.
     * 
     * @param sectionId the section ID
     * @param questionType the question type
     * @return list of questions matching criteria
     */
    @Query("SELECT q FROM Question q WHERE q.sectionId = :sectionId " +
           "AND q.questionType = :questionType ORDER BY q.questionNumber ASC")
    List<Question> findBySectionIdAndQuestionType(@Param("sectionId") Long sectionId,
                                                   @Param("questionType") String questionType);

    /**
     * Check if a question with given UID exists.
     * 
     * @param questionUid the unique question identifier
     * @return true if question exists, false otherwise
     */
    boolean existsByQuestionUid(String questionUid);

    /**
     * Delete all questions belonging to a section.
     * 
     * @param sectionId the section ID
     */
    void deleteBySectionId(Long sectionId);

    /**
     * Get all distinct question types in the database.
     * 
     * @return list of distinct question types
     */
    @Query("SELECT DISTINCT q.questionType FROM Question q ORDER BY q.questionType")
    List<String> findAllDistinctQuestionTypes();

    /**
     * Count total questions for a given test attempt's properties.
     *
     * @param examSource the source of the exam (e.g., "cam18")
     * @param testNumber the test number (e.g., "1")
     * @param skill the skill (e.g., "reading")
     * @return number of questions matching the criteria
     */
    int countBySection_ExamSourceAndSection_TestNumberAndSection_Skill(String examSource, Integer testNumber, String skill);
}
