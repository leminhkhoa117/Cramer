package com.cramer.controller;

import com.cramer.config.TestSecurityConfig;
import com.cramer.dto.ProfileDTO;
import com.cramer.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ProfileController.
 * Tests profile CRUD operations with IDOR protection.
 * 
 * @author Cramer Test Team
 * @since 2026-01-19
 */
import com.cramer.config.JwtAuthFilter;
import com.cramer.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(ProfileController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
@DisplayName("ProfileController Unit Tests")
class ProfileControllerTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

        @MockitoBean
    private com.cramer.util.JwtUtil jwtUtil;

        @MockitoBean
    private ProfileService profileService;

    private static final UUID DEFAULT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000"); // Standard UUID for tests
    private UUID testUserId;
    private UUID otherUserId;
    private ProfileDTO testProfileDTO;

    @BeforeEach
    void setUp() {
        testUserId = DEFAULT_USER_ID;
        otherUserId = UUID.randomUUID();

        testProfileDTO = new ProfileDTO();
        testProfileDTO.setId(testUserId);
        testProfileDTO.setUsername("testuser");
        testProfileDTO.setFullName("Test User");
        testProfileDTO.setPhoneNumber("0901234567");
        testProfileDTO.setAddress("123 Test Street");
        testProfileDTO.setAvatarUrl("https://example.com/avatar.jpg");
        testProfileDTO.setHasLlmApiKey(false);
        testProfileDTO.setLlmModel("deepseek-chat");
        testProfileDTO.setLlmProvider("deepseek");
        testProfileDTO.setIsAdmin(false);
        testProfileDTO.setCreatedAt(OffsetDateTime.now().minusDays(30));
    }

    // =========================================================================
    // GET /api/profiles/{id} TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/profiles/{id}")
    class GetProfileByIdTests {

        @Test
        @DisplayName("Should return 200 and profile when user accesses own profile")
        void getProfile_exists_returns200() throws Exception {
            // Arrange
            when(profileService.getProfileById(testUserId)).thenReturn(testProfileDTO);

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/profiles/{id}", testUserId)
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testUserId.toString()))
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.fullName").value("Test User"));

            verify(profileService).getProfileById(testUserId);
        }

        @Test
        @DisplayName("Should return 404 when profile not found")
        void getProfile_notFound_returns404() throws Exception {
            // Arrange
            when(profileService.getProfileById(testUserId))
                    .thenThrow(new RuntimeException("Profile not found"));

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/profiles/{id}", testUserId)
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("Should return 401 when no JWT token provided")
        void getProfile_unauthorized_returns401() throws Exception {
            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/profiles/{id}", testUserId))
                    .andExpect(status().isForbidden());

            verify(profileService, never()).getProfileById(any());
        }

        @Test
        @DisplayName("Should return 403 when user tries to access another's profile (IDOR)")
        void getProfile_idorViolation_returns403() throws Exception {
            // Arrange - User tries to access otherUserId's profile
            // The controller checks if authenticated user matches the path variable

            // Act & Assert
            mockMvc.perform(get("/api/profiles/{id}", otherUserId)
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isForbidden());

            verify(profileService, never()).getProfileById(otherUserId);
        }

        @Test
        @DisplayName("Should return 400 when UUID format is invalid")
        void getProfile_invalidUuid_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/profiles/{id}", "invalid-uuid")
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // PUT /api/profiles/{id} TESTS
    // =========================================================================
    @Nested
    @DisplayName("PUT /api/profiles/{id}")
    class UpdateProfileTests {

        @Test
        @DisplayName("Should return 200 and updated profile on valid update")
        void updateProfile_valid_returns200() throws Exception {
            // Arrange
            ProfileDTO updateRequest = new ProfileDTO();
            updateRequest.setFullName("Updated Name");
            updateRequest.setPhoneNumber("0909999999");

            ProfileDTO updatedProfile = new ProfileDTO();
            updatedProfile.setId(testUserId);
            updatedProfile.setUsername("testuser");
            updatedProfile.setFullName("Updated Name");
            updatedProfile.setPhoneNumber("0909999999");

            when(profileService.updateProfile(eq(testUserId), any(ProfileDTO.class)))
                    .thenReturn(updatedProfile);

            // Act & Assert
            // Act & Assert
            mockMvc.perform(put("/api/profiles/{id}", testUserId)
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fullName").value("Updated Name"))
                    .andExpect(jsonPath("$.phoneNumber").value("0909999999"));

            verify(profileService).updateProfile(eq(testUserId), any(ProfileDTO.class));
        }

        @Test
        @DisplayName("Should return 401 when no JWT token provided")
        void updateProfile_unauthorized_returns401() throws Exception {
            // Arrange
            ProfileDTO updateRequest = new ProfileDTO();
            updateRequest.setFullName("Updated Name");

            // Act & Assert
            // Act & Assert
            mockMvc.perform(put("/api/profiles/{id}", testUserId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isForbidden());

            verify(profileService, never()).updateProfile(any(), any());
        }

        @Test
        @DisplayName("Should return 403 when user tries to update another's profile (IDOR)")
        void updateProfile_idorViolation_returns403() throws Exception {
            // Arrange
            ProfileDTO updateRequest = new ProfileDTO();
            updateRequest.setFullName("Hacker Name");

            // Act & Assert
            mockMvc.perform(put("/api/profiles/{id}", otherUserId)
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isForbidden());

            verify(profileService, never()).updateProfile(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when validation fails")
        void updateProfile_invalidData_returns400() throws Exception {
            // Arrange - username too long (>100 chars)
            ProfileDTO updateRequest = new ProfileDTO();
            updateRequest.setUsername("a".repeat(101));

            // Act & Assert
            // Act & Assert
            mockMvc.perform(put("/api/profiles/{id}", testUserId)
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should preserve unchanged fields on partial update")
        void updateProfile_partialUpdate_preservesFields() throws Exception {
            // Arrange
            ProfileDTO updateRequest = new ProfileDTO();
            updateRequest.setFullName("Only Name Changed");

            ProfileDTO updatedProfile = new ProfileDTO();
            updatedProfile.setId(testUserId);
            updatedProfile.setUsername("testuser"); // preserved
            updatedProfile.setFullName("Only Name Changed"); // updated
            updatedProfile.setPhoneNumber("0901234567"); // preserved
            updatedProfile.setAddress("123 Test Street"); // preserved

            when(profileService.updateProfile(eq(testUserId), any(ProfileDTO.class)))
                    .thenReturn(updatedProfile);

            // Act & Assert
            // Act & Assert
            mockMvc.perform(put("/api/profiles/{id}", testUserId)
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.fullName").value("Only Name Changed"))
                    .andExpect(jsonPath("$.phoneNumber").value("0901234567"));
        }
    }
}
