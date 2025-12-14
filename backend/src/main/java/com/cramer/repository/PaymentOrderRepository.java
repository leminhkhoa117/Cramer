package com.cramer.repository;

import com.cramer.entity.PaymentOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for PaymentOrder entity.
 * Handles payment tracking and history queries.
 */
@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    /**
     * Find order by PayOS order code (unique).
     */
    Optional<PaymentOrder> findByOrderCode(Long orderCode);

    /**
     * Find order by PayOS payment link ID.
     */
    Optional<PaymentOrder> findByPaymentLinkId(String paymentLinkId);

    /**
     * Check if order code already exists.
     */
    boolean existsByOrderCode(Long orderCode);

    /**
     * Find all orders for a user, ordered by creation date descending.
     */
    Page<PaymentOrder> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find all orders for a user with a specific status.
     */
    List<PaymentOrder> findByUserIdAndStatus(UUID userId, PaymentOrder.Status status);

    /**
     * Find pending orders for a user (not yet paid).
     */
    @Query("SELECT o FROM PaymentOrder o WHERE o.userId = :userId AND o.status = 'PENDING' ORDER BY o.createdAt DESC")
    List<PaymentOrder> findPendingByUserId(@Param("userId") UUID userId);

    /**
     * Find successful payments for a user.
     */
    @Query("SELECT o FROM PaymentOrder o WHERE o.userId = :userId AND o.status = 'PAID' ORDER BY o.paidAt DESC")
    List<PaymentOrder> findPaidByUserId(@Param("userId") UUID userId);

    /**
     * Count successful payments for a user.
     */
    @Query("SELECT COUNT(o) FROM PaymentOrder o WHERE o.userId = :userId AND o.status = 'PAID'")
    long countPaidByUserId(@Param("userId") UUID userId);

    /**
     * Find pending subscription orders for a specific tier.
     */
    @Query("SELECT o FROM PaymentOrder o WHERE o.userId = :userId AND o.type = 'SUBSCRIPTION' " +
           "AND o.tierId = :tierId AND o.status = 'PENDING'")
    List<PaymentOrder> findPendingSubscriptionByUserAndTier(
            @Param("userId") UUID userId, 
            @Param("tierId") Integer tierId);
}
