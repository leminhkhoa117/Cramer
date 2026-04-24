package com.cramer.service.unit;

import com.cramer.entity.SubscriptionTier;
import com.cramer.entity.UserSubscription;
import com.cramer.repository.UserSubscriptionRepository;
import com.cramer.service.scheduled.SubscriptionExpirationScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionExpirationScheduler Unit Tests")
class SubscriptionExpirationSchedulerTest {

    @Mock
    private UserSubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionExpirationScheduler scheduler;

    @Test
    @DisplayName("Should flip all expired-active rows to EXPIRED")
    void shouldFlipExpiredActiveRows() {
        SubscriptionTier paidTier = SubscriptionTier.builder()
                .id(2L).code("cramerich").name("Cramerich").priceVnd(69000).build();

        UserSubscription a = UserSubscription.builder()
                .id(10L).userId(UUID.randomUUID()).tier(paidTier)
                .status(UserSubscription.Status.ACTIVE)
                .startedAt(OffsetDateTime.now().minusMonths(2))
                .expiresAt(OffsetDateTime.now().minusMonths(1))
                .build();
        UserSubscription b = UserSubscription.builder()
                .id(11L).userId(UUID.randomUUID()).tier(paidTier)
                .status(UserSubscription.Status.ACTIVE)
                .startedAt(OffsetDateTime.now().minusMonths(3))
                .expiresAt(OffsetDateTime.now().minusDays(1))
                .build();

        when(subscriptionRepository.findExpiredActiveSubscriptions())
                .thenReturn(List.of(a, b));

        scheduler.expireOldSubscriptions();

        assertThat(a.getStatus()).isEqualTo(UserSubscription.Status.EXPIRED);
        assertThat(b.getStatus()).isEqualTo(UserSubscription.Status.EXPIRED);
        verify(subscriptionRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should be a no-op when no expired rows exist")
    void shouldDoNothingWhenEmpty() {
        when(subscriptionRepository.findExpiredActiveSubscriptions())
                .thenReturn(List.of());

        scheduler.expireOldSubscriptions();

        verify(subscriptionRepository, never()).saveAll(anyList());
    }
}
