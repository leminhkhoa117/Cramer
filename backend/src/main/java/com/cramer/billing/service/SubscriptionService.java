package com.cramer.billing.service;

import com.cramer.billing.domain.CreditCategory;
import com.cramer.billing.domain.SubscriptionTier;
import com.cramer.billing.domain.UserSubscription;
import com.cramer.billing.repository.SubscriptionTierRepository;
import com.cramer.billing.repository.UserCreditRepository;
import com.cramer.billing.repository.UserSubscriptionRepository;
import com.cramer.platform.error.OperationNotAllowedException;
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

    /** All tiers ordered for display (SPEC-15 §2). */
    @Transactional(readOnly = true)
    public java.util.List<SubscriptionTier> listTiers() {
        return tiers.findAll().stream()
                .sorted(java.util.Comparator.comparing(t ->
                        t.getDisplayOrder() != null ? t.getDisplayOrder()
                                : (t.getSortOrder() != null ? t.getSortOrder() : 0)))
                .toList();
    }

    @Transactional(readOnly = true)
    public SubscriptionTier getTierByCode(String code) {
        return tiers.findByCode(code)
                .orElseThrow(() -> ResourceNotFoundException.of("SubscriptionTier", code));
    }

    /** Remaining included AI gradings this period (clamped at 0). */
    @Transactional(readOnly = true)
    public int gradingsRemaining(UUID userId) {
        UserSubscription sub = getOrCreateActive(userId);
        SubscriptionTier tier = tierOf(sub);
        return Math.max(0, tier.getIncludedAiGradings() - sub.getAttemptAisUsed());
    }

    /** Monthly chatbot message limit for the active tier ({@code < 0} = unlimited). */
    @Transactional(readOnly = true)
    public int chatLimit(UUID userId) {
        return tierOf(getOrCreateActive(userId)).getChatbotMonthlyLimit();
    }

    /** Enable/disable AI grading; enabling requires a premium tier (SPEC-15 §2). */
    public UserSubscription setAiGrading(UUID userId, boolean enabled) {
        UserSubscription sub = getOrCreateActive(userId);
        if (enabled && !tierOf(sub).isPremium()) {
            throw new OperationNotAllowedException("AI grading can only be enabled on a premium tier");
        }
        sub.setAiGradingEnabled(enabled);
        return subscriptions.save(sub);
    }

    /**
     * Activate a paid subscription after a verified payment (SPEC-15 §2, §8). Creates a fresh
     * subscription row for the tier with reset counters and a one-month expiry, then grants the
     * tier's initial Lúa as {@code TIER_BONUS} (idempotent by payment reference).
     */
    public UserSubscription activatePaid(UUID userId, SubscriptionTier tier, String paymentReference) {
        UserSubscription s = new UserSubscription();
        s.setUserId(userId);
        s.setTierId(tier.getId());
        s.setStatus("ACTIVE");
        s.setExpiresAt(OffsetDateTime.now().plusMonths(1));
        s.setAutoRenew(false);
        s.setAttemptsUsed(0);
        s.setAttemptAisUsed(0);
        s.setChatbotUsed(0);
        s.setAiGradingEnabled(true);
        s.setPaymentReference(paymentReference);
        UserSubscription saved = subscriptions.save(s);
        if (tier.getInitialLua() != null && tier.getInitialLua() > 0) {
            creditService.earn(userId, tier.getInitialLua(), CreditCategory.TIER_BONUS,
                    "tierbonus_" + paymentReference, "Tier activation bonus: " + tier.getCode());
        }
        return saved;
    }

    /**
     * Admin-initiated tier change (SPEC-17 §2): activate the tier for {@code months} with reset
     * counters; no Lúa bonus (audited by the admin module). A free tier sets a null expiry.
     */
    public UserSubscription adminSetTier(UUID userId, SubscriptionTier tier, int months) {
        UserSubscription s = new UserSubscription();
        s.setUserId(userId);
        s.setTierId(tier.getId());
        s.setStatus("ACTIVE");
        s.setExpiresAt(tier.isPremium() ? OffsetDateTime.now().plusMonths(Math.max(1, months)) : null);
        s.setAutoRenew(false);
        s.setAttemptsUsed(0);
        s.setAttemptAisUsed(0);
        s.setChatbotUsed(0);
        s.setAiGradingEnabled(tier.isPremium());
        s.setPaymentReference("admin");
        return subscriptions.save(s);
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
