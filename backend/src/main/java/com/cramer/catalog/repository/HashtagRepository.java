package com.cramer.catalog.repository;

import com.cramer.catalog.domain.Hashtag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Spring Data repository for {@link Hashtag} (SPEC-11). Active = not soft-deleted. */
public interface HashtagRepository extends JpaRepository<Hashtag, Long> {

    Optional<Hashtag> findByCode(String code);

    List<Hashtag> findByCodeIn(List<String> codes);

    List<Hashtag> findByIsActiveTrueOrderByUseCountDesc();

    List<Hashtag> findByCategoryAndIsActiveTrueOrderByUseCountDesc(String category);

    List<Hashtag> findByIsActiveTrueAndNameContainingIgnoreCaseOrderByUseCountDesc(String name);
}
