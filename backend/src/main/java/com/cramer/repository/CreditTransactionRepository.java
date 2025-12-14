package com.cramer.repository;

import com.cramer.entity.CreditTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for CreditTransaction entity.
 */
@Repository
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, Long> {

    /**
     * Find all transactions for a user with pagination.
     */
    Page<CreditTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find recent transactions for a user.
     */
    List<CreditTransaction> findTop10ByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find transactions by type.
     */
    Page<CreditTransaction> findByUserIdAndTypeOrderByCreatedAtDesc(
            UUID userId, CreditTransaction.Type type, Pageable pageable);

    /**
     * Find transactions by category.
     */
    Page<CreditTransaction> findByUserIdAndCategoryOrderByCreatedAtDesc(
            UUID userId, CreditTransaction.Category category, Pageable pageable);

    /**
     * Find transactions within a date range.
     */
    @Query("SELECT t FROM CreditTransaction t WHERE t.userId = :userId " +
           "AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<CreditTransaction> findByUserIdAndDateRange(
            @Param("userId") UUID userId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate);

    /**
     * Calculate total earned in a period.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM CreditTransaction t " +
           "WHERE t.userId = :userId AND t.type = 'EARN' " +
           "AND t.createdAt BETWEEN :startDate AND :endDate")
    int sumEarnedInPeriod(
            @Param("userId") UUID userId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate);

    /**
     * Calculate total spent in a period.
     */
    @Query("SELECT COALESCE(SUM(ABS(t.amount)), 0) FROM CreditTransaction t " +
           "WHERE t.userId = :userId AND t.type = 'SPEND' " +
           "AND t.createdAt BETWEEN :startDate AND :endDate")
    int sumSpentInPeriod(
            @Param("userId") UUID userId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate);
}
