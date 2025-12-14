package com.cramer.repository;

import com.cramer.entity.LuaPack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Lúa pack definitions.
 */
@Repository
public interface LuaPackRepository extends JpaRepository<LuaPack, Long> {

    /**
     * Find all active packs ordered by display order.
     */
    @Query("SELECT p FROM LuaPack p WHERE p.isActive = true ORDER BY p.displayOrder ASC")
    List<LuaPack> findAllActive();

    /**
     * Find a pack by code.
     */
    Optional<LuaPack> findByCode(String code);

    /**
     * Find a pack by code if active.
     */
    Optional<LuaPack> findByCodeAndIsActiveTrue(String code);
}
