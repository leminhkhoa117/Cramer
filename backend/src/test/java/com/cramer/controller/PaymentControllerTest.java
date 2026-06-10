package com.cramer.controller;

import com.cramer.config.JwtAuthFilter;
import com.cramer.config.SecurityConfig;
import com.cramer.dto.PayOSWebhookDTO;
import com.cramer.dto.PaymentCreateDTO;
import com.cramer.dto.PaymentOrderDTO;
import com.cramer.dto.PaymentResponseDTO;
import com.cramer.entity.PaymentOrder;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for PaymentController.
 * Tests subscription payments, Lúa pack purchases, and webhook handling.
 * 
 * @author Cramer Test Team
 * @since 2026-01-19
 */
import com.cramer.config.JwtAuthFilter;
import com.cramer.config.SecurityConfig;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
@DisplayName("PaymentController Unit Tests")
class PaymentControllerTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private com.cramer.util.JwtUtil jwtUtil;

    @MockitoBean
    private PaymentService paymentService;

    private static final UUID DEFAULT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000"); // Standard UUID for tests
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = DEFAULT_USER_ID;
    }

    // =========================================================================
    // POST /api/payments/subscription TESTS
    // =========================================================================
    @Nested
    @DisplayName("POST /api/payments/subscription")
    class CreateSubscriptionPaymentTests {

        @Test
        @DisplayName("Should return 200 and payment link with tierId")
        void createSubscriptionPayment_withTierId_returns200() throws Exception {
            // Arrange
            PaymentResponseDTO response = PaymentResponseDTO.builder()
                    .orderCode(123456789L)
                    .checkoutUrl("https://pay.payos.vn/checkout/123456789")
                    .status(PaymentOrder.Status.PENDING)
                    .amountVnd(79000)
                    .build();

            when(paymentService.createSubscriptionPayment(testUserId, 2)).thenReturn(response);

            String requestBody = """
                {
                    "type": "SUBSCRIPTION",
                    "tierId": 2
                }
                """;

            // Act & Assert
            mockMvc.perform(post("/api/payments/subscription")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderCode").value(123456789))
                    .andExpect(jsonPath("$.checkoutUrl").value("https://pay.payos.vn/checkout/123456789"));

            verify(paymentService).createSubscriptionPayment(testUserId, 2);
        }

        @Test
        @DisplayName("Should return 200 and payment link with tierCode")
        void createSubscriptionPayment_withTierCode_returns200() throws Exception {
            // Arrange
            PaymentResponseDTO response = PaymentResponseDTO.builder()
                    .orderCode(123456790L)
                    .checkoutUrl("https://pay.payos.vn/checkout/123456790")
                    .status(PaymentOrder.Status.PENDING)
                    .build();

            when(paymentService.createSubscriptionPaymentByCode(testUserId, "cramerich"))
                    .thenReturn(response);

            String requestBody = """
                {
                    "type": "SUBSCRIPTION",
                    "tierCode": "cramerich"
                }
                """;

            // Act & Assert
            mockMvc.perform(post("/api/payments/subscription")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderCode").value(123456790));

            verify(paymentService).createSubscriptionPaymentByCode(testUserId, "cramerich");
        }

        @Test
        @DisplayName("Should return 400 when type is not SUBSCRIPTION")
        void createSubscriptionPayment_wrongType_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/payments/subscription")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"type\": \"LUA_PACK\", \"luaAmount\": 100}"))
                    .andExpect(status().isBadRequest());

            verify(paymentService, never()).createSubscriptionPayment(any(), anyInt());
        }

        @Test
        @DisplayName("Should return 400 when no tierId or tierCode provided")
        void createSubscriptionPayment_missingTier_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/payments/subscription")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"type\": \"SUBSCRIPTION\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 403 when no JWT token")
        void createSubscriptionPayment_unauthorized_returns403() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/payments/subscription")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"type\": \"SUBSCRIPTION\", \"tierId\": 2}"))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // POST /api/payments/lua TESTS
    // =========================================================================
    @Nested
    @DisplayName("POST /api/payments/lua")
    class CreateLuaPackPaymentTests {

        @Test
        @DisplayName("Should return 200 and payment link for Lúa pack")
        void createLuaPackPayment_valid_returns200() throws Exception {
            // Arrange
            PaymentResponseDTO response = PaymentResponseDTO.builder()
                    .orderCode(987654321L)
                    .checkoutUrl("https://pay.payos.vn/checkout/987654321")
                    .status(PaymentOrder.Status.PENDING)
                    .amountVnd(10000)
                    .build();

            when(paymentService.createLuaPackPayment(testUserId, 100, 10000)).thenReturn(response);

            String requestBody = """
                {
                    "type": "LUA_PACK",
                    "luaAmount": 100,
                    "priceVnd": 10000
                }
                """;

            // Act & Assert
            mockMvc.perform(post("/api/payments/lua")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderCode").value(987654321))
                    .andExpect(jsonPath("$.amountVnd").value(10000));

            verify(paymentService).createLuaPackPayment(testUserId, 100, 10000);
        }

        @Test
        @DisplayName("Should return 400 when type is not LUA_PACK")
        void createLuaPackPayment_wrongType_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/payments/lua")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"type\": \"SUBSCRIPTION\", \"tierId\": 2}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when luaAmount is null")
        void createLuaPackPayment_missingAmount_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/payments/lua")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"type\": \"LUA_PACK\", \"priceVnd\": 10000}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when priceVnd is null")
        void createLuaPackPayment_missingPrice_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/payments/lua")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"type\": \"LUA_PACK\", \"luaAmount\": 100}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // POST /api/payments/webhook TESTS (Public endpoint)
    // =========================================================================
    @Nested
    @DisplayName("POST /api/payments/webhook")
    class WebhookTests {

        @Test
        @DisplayName("Should return 200 and success code on valid webhook")
        void handleWebhook_valid_returns200() throws Exception {
            // Arrange
            doNothing().when(paymentService).handleWebhook(any(PayOSWebhookDTO.class));

            String webhookBody = """
                {
                    "code": "00",
                    "desc": "success",
                    "data": {
                        "orderCode": 123456789,
                        "amount": 79000,
                        "description": "Subscription upgrade",
                        "code": "00"
                    },
                    "signature": "valid-signature"
                }
                """;

            // Act & Assert - No auth required for webhook
            mockMvc.perform(post("/api/payments/webhook")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(webhookBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("00"))
                    .andExpect(jsonPath("$.message").value("Success"));

            verify(paymentService).handleWebhook(any(PayOSWebhookDTO.class));
        }

        @Test
        @DisplayName("Should return 403 when signature is invalid")
        void handleWebhook_invalidSignature_returns403() throws Exception {
            // Arrange
            doThrow(new SecurityException("Invalid signature"))
                    .when(paymentService).handleWebhook(any(PayOSWebhookDTO.class));

            String webhookBody = """
                {
                    "code": "00",
                    "data": {"orderCode": 123456789},
                    "signature": "invalid-signature"
                }
                """;

            // Act & Assert
            mockMvc.perform(post("/api/payments/webhook")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(webhookBody))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("403"));
        }

        @Test
        @DisplayName("Should return 200 with error code on processing error (prevent retry)")
        void handleWebhook_processingError_returns200WithErrorCode() throws Exception {
            // Arrange
            doThrow(new RuntimeException("Processing error"))
                    .when(paymentService).handleWebhook(any(PayOSWebhookDTO.class));

            String webhookBody = """
                {
                    "code": "00",
                    "data": {"orderCode": 123456789}
                }
                """;

            // Act & Assert - Returns 200 to prevent PayOS from retrying
            mockMvc.perform(post("/api/payments/webhook")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(webhookBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("99"));
        }
    }

    // =========================================================================
    // GET /api/payments/status/{orderCode} TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/payments/status/{orderCode}")
    class GetPaymentStatusTests {

        @Test
        @DisplayName("Should return 200 and order status")
        void getPaymentStatus_valid_returns200() throws Exception {
            // Arrange
            PaymentOrderDTO order = PaymentOrderDTO.builder()
                    .orderCode(123456789L)
                    .userId(testUserId)
                    .type(PaymentOrder.Type.SUBSCRIPTION)
                    .status(PaymentOrder.Status.PAID)
                    .amountVnd(79000)
                    .build();

            when(paymentService.getOrderByCode(123456789L)).thenReturn(order);

            // Act & Assert
            mockMvc.perform(get("/api/payments/status/{orderCode}", 123456789L)
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderCode").value(123456789))
                    .andExpect(jsonPath("$.status").value("PAID"));
        }

        @Test
        @DisplayName("Should return 403 when viewing another user's order")
        void getPaymentStatus_forbidden_returns403() throws Exception {
            // Arrange
            UUID otherUserId = UUID.randomUUID();
            PaymentOrderDTO order = PaymentOrderDTO.builder()
                    .orderCode(123456789L)
                    .userId(otherUserId) // Different user
                    .build();

            when(paymentService.getOrderByCode(123456789L)).thenReturn(order);

            // Act & Assert
            mockMvc.perform(get("/api/payments/status/{orderCode}", 123456789L)
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 404 when order not found")
        void getPaymentStatus_notFound_returns404() throws Exception {
            // Arrange
            when(paymentService.getOrderByCode(999L))
                    .thenThrow(new ResourceNotFoundException("PaymentOrder", "orderCode", 999L));

            // Act & Assert
            mockMvc.perform(get("/api/payments/status/{orderCode}", 999L)
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // GET /api/payments/history TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/payments/history")
    class GetPaymentHistoryTests {

        @Test
        @DisplayName("Should return 200 and paginated history")
        void getPaymentHistory_valid_returns200() throws Exception {
            // Arrange
            PaymentOrderDTO order1 = PaymentOrderDTO.builder()
                    .orderCode(1L)
                    .type(PaymentOrder.Type.SUBSCRIPTION)
                    .status(PaymentOrder.Status.PAID)
                    .build();

            PaymentOrderDTO order2 = PaymentOrderDTO.builder()
                    .orderCode(2L)
                    .type(PaymentOrder.Type.LUA_PACK)
                    .status(PaymentOrder.Status.PAID)
                    .build();

            Page<PaymentOrderDTO> page = new PageImpl<>(
                    List.of(order1, order2),
                    PageRequest.of(0, 20),
                    2
            );

            when(paymentService.getPaymentHistory(eq(testUserId), any(Pageable.class))).thenReturn(page);

            // Act & Assert
            mockMvc.perform(get("/api/payments/history")
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("Should return 200 and empty page when no history")
        void getPaymentHistory_empty_returns200() throws Exception {
            // Arrange
            Page<PaymentOrderDTO> emptyPage = new PageImpl<>(
                    List.of(),
                    PageRequest.of(0, 20),
                    0
            );

            when(paymentService.getPaymentHistory(eq(testUserId), any(Pageable.class))).thenReturn(emptyPage);

            // Act & Assert
            mockMvc.perform(get("/api/payments/history")
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("Should return 403 when no JWT token")
        void getPaymentHistory_unauthorized_returns403() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/payments/history"))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // GET /api/payments/lua-packs TESTS (Public)
    // =========================================================================
    @Nested
    @DisplayName("GET /api/payments/lua-packs")
    class GetLuaPacksTests {

        @Test
        @DisplayName("Should return 200 and lua packs list")
        void getLuaPacks_returns200() throws Exception {
            // Act & Assert - Public endpoint
            mockMvc.perform(get("/api/payments/lua-packs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.packs").isArray())
                    .andExpect(jsonPath("$.packs[0].amount").value(100))
                    .andExpect(jsonPath("$.packs[0].priceVnd").value(10000))
                    .andExpect(jsonPath("$.packs[1].amount").value(500))
                    .andExpect(jsonPath("$.packs[2].amount").value(2000));
        }
    }

    // =========================================================================
    // GET /api/payments/config-status TESTS (Public)
    // =========================================================================
    @Nested
    @DisplayName("GET /api/payments/config-status")
    class GetConfigStatusTests {

        @Test
        @DisplayName("Should return 200 and configured=true when PayOS is set up")
        void getConfigStatus_configured_returns200() throws Exception {
            // Arrange
            when(paymentService.isPayOSConfigured()).thenReturn(true);

            // Act & Assert
            mockMvc.perform(get("/api/payments/config-status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.configured").value(true))
                    .andExpect(jsonPath("$.message").value("PayOS is configured"));
        }

        @Test
        @DisplayName("Should return 200 and configured=false when PayOS is not set up")
        void getConfigStatus_notConfigured_returns200() throws Exception {
            // Arrange
            when(paymentService.isPayOSConfigured()).thenReturn(false);

            // Act & Assert
            mockMvc.perform(get("/api/payments/config-status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.configured").value(false))
                    .andExpect(jsonPath("$.message").value("PayOS credentials not set"));
        }
    }
}
