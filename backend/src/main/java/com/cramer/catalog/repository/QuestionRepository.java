package com.cramer.catalog.repository;

import com.cramer.catalog.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Spring Data repository for {@link Question} (SPEC-11). */
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findBySectionIdOrderByQuestionNumberAsc(Long sectionId);

    void deleteBySectionId(Long sectionId);
}
