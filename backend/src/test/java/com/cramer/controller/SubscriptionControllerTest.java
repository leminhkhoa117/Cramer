package com.cramer.controller;

import com.cramer.config.TestSecurityConfig;
import com.cramer.dto.GradingStatusDTO;
import com.cramer.dto.SubscriptionStatusDTO;
import com.cramer.dto.SubscriptionTierDTO;
import com.cramer.dto.UserSubscriptionDTO;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for SubscriptionController.
 * Tests subscription tiers, user subscription, and AI grading status.
 * 
 * @author Cramer Test Team
 * @since 2026-01-19
 */
import com.cramer.config.JwtAuthFilter;
import com.cramer.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@WebMvcTest(SubscriptionController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
@DisplayName("SubscriptionController Unit Tests")
class SubscriptionControllerTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private com.cramer.util.JwtUtil jwtUtil;

    @MockitoBean
    private SubscriptionService subscriptionService;

    private static final UUID DEFAULT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000"); // Standard UUID
    private UUID testUserId;
    private List<SubscriptionTierDTO> mockTiers;
    private SubscriptionTierDTO cramerieDTO;
    private SubscriptionTierDTO cramerichDTO;

    @BeforeEach
    void setUp() {
        testUserId = DEFAULT_USER_ID;

        cramerieDTO = new SubscriptionTierDTO();
        cramerieDTO.setId(1L);
        cramerieDTO.setCode("cramerie");
        cramerieDTO.setName("Cramerie");
        cramerieDTO.setPriceVnd(0);
        cramerieDTO.setIncludedAiGradings(0);
        cramerieDTO.setDailyChatLimit(20);

        cramerichDTO = new SubscriptionTierDTO();
        cramerichDTO.setId(2L);
        cramerichDTO.setCode("cramerich");
        cramerichDTO.setName("Cramerich");
        cramerichDTO.setPriceVnd(79000);
        cramerichDTO.setIncludedAiGradings(5);
        cramerichDTO.setDailyChatLimit(100);

        mockTiers = List.of(cramerieDTO, cramerichDTO);
    }

    // =========================================================================
    // GET /api/subscriptions/tiers TESTS (Public)
    // =========================================================================
    @Nested
    @DisplayName("GET /api/subscriptions/tiers")
    class GetAllTiersTests {

        @Test
        @DisplayName("Should return 200 and all tiers")
        void getAllTiers_returns200() throws Exception {
            // Arrange
            when(subscriptionService.getAllTiers()).thenReturn(mockTiers);

            // Act & Assert - Public endpoint, but secured by default config
            mockMvc.perform(get("/api/subscriptions/tiers")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].code").value("cramerie"))
                    .andExpect(jsonPath("$[1].code").value("cramerich"));

            verify(subscriptionService).getAllTiers();
        }
    }

    // =========================================================================
    // GET /api/subscriptions/tiers/{code} TESTS (Public)
    // =========================================================================
    @Nested
    @DisplayName("GET /api/subscriptions/tiers/{code}")
    class GetTierByCodeTests {

        @Test
        @DisplayName("Should return 200 and tier when found")
        void getTierByCode_valid_returns200() throws Exception {
            // Arrange
            when(subscriptionService.getTierByCode("cramerich")).thenReturn(cramerichDTO);

            // Act & Assert
            mockMvc.perform(get("/api/subscriptions/tiers/cramerich")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("cramerich"))
                    .andExpect(jsonPath("$.priceVnd").value(79000));

            verify(subscriptionService).getTierByCode("cramerich");
        }

        @Test
        @DisplayName("Should return 404 when tier not found")
        void getTierByCode_notFound_returns404() throws Exception {
            // Arrange
            when(subscriptionService.getTierByCode("invalid"))
                    .thenThrow(new ResourceNotFoundException("Tier", "code", "invalid"));

            // Act & Assert
            mockMvc.perform(get("/api/subscriptions/tiers/invalid")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // GET /api/subscriptions/current TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/subscriptions/current")
    class GetCurrentSubscriptionTests {

        @Test
        @DisplayName("Should return 200 and user subscription")
        void getCurrentSubscription_returns200() throws Exception {
            // Arrange
            UserSubscriptionDTO subscription = new UserSubscriptionDTO();
            subscription.setId(1L);
            subscription.setUserId(testUserId);
            subscription.setTier(cramerieDTO);
            subscription.setStatus("ACTIVE");

            when(subscriptionService.getUserSubscription(testUserId)).thenReturn(subscription);

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/subscriptions/current")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tier.code").value("cramerie"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            verify(subscriptionService).getUserSubscription(testUserId);
        }

        @Test
        @DisplayName("Should return 401 when no JWT token provided")
        void getCurrentSubscription_unauthorized_returns401() throws Exception {
            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/subscriptions/current"))
                    .andExpect(status().isForbidden());

            verify(subscriptionService, never()).getUserSubscription(any());
        }
    }

    // =========================================================================
    // GET /api/subscriptions/grading-status TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/subscriptions/grading-status")
    class GetGradingStatusTests {

        @Test
        @DisplayName("Should return 200 with allowed=true when has quota")
        void getGradingStatus_allowed_returns200() throws Exception {
            // Arrange
            GradingStatusDTO status = new GradingStatusDTO();
            status.setAllowed(true);
            status.setRemaining(3);
            status.setCanUseExtraWithLua(false);

            when(subscriptionService.checkAIGradingAllowed(testUserId)).thenReturn(status);

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/subscriptions/grading-status")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.allowed").value(true))
                    .andExpect(jsonPath("$.remaining").value(3));
        }

        @Test
        @DisplayName("Should return 200 with allowed=false when quota exhausted")
        void getGradingStatus_exhausted_returns200() throws Exception {
            // Arrange
            GradingStatusDTO status = new GradingStatusDTO();
            status.setAllowed(false);
            status.setRemaining(0);
            status.setCanUseExtraWithLua(true);
            status.setLuaBalance(150);

            when(subscriptionService.checkAIGradingAllowed(testUserId)).thenReturn(status);

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/subscriptions/grading-status")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.allowed").value(false))
                    .andExpect(jsonPath("$.remaining").value(0))
                    .andExpect(jsonPath("$.canUseExtraWithLua").value(true));
        }
    }

    // =========================================================================
    // GET /api/subscriptions/gradings-remaining TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/subscriptions/gradings-remaining")
    class GetGradingsRemainingTests {

        @Test
        @DisplayName("Should return 200 and remaining count")
        void getGradingsRemaining_returns200() throws Exception {
            // Arrange
            when(subscriptionService.getMonthlyGradingsRemaining(testUserId)).thenReturn(3);

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/subscriptions/gradings-remaining")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(content().string("3"));
        }
    }

    // =========================================================================
    // GET /api/subscriptions/chat-limit TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/subscriptions/chat-limit")
    class GetChatLimitTests {

        @Test
        @DisplayName("Should return 200 and chat limit")
        void getChatLimit_returns200() throws Exception {
            // Arrange
            when(subscriptionService.getMonthlyChatLimit(testUserId)).thenReturn(100);

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/subscriptions/chat-limit")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(content().string("100"));
        }
    }

    // =========================================================================
    // GET /api/subscriptions/my-status TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/subscriptions/my-status")
    class GetMyStatusTests {

        @Test
        @DisplayName("Should return 200 and full status DTO")
        void getMyStatus_returns200() throws Exception {
            // Arrange
            SubscriptionStatusDTO status = new SubscriptionStatusDTO();
            // Set up mock status with nested structures

            when(subscriptionService.getSubscriptionStatus(testUserId)).thenReturn(status);

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/subscriptions/my-status")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk());

            verify(subscriptionService).getSubscriptionStatus(testUserId);
        }
    }

    // =========================================================================
    // PUT /api/subscriptions/ai-grading TESTS
    // =========================================================================
    @Nested
    @DisplayName("PUT /api/subscriptions/ai-grading")
    class ToggleAiGradingTests {

        @Test
        @DisplayName("Should return 200 when enabling AI grading")
        void toggleAiGrading_enable_returns200() throws Exception {
            // Arrange
            when(subscriptionService.setAiGradingEnabled(testUserId, true)).thenReturn(true);

            // Act & Assert
            mockMvc.perform(put("/api/subscriptions/ai-grading")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"enabled\": true}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.aiGradingEnabled").value(true));

            verify(subscriptionService).setAiGradingEnabled(testUserId, true);
        }

        @Test
        @DisplayName("Should return 200 when disabling AI grading")
        void toggleAiGrading_disable_returns200() throws Exception {
            // Arrange
            when(subscriptionService.setAiGradingEnabled(testUserId, false)).thenReturn(false);

            // Act & Assert
            mockMvc.perform(put("/api/subscriptions/ai-grading")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"enabled\": false}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.aiGradingEnabled").value(false));
        }

        @Test
        @DisplayName("Should return 403 when free tier user tries to enable")
        void toggleAiGrading_cramerieUser_returns403() throws Exception {
            // Arrange
            when(subscriptionService.setAiGradingEnabled(testUserId, true))
                    .thenThrow(new IllegalStateException("Cramerie users cannot enable AI grading"));

            // Act & Assert
            mockMvc.perform(put("/api/subscriptions/ai-grading")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"enabled\": true}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 400 when enabled field is missing")
        void toggleAiGrading_missingField_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(put("/api/subscriptions/ai-grading")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 when no JWT token provided")
        void toggleAiGrading_unauthorized_returns401() throws Exception {
            // Act & Assert
            mockMvc.perform(put("/api/subscriptions/ai-grading")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"enabled\": true}"))
                    .andExpect(status().isForbidden());
        }
    }
}
