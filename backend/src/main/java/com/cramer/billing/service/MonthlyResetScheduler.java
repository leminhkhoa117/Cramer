package com.cramer.billing.service;

import com.cramer.billing.domain.CreditCategory;
import com.cramer.billing.domain.SubscriptionTier;
import com.cramer.billing.domain.UserSubscription;
import com.cramer.billing.repository.SubscriptionTierRepository;
import com.cramer.billing.repository.UserSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Monthly reset job (SPEC-15 §2 fix). On the 1st at 00:10 Asia/Ho_Chi_Minh it resets the
 * per-month subscription counters ({@code attempts_used}, {@code attempt_ais_used},
 * {@code chatbot_used}) for ACTIVE subscriptions and grants each tier's {@code monthly_lua_bonus}
 * (idempotent by {@code monthlybonus_{userId}_{yyyy-MM}}), which the old code never granted.
 */
@Component
public class MonthlyResetScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonthlyResetScheduler.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final UserSubscriptionRepository subscriptions;
    private final SubscriptionTierRepository tiers;
    private final CreditService credits;

    public MonthlyResetScheduler(UserSubscriptionRepository subscriptions, SubscriptionTierRepository tiers,
                                 CreditService credits) {
        this.subscriptions = subscriptions;
        this.tiers = tiers;
        this.credits = credits;
    }

    @Scheduled(cron = "0 10 0 1 * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void resetMonthlyCounters() {
        List<UserSubscription> active = subscriptions.findByStatus("ACTIVE");
        if (active.isEmpty()) {
            return;
        }
        String month = LocalDate.now(ZONE).withDayOfMonth(1).toString();
        Map<Long, SubscriptionTier> tierCache = new HashMap<>();
        int bonusGranted = 0;
        for (UserSubscription sub : active) {
            sub.setAttemptsUsed(0);
            sub.setAttemptAisUsed(0);
            sub.setChatbotUsed(0);
            SubscriptionTier tier = tierCache.computeIfAbsent(sub.getTierId(),
                    id -> tiers.findById(id).orElse(null));
            if (tier != null && tier.getMonthlyLuaBonus() != null && tier.getMonthlyLuaBonus() > 0) {
                credits.earn(sub.getUserId(), tier.getMonthlyLuaBonus(), CreditCategory.TIER_BONUS,
                        "monthlybonus_" + sub.getUserId() + "_" + month, "Monthly Lúa bonus: " + tier.getCode());
                bonusGranted++;
            }
        }
        subscriptions.saveAll(active);
        log.info("Monthly reset: {} subscription(s), {} bonus grant(s)", active.size(), bonusGranted);
    }
}
