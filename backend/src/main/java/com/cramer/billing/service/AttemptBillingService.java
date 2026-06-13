package com.cramer.billing.service;

import com.cramer.billing.domain.CreditCategory;
import com.cramer.billing.domain.SkillQuota;
import com.cramer.billing.domain.SubscriptionTier;
import com.cramer.billing.domain.UserQuota;
import com.cramer.billing.domain.UserSubscription;
import com.cramer.billing.repository.SkillQuotaRepository;
import com.cramer.billing.repository.UserQuotaRepository;
import com.cramer.platform.common.ielts.Skill;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Attempt billing (SPEC-15 §6) implementing {@link AttemptBillingPort}. Premium users are not
 * charged; free users are charged the tier overage (default 10 Lúa) once both the global and
 * per-skill monthly caps are exceeded. The cap check + counter increment is performed under
 * pessimistic row locks so concurrent attempts cannot overrun the caps.
 */
@Service
public class AttemptBillingService implements AttemptBillingPort {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final SubscriptionService subscriptions;
    private final UserQuotaRepository userQuotas;
    private final SkillQuotaRepository skillQuotas;
    private final CreditService credits;

    public AttemptBillingService(SubscriptionService subscriptions,
                                 UserQuotaRepository userQuotas,
                                 SkillQuotaRepository skillQuotas,
                                 CreditService credits) {
        this.subscriptions = subscriptions;
        this.userQuotas = userQuotas;
        this.skillQuotas = skillQuotas;
        this.credits = credits;
    }

    @Override
    @Transactional
    public void chargeAttemptStart(UUID userId, Skill skill, String referenceId) {
        UserSubscription sub = subscriptions.getOrCreateActive(userId);
        SubscriptionTier tier = subscriptions.tierOf(sub);

        if (tier.isPremium()) {
            sub.setAttemptsUsed(sub.getAttemptsUsed() + 1);
            subscriptions.save(sub);
            return;
        }

        LocalDate month = LocalDate.now(ZONE).withDayOfMonth(1);
        UserQuota global = lockOrCreateGlobal(userId, month);
        SkillQuota perSkill = lockOrCreateSkill(userId, skill.name(), month);

        boolean withinCap = global.getAttemptCount() < tier.getMonthlyAttemptLimit()
                && perSkill.getAttemptCount() < tier.getPerSkillAttemptLimit();

        if (!withinCap) {
            // throws 402 INSUFFICIENT_LUA if the user cannot afford the overage
            credits.spend(userId, tier.getAttemptOverageCost(), CreditCategory.ATTEMPT_OVERAGE,
                    referenceId, "Attempt overage: " + skill.name());
        }

        global.setAttemptCount(global.getAttemptCount() + 1);
        perSkill.setAttemptCount(perSkill.getAttemptCount() + 1);
        userQuotas.save(global);
        skillQuotas.save(perSkill);
    }

    private UserQuota lockOrCreateGlobal(UUID userId, LocalDate month) {
        return userQuotas.findForUpdate(userId, month).orElseGet(() -> {
            UserQuota q = new UserQuota();
            q.setUserId(userId);
            q.setQuotaMonth(month);
            q.setAttemptCount(0);
            q.setAttemptAiCount(0);
            return userQuotas.saveAndFlush(q);
        });
    }

    private SkillQuota lockOrCreateSkill(UUID userId, String skill, LocalDate month) {
        return skillQuotas.findForUpdate(userId, skill, month).orElseGet(() -> {
            SkillQuota q = new SkillQuota();
            q.setUserId(userId);
            q.setSkill(skill);
            q.setQuotaMonth(month);
            q.setAttemptCount(0);
            q.setAttemptAiCount(0);
            return skillQuotas.saveAndFlush(q);
        });
    }
}
