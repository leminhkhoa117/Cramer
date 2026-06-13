package com.cramer.billing.repository;

import com.cramer.billing.domain.SubscriptionTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Spring Data repository for {@link SubscriptionTier} (SPEC-15 §2). */
public interface SubscriptionTierRepository extends JpaRepository<SubscriptionTier, Long> {

    Optional<SubscriptionTier> findByCode(String code);

    List<SubscriptionTier> findByIsActiveTrueOrderByDisplayOrderAscIdAsc();
}
