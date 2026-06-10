package com.cramer.config;

import com.cramer.controller.admin.AdminDashboardController;
import com.cramer.entity.Profile;
import com.cramer.exception.GlobalExceptionHandler;
import com.cramer.repository.ProfileRepository;
import com.cramer.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminDashboardController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
@DisplayName("Admin authorization security tests")
class AdminAuthorizationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private ProfileRepository profileRepository;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Should allow ROLE_ADMIN principal on admin API")
    void adminProfileBearerToken_canAccessAdminApi() throws Exception {
        UUID adminId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class))).thenReturn(1);

        mockMvc.perform(get("/api/admin/dashboard/status")
                        .with(user(adminId.toString()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.database").value("operational"));
    }

    @Test
    @DisplayName("Should reject non-admin profile on admin API")
    void nonAdminProfileBearerToken_cannotAccessAdminApi() throws Exception {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        Profile profile = new Profile(userId, "regular-user");
        profile.setIsAdmin(false);

        when(jwtUtil.extractUserId("user-token")).thenReturn(userId.toString());
        when(jwtUtil.validateToken("user-token")).thenReturn(true);
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));

        mockMvc.perform(get("/api/admin/dashboard/status")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("Should reject authenticated admin API request when profile is missing")
    void missingProfileBearerToken_cannotAccessAdminApi() throws Exception {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");

        when(jwtUtil.extractUserId("missing-profile-token")).thenReturn(userId.toString());
        when(jwtUtil.validateToken("missing-profile-token")).thenReturn(true);
        when(profileRepository.findById(userId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/dashboard/status")
                        .header("Authorization", "Bearer missing-profile-token"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("Should reject admin API request without Bearer token")
    void missingBearerToken_cannotAccessAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/status"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(jwtUtil, profileRepository, jdbcTemplate);
    }
}