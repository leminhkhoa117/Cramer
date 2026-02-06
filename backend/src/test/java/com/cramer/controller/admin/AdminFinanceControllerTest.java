package com.cramer.controller.admin;

import com.cramer.config.JwtAuthFilter;
import com.cramer.config.SecurityConfig;
import com.cramer.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AdminFinanceController.
 * Tests finance dashboard endpoints: overview, chart, transactions.
 * 
 * @author Cramer Test Team
 * @since 2026-01-31
 */
@WebMvcTest(AdminFinanceController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
@DisplayName("AdminFinanceController Unit Tests")
class AdminFinanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private com.cramer.util.JwtUtil jwtUtil;

    private static final String DEFAULT_ADMIN_ID = "550e8400-e29b-41d4-a716-446655440000";

    private ResultActions performGet(String url) throws Exception {
        return mockMvc.perform(get(url)
                .with(jwt().jwt(jwtBuilder -> jwtBuilder
                        .subject(DEFAULT_ADMIN_ID)
                        .claim("aud", "authenticated"))));
    }

    // =========================================================================
    // GET /api/admin/finance/overview TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/admin/finance/overview")
    class GetFinanceOverviewTests {

        @Test
        @DisplayName("Should return 200 and finance overview")
        void getFinanceOverview_valid_returns200() throws Exception {
            // Arrange - Mock all queryForObject calls (6 calls in getFinanceOverview)
            when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                    .thenReturn(1000000L)  // totalRevenue
                    .thenReturn(800000L)   // subscriptionRevenue
                    .thenReturn(200000L)   // luaRevenue
                    .thenReturn(10L)       // newSubscriptions
                    .thenReturn(5L)        // luaPacksSold
                    .thenReturn(2L);       // pendingTransactions

            // Act & Assert
            performGet("/api/admin/finance/overview")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalRevenue").value(1000000))
                    .andExpect(jsonPath("$.subscriptionRevenue").value(800000))
                    .andExpect(jsonPath("$.luaRevenue").value(200000))
                    .andExpect(jsonPath("$.newSubscriptions").value(10))
                    .andExpect(jsonPath("$.luaPacksSold").value(5))
                    .andExpect(jsonPath("$.pendingTransactions").value(2));
        }

        @Test
        @DisplayName("Should return 200 with default values when queries return null")
        void getFinanceOverview_nullValues_returns200() throws Exception {
            // Arrange
            when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(null);

            // Act & Assert
            performGet("/api/admin/finance/overview")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalRevenue").value(0))
                    .andExpect(jsonPath("$.subscriptionRevenue").value(0))
                    .andExpect(jsonPath("$.luaRevenue").value(0));
        }

        @Test
        @DisplayName("Should return 500 when database error occurs")
        void getFinanceOverview_dbError_returns500() throws Exception {
            // Arrange
            when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                    .thenThrow(new DataAccessException("DB Error") {});

            // Act & Assert
            performGet("/api/admin/finance/overview")
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("Should return 403 when not authenticated")
        void getFinanceOverview_unauthorized_returns403() throws Exception {
            // Act & Assert - No JWT provided
            mockMvc.perform(get("/api/admin/finance/overview"))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // GET /api/admin/finance/chart TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/admin/finance/chart")
    class GetRevenueChartTests {

        @Test
        @DisplayName("Should return 200 and chart data")
        void getRevenueChart_valid_returns200() throws Exception {
            // Arrange
            List<Map<String, Object>> chartData = new ArrayList<>();
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("date", "2026-01-01");
            dataPoint.put("total", 100000L);
            dataPoint.put("subscriptions", 80000L);
            dataPoint.put("lua", 20000L);
            chartData.add(dataPoint);

            when(jdbcTemplate.queryForList(anyString())).thenReturn(chartData);

            // Act & Assert
            performGet("/api/admin/finance/chart")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].date").value("2026-01-01"))
                    .andExpect(jsonPath("$[0].total").value(100000));
        }

        @Test
        @DisplayName("Should return empty list when database error occurs")
        void getRevenueChart_dbError_returnsEmptyList() throws Exception {
            // Arrange - Controller catches exception and returns empty list
            when(jdbcTemplate.queryForList(anyString()))
                    .thenThrow(new DataAccessException("DB Error") {});

            // Act & Assert
            performGet("/api/admin/finance/chart")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Should return empty list when no data")
        void getRevenueChart_noData_returnsEmptyList() throws Exception {
            // Arrange
            when(jdbcTemplate.queryForList(anyString())).thenReturn(new ArrayList<>());

            // Act & Assert
            performGet("/api/admin/finance/chart")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    // =========================================================================
    // GET /api/admin/finance/breakdown TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/admin/finance/breakdown")
    class GetRevenueBreakdownTests {

        @Test
        @DisplayName("Should return 200 and breakdown data")
        void getRevenueBreakdown_valid_returns200() throws Exception {
            // Arrange
            List<Map<String, Object>> breakdown = new ArrayList<>();
            Map<String, Object> item = new HashMap<>();
            item.put("name", "cramerich");
            item.put("value", 500000L);
            item.put("color", "#8B5CF6");
            breakdown.add(item);

            when(jdbcTemplate.queryForList(anyString())).thenReturn(breakdown);

            // Act & Assert
            performGet("/api/admin/finance/breakdown")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].name").value("Cramerich")) // Controller formats name
                    .andExpect(jsonPath("$[0].value").value(500000));
        }

        @Test
        @DisplayName("Should return empty list when database error occurs")
        void getRevenueBreakdown_dbError_returnsEmptyList() throws Exception {
            // Arrange
            when(jdbcTemplate.queryForList(anyString()))
                    .thenThrow(new DataAccessException("DB Error") {});

            // Act & Assert
            performGet("/api/admin/finance/breakdown")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    // =========================================================================
    // GET /api/admin/finance/top-spenders TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/admin/finance/top-spenders")
    class GetTopSpendersTests {

        @Test
        @DisplayName("Should return 200 and top spenders list")
        void getTopSpenders_valid_returns200() throws Exception {
            // Arrange - queryForList(sql, limit) with varargs
            List<Map<String, Object>> spenders = new ArrayList<>();
            Map<String, Object> spender = new HashMap<>();
            spender.put("user_id", "user-123");
            spender.put("username", "topuser");
            spender.put("full_name", "Top User");
            spender.put("total_spent", 1000000L);
            spenders.add(spender);

            when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(spenders);

            // Act & Assert
            performGet("/api/admin/finance/top-spenders")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].username").value("topuser"))
                    .andExpect(jsonPath("$[0].rank").value(1));
        }

        @Test
        @DisplayName("Should return empty list when database error occurs")
        void getTopSpenders_dbError_returnsEmptyList() throws Exception {
            // Arrange
            when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                    .thenThrow(new DataAccessException("DB Error") {});

            // Act & Assert
            performGet("/api/admin/finance/top-spenders")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    // =========================================================================
    // GET /api/admin/finance/transactions TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/admin/finance/transactions")
    class GetTransactionsTests {

        @Test
        @DisplayName("Should return 200 and paginated transactions")
        void getTransactions_valid_returns200() throws Exception {
            // Arrange - queryForObject for count, queryForList for data
            when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                    .thenReturn(1L);

            List<Map<String, Object>> transactions = new ArrayList<>();
            Map<String, Object> tx = new HashMap<>();
            tx.put("id", 1L);
            tx.put("order_code", "ORD123");
            tx.put("username", "testuser");
            tx.put("amount", 100000L);
            tx.put("status", "PAID");
            transactions.add(tx);

            when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(transactions);

            // Act & Assert
            performGet("/api/admin/finance/transactions")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].order_code").value("ORD123"))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.page").value(0));
        }

        @Test
        @DisplayName("Should return 500 when database error occurs")
        void getTransactions_dbError_returns500() throws Exception {
            // Arrange
            when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                    .thenThrow(new DataAccessException("DB Error") {});

            // Act & Assert
            performGet("/api/admin/finance/transactions")
                    .andExpect(status().isInternalServerError());
        }
    }

    // =========================================================================
    // GET /api/admin/finance/export TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/admin/finance/export")
    class GetExportDataTests {

        @Test
        @DisplayName("Should return 200 and export data")
        void getExportData_valid_returns200() throws Exception {
            // Arrange
            List<Map<String, Object>> exportData = new ArrayList<>();
            Map<String, Object> row = new HashMap<>();
            row.put("Mã đơn hàng", "ORD123");
            row.put("Số tiền (VNĐ)", 100000L);
            exportData.add(row);

            when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(exportData);

            // Act & Assert
            performGet("/api/admin/finance/export")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }
}
