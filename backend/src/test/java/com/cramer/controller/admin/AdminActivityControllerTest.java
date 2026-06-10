package com.cramer.controller.admin;

import com.cramer.config.SecurityConfig;
import com.cramer.config.JwtAuthFilter;
import com.cramer.dto.AdminAuditLogDTO;
import com.cramer.dto.UserActivityDTO;
import com.cramer.exception.GlobalExceptionHandler;
import com.cramer.service.AdminAuditService;
import com.cramer.service.UserActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
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
 * Unit tests for AdminActivityController.
 * Tests user activity and audit log endpoints.
 * 
 * @author Cramer Test Team
 * @since 2026-01-31
 */
@WebMvcTest(AdminActivityController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
@DisplayName("AdminActivityController Unit Tests")
class AdminActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserActivityService userActivityService;

    @MockitoBean
    private AdminAuditService adminAuditService;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private com.cramer.util.JwtUtil jwtUtil;

    private static final String DEFAULT_ADMIN_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_USER_ID = "660e8400-e29b-41d4-a716-446655440001";
    private static final UUID TEST_USER_UUID = UUID.fromString(TEST_USER_ID);

    private UserActivityDTO testActivityDTO;
    private AdminAuditLogDTO testAuditLogDTO;

    @BeforeEach
    void setUp() {
        testActivityDTO = new UserActivityDTO();
        testActivityDTO.setId(1L);
        testActivityDTO.setUserId(TEST_USER_UUID);
        testActivityDTO.setActivityType("TEST_COMPLETED");
        testActivityDTO.setTitle("Completed Cambridge 17 Test 1");
        testActivityDTO.setDescription("Score: 7.5/9");
        testActivityDTO.setCreatedAt(OffsetDateTime.now());

        testAuditLogDTO = new AdminAuditLogDTO();
        testAuditLogDTO.setId(1L);
        testAuditLogDTO.setAdminUserId(UUID.fromString(DEFAULT_ADMIN_ID));
        testAuditLogDTO.setTargetId(TEST_USER_ID);
        testAuditLogDTO.setAction("UPDATE_STATUS");
        testAuditLogDTO.setDescription("Changed status to BANNED");
        testAuditLogDTO.setCreatedAt(OffsetDateTime.now());
    }

    private ResultActions performGet(String url, Object... uriVars) throws Exception {
        return mockMvc.perform(get(url, uriVars)
                .with(jwt().jwt(jwtBuilder -> jwtBuilder
                        .subject(DEFAULT_ADMIN_ID)
                        .claim("aud", "authenticated"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    // =========================================================================
    // GET /api/admin/activities/users/{userId} TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/admin/activities/users/{userId}")
    class GetUserActivitiesTests {

        @Test
        @DisplayName("Should return 200 and paginated user activities")
        void getUserActivities_valid_returns200() throws Exception {
            // Arrange
            List<UserActivityDTO> activities = Arrays.asList(testActivityDTO);
            Page<UserActivityDTO> page = new PageImpl<>(activities, PageRequest.of(0, 20), 1);

            when(userActivityService.getUserActivities(eq(TEST_USER_UUID), any()))
                    .thenReturn(page);

            // Act & Assert
            performGet("/api/admin/activities/users/{userId}", TEST_USER_ID)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].activityType").value("TEST_COMPLETED"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Should return 200 with type filter")
        void getUserActivities_withTypeFilter_returns200() throws Exception {
            // Arrange
            List<UserActivityDTO> activities = Arrays.asList(testActivityDTO);
            Page<UserActivityDTO> page = new PageImpl<>(activities, PageRequest.of(0, 20), 1);

            when(userActivityService.getUserActivitiesByType(eq(TEST_USER_UUID), eq("TEST_COMPLETED"), any()))
                    .thenReturn(page);

            // Act & Assert
            mockMvc.perform(get("/api/admin/activities/users/{userId}", TEST_USER_ID)
                            .param("type", "TEST_COMPLETED")
                            .with(jwt().jwt(jwtBuilder -> jwtBuilder
                                    .subject(DEFAULT_ADMIN_ID)
                                    .claim("aud", "authenticated"))
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].activityType").value("TEST_COMPLETED"));

            verify(userActivityService).getUserActivitiesByType(eq(TEST_USER_UUID), eq("TEST_COMPLETED"), any());
        }
    }

    // =========================================================================
    // GET /api/admin/activities/users/{userId}/recent TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/admin/activities/users/{userId}/recent")
    class GetRecentActivitiesTests {

        @Test
        @DisplayName("Should return 200 and recent activities")
        void getRecentActivities_valid_returns200() throws Exception {
            // Arrange
            List<UserActivityDTO> activities = Arrays.asList(testActivityDTO);

            when(userActivityService.getRecentActivities(eq(TEST_USER_UUID), eq(10)))
                    .thenReturn(activities);

            // Act & Assert
            performGet("/api/admin/activities/users/{userId}/recent", TEST_USER_ID)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].activityType").value("TEST_COMPLETED"));
        }
    }

    // =========================================================================
    // GET /api/admin/activities/audit/users/{userId} TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/admin/activities/audit/users/{userId}")
    class GetAuditLogsForUserTests {

        @Test
        @DisplayName("Should return 200 and audit logs for user")
        void getAuditLogsForUser_valid_returns200() throws Exception {
            // Arrange
            List<AdminAuditLogDTO> logs = Arrays.asList(testAuditLogDTO);
            Page<AdminAuditLogDTO> page = new PageImpl<>(logs, PageRequest.of(0, 20), 1);

            when(adminAuditService.getAuditLogsForUser(eq(TEST_USER_ID), any()))
                    .thenReturn(page);

            // Act & Assert
            performGet("/api/admin/activities/audit/users/{userId}", TEST_USER_ID)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1));
        }
    }

    // =========================================================================
    // GET /api/admin/activities/audit TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/admin/activities/audit")
    class GetAllAuditLogsTests {

        @Test
        @DisplayName("Should return 200 and all audit logs")
        void getAllAuditLogs_valid_returns200() throws Exception {
            // Arrange
            List<AdminAuditLogDTO> logs = Arrays.asList(testAuditLogDTO);
            Page<AdminAuditLogDTO> page = new PageImpl<>(logs, PageRequest.of(0, 20), 1);

            when(adminAuditService.getAllAuditLogs(any())).thenReturn(page);

            // Act & Assert
            performGet("/api/admin/activities/audit")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1));
        }

        @Test
        @DisplayName("Should return 200 with empty list when no logs")
        void getAllAuditLogs_noData_returnsEmpty() throws Exception {
            // Arrange
            Page<AdminAuditLogDTO> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);

            when(adminAuditService.getAllAuditLogs(any())).thenReturn(page);

            // Act & Assert
            performGet("/api/admin/activities/audit")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(0));
        }
    }
}
