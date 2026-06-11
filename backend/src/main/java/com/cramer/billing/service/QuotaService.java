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

    public QuotaService(SubscriptionService subscriptions, UserQuotaRepository userQuotas) {
        this.subscriptions = subscriptions;
        this.userQuotas = userQuotas;
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
}
