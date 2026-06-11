package com.cramer.billing.repository;

import com.cramer.billing.domain.PaymentOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** Spring Data repository for {@link PaymentOrder} (SPEC-15 §8). */
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByOrderCode(Long orderCode);

    /** Lock the order row so a duplicate concurrent webhook cannot grant twice (SPEC-15 §8). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PaymentOrder o where o.orderCode = :orderCode")
    Optional<PaymentOrder> findByOrderCodeForUpdate(@Param("orderCode") Long orderCode);

    Page<PaymentOrder> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
