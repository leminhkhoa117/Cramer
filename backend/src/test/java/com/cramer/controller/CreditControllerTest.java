package com.cramer.controller;

import com.cramer.dto.CreditHistoryDTO;
import com.cramer.dto.CreditTransactionDTO;
import com.cramer.dto.LuaPurchaseResponseDTO;
import com.cramer.dto.UserCreditDTO;
import com.cramer.dto.UserFullStatsDTO;
import com.cramer.entity.CreditTransaction;
import com.cramer.service.CreditService;
import com.cramer.service.LuaCreditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for CreditController.
 * Tests credit balance, transactions, packages, and purchases.
 * 
 * @author Cramer Test Team
 * @since 2026-01-19
 */
@org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest(CreditController.class)
@DisplayName("CreditController Unit Tests")
class CreditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreditService creditService;

    @MockBean
    private LuaCreditService luaCreditService;

    @MockBean
    private com.cramer.util.JwtUtil jwtUtil;

    private static final UUID DEFAULT_USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String DEFAULT_USER_ID_STRING = "550e8400-e29b-41d4-a716-446655440000";
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = DEFAULT_USER_ID;
    }

    private org.springframework.test.web.servlet.ResultActions performGet(String url, Object... uriVars) throws Exception {
        return mockMvc.perform(get(url, uriVars)
                .with(jwt().jwt(jwtBuilder -> jwtBuilder
                        .subject(DEFAULT_USER_ID_STRING)
                        .claim("aud", "authenticated"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private org.springframework.test.web.servlet.ResultActions performPost(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .with(jwt().jwt(jwtBuilder -> jwtBuilder
                        .subject(DEFAULT_USER_ID_STRING)
                        .claim("aud", "authenticated"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    // =========================================================================
    // GET /api/credits TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/credits")
    class GetBalanceTests {

        @Test
        @DisplayName("Should return 200 and user credit balance")
        void getBalance_valid_returns200() throws Exception {
            // Arrange
            UserCreditDTO credits = new UserCreditDTO();
            credits.setBalance(150);
            credits.setLifetimeEarned(300);
            credits.setLifetimeSpent(150);

            when(creditService.getBalance(testUserId)).thenReturn(credits);

            // Act & Assert
            performGet("/api/credits")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(150))
                    .andExpect(jsonPath("$.lifetimeEarned").value(300))
                    .andExpect(jsonPath("$.lifetimeSpent").value(150));

            verify(creditService).getBalance(testUserId);
        }

        @Test
        @DisplayName("Should return 401 when no JWT token")
        void getBalance_unauthorized_returns401() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/credits"))
                    .andExpect(status().isUnauthorized());

            verify(creditService, never()).getBalance(any());
        }
    }

    // =========================================================================
    // GET /api/credits/check/{amount} TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/credits/check/{amount}")
    class HasEnoughCreditsTests {

        @Test
        @DisplayName("Should return 200 and true when user has enough credits")
        void hasEnoughCredits_true_returns200() throws Exception {
            // Arrange
            when(creditService.hasEnoughCredits(testUserId, 50)).thenReturn(true);

            // Act & Assert
            performGet("/api/credits/check/{amount}", 50)
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));
        }

        @Test
        @DisplayName("Should return 200 and false when user has insufficient credits")
        void hasEnoughCredits_false_returns200() throws Exception {
            // Arrange
            when(creditService.hasEnoughCredits(testUserId, 1000)).thenReturn(false);

            // Act & Assert
            performGet("/api/credits/check/{amount}", 1000)
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));
        }
    }

    // =========================================================================
    // GET /api/credits/transactions TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/credits/transactions")
    class GetTransactionHistoryTests {

        @Test
        @DisplayName("Should return 200 and paginated transactions")
        void getTransactionHistory_valid_returns200() throws Exception {
            // Arrange
            CreditTransactionDTO tx1 = new CreditTransactionDTO();
            tx1.setId(1L);
            tx1.setAmount(50);
            tx1.setType("EARN");
            tx1.setCategory("STREAK_BONUS");

            CreditTransactionDTO tx2 = new CreditTransactionDTO();
            tx2.setId(2L);
            tx2.setAmount(-10);
            tx2.setType("SPEND");
            tx2.setCategory("AI_GRADING");

            Page<CreditTransactionDTO> page = new PageImpl<>(
                    List.of(tx1, tx2),
                    PageRequest.of(0, 20),
                    2
            );

            when(creditService.getTransactionHistory(eq(testUserId), any(Pageable.class)))
                    .thenReturn(page);

            // Act & Assert
            performGet("/api/credits/transactions")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].type").value("EARN"))
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("Should return 200 and empty page when no transactions")
        void getTransactionHistory_empty_returns200() throws Exception {
            // Arrange
            Page<CreditTransactionDTO> emptyPage = new PageImpl<>(
                    List.of(),
                    PageRequest.of(0, 20),
                    0
            );

            when(creditService.getTransactionHistory(eq(testUserId), any(Pageable.class)))
                    .thenReturn(emptyPage);

            // Act & Assert
            performGet("/api/credits/transactions")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("Should respect pagination parameters")
        void getTransactionHistory_withParams_returns200() throws Exception {
            // Arrange
            Page<CreditTransactionDTO> page = new PageImpl<>(
                    List.of(),
                    PageRequest.of(2, 10),
                    25
            );

            when(creditService.getTransactionHistory(eq(testUserId), any(Pageable.class)))
                    .thenReturn(page);

            // Act & Assert
            performGet("/api/credits/transactions?page=2&size=10")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pageNumber").value(2))
                    .andExpect(jsonPath("$.pageSize").value(10));
        }
    }

    // =========================================================================
    // GET /api/credits/stats TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/credits/stats")
    class GetUserStatsTests {

        @Test
        @DisplayName("Should return 200 and full user stats")
        void getUserStats_valid_returns200() throws Exception {
            // Arrange
            UserFullStatsDTO stats = new UserFullStatsDTO();
            stats.setUserId(testUserId);
            stats.setLuaBalance(200);
            stats.setCurrentStreak(5);

            when(creditService.getUserStats(testUserId)).thenReturn(stats);

            // Act & Assert
            performGet("/api/credits/stats")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.luaBalance").value(200))
                    .andExpect(jsonPath("$.currentStreak").value(5));

            verify(creditService).getUserStats(testUserId);
        }
    }

    // =========================================================================
    // GET /api/credits/packages TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/credits/packages")
    class GetAvailablePackagesTests {

        @Test
        @DisplayName("Should return 200 and list of Lúa packages")
        void getAvailablePackages_returns200() throws Exception {
            // Arrange
            LuaCreditService.LuaPackage pkg1 = new LuaCreditService.LuaPackage(
                    "lua_100", "Túi Lúa", 100, 10000, 0);
            LuaCreditService.LuaPackage pkg2 = new LuaCreditService.LuaPackage(
                    "lua_500", "Bao Lúa", 500, 45000, 10);

            when(luaCreditService.getAvailablePackages()).thenReturn(List.of(pkg1, pkg2));

            // Act & Assert - This could be public or require auth depending on design
            mockMvc.perform(get("/api/credits/packages")
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].code").value("lua_100"))
                    .andExpect(jsonPath("$[0].luaAmount").value(100))
                    .andExpect(jsonPath("$[1].code").value("lua_500"))
                    .andExpect(jsonPath("$[1].bonusPercent").value(10));
        }
    }

    // =========================================================================
    // POST /api/credits/purchase TESTS
    // =========================================================================
    @Nested
    @DisplayName("POST /api/credits/purchase")
    class PurchasePackageTests {

        @Test
        @DisplayName("Should return 200 and payment link on successful purchase init")
        void purchasePackage_valid_returns200() throws Exception {
            // Arrange
            LuaPurchaseResponseDTO response = new LuaPurchaseResponseDTO();
            response.setSuccess(true);
            response.setCheckoutUrl("https://pay.payos.vn/checkout/123456");
            response.setOrderCode(123456L);

            when(luaCreditService.initiatePurchase(testUserId, "lua_100")).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/credits/purchase")
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"packageCode\": \"lua_100\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.checkoutUrl").value("https://pay.payos.vn/checkout/123456"));

            verify(luaCreditService).initiatePurchase(testUserId, "lua_100");
        }

        @Test
        @DisplayName("Should return 400 when package code is invalid")
        void purchasePackage_invalidPackage_returns400() throws Exception {
            // Arrange
            LuaPurchaseResponseDTO response = new LuaPurchaseResponseDTO();
            response.setSuccess(false);
            response.setMessage("Invalid package code");

            when(luaCreditService.initiatePurchase(testUserId, "invalid_code")).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/credits/purchase")
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"packageCode\": \"invalid_code\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should return 403 when no JWT token")
        void purchasePackage_unauthorized_returns403() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/credits/purchase")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"packageCode\": \"lua_100\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // GET /api/credits/history TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/credits/history")
    class GetHistoryTests {

        @Test
        @DisplayName("Should return 200 and filtered history - all")
        void getHistory_all_returns200() throws Exception {
            // Arrange
            CreditHistoryDTO item1 = new CreditHistoryDTO();
            item1.setId(1L);
            item1.setType(CreditTransaction.Type.EARN);
            item1.setAmount(50);

            Page<CreditHistoryDTO> page = new PageImpl<>(
                    List.of(item1),
                    PageRequest.of(0, 20),
                    1
            );

            when(luaCreditService.getTransactionHistory(eq(testUserId), eq("all"), any(Pageable.class)))
                    .thenReturn(page);

            // Act & Assert
            performGet("/api/credits/history?type=all")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].type").value("EARN"));
        }

        @Test
        @DisplayName("Should return 200 and filtered history - earn only")
        void getHistory_earn_returns200() throws Exception {
            // Arrange
            Page<CreditHistoryDTO> page = new PageImpl<>(
                    List.of(),
                    PageRequest.of(0, 20),
                    0
            );

            when(luaCreditService.getTransactionHistory(eq(testUserId), eq("earn"), any(Pageable.class)))
                    .thenReturn(page);

            // Act & Assert
            performGet("/api/credits/history?type=earn")
                    .andExpect(status().isOk());

            verify(luaCreditService).getTransactionHistory(eq(testUserId), eq("earn"), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return 200 and filtered history - spend only")
        void getHistory_spend_returns200() throws Exception {
            // Arrange
            Page<CreditHistoryDTO> page = new PageImpl<>(
                    List.of(),
                    PageRequest.of(0, 20),
                    0
            );

            when(luaCreditService.getTransactionHistory(eq(testUserId), eq("spend"), any(Pageable.class)))
                    .thenReturn(page);

            // Act & Assert
            performGet("/api/credits/history?type=spend")
                    .andExpect(status().isOk());

            verify(luaCreditService).getTransactionHistory(eq(testUserId), eq("spend"), any(Pageable.class));
        }

        @Test
        @DisplayName("Should default to 'all' when type not specified")
        void getHistory_defaultType_returns200() throws Exception {
            // Arrange
            Page<CreditHistoryDTO> page = new PageImpl<>(
                    List.of(),
                    PageRequest.of(0, 20),
                    0
            );

            when(luaCreditService.getTransactionHistory(eq(testUserId), eq("all"), any(Pageable.class)))
                    .thenReturn(page);

            // Act & Assert
            performGet("/api/credits/history")
                    .andExpect(status().isOk());

            verify(luaCreditService).getTransactionHistory(eq(testUserId), eq("all"), any(Pageable.class));
        }
    }
}
