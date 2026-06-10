package com.cramer.controller.admin;

import com.cramer.config.JwtAuthFilter;
import com.cramer.config.SecurityConfig;
import com.cramer.dto.AdminUserDTO;
import com.cramer.dto.AdminUserListResponse;
import com.cramer.exception.GlobalExceptionHandler;
import com.cramer.service.AdminUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.OffsetDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AdminUserController.
 * Tests user management endpoints: list, details, status, credits, subscription.
 * 
 * @author Cramer Test Team
 * @since 2026-01-31
 */
@WebMvcTest(AdminUserController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
@DisplayName("AdminUserController Unit Tests")
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

        @MockitoBean
    private AdminUserService adminUserService;

        @MockitoBean
    private com.cramer.util.JwtUtil jwtUtil;

    private static final String DEFAULT_ADMIN_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_USER_ID = "660e8400-e29b-41d4-a716-446655440001";

    private AdminUserDTO testUserDTO;

    @BeforeEach
    void setUp() {
        testUserDTO = new AdminUserDTO();
        testUserDTO.setId(TEST_USER_ID);
        testUserDTO.setUsername("testuser");
        testUserDTO.setEmail("test@example.com");
        testUserDTO.setFullName("Test User");
        testUserDTO.setCredits(100);
        testUserDTO.setSubscription("cramerie");
        testUserDTO.setAccountStatus("ACTIVE");
        testUserDTO.setCreatedAt(OffsetDateTime.now());
    }

    private ResultActions performGet(String url, Object... uriVars) throws Exception {
        return mockMvc.perform(get(url, uriVars)
                .header("X-User-Id", DEFAULT_ADMIN_ID)
                .with(jwt().jwt(jwtBuilder -> jwtBuilder
                        .subject(DEFAULT_ADMIN_ID)
                        .claim("aud", "authenticated"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private ResultActions performPatch(String url, Object body) throws Exception {
        return mockMvc.perform(patch(url)
                .header("X-User-Id", DEFAULT_ADMIN_ID)
                .with(jwt().jwt(jwtBuilder -> jwtBuilder
                        .subject(DEFAULT_ADMIN_ID)
                        .claim("aud", "authenticated"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    // =========================================================================
    // GET /api/admin/users TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/admin/users")
    class GetUsersTests {

        @Test
        @DisplayName("Should return 200 and paginated user list")
        void getUsers_valid_returns200() throws Exception {
            // Arrange
            List<AdminUserDTO> users = Arrays.asList(testUserDTO);
            AdminUserListResponse response = new AdminUserListResponse(users, 0, 25, 1L);

            when(adminUserService.getUsers(
                    anyInt(), anyInt(), any(), any(), any(), anyString(), anyString()))
                    .thenReturn(response);

            // Act & Assert
            performGet("/api/admin/users")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users").isArray())
                    .andExpect(jsonPath("$.users.length()").value(1))
                    .andExpect(jsonPath("$.users[0].username").value("testuser"))
                    .andExpect(jsonPath("$.totalItems").value(1))
                    .andExpect(jsonPath("$.currentPage").value(0))
                    .andExpect(jsonPath("$.pageSize").value(25));

            verify(adminUserService).getUsers(0, 25, null, null, null, "createdAt", "desc");
        }

        @Test
        @DisplayName("Should return 200 with search filter")
        void getUsers_withSearch_returns200() throws Exception {
            // Arrange
            AdminUserListResponse response = new AdminUserListResponse(
                    Collections.emptyList(), 0, 25, 0L);

            when(adminUserService.getUsers(
                    eq(0), eq(25), eq("john"), isNull(), isNull(), eq("createdAt"), eq("desc")))
                    .thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/admin/users")
                    .param("search", "john")
                    .header("X-User-Id", DEFAULT_ADMIN_ID)
                    .with(jwt().jwt(jwtBuilder -> jwtBuilder
                            .subject(DEFAULT_ADMIN_ID)
                            .claim("aud", "authenticated"))
                            .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users").isArray());

            verify(adminUserService).getUsers(0, 25, "john", null, null, "createdAt", "desc");
        }
    }

    // =========================================================================
    // GET /api/admin/users/{id} TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/admin/users/{id}")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should return 200 and user details when found")
        void getUserById_exists_returns200() throws Exception {
            // Arrange
            when(adminUserService.getUserById(TEST_USER_ID)).thenReturn(testUserDTO);

            // Act & Assert
            performGet("/api/admin/users/{id}", TEST_USER_ID)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(TEST_USER_ID))
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.email").value("test@example.com"));

            verify(adminUserService).getUserById(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void getUserById_notFound_returns404() throws Exception {
            // Arrange
            when(adminUserService.getUserById("nonexistent")).thenReturn(null);

            // Act & Assert
            performGet("/api/admin/users/{id}", "nonexistent")
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // GET /api/admin/users/stats TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/admin/users/stats")
    class GetUserStatsTests {

        @Test
        @DisplayName("Should return 200 and user statistics")
        void getUserStats_valid_returns200() throws Exception {
            // Arrange
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", 100L);
            stats.put("activeUsers", 80L);
            stats.put("bannedUsers", 5L);

            when(adminUserService.getUserStats()).thenReturn(stats);

            // Act & Assert
            performGet("/api/admin/users/stats")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalUsers").value(100))
                    .andExpect(jsonPath("$.activeUsers").value(80));
        }
    }

    // =========================================================================
    // PATCH /api/admin/users/{id}/status TESTS
    // =========================================================================
    @Nested
    @DisplayName("PATCH /api/admin/users/{id}/status")
    class UpdateUserStatusTests {

        @Test
        @DisplayName("Should return 200 when banning user")
        void updateUserStatus_ban_returns200() throws Exception {
            // Arrange
            Map<String, String> request = new HashMap<>();
            request.put("status", "BANNED");
            request.put("reason", "Violation of terms");

            AdminUserDTO bannedUser = new AdminUserDTO();
            bannedUser.setId(TEST_USER_ID);
            bannedUser.setAccountStatus("BANNED");

            when(adminUserService.updateUserStatus(
                    eq(TEST_USER_ID), eq("BANNED"), eq("Violation of terms"), eq(DEFAULT_ADMIN_ID)))
                    .thenReturn(bannedUser);

            // Act & Assert
            performPatch("/api/admin/users/" + TEST_USER_ID + "/status", request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountStatus").value("BANNED"));
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void updateUserStatus_notFound_returns404() throws Exception {
            // Arrange
            Map<String, String> request = new HashMap<>();
            request.put("status", "BANNED");
            request.put("reason", "Test");

            when(adminUserService.updateUserStatus(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(null);

            // Act & Assert
            performPatch("/api/admin/users/nonexistent/status", request)
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // PATCH /api/admin/users/{id}/credits TESTS
    // =========================================================================
    @Nested
    @DisplayName("PATCH /api/admin/users/{id}/credits")
    class UpdateUserCreditsTests {

        @Test
        @DisplayName("Should return 200 when adding credits")
        void updateUserCredits_add_returns200() throws Exception {
            // Arrange
            Map<String, Object> request = new HashMap<>();
            request.put("amount", 50);
            request.put("action", "ADD");
            request.put("reason", "Bonus");

            AdminUserDTO updatedUser = new AdminUserDTO();
            updatedUser.setId(TEST_USER_ID);
            updatedUser.setCredits(150);

            when(adminUserService.updateUserCredits(
                    eq(TEST_USER_ID), eq(50), eq("ADD"), eq("Bonus"), eq(DEFAULT_ADMIN_ID)))
                    .thenReturn(updatedUser);

            // Act & Assert
            performPatch("/api/admin/users/" + TEST_USER_ID + "/credits", request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.credits").value(150));
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void updateUserCredits_notFound_returns404() throws Exception {
            // Arrange
            Map<String, Object> request = new HashMap<>();
            request.put("amount", 50);
            request.put("action", "ADD");
            request.put("reason", "Test");

            when(adminUserService.updateUserCredits(anyString(), anyInt(), anyString(), anyString(), anyString()))
                    .thenReturn(null);

            // Act & Assert
            performPatch("/api/admin/users/nonexistent/credits", request)
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // PATCH /api/admin/users/{id}/subscription TESTS
    // =========================================================================
    @Nested
    @DisplayName("PATCH /api/admin/users/{id}/subscription")
    class UpdateUserSubscriptionTests {

        @Test
        @DisplayName("Should return 200 when upgrading to cramerich")
        void updateUserSubscription_upgrade_returns200() throws Exception {
            // Arrange
            Map<String, Object> request = new HashMap<>();
            request.put("tierCode", "cramerich");
            request.put("durationMonths", 3);
            request.put("reason", "Promotional upgrade");

            AdminUserDTO updatedUser = new AdminUserDTO();
            updatedUser.setId(TEST_USER_ID);
            updatedUser.setSubscription("cramerich");

            when(adminUserService.updateUserSubscription(
                    eq(TEST_USER_ID), eq("cramerich"), eq(3), eq("Promotional upgrade"), eq(DEFAULT_ADMIN_ID)))
                    .thenReturn(updatedUser);

            // Act & Assert
            performPatch("/api/admin/users/" + TEST_USER_ID + "/subscription", request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.subscription").value("cramerich"));
        }

        @Test
        @DisplayName("Should return 400 when tier code is invalid")
        void updateUserSubscription_invalidTier_returns400() throws Exception {
            // Arrange
            Map<String, Object> request = new HashMap<>();
            request.put("tierCode", "invalid_tier");
            request.put("reason", "Test");

            // Act & Assert
            performPatch("/api/admin/users/" + TEST_USER_ID + "/subscription", request)
                    .andExpect(status().isBadRequest());

            verify(adminUserService, never()).updateUserSubscription(any(), any(), anyInt(), any(), any());
        }

        @Test
        @DisplayName("Should return 400 when tier code is empty")
        void updateUserSubscription_emptyTier_returns400() throws Exception {
            // Arrange
            Map<String, Object> request = new HashMap<>();
            request.put("tierCode", "");
            request.put("reason", "Test");

            // Act & Assert
            performPatch("/api/admin/users/" + TEST_USER_ID + "/subscription", request)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void updateUserSubscription_notFound_returns404() throws Exception {
            // Arrange
            Map<String, Object> request = new HashMap<>();
            request.put("tierCode", "cramerich");
            request.put("durationMonths", 1);
            request.put("reason", "Test");

            when(adminUserService.updateUserSubscription(anyString(), anyString(), anyInt(), anyString(), anyString()))
                    .thenReturn(null);

            // Act & Assert
            performPatch("/api/admin/users/nonexistent/subscription", request)
                    .andExpect(status().isNotFound());
        }
    }
}
