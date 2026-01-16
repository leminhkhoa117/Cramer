package com.cramer.service.unit;

import com.cramer.dto.GradingStatusDTO;
import com.cramer.dto.SubscriptionTierDTO;
import com.cramer.dto.UserSubscriptionDTO;
import com.cramer.entity.SubscriptionTier;
import com.cramer.entity.UserCredit;
import com.cramer.entity.UserSubscription;
import com.cramer.repository.SubscriptionTierRepository;
import com.cramer.repository.UserCreditRepository;
import com.cramer.repository.UserSubscriptionRepository;
import com.cramer.service.CreditService;
import com.cramer.service.implement.SubscriptionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SubscriptionServiceImpl.
 * Tests subscription tier management and AI grading availability.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionServiceImpl Unit Tests")
class SubscriptionServiceImplTest {

    @Mock
    private SubscriptionTierRepository tierRepository;

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @Mock
    private UserCreditRepository userCreditRepository;

    @Mock
    private CreditService creditService;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private UUID testUserId;
    private SubscriptionTier cramerieFreeTier;
    private SubscriptionTier cramerichTier;
    private UserSubscription testSubscription;
    private UserCredit testUserCredit;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();

        // Setup tiers using builder (Lombok @Builder)
        cramerieFreeTier = SubscriptionTier.builder()
                .id(1L)
                .code("cramerie")
                .name("Cramerie (Free)")
                .priceVnd(0)
                .monthlyAttemptLimit(0)
                .monthlyAttemptAiLimit(3) // Cramerie has 3 ATTEMPT_AIs/month
                .build();

        cramerichTier = SubscriptionTier.builder()
                .id(2L)
                .code("cramerich")
                .name("Cramerich")
                .priceVnd(69000)
                .monthlyAttemptLimit(10)
                .monthlyAttemptAiLimit(5)
                .build();

        // Setup subscription using builder
        testSubscription = UserSubscription.builder()
                .id(1L)
                .userId(testUserId)
                .tier(cramerichTier)
                .status(UserSubscription.Status.ACTIVE)
                .attemptAisUsed(2)
                .attemptsUsed(3)
                .startedAt(OffsetDateTime.now().minusDays(15))
                .expiresAt(OffsetDateTime.now().plusDays(15))
                .build();

        // Setup user credit using builder
        testUserCredit = UserCredit.builder()
                .id(1L)
                .userId(testUserId)
                .balance(150)
                .lifetimeEarned(200)
                .lifetimeSpent(50)
                .build();
    }

    @Nested
    @DisplayName("getAllTiers Tests")
    class GetAllTiersTests {

        @Test
        @DisplayName("Should return all subscription tiers")
        void shouldReturnAllTiers() {
            // Given
            List<SubscriptionTier> tiers = List.of(cramerieFreeTier, cramerichTier);
            when(tierRepository.findByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(tiers);

            // When
            List<SubscriptionTierDTO> result = subscriptionService.getAllTiers();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(SubscriptionTierDTO::getCode)
                    .containsExactlyInAnyOrder("cramerie", "cramerich");
        }

        @Test
        @DisplayName("Should return empty list when no tiers exist")
        void shouldReturnEmptyListWhenNoTiers() {
            // Given
            when(tierRepository.findByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of());

            // When
            List<SubscriptionTierDTO> result = subscriptionService.getAllTiers();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUserSubscription Tests")
    class GetUserSubscriptionTests {

        @Test
        @DisplayName("Should return active subscription for user")
        void shouldReturnActiveSubscription() {
            // Given
            when(userSubscriptionRepository.findActiveByUserId(testUserId))
                    .thenReturn(Optional.of(testSubscription));

            // When
            UserSubscriptionDTO result = subscriptionService.getUserSubscription(testUserId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(testUserId);
            assertThat(result.getTier().getCode()).isEqualTo("cramerich");
        }

        @Test
        @DisplayName("Should create free tier when no subscription exists")
        void shouldCreateFreeTierWhenNoSubscription() {
            // Given - no existing subscription
            when(userSubscriptionRepository.findActiveByUserId(testUserId))
                    .thenReturn(Optional.empty());
            when(userSubscriptionRepository.existsByUserId(testUserId))
                    .thenReturn(false);
            when(tierRepository.findFreeTier())
                    .thenReturn(Optional.of(cramerieFreeTier));
            when(userSubscriptionRepository.save(any(UserSubscription.class)))
                    .thenAnswer(invocation -> {
                        UserSubscription sub = invocation.getArgument(0);
                        sub.setId(99L);
                        return sub;
                    });

            // When
            UserSubscriptionDTO result = subscriptionService.getUserSubscription(testUserId);

            // Then
            assertThat(result).isNotNull();
            verify(userSubscriptionRepository).save(any(UserSubscription.class));
        }
    }

    @Nested
    @DisplayName("checkAIGradingAllowed Tests")
    class CheckAIGradingAllowedTests {

        @Test
        @DisplayName("Should allow grading when user has remaining quota")
        void shouldAllowGradingWhenHasRemainingQuota() {
            // Given - User has used 2/5 monthly AI gradings
            when(userSubscriptionRepository.findActiveByUserId(testUserId))
                    .thenReturn(Optional.of(testSubscription));
            when(userCreditRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testUserCredit));

            // When
            GradingStatusDTO result = subscriptionService.checkAIGradingAllowed(testUserId);

            // Then
            assertThat(result.getAllowed()).isTrue();
            assertThat(result.getUsed()).isEqualTo(2);
            assertThat(result.getLimit()).isEqualTo(5);
            assertThat(result.getRemaining()).isEqualTo(3);
            assertThat(result.getTierCode()).isEqualTo("cramerich");
        }

        @Test
        @DisplayName("Should allow grading with Lua when quota exhausted")
        void shouldDenyGradingWhenQuotaExhaustedButAllowWithLua() {
            // Given - User has exhausted monthly quota but has enough Lúa (150 >= 10)
            testSubscription.setAttemptAisUsed(5); // All 5 used
            when(userSubscriptionRepository.findActiveByUserId(testUserId))
                    .thenReturn(Optional.of(testSubscription));
            when(userCreditRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testUserCredit));

            // When
            GradingStatusDTO result = subscriptionService.checkAIGradingAllowed(testUserId);

            // Then - allowed = true because luaBalance (150) >= 10
            assertThat(result.getAllowed()).isTrue();
            assertThat(result.getRemaining()).isZero();
            assertThat(result.getCanUseExtraWithLua()).isTrue();
            assertThat(result.getLuaBalance()).isEqualTo(150);
        }

        @Test
        @DisplayName("Should deny grading for free tier user when exhausted")
        void shouldDenyGradingForFreeTierUser() {
            // Given - Free tier has 3 monthly gradings, user exhausted all 3
            testSubscription.setTier(cramerieFreeTier);
            testSubscription.setAttemptAisUsed(3); // Exhausted all 3
            when(userSubscriptionRepository.findActiveByUserId(testUserId))
                    .thenReturn(Optional.of(testSubscription));
            when(userCreditRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testUserCredit));

            // When
            GradingStatusDTO result = subscriptionService.checkAIGradingAllowed(testUserId);

            // Then - Should deny because 3/3 used
            assertThat(result.getRemaining()).isZero();
            assertThat(result.getLimit()).isEqualTo(3);
            assertThat(result.getTierCode()).isEqualTo("cramerie");
        }
    }

    @Nested
    @DisplayName("incrementAIGradingUsage Tests")
    class IncrementAIGradingUsageTests {

        @Test
        @DisplayName("Should increment AI usage when called")
        void shouldIncrementUsage() {
            // Given
            int currentUsage = testSubscription.getAttemptAisUsed();
            when(userSubscriptionRepository.findActiveByUserId(testUserId))
                    .thenReturn(Optional.of(testSubscription));
            when(userSubscriptionRepository.save(any(UserSubscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            subscriptionService.incrementAIGradingUsage(testUserId);

            // Then
            verify(userSubscriptionRepository).save(argThat(sub -> 
                sub.getAttemptAisUsed() == currentUsage + 1
            ));
        }
    }

    @Nested
    @DisplayName("initializeNewUser Tests")
    class InitializeNewUserTests {

        @Test
        @DisplayName("Should create free tier subscription for new user")
        void shouldCreateFreeTierSubscription() {
            // Given
            UUID newUserId = UUID.randomUUID();
            when(userSubscriptionRepository.existsByUserId(newUserId))
                    .thenReturn(false);
            when(tierRepository.findFreeTier())
                    .thenReturn(Optional.of(cramerieFreeTier));
            when(userSubscriptionRepository.save(any(UserSubscription.class)))
                    .thenAnswer(invocation -> {
                        UserSubscription sub = invocation.getArgument(0);
                        sub.setId(99L);
                        return sub;
                    });

            // When
            UserSubscriptionDTO result = subscriptionService.initializeNewUser(newUserId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(newUserId);
            verify(userSubscriptionRepository).save(argThat(sub ->
                sub.getTier().getCode().equals("cramerie") &&
                sub.getStatus() == UserSubscription.Status.ACTIVE &&
                sub.getAttemptAisUsed() == 0
            ));
        }
    }

    @Nested
    @DisplayName("getMonthlyGradingsRemaining Tests")
    class GetMonthlyGradingsRemainingTests {

        @Test
        @DisplayName("Should return correct remaining count")
        void shouldReturnRemainingCount() {
            // Given - 5 limit, 2 used = 3 remaining
            when(userSubscriptionRepository.findActiveByUserId(testUserId))
                    .thenReturn(Optional.of(testSubscription));

            // When
            int result = subscriptionService.getMonthlyGradingsRemaining(testUserId);

            // Then
            assertThat(result).isEqualTo(3);
        }

        @Test
        @DisplayName("Should return zero when no subscription")
        void shouldReturnZeroWhenNoSubscription() {
            // Given - no active subscription exists
            when(userSubscriptionRepository.findActiveByUserId(testUserId))
                    .thenReturn(Optional.empty());

            // When
            int result = subscriptionService.getMonthlyGradingsRemaining(testUserId);

            // Then - returns 0 when no subscription found (per implementation)
            assertThat(result).isZero();
        }
    }

    @Nested
    @DisplayName("getTierByCode Tests")
    class GetTierByCodeTests {

        @Test
        @DisplayName("Should return tier when found")
        void shouldReturnTierWhenFound() {
            // Given
            when(tierRepository.findByCode("cramerich"))
                    .thenReturn(Optional.of(cramerichTier));

            // When
            SubscriptionTierDTO result = subscriptionService.getTierByCode("cramerich");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("cramerich");
            assertThat(result.getPriceVnd()).isEqualTo(69000);
        }

        @Test
        @DisplayName("Should throw when tier not found")
        void shouldThrowWhenTierNotFound() {
            // Given
            when(tierRepository.findByCode("invalid_tier"))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> 
                subscriptionService.getTierByCode("invalid_tier")
            ).isInstanceOf(RuntimeException.class);
        }
    }
}
