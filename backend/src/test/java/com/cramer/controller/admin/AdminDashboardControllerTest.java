package com.cramer.controller.admin;

import com.cramer.config.JwtAuthFilter;
import com.cramer.config.SecurityConfig;
import com.cramer.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AdminDashboardController.
 * Tests dashboard statistics, recent activities, and system status endpoints.
 * 
 * @author Cramer Test Team
 * @since 2026-01-31
 */
@WebMvcTest(AdminDashboardController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
@DisplayName("AdminDashboardController Unit Tests")
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

        @MockitoBean
    private JdbcTemplate jdbcTemplate;

        @MockitoBean
    private com.cramer.util.JwtUtil jwtUtil;

    private static final String DEFAULT_ADMIN_ID = "550e8400-e29b-41d4-a716-446655440000";

    private ResultActions performGet(String url) throws Exception {
        return mockMvc.perform(get(url)
                .with(jwt().jwt(jwtBuilder -> jwtBuilder
                        .subject(DEFAULT_ADMIN_ID)
                        .claim("aud", "authenticated"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    // =========================================================================
    // GET /api/admin/dashboard/stats TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/admin/dashboard/stats")
    class GetDashboardStatsTests {

        @Test
        @DisplayName("Should return 200 and dashboard statistics")
        void getDashboardStats_valid_returns200() throws Exception {
            // Arrange - Mock queryForObject calls (all use sql String and Class type only)
            when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                    .thenReturn(150L)   // totalUsers
                    .thenReturn(45L)    // activeUsers
                    .thenReturn(12L)    // newUsersThisMonth
                    .thenReturn(500L)   // totalTestAttempts
                    .thenReturn(1000L)  // totalQuestions
                    .thenReturn(10L)    // publishedTests
                    .thenReturn(5000L)  // totalVocabulary
                    .thenReturn(1000000L); // totalRevenue

            // Act & Assert
            performGet("/api/admin/dashboard/stats")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalUsers").value(150))
                    .andExpect(jsonPath("$.activeUsers").value(45))
                    .andExpect(jsonPath("$.newUsersThisMonth").value(12))
                    .andExpect(jsonPath("$.totalTestAttempts").value(500))
                    .andExpect(jsonPath("$.totalQuestions").value(1000))
                    .andExpect(jsonPath("$.publishedTests").value(10))
                    .andExpect(jsonPath("$.totalVocabulary").value(5000))
                    .andExpect(jsonPath("$.totalRevenue").value(1000000))
                    .andExpect(jsonPath("$.changes").exists());
        }

        @Test
        @DisplayName("Should return 200 with default values when queries return null")
        void getDashboardStats_nullValues_returns200WithDefaults() throws Exception {
            // Arrange - All queries return null
            when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(null);

            // Act & Assert
            performGet("/api/admin/dashboard/stats")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalUsers").value(0))
                    .andExpect(jsonPath("$.activeUsers").value(0))
                    .andExpect(jsonPath("$.newUsersThisMonth").value(0))
                    .andExpect(jsonPath("$.totalTestAttempts").value(0))
                    .andExpect(jsonPath("$.totalQuestions").value(0))
                    .andExpect(jsonPath("$.publishedTests").value(0))
                    .andExpect(jsonPath("$.totalVocabulary").value(0))
                    .andExpect(jsonPath("$.totalRevenue").value(0))
                    .andExpect(jsonPath("$.changes").exists());
        }

        @Test
        @DisplayName("Should return 500 when database error occurs on first query")
        void getDashboardStats_dbError_returns500() throws Exception {
            // Arrange - First query throws exception, controller catches and returns 500
            when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                    .thenThrow(new DataAccessException("DB Error") {});

            // Act & Assert
            performGet("/api/admin/dashboard/stats")
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("Should return 403 when not authenticated")
        void getDashboardStats_unauthorized_returns403() throws Exception {
            // Act & Assert - No JWT provided
            mockMvc.perform(get("/api/admin/dashboard/stats"))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // GET /api/admin/dashboard/activities TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/admin/dashboard/activities")
    class GetRecentActivitiesTests {

        @Test
        @DisplayName("Should return 200 and recent activities")
        void getRecentActivities_valid_returns200() throws Exception {
            // Arrange - queryForList(String sql, Object... args) where args = [limit]
            List<Map<String, Object>> activities = new ArrayList<>();
            Map<String, Object> activity = new HashMap<>();
            activity.put("id", 1L);
            activity.put("type", "TEST_COMPLETED");
            activity.put("action", "Completed Cambridge 17 Test 1");
            activity.put("user", "testuser");
            activity.put("created_at", new java.sql.Timestamp(System.currentTimeMillis()));
            activities.add(activity);

            // Mock queryForList with varargs (sql, limit)
            when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(activities);

            // Act & Assert
            performGet("/api/admin/dashboard/activities")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].type").value("TEST_COMPLETED"));
        }

        @Test
        @DisplayName("Should return 200 with custom limit")
        void getRecentActivities_withLimit_returns200() throws Exception {
            // Arrange
            when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(new ArrayList<>());

            // Act & Assert
            mockMvc.perform(get("/api/admin/dashboard/activities")
                            .param("limit", "10")
                            .with(jwt().jwt(jwtBuilder -> jwtBuilder
                                    .subject(DEFAULT_ADMIN_ID)
                                    .claim("aud", "authenticated"))
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should return empty list when no activities")
        void getRecentActivities_noData_returnsEmptyList() throws Exception {
            // Arrange
            when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(new ArrayList<>());

            // Act & Assert
            performGet("/api/admin/dashboard/activities")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Should return empty list when database error occurs")
        void getRecentActivities_dbError_returnsEmptyList() throws Exception {
            // Arrange - Controller catches exception and returns empty list
            when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                    .thenThrow(new DataAccessException("DB Error") {});

            // Act & Assert
            performGet("/api/admin/dashboard/activities")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    // =========================================================================
    // GET /api/admin/dashboard/status TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/admin/dashboard/status")
    class GetSystemStatusTests {

        @Test
        @DisplayName("Should return 200 with all operational status")
        void getSystemStatus_allOperational_returns200() throws Exception {
            // Arrange - queryForObject("SELECT 1", Integer.class)
            when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class))).thenReturn(1);

            // Act & Assert
            performGet("/api/admin/dashboard/status")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.database").value("operational"))
                    .andExpect(jsonPath("$.apiServer").value("operational"))
                    .andExpect(jsonPath("$.paymentGateway").value("operational"))
                    .andExpect(jsonPath("$.aiGrading").value("operational"));
        }

        @Test
        @DisplayName("Should return 200 with database down status when connection fails")
        void getSystemStatus_databaseDown_returnsDownStatus() throws Exception {
            // Arrange
            when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class)))
                    .thenThrow(new DataAccessException("Connection failed") {});

            // Act & Assert
            performGet("/api/admin/dashboard/status")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.database").value("down"))
                    .andExpect(jsonPath("$.apiServer").value("operational"));
        }
    }
}
