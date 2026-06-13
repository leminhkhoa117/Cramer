package com.cramer.assessment.repository;

import com.cramer.assessment.domain.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Spring Data repository for {@link UserAnswer} (SPEC-12). */
public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {

    List<UserAnswer> findByAttemptIdOrderByQuestionIdAsc(Long attemptId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from UserAnswer ua where ua.attemptId = :attemptId")
    void deleteByAttemptId(@Param("attemptId") Long attemptId);

    /**
     * Whether any answer exists for a test, resolved via the FK chain
     * {@code user_answers → questions → sections.test_id}. Native to avoid importing catalog
     * entities (module-boundary safe). Backs the catalog test-deletion guard (SPEC-11 §4.1).
     */
    @Query(value = "select exists(select 1 from user_answers ua "
            + "join questions q on ua.question_id = q.id "
            + "join sections s on q.section_id = s.id "
            + "where s.test_id = :testId)", nativeQuery = true)
    boolean existsForTestId(@Param("testId") long testId);
}
