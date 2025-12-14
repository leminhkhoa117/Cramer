package com.cramer.repository;

import com.cramer.entity.UserCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserCredit entity.
 */
@Repository
public interface UserCreditRepository extends JpaRepository<UserCredit, Long> {

    /**
     * Find credits by user ID.
     */
    Optional<UserCredit> findByUserId(UUID userId);

    /**
     * Check if user has credit record.
     */
    boolean existsByUserId(UUID userId);

    /**
     * Get current balance for a user.
     */
    @Query("SELECT COALESCE(c.balance, 0) FROM UserCredit c WHERE c.userId = :userId")
    Optional<Integer> getBalanceByUserId(@Param("userId") UUID userId);

    /**
     * Add credits to user balance (atomic update).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UserCredit c SET c.balance = c.balance + :amount, " +
           "c.lifetimeEarned = c.lifetimeEarned + :amount WHERE c.userId = :userId")
    int addCredits(@Param("userId") UUID userId, @Param("amount") int amount);

    /**
     * Subtract credits from user balance (atomic update).
     * Only succeeds if balance is sufficient.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UserCredit c SET c.balance = c.balance - :amount, " +
           "c.lifetimeSpent = c.lifetimeSpent + :amount " +
           "WHERE c.userId = :userId AND c.balance >= :amount")
    int subtractCredits(@Param("userId") UUID userId, @Param("amount") int amount);
}
