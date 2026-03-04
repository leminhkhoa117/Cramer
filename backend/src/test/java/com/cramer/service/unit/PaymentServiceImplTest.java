package com.cramer.service.unit;

import com.cramer.config.PayOSConfig;
import com.cramer.dto.PayOSWebhookDTO;
import com.cramer.dto.PaymentResponseDTO;
import com.cramer.entity.CreditTransaction;
import com.cramer.entity.PaymentOrder;
import com.cramer.entity.SubscriptionTier;
import com.cramer.entity.UserSubscription;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.repository.PaymentOrderRepository;
import com.cramer.repository.SubscriptionTierRepository;
import com.cramer.repository.UserSubscriptionRepository;
import com.cramer.service.CreditService;
import com.cramer.service.implement.PaymentServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentServiceImpl.
 * Tests PayOS payment gateway integration, signature verification, and webhook handling.
 * 
 * @author Cramer Test Team
 * @since 2026-01-11
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl Unit Tests")
class PaymentServiceImplTest {

    @Mock
    private PayOSConfig payOSConfig;

    @Mock
    private PaymentOrderRepository paymentOrderRepository;

    @Mock
    private SubscriptionTierRepository tierRepository;

    @Mock
    private UserSubscriptionRepository subscriptionRepository;

    @Mock
    private CreditService creditService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private UUID testUserId;
    private SubscriptionTier premiumTier;
    private PaymentOrder testOrder;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();

        premiumTier = SubscriptionTier.builder()
                .id(2L)
                .code("cramerich")
                .name("Cramerich")
                .priceVnd(79000)
                .monthlyAttemptAiLimit(20)
                .dailyChatLimit(100)
                .initialLua(100)
                .displayOrder(2)
                .isActive(true)
                .build();

        testOrder = PaymentOrder.builder()
                .id(1L)
                .userId(testUserId)
                .orderCode(1234567890123L)
                .type(PaymentOrder.Type.SUBSCRIPTION)
                .tierId(2L)
                .tierCode("cramerich")
                .amountVnd(79000)
                .description("CRAMER CRAMERICH")
                .status(PaymentOrder.Status.PENDING)
                .expiresAt(OffsetDateTime.now().plusHours(24))
                .build();
    }

    // =========================================================================
    // CREATE SUBSCRIPTION PAYMENT TESTS
    // =========================================================================
    @Nested
    @DisplayName("createSubscriptionPayment() Tests")
    class CreateSubscriptionPaymentTests {

        @Test
        @DisplayName("Should throw exception for free tier payment")
        void createSubscriptionPayment_freeTier_throwsException() {
            // Arrange
            SubscriptionTier freeTier = SubscriptionTier.builder()
                    .id(1L)
                    .code("cramerie")
                    .priceVnd(0)
                    .build();
            
            when(tierRepository.findById(1L)).thenReturn(Optional.of(freeTier));

            // Act & Assert
            assertThatThrownBy(() -> paymentService.createSubscriptionPayment(testUserId, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot create payment for free tier");
        }

        @Test
        @DisplayName("Should throw exception when tier not found")
        void createSubscriptionPayment_tierNotFound_throwsException() {
            // Arrange
            when(tierRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentService.createSubscriptionPayment(testUserId, 99))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should create mock response when PayOS not configured")
        void createSubscriptionPayment_payosNotConfigured_returnsMockResponse() {
            // Arrange
            when(tierRepository.findById(2L)).thenReturn(Optional.of(premiumTier));
            when(payOSConfig.isConfigured()).thenReturn(false);
            when(paymentOrderRepository.existsByOrderCode(anyLong())).thenReturn(false);
            when(paymentOrderRepository.save(any(PaymentOrder.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            PaymentResponseDTO result = paymentService.createSubscriptionPayment(testUserId, 2);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCheckoutUrl()).contains("mock");
            verify(paymentOrderRepository, times(2)).save(any(PaymentOrder.class));
        }
    }

    // =========================================================================
    // CREATE LUA PACK PAYMENT TESTS
    // =========================================================================
    @Nested
    @DisplayName("createLuaPackPayment() Tests")
    class CreateLuaPackPaymentTests {

        @Test
        @DisplayName("Should create payment for valid Túi Lúa pack (100 Lúa)")
        void createLuaPackPayment_validTuiLua_createsPayment() {
            // Arrange
            when(payOSConfig.isConfigured()).thenReturn(false);
            when(paymentOrderRepository.existsByOrderCode(anyLong())).thenReturn(false);
            when(paymentOrderRepository.save(any(PaymentOrder.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            PaymentResponseDTO result = paymentService.createLuaPackPayment(testUserId, 100, 10000);

            // Assert
            assertThat(result).isNotNull();
            
            ArgumentCaptor<PaymentOrder> captor = ArgumentCaptor.forClass(PaymentOrder.class);
            verify(paymentOrderRepository, atLeastOnce()).save(captor.capture());
            
            PaymentOrder saved = captor.getAllValues().get(0);
            assertThat(saved.getLuaAmount()).isEqualTo(100);
            assertThat(saved.getAmountVnd()).isEqualTo(10000);
            assertThat(saved.getType()).isEqualTo(PaymentOrder.Type.LUA_PACK);
        }

        @Test
        @DisplayName("Should create payment for valid Bao Lúa pack (500 Lúa)")
        void createLuaPackPayment_validBaoLua_createsPayment() {
            // Arrange
            when(payOSConfig.isConfigured()).thenReturn(false);
            when(paymentOrderRepository.existsByOrderCode(anyLong())).thenReturn(false);
            when(paymentOrderRepository.save(any(PaymentOrder.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            PaymentResponseDTO result = paymentService.createLuaPackPayment(testUserId, 500, 45000);

            // Assert
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should create payment for valid Xe Lúa pack (2000 Lúa)")
        void createLuaPackPayment_validXeLua_createsPayment() {
            // Arrange
            when(payOSConfig.isConfigured()).thenReturn(false);
            when(paymentOrderRepository.existsByOrderCode(anyLong())).thenReturn(false);
            when(paymentOrderRepository.save(any(PaymentOrder.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            PaymentResponseDTO result = paymentService.createLuaPackPayment(testUserId, 2000, 150000);

            // Assert
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception for invalid Lúa pack amount")
        void createLuaPackPayment_invalidAmount_throwsException() {
            // Act & Assert
            assertThatThrownBy(() -> paymentService.createLuaPackPayment(testUserId, 999, 50000))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid Lúa pack");
        }

        @Test
        @DisplayName("Should throw exception for mismatched price")
        void createLuaPackPayment_mismatchedPrice_throwsException() {
            // Act & Assert - 100 Lúa should cost 10000, not 5000
            assertThatThrownBy(() -> paymentService.createLuaPackPayment(testUserId, 100, 5000))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid Lúa pack");
        }
    }

    // =========================================================================
    // WEBHOOK HANDLING TESTS
    // =========================================================================
    @Nested
    @DisplayName("handleWebhook() Tests")
    class HandleWebhookTests {

        @Test
        @DisplayName("Should process successful subscription payment webhook")
        void handleWebhook_successfulSubscription_upgradesUser() {
            // Arrange
            testOrder.setStatus(PaymentOrder.Status.PENDING);
            
            PayOSWebhookDTO.WebhookData data = new PayOSWebhookDTO.WebhookData();
            data.setOrderCode(testOrder.getOrderCode());
            data.setAmount(79000);
            data.setDescription("CRAMER CRAMERICH");
            data.setTransactionDateTime("2026-01-11 12:00:00");
            
            PayOSWebhookDTO webhook = new PayOSWebhookDTO();
            webhook.setCode("00");
            webhook.setSuccess(true);
            webhook.setData(data);
            webhook.setSignature("mock-signature");

            when(payOSConfig.isConfigured()).thenReturn(false); // Skip signature verification
            when(paymentOrderRepository.findByOrderCode(testOrder.getOrderCode()))
                    .thenReturn(Optional.of(testOrder));
            when(tierRepository.findById(2L)).thenReturn(Optional.of(premiumTier));
            when(subscriptionRepository.findActiveByUserId(testUserId))
                    .thenReturn(Optional.empty());
            when(subscriptionRepository.save(any(UserSubscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(paymentOrderRepository.save(any(PaymentOrder.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            paymentService.handleWebhook(webhook);

            // Assert - subscription created
            verify(subscriptionRepository).save(any(UserSubscription.class));
            
            // Assert - bonus Lúa credited
            verify(creditService).earnCredits(
                    eq(testUserId),
                    eq(100), // initialLua from premiumTier
                    eq(CreditTransaction.Category.TIER_BONUS),
                    anyString(),
                    any()
            );
        }

        @Test
        @DisplayName("Should process successful Lúa pack payment webhook")
        void handleWebhook_successfulLuaPack_creditsLua() {
            // Arrange
            PaymentOrder luaOrder = PaymentOrder.builder()
                    .id(2L)
                    .userId(testUserId)
                    .orderCode(9876543210123L)
                    .type(PaymentOrder.Type.LUA_PACK)
                    .luaAmount(500)
                    .amountVnd(45000)
                    .status(PaymentOrder.Status.PENDING)
                    .build();
            
            PayOSWebhookDTO.WebhookData data = new PayOSWebhookDTO.WebhookData();
            data.setOrderCode(luaOrder.getOrderCode());
            data.setAmount(45000);
            data.setTransactionDateTime("2026-01-11 12:00:00");
            
            PayOSWebhookDTO webhook = new PayOSWebhookDTO();
            webhook.setCode("00");
            webhook.setSuccess(true);
            webhook.setData(data);
            webhook.setSignature("mock-signature");

            when(payOSConfig.isConfigured()).thenReturn(false);
            when(paymentOrderRepository.findByOrderCode(luaOrder.getOrderCode()))
                    .thenReturn(Optional.of(luaOrder));
            when(paymentOrderRepository.save(any(PaymentOrder.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            paymentService.handleWebhook(webhook);

            // Assert - Lúa credited
            verify(creditService).earnCredits(
                    eq(testUserId),
                    eq(500),
                    eq(CreditTransaction.Category.PURCHASE),
                    contains("Bao Lúa"),
                    any()
            );
        }

        @Test
        @DisplayName("Should skip already paid orders (idempotent)")
        void handleWebhook_alreadyPaid_skipsProcessing() {
            // Arrange
            testOrder.setStatus(PaymentOrder.Status.PAID);
            testOrder.setPaidAt(OffsetDateTime.now());
            
            PayOSWebhookDTO.WebhookData data = new PayOSWebhookDTO.WebhookData();
            data.setOrderCode(testOrder.getOrderCode());
            
            PayOSWebhookDTO webhook = new PayOSWebhookDTO();
            webhook.setCode("00");
            webhook.setSuccess(true);
            webhook.setData(data);
            webhook.setSignature("mock-signature");

            when(payOSConfig.isConfigured()).thenReturn(false);
            when(paymentOrderRepository.findByOrderCode(testOrder.getOrderCode()))
                    .thenReturn(Optional.of(testOrder));

            // Act
            paymentService.handleWebhook(webhook);

            // Assert - no further processing
            verify(creditService, never()).earnCredits(any(), anyInt(), any(), any(), any());
            verify(subscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject webhook with invalid signature")
        void handleWebhook_invalidSignature_throwsSecurityException() {
            // Arrange
            PayOSWebhookDTO.WebhookData data = new PayOSWebhookDTO.WebhookData();
            data.setOrderCode(12345L);
            data.setAmount(79000);
            
            PayOSWebhookDTO webhook = new PayOSWebhookDTO();
            webhook.setCode("00");
            webhook.setSuccess(true);
            webhook.setData(data);
            webhook.setSignature("invalid-signature");

            when(payOSConfig.isConfigured()).thenReturn(true);
            when(payOSConfig.getChecksumKey()).thenReturn("test-checksum-key");

            // Act & Assert
            assertThatThrownBy(() -> paymentService.handleWebhook(webhook))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("Invalid webhook signature");
        }

        @Test
        @DisplayName("Should handle non-success webhook gracefully")
        void handleWebhook_nonSuccess_skipsProcessing() {
            // Arrange - need valid data to pass null check in verifyWebhookSignature
            PayOSWebhookDTO.WebhookData webhookData = new PayOSWebhookDTO.WebhookData();
            webhookData.setOrderCode(12345L);
            webhookData.setAmount(79000);
            webhookData.setDescription("Test");
            
            PayOSWebhookDTO webhook = new PayOSWebhookDTO();
            webhook.setCode("01"); // Non-success code
            webhook.setSuccess(false);
            webhook.setData(webhookData);
            webhook.setSignature("mock-signature");

            // Mock PayOS as not configured - this makes verifyWebhookSignature return true
            when(payOSConfig.isConfigured()).thenReturn(false);

            // Act - should not throw, and should skip processing because success=false
            paymentService.handleWebhook(webhook);

            // Assert - no order lookup because webhook.isSuccess() = false
            verify(paymentOrderRepository, never()).findByOrderCode(anyLong());
        }
    }

    // =========================================================================
    // SIGNATURE GENERATION TESTS
    // =========================================================================
    @Nested
    @DisplayName("generateSignature() Tests")
    class GenerateSignatureTests {

        @Test
        @DisplayName("Should generate consistent HMAC-SHA256 signature")
        void generateSignature_sameData_sameSignature() {
            // Arrange
            when(payOSConfig.getChecksumKey()).thenReturn("test-secret-key");
            
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("amount", 79000);
            data.put("orderCode", 12345L);
            data.put("description", "Test payment");

            // Act
            String signature1 = paymentService.generateSignature(data);
            String signature2 = paymentService.generateSignature(data);

            // Assert
            assertThat(signature1).isNotNull();
            assertThat(signature1).isNotEmpty();
            assertThat(signature1).isEqualTo(signature2); // Deterministic
        }

        @Test
        @DisplayName("Should generate different signature for different data")
        void generateSignature_differentData_differentSignature() {
            // Arrange
            when(payOSConfig.getChecksumKey()).thenReturn("test-secret-key");
            
            Map<String, Object> data1 = Map.of("amount", 79000, "orderCode", 12345L);
            Map<String, Object> data2 = Map.of("amount", 79001, "orderCode", 12345L); // Different amount

            // Act
            String signature1 = paymentService.generateSignature(data1);
            String signature2 = paymentService.generateSignature(data2);

            // Assert
            assertThat(signature1).isNotEqualTo(signature2);
        }

        @Test
        @DisplayName("Should sort keys alphabetically before signing")
        void generateSignature_sortsKeysAlphabetically() {
            // Arrange
            when(payOSConfig.getChecksumKey()).thenReturn("test-secret-key");
            
            // Same data, different insertion order
            Map<String, Object> data1 = new LinkedHashMap<>();
            data1.put("z", "last");
            data1.put("a", "first");
            
            Map<String, Object> data2 = new LinkedHashMap<>();
            data2.put("a", "first");
            data2.put("z", "last");

            // Act
            String signature1 = paymentService.generateSignature(data1);
            String signature2 = paymentService.generateSignature(data2);

            // Assert - signatures should be equal because keys are sorted
            assertThat(signature1).isEqualTo(signature2);
        }
    }

    // =========================================================================
    // WEBHOOK SIGNATURE VERIFICATION TESTS
    // =========================================================================
    @Nested
    @DisplayName("verifyWebhookSignature() Tests")
    class VerifyWebhookSignatureTests {

        @Test
        @DisplayName("Should return false when signature is null")
        void verifyWebhookSignature_nullSignature_returnsFalse() {
            // Arrange
            PayOSWebhookDTO webhook = new PayOSWebhookDTO();
            webhook.setSignature(null);
            webhook.setData(new PayOSWebhookDTO.WebhookData());

            // Act
            boolean result = paymentService.verifyWebhookSignature(webhook);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when data is null")
        void verifyWebhookSignature_nullData_returnsFalse() {
            // Arrange
            PayOSWebhookDTO webhook = new PayOSWebhookDTO();
            webhook.setSignature("some-signature");
            webhook.setData(null);

            // Act
            boolean result = paymentService.verifyWebhookSignature(webhook);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true in mock mode (PayOS not configured)")
        void verifyWebhookSignature_notConfigured_returnsTrue() {
            // Arrange
            when(payOSConfig.isConfigured()).thenReturn(false);
            
            PayOSWebhookDTO.WebhookData data = new PayOSWebhookDTO.WebhookData();
            data.setOrderCode(12345L);
            
            PayOSWebhookDTO webhook = new PayOSWebhookDTO();
            webhook.setSignature("any-signature");
            webhook.setData(data);

            // Act
            boolean result = paymentService.verifyWebhookSignature(webhook);

            // Assert
            assertThat(result).isTrue();
        }
    }

    // =========================================================================
    // UTILITY METHODS TESTS
    // =========================================================================
    @Nested
    @DisplayName("Utility Methods Tests")
    class UtilityMethodsTests {

        @Test
        @DisplayName("Should check PayOS configuration status")
        void isPayOSConfigured_delegatesToConfig() {
            // Arrange
            when(payOSConfig.isConfigured()).thenReturn(true);

            // Act
            boolean result = paymentService.isPayOSConfigured();

            // Assert
            assertThat(result).isTrue();
            verify(payOSConfig).isConfigured();
        }

        @Test
        @DisplayName("Should get order by order code")
        void getOrderByCode_existingOrder_returnsOrder() {
            // Arrange
            when(paymentOrderRepository.findByOrderCode(testOrder.getOrderCode()))
                    .thenReturn(Optional.of(testOrder));

            // Act
            var result = paymentService.getOrderByCode(testOrder.getOrderCode());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getOrderCode()).isEqualTo(testOrder.getOrderCode());
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void getOrderByCode_notFound_throwsException() {
            // Arrange
            when(paymentOrderRepository.findByOrderCode(99999L))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentService.getOrderByCode(99999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
