package com.cramer.service.implement;

import com.cramer.dto.*;
import com.cramer.entity.CreditTransaction;
import com.cramer.entity.UserCredit;
import com.cramer.entity.UserStreak;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.repository.*;
import com.cramer.service.CreditService;
import com.cramer.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of CreditService.
 * Manages Lúa credits, transactions, and user statistics.
 */
@Service
@Transactional
public class CreditServiceImpl implements CreditService {

    private static final Logger logger = LoggerFactory.getLogger(CreditServiceImpl.class);

    private final UserCreditRepository creditRepository;
    private final CreditTransactionRepository transactionRepository;
    private final UserStreakRepository streakRepository;
    private final VocabularyRepository vocabularyRepository;
    private final SubscriptionService subscriptionService;

    @Autowired
    public CreditServiceImpl(
            UserCreditRepository creditRepository,
            CreditTransactionRepository transactionRepository,
            UserStreakRepository streakRepository,
            VocabularyRepository vocabularyRepository,
            @Lazy SubscriptionService subscriptionService) {
        this.creditRepository = creditRepository;
        this.transactionRepository = transactionRepository;
        this.streakRepository = streakRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.subscriptionService = subscriptionService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserCreditDTO getBalance(UUID userId) {
        logger.info("💰 Fetching credit balance for user: {}", userId);
        
        UserCredit credit = creditRepository.findByUserId(userId)
                .orElseGet(() -> {
                    logger.info("⚠️ No credits found for user {}, initializing", userId);
                    return createDefaultCredits(userId);
                });
        
        return UserCreditDTO.fromEntity(credit);
    }

    @Override
    public CreditTransactionDTO earnCredits(UUID userId, int amount, 
                                            CreditTransaction.Category category, String description) {
        return earnCredits(userId, amount, category, description, null);
    }

    @Override
    public CreditTransactionDTO earnCredits(UUID userId, int amount, 
                                            CreditTransaction.Category category, String description,
                                            String referenceId) {
        logger.info("💰 Earning {} Lúa for user {} - {}", amount, userId, category);
        
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        // Get or create credits
        UserCredit credit = creditRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultCredits(userId));
        
        // Add credits
        credit.addCredits(amount);
        credit = creditRepository.save(credit);
        
        // Create transaction
        CreditTransaction transaction = CreditTransaction.builder()
                .userId(userId)
                .amount(amount)
                .balanceAfter(credit.getBalance())
                .type(CreditTransaction.Type.EARN)
                .category(category)
                .description(description)
                .referenceId(referenceId)
                .build();
        
        transaction = transactionRepository.save(transaction);
        logger.info("✅ Earned {} Lúa for user {}. New balance: {}", amount, userId, credit.getBalance());
        
        return CreditTransactionDTO.fromEntity(transaction);
    }

    @Override
    public CreditTransactionDTO spendCredits(UUID userId, int amount, 
                                             CreditTransaction.Category category, String description) {
        return spendCredits(userId, amount, category, description, null);
    }

    @Override
    public CreditTransactionDTO spendCredits(UUID userId, int amount, 
                                             CreditTransaction.Category category, String description,
                                             String referenceId) {
        logger.info("💸 Spending {} Lúa for user {} - {}", amount, userId, category);
        
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        // Get credits
        UserCredit credit = creditRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No credit record for user: " + userId));
        
        // Check and spend
        credit.spendCredits(amount);
        credit = creditRepository.save(credit);
        
        // Create transaction (negative amount for spending)
        CreditTransaction transaction = CreditTransaction.builder()
                .userId(userId)
                .amount(-amount)
                .balanceAfter(credit.getBalance())
                .type(CreditTransaction.Type.SPEND)
                .category(category)
                .description(description)
                .referenceId(referenceId)
                .build();
        
        transaction = transactionRepository.save(transaction);
        logger.info("✅ Spent {} Lúa for user {}. New balance: {}", amount, userId, credit.getBalance());
        
        return CreditTransactionDTO.fromEntity(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasEnoughCredits(UUID userId, int amount) {
        return creditRepository.findByUserId(userId)
                .map(c -> c.hasEnoughCredits(amount))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CreditTransactionDTO> getTransactionHistory(UUID userId, Pageable pageable) {
        logger.info("📜 Fetching transaction history for user: {}", userId);
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(CreditTransactionDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public UserFullStatsDTO getUserStats(UUID userId) {
        logger.info("📊 Fetching full stats for user: {}", userId);
        
        // Get subscription
        UserSubscriptionDTO subscription = subscriptionService.getUserSubscription(userId);
        
        // Get credits
        UserCreditDTO credits = getBalance(userId);
        
        // Get streak
        UserStreak streak = streakRepository.findByUserId(userId).orElse(null);
        
        // Get vocabulary stats
        long totalVocabulary = vocabularyRepository.countByUserId(userId);
        long masteredVocabulary = vocabularyRepository.countByUserIdAndIsMastered(userId, true);
        
        // Get daily chat remaining
        int dailyChatLimit = subscription.getTier() != null 
                ? subscription.getTier().getDailyChatLimit() : 20;
        // TODO: Calculate actual remaining from chatbot_usage table
        
        return UserFullStatsDTO.builder()
                .userId(userId)
                .currentTier(subscription.getTier())
                .aiGradingsRemaining(subscription.getAiGradingsRemaining())
                .dailyChatRemaining(dailyChatLimit) // TODO: Calculate actual
                .isSubscriptionActive(subscription.getIsActive())
                .luaBalance(credits.getBalance())
                .lifetimeEarned(credits.getLifetimeEarned())
                .lifetimeSpent(credits.getLifetimeSpent())
                .currentStreak(streak != null ? streak.getCurrentStreak() : 0)
                .longestStreak(streak != null ? streak.getLongestStreak() : 0)
                .lastLoginDate(streak != null ? streak.getLastLoginDate() : null)
                .totalVocabulary(totalVocabulary)
                .masteredVocabulary(masteredVocabulary)
                .build();
    }

    @Override
    public UserCreditDTO initializeCredits(UUID userId, int initialAmount) {
        logger.info("🆕 Initializing credits for user {} with {} Lúa", userId, initialAmount);
        
        // Check if already exists
        if (creditRepository.existsByUserId(userId)) {
            logger.info("⚠️ Credits already exist for user {}", userId);
            return getBalance(userId);
        }
        
        // Create credit record
        UserCredit credit = UserCredit.builder()
                .userId(userId)
                .balance(initialAmount)
                .lifetimeEarned(initialAmount)
                .lifetimeSpent(0)
                .build();
        
        credit = creditRepository.save(credit);
        
        // Create initial transaction
        CreditTransaction transaction = CreditTransaction.builder()
                .userId(userId)
                .amount(initialAmount)
                .balanceAfter(credit.getBalance())
                .type(CreditTransaction.Type.EARN)
                .category(CreditTransaction.Category.INITIAL_BONUS)
                .description("Lúa khởi tạo tài khoản")
                .build();
        
        transactionRepository.save(transaction);
        logger.info("✅ Initialized {} Lúa for user {}", initialAmount, userId);
        
        return UserCreditDTO.fromEntity(credit);
    }

    /**
     * Create default credits record for a user.
     */
    private UserCredit createDefaultCredits(UUID userId) {
        UserCredit credit = UserCredit.builder()
                .userId(userId)
                .balance(0)
                .lifetimeEarned(0)
                .lifetimeSpent(0)
                .build();
        return creditRepository.save(credit);
    }
}
