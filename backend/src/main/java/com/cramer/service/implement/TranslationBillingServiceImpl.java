package com.cramer.service.implement;

import com.cramer.entity.CreditTransaction;
import com.cramer.entity.SubscriptionTier;
import com.cramer.entity.TranslationUsage;
import com.cramer.entity.UserSubscription;
import com.cramer.repository.TranslationUsageRepository;
import com.cramer.repository.UserSubscriptionRepository;
import com.cramer.service.CreditService;
import com.cramer.service.TranslationBillingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementation of TranslationBillingService.
 * Handles translation quota checking and Lúa billing.
 */
@Service
@Transactional
public class TranslationBillingServiceImpl implements TranslationBillingService {

    private static final Logger logger = LoggerFactory.getLogger(TranslationBillingServiceImpl.class);

    // Default overage cost if not set in tier
    private static final int DEFAULT_TRANSLATION_OVERAGE_COST = 1;

    private final TranslationUsageRepository translationUsageRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final CreditService creditService;

    @Autowired
    public TranslationBillingServiceImpl(
            TranslationUsageRepository translationUsageRepository,
            UserSubscriptionRepository subscriptionRepository,
            @Lazy CreditService creditService) {
        this.translationUsageRepository = translationUsageRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.creditService = creditService;
    }

    @Override
    @Transactional(readOnly = true)
    public TranslationBillingResult preCheckTranslation(UUID userId) {
        logger.debug("🔍 Pre-checking translation for user: {}", userId);
        return checkTranslationInternal(userId, false);
    }

    @Override
    public TranslationBillingResult processTranslationBilling(UUID userId) {
        logger.info("💳 Processing translation billing for user: {}", userId);
        return checkTranslationInternal(userId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public int getRemainingTranslations(UUID userId) {
        UserSubscription sub = subscriptionRepository.findActiveByUserId(userId).orElse(null);
        if (sub == null || sub.getTier() == null) {
            return 0;
        }

        SubscriptionTier tier = sub.getTier();
        Integer limit = tier.getMonthlyTranslationLimit();

        if (limit == null || limit < 0) {
            return -1; // Unlimited
        }

        int used = getCurrentMonthUsage(userId);
        return Math.max(0, limit - used);
    }

    // ===== PRIVATE HELPERS =====

    private TranslationBillingResult checkTranslationInternal(UUID userId, boolean processCharge) {
        // Get user's subscription and tier
        UserSubscription sub = subscriptionRepository.findActiveByUserId(userId).orElse(null);
        if (sub == null || sub.getTier() == null) {
            logger.warn("⚠️ No subscription found for user {}", userId);
            return TranslationBillingResult.blocked("Không tìm thấy gói đăng ký. Vui lòng đăng nhập lại.");
        }

        SubscriptionTier tier = sub.getTier();
        Integer limit = tier.getMonthlyTranslationLimit();
        int overageCost = tier.getTranslationOverageCost() != null
                ? tier.getTranslationOverageCost()
                : DEFAULT_TRANSLATION_OVERAGE_COST;

        // Check if unlimited
        if (limit != null && limit < 0) {
            logger.debug("⭐ User {} has unlimited translations", userId);
            if (processCharge) {
                incrementUsage(userId);
            }
            return TranslationBillingResult.allowed(-1);
        }

        // Get current usage
        int used = getCurrentMonthUsage(userId);
        int remaining = (limit != null) ? Math.max(0, limit - used) : 0;

        // Within quota
        if (remaining > 0) {
            logger.debug("✅ User {} within translation quota ({}/{})", userId, used, limit);
            if (processCharge) {
                incrementUsage(userId);
            }
            return TranslationBillingResult.allowed(remaining - 1);
        }

        // Over quota - need to charge Lúa
        logger.info("⚠️ User {} exceeded translation quota, checking Lúa balance", userId);

        if (!creditService.hasEnoughCredits(userId, overageCost)) {
            logger.warn("❌ User {} has insufficient Lúa ({} required)", userId, overageCost);
            return TranslationBillingResult.blocked(
                    "Đã hết lượt dịch miễn phí trong tháng. Cần " + overageCost + " Lúa để tiếp tục.");
        }

        if (processCharge) {
            // Charge Lúa
            creditService.spendCredits(userId, overageCost,
                    CreditTransaction.Category.AI_GRADING,
                    "Dịch từ vựng (vượt hạn mức tháng)");
            incrementUsage(userId);
            logger.info("💰 Charged {} Lúa for translation overage, user {}", overageCost, userId);
            return TranslationBillingResult.charged(overageCost, 0);
        }

        // Pre-check: would be charged
        return new TranslationBillingResult(true, false, overageCost, 0,
                "Sẽ trừ " + overageCost + " Lúa");
    }

    /**
     * Get current month's translation usage.
     */
    private int getCurrentMonthUsage(UUID userId) {
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
        return translationUsageRepository.findByUserIdAndUsageMonth(userId, firstOfMonth)
                .map(TranslationUsage::getTranslationsUsed)
                .orElse(0);
    }

    /**
     * Increment translation usage for current month.
     * Creates record if doesn't exist.
     */
    private void incrementUsage(UUID userId) {
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);

        // Try to increment existing
        int updated = translationUsageRepository.incrementTranslationsUsed(userId, firstOfMonth);

        if (updated == 0) {
            // No existing record, create new
            try {
                TranslationUsage usage = TranslationUsage.builder()
                        .userId(userId)
                        .usageMonth(firstOfMonth)
                        .translationsUsed(1)
                        .build();
                translationUsageRepository.save(Objects.requireNonNull(usage));
                logger.debug("🆕 Created new translation usage record for user {}", userId);
            } catch (DataIntegrityViolationException e) {
                // Race condition - record was created by another thread
                translationUsageRepository.incrementTranslationsUsed(userId, firstOfMonth);
                logger.debug("🔄 Incremented existing translation usage (race condition handled)");
            }
        }
    }
}
