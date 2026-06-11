package com.cramer.billing.service;

import com.cramer.billing.domain.CreditCategory;
import com.cramer.billing.domain.SubscriptionTier;
import com.cramer.billing.domain.UserSubscription;
import com.cramer.billing.repository.SubscriptionTierRepository;
import com.cramer.billing.repository.UserCreditRepository;
import com.cramer.billing.repository.UserSubscriptionRepository;
import com.cramer.platform.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Subscription lifecycle (SPEC-15 §2). Resolves the active subscription, auto-creating a free
 * Cramerie subscription (and granting initial Lúa once) when the user has none active.
 */
@Service
@Transactional
public class SubscriptionService {

    static final String FREE_TIER_CODE = "cramerie";

    private final UserSubscriptionRepository subscriptions;
    private final SubscriptionTierRepository tiers;
    private final UserCreditRepository credits;
    private final CreditService creditService;

    public SubscriptionService(UserSubscriptionRepository subscriptions,
                               SubscriptionTierRepository tiers,
                               UserCreditRepository credits,
                               CreditService creditService) {
        this.subscriptions = subscriptions;
        this.tiers = tiers;
        this.credits = credits;
        this.creditService = creditService;
    }

    /** The user's active subscription, creating a free one if none is active (SPEC-15 §2). */
    public UserSubscription getOrCreateActive(UUID userId) {
        UserSubscription latest = subscriptions.findFirstByUserIdOrderByStartedAtDesc(userId).orElse(null);
        if (latest != null && isActive(latest)) {
            return latest;
        }
        return createFree(userId);
    }

    public SubscriptionTier tierOf(UserSubscription sub) {
        return tiers.findById(sub.getTierId())
                .orElseThrow(() -> ResourceNotFoundException.of("SubscriptionTier", sub.getTierId()));
    }

    public boolean isPremium(UserSubscription sub) {
        return tierOf(sub).isPremium();
    }

    public UserSubscription save(UserSubscription sub) {
        return subscriptions.save(sub);
    }

    private boolean isActive(UserSubscription sub) {
        return "ACTIVE".equals(sub.getStatus())
                && (sub.getExpiresAt() == null || sub.getExpiresAt().isAfter(OffsetDateTime.now()));
    }

    private UserSubscription createFree(UUID userId) {
        SubscriptionTier free = tiers.findByCode(FREE_TIER_CODE)
                .orElseThrow(() -> new IllegalStateException("Free tier '" + FREE_TIER_CODE + "' is not configured"));
        UserSubscription s = new UserSubscription();
        s.setUserId(userId);
        s.setTierId(free.getId());
        s.setStatus("ACTIVE");
        s.setExpiresAt(null);
        s.setAutoRenew(false);
        s.setAttemptsUsed(0);
        s.setAttemptAisUsed(0);
        s.setChatbotUsed(0);
        s.setAiGradingEnabled(true);
        UserSubscription saved = subscriptions.save(s);

        // Grant initial Lúa only if the user has no credits row yet (idempotent by reference).
        if (credits.findByUserId(userId).isEmpty() && free.getInitialLua() > 0) {
            creditService.earn(userId, free.getInitialLua(), CreditCategory.INITIAL_BONUS,
                    "initial_" + userId, "Initial Lúa for new account");
        }
        return saved;
    }
}
