package com.cramer.billing.repository;

import com.cramer.billing.domain.CreditTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data repository for {@link CreditTransaction} (SPEC-15 §3). */
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, Long> {

    /** Idempotency probe: a prior transaction for the same (user, reference, category). */
    Optional<CreditTransaction> findFirstByUserIdAndReferenceIdAndCategory(
            UUID userId, String referenceId, String category);

    Page<CreditTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
