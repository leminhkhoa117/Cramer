package com.cramer.service.implement;

import com.cramer.dto.*;
import com.cramer.entity.CreditTransaction;
import com.cramer.entity.UserCredit;

import com.cramer.repository.*;
import com.cramer.repository.ChatbotUsageRepository;
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

import java.util.Objects;
import java.util.UUID;

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
    private final VocabularyRepository vocabularyRepository;
    private final ChatbotUsageRepository chatbotUsageRepository;
    private final SubscriptionService subscriptionService;

    @Autowired
    public CreditServiceImpl(
            UserCreditRepository creditRepository,
            CreditTransactionRepository transactionRepository,
            VocabularyRepository vocabularyRepository,
            ChatbotUsageRepository chatbotUsageRepository,
            @Lazy SubscriptionService subscriptionService) {
        this.creditRepository = creditRepository;
        this.transactionRepository = transactionRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.chatbotUsageRepository = chatbotUsageRepository;
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
    @SuppressWarnings("null")
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

        transaction = Objects.requireNonNull(transactionRepository.save(transaction));
        logger.info("✅ Earned {} Lúa for user {}. New balance: {}", amount, userId, credit.getBalance());

        return CreditTransactionDTO.fromEntity(transaction);
    }

    @Override
    public CreditTransactionDTO spendCredits(UUID userId, int amount,
            CreditTransaction.Category category, String description) {
        return spendCredits(userId, amount, category, description, null);
    }

    @Override
    @SuppressWarnings("null")
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

        transaction = Objects.requireNonNull(transactionRepository.save(transaction));
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

        // Get vocabulary stats
        long totalVocabulary = vocabularyRepository.countByUserId(userId);
        long masteredVocabulary = vocabularyRepository.countByUserIdAndIsMastered(userId, true);

        // Get daily chat remaining
        int dailyChatLimit = subscription.getTier() != null
                ? subscription.getTier().getDailyChatLimit()
                : 20;

        // Calculate actual remaining from chatbot_usage table
        int usedToday = chatbotUsageRepository.findByUserIdAndUsageDate(userId, java.time.LocalDate.now())
                .map(usage -> usage.getMessagesUsed())
                .orElse(0);
        int dailyChatRemaining = Math.max(0, dailyChatLimit - usedToday);

        return UserFullStatsDTO.builder()
                .userId(userId)
                .currentTier(subscription.getTier())
                .attemptAisRemaining(subscription.getAttemptAisRemaining())
                .dailyChatRemaining(dailyChatRemaining)
                .isSubscriptionActive(subscription.getIsActive())
                .luaBalance(credits.getBalance())
                .lifetimeEarned(credits.getLifetimeEarned())
                .lifetimeSpent(credits.getLifetimeSpent())
                .currentStreak(0)  // Streak feature removed
                .longestStreak(0)  // Streak feature removed
                .lastLoginDate(null)  // Streak feature removed
                .totalVocabulary(totalVocabulary)
                .masteredVocabulary(masteredVocabulary)
                .build();
    }

    @Override
    @SuppressWarnings("null")
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

        credit = Objects.requireNonNull(creditRepository.save(credit));

        // Create initial transaction
        CreditTransaction transaction = CreditTransaction.builder()
                .userId(userId)
                .amount(initialAmount)
                .balanceAfter(credit.getBalance())
                .type(CreditTransaction.Type.EARN)
                .category(CreditTransaction.Category.INITIAL_BONUS)
                .description("Lúa khởi tạo tài khoản")
                .build();

        transactionRepository.save(Objects.requireNonNull(transaction));
        logger.info("✅ Initialized {} Lúa for user {}", initialAmount, userId);

        return UserCreditDTO.fromEntity(credit);
    }

    /**
     * Create default credits record for a user.
     */
    @SuppressWarnings("null")
    private UserCredit createDefaultCredits(UUID userId) {
        UserCredit credit = UserCredit.builder()
                .userId(userId)
                .balance(0)
                .lifetimeEarned(0)
                .lifetimeSpent(0)
                .build();
        return Objects.requireNonNull(creditRepository.save(credit));
    }
}
