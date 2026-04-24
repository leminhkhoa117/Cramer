package com.cramer.service.unit;

import com.cramer.entity.SubscriptionTier;
import com.cramer.entity.UserSubscription;
import com.cramer.repository.TranslationUsageRepository;
import com.cramer.repository.UserSubscriptionRepository;
import com.cramer.service.CreditService;
import com.cramer.service.TranslationBillingService.TranslationBillingResult;
import com.cramer.service.implement.TranslationBillingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TranslationBillingServiceImpl}, focusing on the race-condition
 * handling for translation_usage row creation (bug T6 — see BUG_AUDIT_2026-04-23.md).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TranslationBillingServiceImpl Unit Tests (T6 race handling)")
class TranslationBillingServiceImplTest {

    @Mock
    private TranslationUsageRepository translationUsageRepository;
    @Mock
    private UserSubscriptionRepository subscriptionRepository;
    @Mock
    private CreditService creditService;

    private TranslationBillingServiceImpl service;
    private TranslationBillingServiceImpl spySelf;

    private UUID userId;
    private SubscriptionTier freeTier;
    private UserSubscription freeSub;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        freeTier = SubscriptionTier.builder()
                .id(1L).code("cramerie").name("Cramerie")
                .priceVnd(0)
                .monthlyTranslationLimit(10)
                .translationOverageCost(1)
                .build();
        freeSub = UserSubscription.builder()
                .id(1L).userId(userId).tier(freeTier)
                .status(UserSubscription.Status.ACTIVE)
                .build();

        service = new TranslationBillingServiceImpl(
                translationUsageRepository, subscriptionRepository, creditService, null);
        // Inject a spy of the service as `self` so we can verify proxy-like calls
        // and override the REQUIRES_NEW createUsageRow behavior in race tests.
        spySelf = spy(service);
        ReflectionTestUtils.setField(service, "self", spySelf);
    }

    @Test
    @DisplayName("processTranslationBilling: existing row → increments and does NOT create row")
    void processBilling_existingRow_incrementsOnly() {
        when(subscriptionRepository.findActiveByUserId(userId))
                .thenReturn(Optional.of(freeSub));
        when(translationUsageRepository.findByUserIdAndUsageMonth(eq(userId), any(LocalDate.class)))
                .thenReturn(Optional.empty()); // for getCurrentMonthUsage → 0
        when(translationUsageRepository.incrementTranslationsUsed(eq(userId), any(LocalDate.class)))
                .thenReturn(1); // existing row updated

        TranslationBillingResult result = service.processTranslationBilling(userId);

        assertThat(result.allowed()).isTrue();
        verify(translationUsageRepository, never()).save(any());
        verify(spySelf, never()).createUsageRow(any(), any());
    }

    @Test
    @DisplayName("processTranslationBilling: no row → creates via REQUIRES_NEW path")
    void processBilling_noRow_createsViaSelfProxy() {
        when(subscriptionRepository.findActiveByUserId(userId))
                .thenReturn(Optional.of(freeSub));
        when(translationUsageRepository.findByUserIdAndUsageMonth(eq(userId), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(translationUsageRepository.incrementTranslationsUsed(eq(userId), any(LocalDate.class)))
                .thenReturn(0); // no existing row
        // self.createUsageRow succeeds (default spy behavior)
        doNothing().when(spySelf).createUsageRow(eq(userId), any(LocalDate.class));

        TranslationBillingResult result = service.processTranslationBilling(userId);

        assertThat(result.allowed()).isTrue();
        verify(spySelf).createUsageRow(eq(userId), any(LocalDate.class));
    }

    @Test
    @DisplayName("processTranslationBilling: race on INSERT → catches and re-increments successfully")
    void processBilling_raceOnInsert_recoversByIncrement() {
        when(subscriptionRepository.findActiveByUserId(userId))
                .thenReturn(Optional.of(freeSub));
        when(translationUsageRepository.findByUserIdAndUsageMonth(eq(userId), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        // First increment returns 0 (no row), second (after race) returns 1
        when(translationUsageRepository.incrementTranslationsUsed(eq(userId), any(LocalDate.class)))
                .thenReturn(0).thenReturn(1);
        // INSERT throws because another thread already created the row
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(spySelf).createUsageRow(eq(userId), any(LocalDate.class));

        TranslationBillingResult result = service.processTranslationBilling(userId);

        assertThat(result.allowed()).isTrue();
        // Increment called twice: first miss, then recovery after race
        verify(translationUsageRepository, atLeast(2))
                .incrementTranslationsUsed(eq(userId), any(LocalDate.class));
    }

    @Test
    @DisplayName("processTranslationBilling: race + retry still 0 → re-throws original exception")
    void processBilling_raceAndRetryFails_propagatesException() {
        when(subscriptionRepository.findActiveByUserId(userId))
                .thenReturn(Optional.of(freeSub));
        when(translationUsageRepository.findByUserIdAndUsageMonth(eq(userId), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(translationUsageRepository.incrementTranslationsUsed(eq(userId), any(LocalDate.class)))
                .thenReturn(0); // both attempts return 0 (extremely unlikely scenario)
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(spySelf).createUsageRow(eq(userId), any(LocalDate.class));

        try {
            service.processTranslationBilling(userId);
        } catch (DataIntegrityViolationException expected) {
            // success
            return;
        }
        // If we got here, the test failed
        org.junit.jupiter.api.Assertions.fail("Expected DataIntegrityViolationException to be re-thrown");
    }
}
