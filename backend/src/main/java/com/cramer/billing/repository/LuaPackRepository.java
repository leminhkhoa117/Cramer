package com.cramer.billing.repository;

import com.cramer.billing.domain.LuaPack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Spring Data repository for {@link LuaPack} (SPEC-15 §4). */
public interface LuaPackRepository extends JpaRepository<LuaPack, Long> {

    List<LuaPack> findByIsActiveTrueOrderByDisplayOrderAscIdAsc();

    Optional<LuaPack> findByCodeAndIsActiveTrue(String code);
}
