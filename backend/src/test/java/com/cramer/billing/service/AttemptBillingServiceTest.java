package com.cramer.billing.service;

import com.cramer.billing.domain.SkillQuota;
import com.cramer.billing.domain.SubscriptionTier;
import com.cramer.billing.domain.UserQuota;
import com.cramer.billing.domain.UserSubscription;
import com.cramer.billing.repository.SkillQuotaRepository;
import com.cramer.billing.repository.UserQuotaRepository;
import com.cramer.platform.common.ielts.Skill;
import com.cramer.platform.error.QuotaExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttemptBillingServiceTest {

    @Mock SubscriptionService subscriptions;
    @Mock UserQuotaRepository userQuotas;
    @Mock SkillQuotaRepository skillQuotas;
    @Mock CreditService credits;

    private AttemptBillingService service() {
        return new AttemptBillingService(subscriptions, userQuotas, skillQuotas, credits);
    }

    private SubscriptionTier tier(boolean premium) {
        SubscriptionTier t = new SubscriptionTier();
        t.setPriceVnd(premium ? 69000 : 0);
        t.setMonthlyAttemptLimit(60);
        t.setPerSkillAttemptLimit(20);
        t.setAttemptOverageCost(10);
        return t;
    }

    private UserSubscription sub() {
        UserSubscription s = new UserSubscription();
        s.setAttemptsUsed(3);
        return s;
    }

    private UserQuota userQuota(int count) {
        UserQuota q = new UserQuota();
        q.setAttemptCount(count);
        return q;
    }

    private SkillQuota skillQuota(int count) {
        SkillQuota q = new SkillQuota();
        q.setAttemptCount(count);
        return q;
    }

    @Test
    @DisplayName("premium users are not charged and only bump the subscription counter")
    void premiumBypass() {
        UUID user = UUID.randomUUID();
        UserSubscription s = sub();
        when(subscriptions.getOrCreateActive(user)).thenReturn(s);
        when(subscriptions.tierOf(s)).thenReturn(tier(true));

        service().chargeAttemptStart(user, Skill.READING, "attempt_1");

        assertThat(s.getAttemptsUsed()).isEqualTo(4);
        verify(subscriptions).save(s);
        verify(credits, never()).spend(any(), anyInt(), any(), any(), any());
    }

    @Test
    @DisplayName("free users within caps are not charged; both counters increment")
    void freeWithinCap() {
        UUID user = UUID.randomUUID();
        UserSubscription s = sub();
        when(subscriptions.getOrCreateActive(user)).thenReturn(s);
        when(subscriptions.tierOf(s)).thenReturn(tier(false));
        UserQuota uq = userQuota(5);
        SkillQuota sq = skillQuota(2);
        when(userQuotas.findForUpdate(eq(user), any(LocalDate.class))).thenReturn(Optional.of(uq));
        when(skillQuotas.findForUpdate(eq(user), eq("READING"), any(LocalDate.class))).thenReturn(Optional.of(sq));

        service().chargeAttemptStart(user, Skill.READING, "attempt_1");

        assertThat(uq.getAttemptCount()).isEqualTo(6);
        assertThat(sq.getAttemptCount()).isEqualTo(3);
        verify(credits, never()).spend(any(), anyInt(), any(), any(), any());
    }

    @Test
    @DisplayName("free users over a cap are charged the tier overage, then counters increment")
    void freeOverCapCharges() {
        UUID user = UUID.randomUUID();
        UserSubscription s = sub();
        when(subscriptions.getOrCreateActive(user)).thenReturn(s);
        when(subscriptions.tierOf(s)).thenReturn(tier(false));
        UserQuota uq = userQuota(60); // at global cap
        SkillQuota sq = skillQuota(2);
        when(userQuotas.findForUpdate(eq(user), any(LocalDate.class))).thenReturn(Optional.of(uq));
        when(skillQuotas.findForUpdate(eq(user), eq("READING"), any(LocalDate.class))).thenReturn(Optional.of(sq));

        service().chargeAttemptStart(user, Skill.READING, "attempt_42");

        verify(credits).spend(eq(user), eq(10), any(), eq("attempt_42"), anyString());
        assertThat(uq.getAttemptCount()).isEqualTo(61);
    }

    @Test
    @DisplayName("over cap with insufficient Lúa propagates 402 and does not increment counters")
    void freeOverCapInsufficient() {
        UUID user = UUID.randomUUID();
        UserSubscription s = sub();
        when(subscriptions.getOrCreateActive(user)).thenReturn(s);
        when(subscriptions.tierOf(s)).thenReturn(tier(false));
        UserQuota uq = userQuota(60);
        SkillQuota sq = skillQuota(2);
        when(userQuotas.findForUpdate(eq(user), any(LocalDate.class))).thenReturn(Optional.of(uq));
        when(skillQuotas.findForUpdate(eq(user), eq("READING"), any(LocalDate.class))).thenReturn(Optional.of(sq));
        doThrow(new QuotaExceededException("INSUFFICIENT_LUA", "broke"))
                .when(credits).spend(eq(user), eq(10), any(), anyString(), anyString());

        assertThatThrownBy(() -> service().chargeAttemptStart(user, Skill.READING, "attempt_42"))
                .isInstanceOf(QuotaExceededException.class);
        assertThat(uq.getAttemptCount()).isEqualTo(60); // unchanged
        verify(userQuotas, never()).save(any());
    }
}
