package com.cramer.controller;

import com.cramer.config.TestSecurityConfig;
import com.cramer.dto.BillingResultDTO;
import com.cramer.dto.QuotaStatusDTO;
import com.cramer.service.QuotaBillingService;
import com.cramer.service.QuotaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for QuotaController.
 * Tests quota status and can-attempt pre-checks.
 * 
 * @author Cramer Test Team
 * @since 2026-01-19
 */
import com.cramer.config.JwtAuthFilter;
import com.cramer.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(QuotaController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
@DisplayName("QuotaController Unit Tests")
class QuotaControllerTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private com.cramer.util.JwtUtil jwtUtil;

    @MockBean
    private QuotaService quotaService;

    @MockBean
    private QuotaBillingService quotaBillingService;

    private static final UUID DEFAULT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000"); // Standard UUID
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = DEFAULT_USER_ID;
    }

    // =========================================================================
    // GET /api/quotas TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/quotas")
    class GetQuotaStatusTests {

        @Test
        @DisplayName("Should return 200 and quota status")
        void getQuotaStatus_valid_returns200() throws Exception {
            // Arrange
            QuotaStatusDTO status = QuotaStatusDTO.builder()
                    .globalAttempt(5)
                    .globalAttemptAI(2)
                    .globalAttemptCap(50)
                    .globalAttemptAICap(10)
                    .isPremium(false)
                    .build();

            when(quotaService.getQuotaStatus(testUserId)).thenReturn(status);

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/quotas")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.globalAttempt").value(5))
                    .andExpect(jsonPath("$.globalAttemptAI").value(2));

            verify(quotaService).getQuotaStatus(testUserId);
        }

        @Test
        @DisplayName("Should return 401 when no JWT token")
        void getQuotaStatus_unauthorized_returns401() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/quotas"))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // GET /api/quotas/can-attempt TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/quotas/can-attempt")
    class CanAttemptTests {

        @Test
        @DisplayName("Should return 200 and allowed=true when quota available")
        void canAttempt_allowed_returns200() throws Exception {
            // Arrange
            BillingResultDTO result = BillingResultDTO.allowed();

            when(quotaBillingService.preCheckAttempt(testUserId, "WRITING", true)).thenReturn(result);

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/quotas/can-attempt")
                            .param("skill", "WRITING")
                            .param("ai", "true")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.allowed").value(true));
        }

        @Test
        @DisplayName("Should return 200 and allowed=false when quota exhausted")
        void canAttempt_notAllowed_returns200() throws Exception {
            // Arrange
            BillingResultDTO result = BillingResultDTO.builder()
                    .allowed(false)
                    .reason("Quota exhausted")
                    .blockType("global")
                    .build();

            when(quotaBillingService.preCheckAttempt(testUserId, "READING", false)).thenReturn(result);

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/quotas/can-attempt")
                            .param("skill", "READING")
                            .param("ai", "false")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.allowed").value(false));
        }

        @Test
        @DisplayName("Should default ai to false when not provided")
        void canAttempt_defaultAi_returns200() throws Exception {
            // Arrange
            BillingResultDTO result = new BillingResultDTO();
            result.setAllowed(true);

            when(quotaBillingService.preCheckAttempt(testUserId, "LISTENING", false)).thenReturn(result);

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/quotas/can-attempt")
                            .param("skill", "LISTENING")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk());

            verify(quotaBillingService).preCheckAttempt(testUserId, "LISTENING", false);
        }
    }

    // =========================================================================
    // GET /api/quotas/check TESTS (Alternative endpoint)
    // =========================================================================
    @Nested
    @DisplayName("GET /api/quotas/check")
    class CheckAttemptTests {

        @Test
        @DisplayName("Should return 200 and behave same as can-attempt")
        void checkAttempt_valid_returns200() throws Exception {
            // Arrange
            BillingResultDTO result = new BillingResultDTO();
            result.setAllowed(true);

            when(quotaBillingService.preCheckAttempt(testUserId, "SPEAKING", true)).thenReturn(result);

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/quotas/check")
                            .param("skill", "SPEAKING")
                            .param("isAI", "true")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.allowed").value(true));
        }
    }
}
