package com.cramer.repository;

import com.cramer.entity.SubscriptionTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SubscriptionTier entity.
 */
@Repository
public interface SubscriptionTierRepository extends JpaRepository<SubscriptionTier, Long> {

    /**
     * Find a tier by its unique code.
     */
    Optional<SubscriptionTier> findByCode(String code);

    /**
     * Find all active tiers ordered by display order.
     */
    List<SubscriptionTier> findByIsActiveTrueOrderByDisplayOrderAsc();

    /**
     * Find the free tier (cramerie).
     */
    @Query("SELECT t FROM SubscriptionTier t WHERE t.code = 'cramerie' AND t.isActive = true")
    Optional<SubscriptionTier> findFreeTier();

    /**
     * Check if a tier code exists.
     */
    boolean existsByCode(String code);
}
