package com.cramer.billing.service;

import com.cramer.billing.domain.CreditCategory;
import com.cramer.billing.domain.SubscriptionTier;
import com.cramer.billing.domain.UserSubscription;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * AI-grading billing (SPEC-15 §6) implementing {@link UsageBillingPort}. A premium user with
 * remaining {@code included_ai_gradings} grades for free (counter increments); otherwise the
 * grading costs the tier AI overage (canonical <strong>20 Lúa</strong>), charged after success
 * and idempotent by reference.
 */
@Service
public class UsageBillingService implements UsageBillingPort {

    private final SubscriptionService subscriptions;
    private final CreditService credits;

    public UsageBillingService(SubscriptionService subscriptions, CreditService credits) {
        this.subscriptions = subscriptions;
        this.credits = credits;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canGrade(UUID userId) {
        UserSubscription sub = subscriptions.getOrCreateActive(userId);
        SubscriptionTier tier = subscriptions.tierOf(sub);
        if (coveredByAllowance(sub, tier)) {
            return true;
        }
        return credits.balance(userId) >= tier.getAttemptAiOverageCost();
    }

    @Override
    @Transactional
    public void chargeAiGrading(UUID userId, String reference) {
        UserSubscription sub = subscriptions.getOrCreateActive(userId);
        SubscriptionTier tier = subscriptions.tierOf(sub);
        if (coveredByAllowance(sub, tier)) {
            sub.setAttemptAisUsed(sub.getAttemptAisUsed() + 1);
            subscriptions.save(sub);
            return;
        }
        // Over the monthly allowance: charge the AI overage (throws 402 if unaffordable).
        credits.spend(userId, tier.getAttemptAiOverageCost(), CreditCategory.AI_GRADING, reference, "AI essay grading");
        sub.setAttemptAisUsed(sub.getAttemptAisUsed() + 1);
        subscriptions.save(sub);
    }

    @Override
    @Transactional
    public void refund(UUID userId, String reference) {
        // Only reverse a real charge; a grading covered by the allowance left no spend row.
        if (credits.hasTransaction(userId, reference, CreditCategory.AI_GRADING)) {
            UserSubscription sub = subscriptions.getOrCreateActive(userId);
            SubscriptionTier tier = subscriptions.tierOf(sub);
            credits.refund(userId, tier.getAttemptAiOverageCost(), CreditCategory.AI_GRADING,
                    "refund_" + reference, "AI grading refund");
        }
    }

    private boolean coveredByAllowance(UserSubscription sub, SubscriptionTier tier) {
        return tier.isPremium() && sub.getAttemptAisUsed() < tier.getIncludedAiGradings();
    }
}
