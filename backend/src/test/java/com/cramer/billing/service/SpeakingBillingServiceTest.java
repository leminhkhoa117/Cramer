package com.cramer.billing.service;

import com.cramer.billing.domain.CreditCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpeakingBillingServiceTest {

    @Mock CreditService credits;

    private SpeakingBillingService service() {
        return new SpeakingBillingService(credits);
    }

    @Test
    @DisplayName("deduct spends with the session reference (idempotent by session)")
    void deductSpends() {
        UUID user = UUID.randomUUID();
        service().deduct(user, 42L, 15);
        verify(credits).spend(eq(user), eq(15), eq(CreditCategory.SPEAKING_SESSION), eq("session_42"),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("refund credits with the refund_session reference (idempotent)")
    void refundCredits() {
        UUID user = UUID.randomUUID();
        service().refund(user, 42L, 15);
        verify(credits).refund(eq(user), eq(15), eq(CreditCategory.SPEAKING_REFUND), eq("refund_session_42"),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("zero-cost sessions neither deduct nor refund")
    void zeroCostNoop() {
        UUID user = UUID.randomUUID();
        service().deduct(user, 1L, 0);
        service().refund(user, 1L, 0);
        verify(credits, never()).spend(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(credits, never()).refund(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("canAfford reflects the balance")
    void canAfford() {
        UUID user = UUID.randomUUID();
        when(credits.balance(user)).thenReturn(20);
        assertThat(service().canAfford(user, 15)).isTrue();
        assertThat(service().canAfford(user, 25)).isFalse();
    }
}
