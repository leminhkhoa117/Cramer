package com.cramer.catalog.repository;

import com.cramer.catalog.domain.Section;
import com.cramer.catalog.domain.SectionStatus;
import com.cramer.platform.common.ielts.Skill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Spring Data repository for {@link Section} (SPEC-11). Supports both the FK path and the legacy
 * {@code exam_source}/{@code test_number} lookup shim (SPEC-11 §1.1). */
public interface SectionRepository extends JpaRepository<Section, Long> {

    // Legacy lookup shim (test delivery keys on exam_source/test_number/skill)
    List<Section> findByExamSourceAndTestNumberAndSkillAndStatusOrderByPartNumberAsc(
            String examSource, Integer testNumber, Skill skill, SectionStatus status);

    // FK path
    List<Section> findByTestIdAndSkillAndStatusOrderByPartNumberAsc(
            Long testId, Skill skill, SectionStatus status);

    List<Section> findByTestIdOrderByPartNumberAsc(Long testId);

    List<Section> findByTestId(Long testId);

    java.util.Optional<Section> findByTestIdAndSkillAndPartNumber(Long testId, Skill skill, Integer partNumber);

    void deleteByTestId(Long testId);

    /** Publish cascade (SPEC-11 §4.1): set status for all FK-linked sections of a test. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Section s set s.status = :status where s.testId = :testId")
    int updateStatusByTestId(@Param("testId") Long testId, @Param("status") SectionStatus status);

    /** Publish cascade for legacy sections keyed by exam_source/test_number (SPEC-11 §4.1). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Section s set s.status = :status where s.examSource = :examSource and s.testNumber = :testNumber")
    int updateStatusByExam(@Param("examSource") String examSource,
                           @Param("testNumber") Integer testNumber,
                           @Param("status") SectionStatus status);

    // --- Course browsing (published only; SPEC-11 §3) ---

    @Query(value = "select distinct s.examSource from Section s "
            + "where s.status = com.cramer.catalog.domain.SectionStatus.PUBLISHED and s.examSource is not null "
            + "and (:search is null or lower(s.examSource) like lower(concat('%', :search, '%'))) "
            + "order by s.examSource",
            countQuery = "select count(distinct s.examSource) from Section s "
            + "where s.status = com.cramer.catalog.domain.SectionStatus.PUBLISHED and s.examSource is not null "
            + "and (:search is null or lower(s.examSource) like lower(concat('%', :search, '%')))")
    Page<String> findDistinctPublishedExamSources(@Param("search") String search, Pageable pageable);

    @Query("select distinct s.testNumber from Section s "
            + "where s.status = com.cramer.catalog.domain.SectionStatus.PUBLISHED "
            + "and s.examSource = :examSource and s.testNumber is not null order by s.testNumber")
    List<Integer> findDistinctPublishedTestNumbers(@Param("examSource") String examSource);
}
