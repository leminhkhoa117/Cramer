package com.cramer.billing.service;

import com.cramer.billing.domain.CreditCategory;
import com.cramer.billing.domain.SubscriptionTier;
import com.cramer.billing.domain.UserSubscription;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Chat billing (SPEC-15 §6) implementing {@link ChatBillingPort}. The monthly subscription
 * counter {@code chatbot_used} against the tier {@code chatbot_monthly_limit} is the source of
 * truth ({@code < 0} = unlimited). Within allowance → counter++; over allowance → charge tier
 * {@code chatbot_overage_cost} (category {@code CHAT_EXTENSION}). Charged after a successful reply.
 */
@Service
public class ChatBillingService implements ChatBillingPort {

    private final SubscriptionService subscriptions;
    private final CreditService credits;

    public ChatBillingService(SubscriptionService subscriptions, CreditService credits) {
        this.subscriptions = subscriptions;
        this.credits = credits;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canChat(UUID userId) {
        UserSubscription sub = subscriptions.getOrCreateActive(userId);
        SubscriptionTier tier = subscriptions.tierOf(sub);
        if (isUnlimited(tier) || withinAllowance(sub, tier)) {
            return true;
        }
        return credits.balance(userId) >= tier.getChatbotOverageCost();
    }

    @Override
    @Transactional(readOnly = true)
    public int remaining(UUID userId) {
        UserSubscription sub = subscriptions.getOrCreateActive(userId);
        SubscriptionTier tier = subscriptions.tierOf(sub);
        if (isUnlimited(tier)) {
            return -1;
        }
        return Math.max(0, tier.getChatbotMonthlyLimit() - sub.getChatbotUsed());
    }

    @Override
    @Transactional
    public void chargeChat(UUID userId, String reference) {
        UserSubscription sub = subscriptions.getOrCreateActive(userId);
        SubscriptionTier tier = subscriptions.tierOf(sub);
        if (!isUnlimited(tier) && !withinAllowance(sub, tier)) {
            credits.spend(userId, tier.getChatbotOverageCost(), CreditCategory.CHAT_EXTENSION, reference, "Chat overage");
        }
        sub.setChatbotUsed(sub.getChatbotUsed() + 1);
        subscriptions.save(sub);
    }

    private boolean isUnlimited(SubscriptionTier tier) {
        return tier.getChatbotMonthlyLimit() != null && tier.getChatbotMonthlyLimit() < 0;
    }

    private boolean withinAllowance(UserSubscription sub, SubscriptionTier tier) {
        return sub.getChatbotUsed() < tier.getChatbotMonthlyLimit();
    }
}
