package com.cramer.writing.repository;

import com.cramer.writing.domain.WritingSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Spring Data repository for {@link WritingSubmission} (SPEC-13). */
public interface WritingSubmissionRepository extends JpaRepository<WritingSubmission, Long> {

    List<WritingSubmission> findByAttemptIdOrderByTaskNumberAsc(Long attemptId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from WritingSubmission w where w.attemptId = :attemptId")
    void deleteByAttemptId(@Param("attemptId") Long attemptId);
}
