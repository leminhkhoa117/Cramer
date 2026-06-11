package com.cramer.billing.service;

import com.cramer.billing.domain.CreditCategory;
import com.cramer.billing.domain.SubscriptionTier;
import com.cramer.billing.domain.TranslationUsage;
import com.cramer.billing.domain.UserSubscription;
import com.cramer.billing.repository.TranslationUsageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Translation billing (SPEC-15 §6) implementing {@link TranslationBillingPort}. Monthly usage in
 * {@code translation_usage} against the tier {@code monthly_translation_limit} ({@code < 0} =
 * unlimited). Within allowance → counter++; over allowance → charge tier
 * {@code translation_overage_cost} (category {@code VOCABULARY_TRANSLATION}). Charged after success.
 */
@Service
public class TranslationBillingService implements TranslationBillingPort {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final SubscriptionService subscriptions;
    private final TranslationUsageRepository usage;
    private final CreditService credits;

    public TranslationBillingService(SubscriptionService subscriptions,
                                     TranslationUsageRepository usage,
                                     CreditService credits) {
        this.subscriptions = subscriptions;
        this.usage = usage;
        this.credits = credits;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canTranslate(UUID userId) {
        UserSubscription sub = subscriptions.getOrCreateActive(userId);
        SubscriptionTier tier = subscriptions.tierOf(sub);
        if (isUnlimited(tier)) {
            return true;
        }
        int used = usage.findByUserIdAndUsageMonth(userId, month())
                .map(TranslationUsage::getTranslationsUsed).orElse(0);
        if (used < tier.getMonthlyTranslationLimit()) {
            return true;
        }
        return credits.balance(userId) >= tier.getTranslationOverageCost();
    }

    @Override
    @Transactional
    public void chargeTranslation(UUID userId, String reference) {
        UserSubscription sub = subscriptions.getOrCreateActive(userId);
        SubscriptionTier tier = subscriptions.tierOf(sub);
        TranslationUsage row = lockOrCreate(userId);

        if (!isUnlimited(tier) && row.getTranslationsUsed() >= tier.getMonthlyTranslationLimit()) {
            credits.spend(userId, tier.getTranslationOverageCost(), CreditCategory.VOCABULARY_TRANSLATION,
                    reference, "Translation overage");
        }
        row.setTranslationsUsed(row.getTranslationsUsed() + 1);
        usage.save(row);
    }

    private TranslationUsage lockOrCreate(UUID userId) {
        return usage.findForUpdate(userId, month()).orElseGet(() -> {
            TranslationUsage t = new TranslationUsage();
            t.setUserId(userId);
            t.setUsageMonth(month());
            t.setTranslationsUsed(0);
            return usage.saveAndFlush(t);
        });
    }

    private boolean isUnlimited(SubscriptionTier tier) {
        return tier.getMonthlyTranslationLimit() != null && tier.getMonthlyTranslationLimit() < 0;
    }

    private LocalDate month() {
        return LocalDate.now(ZONE).withDayOfMonth(1);
    }
}
