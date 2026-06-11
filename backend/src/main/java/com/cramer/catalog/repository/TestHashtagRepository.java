package com.cramer.catalog.repository;

import com.cramer.catalog.domain.TestHashtag;
import com.cramer.catalog.domain.TestHashtagId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Spring Data repository for the {@link TestHashtag} junction (SPEC-11). */
public interface TestHashtagRepository extends JpaRepository<TestHashtag, TestHashtagId> {

    List<TestHashtag> findByIdTestId(Long testId);

    void deleteByIdTestId(Long testId);
}
