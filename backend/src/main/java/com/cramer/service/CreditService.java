package com.cramer.service;

import com.cramer.dto.CreditTransactionDTO;
import com.cramer.dto.UserCreditDTO;
import com.cramer.dto.UserFullStatsDTO;
import com.cramer.entity.CreditTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for credit (Lúa) operations.
 * Manages user credits, transactions, and balance.
 */
public interface CreditService {

    /**
     * Get user's current credit balance.
     *
     * @param userId the user's UUID
     * @return credit DTO with balance and lifetime stats
     */
    UserCreditDTO getBalance(UUID userId);

    /**
     * Earn credits (add to balance).
     *
     * @param userId      the user's UUID
     * @param amount      the amount to add (positive)
     * @param category    the earning category
     * @param description description of the transaction
     * @return the transaction DTO
     */
    CreditTransactionDTO earnCredits(UUID userId, int amount, 
                                     CreditTransaction.Category category, String description);

    /**
     * Earn credits with reference ID.
     *
     * @param userId      the user's UUID
     * @param amount      the amount to add (positive)
     * @param category    the earning category
     * @param description description of the transaction
     * @param referenceId external reference (order ID, achievement ID, etc.)
     * @return the transaction DTO
     */
    CreditTransactionDTO earnCredits(UUID userId, int amount, 
                                     CreditTransaction.Category category, String description, 
                                     String referenceId);

    /**
     * Spend credits (subtract from balance).
     *
     * @param userId      the user's UUID
     * @param amount      the amount to spend (positive)
     * @param category    the spending category
     * @param description description of the transaction
     * @return the transaction DTO
     * @throws IllegalStateException if insufficient balance
     */
    CreditTransactionDTO spendCredits(UUID userId, int amount, 
                                      CreditTransaction.Category category, String description);

    /**
     * Spend credits with reference ID.
     *
     * @param userId      the user's UUID
     * @param amount      the amount to spend (positive)
     * @param category    the spending category
     * @param description description of the transaction
     * @param referenceId external reference (order ID, etc.)
     * @return the transaction DTO
     * @throws IllegalStateException if insufficient balance
     */
    CreditTransactionDTO spendCredits(UUID userId, int amount, 
                                      CreditTransaction.Category category, String description,
                                      String referenceId);

    /**
     * Check if user has enough credits.
     *
     * @param userId the user's UUID
     * @param amount the amount to check
     * @return true if balance >= amount
     */
    boolean hasEnoughCredits(UUID userId, int amount);

    /**
     * Get transaction history with pagination.
     *
     * @param userId   the user's UUID
     * @param pageable pagination parameters
     * @return page of transaction DTOs
     */
    Page<CreditTransactionDTO> getTransactionHistory(UUID userId, Pageable pageable);

    /**
     * Get aggregated user stats (subscription, credits, streak, achievements).
     *
     * @param userId the user's UUID
     * @return full stats DTO
     */
    UserFullStatsDTO getUserStats(UUID userId);

    /**
     * Initialize credits for a new user.
     *
     * @param userId        the user's UUID
     * @param initialAmount the initial bonus amount
     * @return the credit DTO
     */
    UserCreditDTO initializeCredits(UUID userId, int initialAmount);
}
