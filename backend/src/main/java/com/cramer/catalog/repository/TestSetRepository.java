package com.cramer.catalog.repository;

import com.cramer.catalog.domain.TestSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Spring Data repository for {@link TestSet} (SPEC-11). */
public interface TestSetRepository extends JpaRepository<TestSet, Long> {

    Optional<TestSet> findByCode(String code);

    boolean existsByCode(String code);

    List<TestSet> findByIsPublishedTrueOrderByDisplayOrderAscIdAsc();

    List<TestSet> findAllByOrderByDisplayOrderAscIdAsc();
}
