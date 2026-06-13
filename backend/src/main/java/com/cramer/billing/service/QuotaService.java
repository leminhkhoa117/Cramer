package com.cramer.billing.service;

import com.cramer.billing.domain.SubscriptionTier;
import com.cramer.billing.domain.UserQuota;
import com.cramer.billing.domain.UserSubscription;
import com.cramer.billing.repository.UserQuotaRepository;
import com.cramer.billing.web.dto.QuotaStatusView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Quota status reporting (SPEC-15 §5). <strong>Fix:</strong> the status is <em>tier-aware</em> —
 * premium users see premium/unlimited status, not the free caps (the old endpoint always showed
 * free caps even though billing bypassed them). The atomic check+increment lives in
 * {@link AttemptBillingService}; this is read-only reporting.
 */
@Service
@Transactional(readOnly = true)
public class QuotaService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final SubscriptionService subscriptions;
    private final UserQuotaRepository userQuotas;
    private final CreditService credits;

    public QuotaService(SubscriptionService subscriptions, UserQuotaRepository userQuotas, CreditService credits) {
        this.subscriptions = subscriptions;
        this.userQuotas = userQuotas;
        this.credits = credits;
    }

    public QuotaStatusView status(UUID userId) {
        UserSubscription sub = subscriptions.getOrCreateActive(userId);
        SubscriptionTier tier = subscriptions.tierOf(sub);

        if (tier.isPremium()) {
            // Premium users are not capped by the free monthly quota rows.
            return new QuotaStatusView(true, tier.getCode(),
                    tier.getMonthlyAttemptLimit(), sub.getAttemptsUsed(),
                    tier.getMonthlyAttemptAiLimit(), sub.getAttemptAisUsed(), true);
        }

        LocalDate month = LocalDate.now(ZONE).withDayOfMonth(1);
        UserQuota q = userQuotas.findByUserIdAndQuotaMonth(userId, month).orElse(null);
        int used = q == null ? 0 : q.getAttemptCount();
        int aiUsed = q == null ? 0 : q.getAttemptAiCount();
        return new QuotaStatusView(false, tier.getCode(),
                tier.getMonthlyAttemptLimit(), used, tier.getMonthlyAttemptAiLimit(), aiUsed, false);
    }

    /**
     * Pre-check whether the caller may start an attempt (SPEC-15 §6, §9 {@code /can-attempt}).
     * Premium and within-cap users are allowed without Lúa; over-cap users need to cover the tier
     * overage cost (regular 10 / AI 20 by default). Read-only — no counter mutation.
     */
    public com.cramer.billing.web.dto.CanAttemptView canAttempt(UUID userId, String skill, boolean ai) {
        UserSubscription sub = subscriptions.getOrCreateActive(userId);
        SubscriptionTier tier = subscriptions.tierOf(sub);
        int balance = credits.balance(userId);
        if (tier.isPremium()) {
            return new com.cramer.billing.web.dto.CanAttemptView(true, true, false, 0, balance, "premium");
        }
        QuotaStatusView status = status(userId);
        int limit = ai ? status.globalAiLimit() : status.globalLimit();
        int used = ai ? status.globalAiUsed() : status.globalUsed();
        boolean withinCap = limit < 0 || used < limit;
        if (withinCap) {
            return new com.cramer.billing.web.dto.CanAttemptView(true, false, false, 0, balance, "within monthly quota");
        }
        int cost = ai ? tier.getAttemptAiOverageCost() : tier.getAttemptOverageCost();
        boolean affordable = balance >= cost;
        return new com.cramer.billing.web.dto.CanAttemptView(affordable, false, true, cost, balance,
                affordable ? "monthly quota reached; Lúa overage applies"
                        : "monthly quota reached and insufficient Lúa");
    }
}
