package com.cramer.billing.service;

import com.cramer.billing.domain.CreditCategory;
import com.cramer.billing.domain.SubscriptionTier;
import com.cramer.billing.domain.UserSubscription;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsageBillingServiceTest {

    @Mock SubscriptionService subscriptions;
    @Mock CreditService credits;

    private UsageBillingService service() {
        return new UsageBillingService(subscriptions, credits);
    }

    private SubscriptionTier tier(boolean premium, int includedAiGradings) {
        SubscriptionTier t = new SubscriptionTier();
        t.setPriceVnd(premium ? 69000 : 0);
        t.setIncludedAiGradings(includedAiGradings);
        t.setAttemptAiOverageCost(20);
        return t;
    }

    private UserSubscription sub(int aiUsed) {
        UserSubscription s = new UserSubscription();
        s.setAttemptAisUsed(aiUsed);
        return s;
    }

    @Test
    @DisplayName("premium grading within the monthly allowance is free and bumps the counter")
    void includedAllowanceFree() {
        UUID user = UUID.randomUUID();
        UserSubscription s = sub(2);
        when(subscriptions.getOrCreateActive(user)).thenReturn(s);
        when(subscriptions.tierOf(s)).thenReturn(tier(true, 5));

        service().chargeAiGrading(user, "wsub_1");

        assertThat(s.getAttemptAisUsed()).isEqualTo(3);
        verify(credits, never()).spend(any(), anyInt(), any(), any(), any());
    }

    @Test
    @DisplayName("grading beyond the allowance costs the canonical 20 Lúa (idempotent by reference)")
    void overageCharges20() {
        UUID user = UUID.randomUUID();
        UserSubscription s = sub(0);
        when(subscriptions.getOrCreateActive(user)).thenReturn(s);
        when(subscriptions.tierOf(s)).thenReturn(tier(false, 0));

        service().chargeAiGrading(user, "wsub_2");

        verify(credits).spend(eq(user), eq(20), eq(CreditCategory.AI_GRADING), eq("wsub_2"), any());
    }

    @Test
    @DisplayName("refund reverses only an actual charge")
    void refundOnlyIfCharged() {
        UUID user = UUID.randomUUID();
        when(credits.hasTransaction(user, "wsub_2", CreditCategory.AI_GRADING)).thenReturn(true);
        UserSubscription s = sub(1);
        when(subscriptions.getOrCreateActive(user)).thenReturn(s);
        when(subscriptions.tierOf(s)).thenReturn(tier(false, 0));

        service().refund(user, "wsub_2");

        verify(credits).refund(eq(user), eq(20), eq(CreditCategory.AI_GRADING), eq("refund_wsub_2"), any());
    }

    @Test
    @DisplayName("refund is a no-op when nothing was charged (allowance-covered grading)")
    void refundNoopWhenFree() {
        UUID user = UUID.randomUUID();
        when(credits.hasTransaction(user, "wsub_1", CreditCategory.AI_GRADING)).thenReturn(false);

        service().refund(user, "wsub_1");

        verify(credits, never()).refund(any(), anyInt(), any(), any(), any());
    }
}
