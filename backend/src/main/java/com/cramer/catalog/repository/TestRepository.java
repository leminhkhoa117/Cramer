package com.cramer.catalog.repository;

import com.cramer.catalog.domain.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Spring Data repository for {@link Test} (SPEC-11). */
public interface TestRepository extends JpaRepository<Test, Long> {

    List<Test> findBySetIdOrderByTestNumberAsc(Long setId);

    List<Test> findBySetIdAndIsPublishedTrueOrderByTestNumberAsc(Long setId);

    Optional<Test> findBySetIdAndTestNumber(Long setId, Integer testNumber);

    long countBySetId(Long setId);

    @Query("select coalesce(max(t.testNumber), 0) from Test t where t.setId = :setId")
    int maxTestNumber(@Param("setId") Long setId);
}
