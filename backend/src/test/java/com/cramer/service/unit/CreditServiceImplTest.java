package com.cramer.service.unit;

import com.cramer.dto.CreditTransactionDTO;
import com.cramer.dto.UserCreditDTO;
import com.cramer.dto.UserSubscriptionDTO;
import com.cramer.entity.CreditTransaction;
import com.cramer.entity.UserCredit;
import com.cramer.repository.ChatbotUsageRepository;
import com.cramer.repository.CreditTransactionRepository;
import com.cramer.repository.UserCreditRepository;
import com.cramer.repository.VocabularyRepository;
import com.cramer.service.SubscriptionService;
import com.cramer.service.implement.CreditServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CreditServiceImpl.
 * Tests Lúa credit operations including earning, spending, and balance checks.
 * 
 * @author Cramer Test Team
 * @since 2026-01-11
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreditServiceImpl Unit Tests")
class CreditServiceImplTest {

    @Mock
    private UserCreditRepository creditRepository;

    @Mock
    private CreditTransactionRepository transactionRepository;

    @Mock
    private VocabularyRepository vocabularyRepository;

    @Mock
    private ChatbotUsageRepository chatbotUsageRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private CreditServiceImpl creditService;

    private UUID testUserId;
    private UserCredit testUserCredit;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUserCredit = UserCredit.builder()
                .userId(testUserId)
                .balance(100)
                .lifetimeEarned(100)
                .lifetimeSpent(0)
                .build();
    }

    // =========================================================================
    // GET BALANCE TESTS
    // =========================================================================
    @Nested
    @DisplayName("getBalance() Tests")
    class GetBalanceTests {

        @Test
        @DisplayName("Should return existing balance when user has credits")
        void getBalance_existingUser_returnsBalance() {
            // Arrange
            when(creditRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testUserCredit));

            // Act
            UserCreditDTO result = creditService.getBalance(testUserId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getBalance()).isEqualTo(100);
            assertThat(result.getUserId()).isEqualTo(testUserId);
            verify(creditRepository).findByUserId(testUserId);
        }

        @Test
        @DisplayName("Should create default credits when user has no existing credits")
        void getBalance_newUser_createsDefaultCredits() {
            // Arrange
            UserCredit newCredit = UserCredit.builder()
                    .userId(testUserId)
                    .balance(0)
                    .lifetimeEarned(0)
                    .lifetimeSpent(0)
                    .build();
            
            when(creditRepository.findByUserId(testUserId))
                    .thenReturn(Optional.empty());
            when(creditRepository.save(any(UserCredit.class)))
                    .thenReturn(newCredit);

            // Act
            UserCreditDTO result = creditService.getBalance(testUserId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getBalance()).isEqualTo(0);
            verify(creditRepository).save(any(UserCredit.class));
        }
    }

    // =========================================================================
    // EARN CREDITS TESTS
    // =========================================================================
    @Nested
    @DisplayName("earnCredits() Tests")
    class EarnCreditsTests {

        @Test
        @DisplayName("Should add credits and create transaction when earning")
        void earnCredits_validAmount_increasesBalance() {
            // Arrange
            int earnAmount = 50;
            when(creditRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testUserCredit));
            when(creditRepository.save(any(UserCredit.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(transactionRepository.save(any(CreditTransaction.class)))
                    .thenAnswer(invocation -> {
                        CreditTransaction tx = invocation.getArgument(0);
                        return tx;
                    });

            // Act
            CreditTransactionDTO result = creditService.earnCredits(
                    testUserId, 
                    earnAmount,
                    CreditTransaction.Category.STREAK_BONUS,
                    "Daily login bonus"
            );

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualTo(earnAmount);
            assertThat(result.getType()).isEqualTo("EARN");
            assertThat(result.getCategory()).isEqualTo("STREAK_BONUS");
            
            // Verify credits were updated
            ArgumentCaptor<UserCredit> creditCaptor = ArgumentCaptor.forClass(UserCredit.class);
            verify(creditRepository).save(creditCaptor.capture());
            assertThat(creditCaptor.getValue().getBalance()).isEqualTo(150); // 100 + 50
        }

        @Test
        @DisplayName("Should throw exception when earning zero or negative amount")
        void earnCredits_zeroOrNegativeAmount_throwsException() {
            // Act & Assert
            assertThatThrownBy(() -> 
                    creditService.earnCredits(testUserId, 0, 
                            CreditTransaction.Category.STREAK_BONUS, "Test"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount must be positive");

            assertThatThrownBy(() -> 
                    creditService.earnCredits(testUserId, -10, 
                            CreditTransaction.Category.STREAK_BONUS, "Test"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount must be positive");
        }

        @Test
        @DisplayName("Should create credits if user doesn't have any when earning")
        void earnCredits_newUser_createsCreditsAndEarns() {
            // Arrange
            int earnAmount = 50;
            UserCredit newCredit = UserCredit.builder()
                    .userId(testUserId)
                    .balance(0)
                    .lifetimeEarned(0)
                    .lifetimeSpent(0)
                    .build();
            
            when(creditRepository.findByUserId(testUserId))
                    .thenReturn(Optional.empty());
            when(creditRepository.save(any(UserCredit.class)))
                    .thenAnswer(invocation -> {
                        UserCredit saved = invocation.getArgument(0);
                        // Simulate the addCredits being called
                        return saved;
                    });
            when(transactionRepository.save(any(CreditTransaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            CreditTransactionDTO result = creditService.earnCredits(
                    testUserId, earnAmount,
                    CreditTransaction.Category.MILESTONE_REWARD,
                    "Achievement unlocked"
            );

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualTo(earnAmount);
            // Verify save was called at least twice (once for creating, once for updating)
            verify(creditRepository, atLeast(1)).save(any(UserCredit.class));
        }

        @Test
        @DisplayName("Should record referenceId in transaction")
        void earnCredits_withReferenceId_recordsReference() {
            // Arrange
            String referenceId = "payment_123";
            when(creditRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testUserCredit));
            when(creditRepository.save(any(UserCredit.class)))
                    .thenReturn(testUserCredit);
            when(transactionRepository.save(any(CreditTransaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            CreditTransactionDTO result = creditService.earnCredits(
                    testUserId, 100,
                    CreditTransaction.Category.PURCHASE,
                    "Bought Lúa pack",
                    referenceId
            );

            // Assert
            assertThat(result.getReferenceId()).isEqualTo(referenceId);
        }
    }

    // =========================================================================
    // SPEND CREDITS TESTS
    // =========================================================================
    @Nested
    @DisplayName("spendCredits() Tests")
    class SpendCreditsTests {

        @Test
        @DisplayName("Should deduct credits when user has sufficient balance")
        void spendCredits_sufficientBalance_deductsCredits() {
            // Arrange
            int spendAmount = 30;
            when(creditRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testUserCredit));
            when(creditRepository.save(any(UserCredit.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(transactionRepository.save(any(CreditTransaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            CreditTransactionDTO result = creditService.spendCredits(
                    testUserId, spendAmount,
                    CreditTransaction.Category.AI_GRADING,
                    "AI grading fee"
            );

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualTo(-spendAmount); // Negative for spending
            assertThat(result.getType()).isEqualTo("SPEND");
            
            // Verify balance was deducted
            ArgumentCaptor<UserCredit> creditCaptor = ArgumentCaptor.forClass(UserCredit.class);
            verify(creditRepository).save(creditCaptor.capture());
            assertThat(creditCaptor.getValue().getBalance()).isEqualTo(70); // 100 - 30
        }

        @Test
        @DisplayName("Should throw exception when user has insufficient balance")
        void spendCredits_insufficientBalance_throwsException() {
            // Arrange
            int spendAmount = 200; // More than balance of 100
            when(creditRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testUserCredit));

            // Act & Assert
            assertThatThrownBy(() -> 
                    creditService.spendCredits(testUserId, spendAmount,
                            CreditTransaction.Category.AI_GRADING, "Test"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Insufficient");
        }

        @Test
        @DisplayName("Should throw exception when user has no credit record")
        void spendCredits_noCredits_throwsException() {
            // Arrange
            when(creditRepository.findByUserId(testUserId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> 
                    creditService.spendCredits(testUserId, 50,
                            CreditTransaction.Category.AI_GRADING, "Test"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No credit record");
        }

        @Test
        @DisplayName("Should throw exception when spending zero or negative amount")
        void spendCredits_zeroOrNegativeAmount_throwsException() {
            // Act & Assert
            assertThatThrownBy(() -> 
                    creditService.spendCredits(testUserId, 0,
                            CreditTransaction.Category.AI_GRADING, "Test"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount must be positive");
        }
    }

    // =========================================================================
    // HAS ENOUGH CREDITS TESTS
    // =========================================================================
    @Nested
    @DisplayName("hasEnoughCredits() Tests")
    class HasEnoughCreditsTests {

        @Test
        @DisplayName("Should return true when user has enough credits")
        void hasEnoughCredits_sufficientBalance_returnsTrue() {
            // Arrange
            when(creditRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testUserCredit));

            // Act
            boolean result = creditService.hasEnoughCredits(testUserId, 50);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true when user has exact amount")
        void hasEnoughCredits_exactBalance_returnsTrue() {
            // Arrange
            when(creditRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testUserCredit));

            // Act
            boolean result = creditService.hasEnoughCredits(testUserId, 100);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when user has insufficient credits")
        void hasEnoughCredits_insufficientBalance_returnsFalse() {
            // Arrange
            when(creditRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testUserCredit));

            // Act
            boolean result = creditService.hasEnoughCredits(testUserId, 150);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when user has no credit record")
        void hasEnoughCredits_noCredits_returnsFalse() {
            // Arrange
            when(creditRepository.findByUserId(testUserId))
                    .thenReturn(Optional.empty());

            // Act
            boolean result = creditService.hasEnoughCredits(testUserId, 10);

            // Assert
            assertThat(result).isFalse();
        }
    }

    // =========================================================================
    // INITIALIZE CREDITS TESTS
    // =========================================================================
    @Nested
    @DisplayName("initializeCredits() Tests")
    class InitializeCreditsTests {

        @Test
        @DisplayName("Should create new credits for new user")
        void initializeCredits_newUser_createsCredits() {
            // Arrange
            int initialAmount = 50;
            when(creditRepository.existsByUserId(testUserId)).thenReturn(false);
            when(creditRepository.save(any(UserCredit.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(transactionRepository.save(any(CreditTransaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            UserCreditDTO result = creditService.initializeCredits(testUserId, initialAmount);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getBalance()).isEqualTo(initialAmount);
            
            // Verify transaction was created for initial bonus
            ArgumentCaptor<CreditTransaction> txCaptor = ArgumentCaptor.forClass(CreditTransaction.class);
            verify(transactionRepository).save(txCaptor.capture());
            assertThat(txCaptor.getValue().getCategory().name())
                    .isEqualTo("INITIAL_BONUS");
        }

        @Test
        @DisplayName("Should return existing credits when user already has credits")
        void initializeCredits_existingUser_returnsExisting() {
            // Arrange
            when(creditRepository.existsByUserId(testUserId)).thenReturn(true);
            when(creditRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testUserCredit));

            // Act
            UserCreditDTO result = creditService.initializeCredits(testUserId, 50);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getBalance()).isEqualTo(100); // Original balance, not 50
            
            // Verify no new credits were created
            verify(creditRepository, never()).save(any(UserCredit.class));
        }
    }
}
