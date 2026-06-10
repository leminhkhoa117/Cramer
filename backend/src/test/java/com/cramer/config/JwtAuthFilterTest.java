package com.cramer.config;

import com.cramer.entity.Profile;
import com.cramer.repository.ProfileRepository;
import com.cramer.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtAuthFilter.
 * Tests JWT authentication flow, token validation, and security context setup.
 * 
 * @author Cramer Test Team
 * @since 2026-01-11
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthFilter Unit Tests")
class JwtAuthFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    private String validToken;
    private String testUserId;

    @BeforeEach
    void setUp() {
        // Clear security context before each test
        SecurityContextHolder.clearContext();
        
        validToken = "valid.jwt.token";
        testUserId = UUID.randomUUID().toString();
    }

    // =========================================================================
    // NO AUTHORIZATION HEADER TESTS
    // =========================================================================
    @Nested
    @DisplayName("No Authorization Header Tests")
    class NoAuthorizationHeaderTests {

        @Test
        @DisplayName("Should continue filter chain when Authorization header is missing")
        void doFilterInternal_noAuthHeader_continuesChain() throws Exception {
            // Arrange
            when(request.getHeader("Authorization")).thenReturn(null);

            // Act
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Should continue filter chain when Authorization header doesn't start with Bearer")
        void doFilterInternal_nonBearerAuth_continuesChain() throws Exception {
            // Arrange
            when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

            // Act
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(jwtUtil, never()).extractUserId(anyString());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    // =========================================================================
    // VALID TOKEN TESTS
    // =========================================================================
    @Nested
    @DisplayName("Valid Token Tests")
    class ValidTokenTests {

        @Test
        @DisplayName("Should set authentication for valid JWT token")
        void doFilterInternal_validToken_setsAuthentication() throws Exception {
            // Arrange
            when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
            when(jwtUtil.extractUserId(validToken)).thenReturn(testUserId);
            when(jwtUtil.validateToken(validToken)).thenReturn(true);

            // Act
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getName()).isEqualTo(testUserId);
            assertThat(auth.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_USER");
        }

                @Test
                @DisplayName("Should grant admin authority for admin profile on admin path")
                void doFilterInternal_adminProfileOnAdminPath_grantsAdminAuthority() throws Exception {
                    // Arrange
                    UUID adminId = UUID.fromString(testUserId);
                    Profile adminProfile = new Profile(adminId, "admin-user");
                    adminProfile.setIsAdmin(true);

                    when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
                    when(request.getRequestURI()).thenReturn("/api/admin/dashboard/stats");
                    when(request.getContextPath()).thenReturn("");
                    when(jwtUtil.extractUserId(validToken)).thenReturn(testUserId);
                    when(jwtUtil.validateToken(validToken)).thenReturn(true);
                    when(profileRepository.findById(adminId)).thenReturn(java.util.Optional.of(adminProfile));

                    // Act
                    jwtAuthFilter.doFilterInternal(request, response, filterChain);

                    // Assert
                    verify(filterChain).doFilter(request, response);

                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    assertThat(auth).isNotNull();
                    assertThat(auth.getAuthorities())
                        .extracting("authority")
                            .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
                }

                @Test
                @DisplayName("Should not grant admin authority for non-admin profile on admin path")
                void doFilterInternal_nonAdminProfileOnAdminPath_grantsOnlyUserAuthority() throws Exception {
                    // Arrange
                    UUID userUuid = UUID.fromString(testUserId);
                    Profile profile = new Profile(userUuid, "regular-user");
                    profile.setIsAdmin(false);

                    when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
                    when(request.getRequestURI()).thenReturn("/api/admin/dashboard/stats");
                    when(request.getContextPath()).thenReturn("");
                    when(jwtUtil.extractUserId(validToken)).thenReturn(testUserId);
                    when(jwtUtil.validateToken(validToken)).thenReturn(true);
                    when(profileRepository.findById(userUuid)).thenReturn(java.util.Optional.of(profile));

                    // Act
                    jwtAuthFilter.doFilterInternal(request, response, filterChain);

                    // Assert
                    verify(filterChain).doFilter(request, response);

                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    assertThat(auth).isNotNull();
                    assertThat(auth.getAuthorities())
                        .extracting("authority")
                        .containsExactly("ROLE_USER");
                }

        @Test
        @DisplayName("Should extract Bearer token correctly")
        void doFilterInternal_bearerToken_extractsToken() throws Exception {
            // Arrange
            when(request.getHeader("Authorization")).thenReturn("Bearer my-jwt-token");
            when(jwtUtil.extractUserId("my-jwt-token")).thenReturn(testUserId);
            when(jwtUtil.validateToken("my-jwt-token")).thenReturn(true);

            // Act
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(jwtUtil).extractUserId("my-jwt-token");
            verify(jwtUtil).validateToken("my-jwt-token");
        }
    }

    // =========================================================================
    // INVALID TOKEN TESTS
    // =========================================================================
    @Nested
    @DisplayName("Invalid Token Tests")
    class InvalidTokenTests {

        @Test
        @DisplayName("Should not set authentication for invalid JWT token")
        void doFilterInternal_invalidToken_noAuthentication() throws Exception {
            // Arrange
            when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
            when(jwtUtil.extractUserId(validToken)).thenReturn(testUserId);
            when(jwtUtil.validateToken(validToken)).thenReturn(false);

            // Act
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Should handle null userId from token")
        void doFilterInternal_nullUserId_noAuthentication() throws Exception {
            // Arrange
            when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
            when(jwtUtil.extractUserId(validToken)).thenReturn(null);

            // Act
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(jwtUtil, never()).validateToken(anyString());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    // =========================================================================
    // EXCEPTION HANDLING TESTS
    // =========================================================================
    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Should continue filter chain when JWT processing throws exception")
        void doFilterInternal_jwtException_continuesChain() throws Exception {
            // Arrange
            when(request.getHeader("Authorization")).thenReturn("Bearer malformed.token");
            when(jwtUtil.extractUserId("malformed.token")).thenThrow(new RuntimeException("Invalid JWT"));

            // Act - should not throw
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // Assert - filter chain should still continue
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Should handle validation exception gracefully")
        void doFilterInternal_validationException_continuesChain() throws Exception {
            // Arrange
            when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
            when(jwtUtil.extractUserId(validToken)).thenReturn(testUserId);
            when(jwtUtil.validateToken(validToken)).thenThrow(new RuntimeException("Token expired"));

            // Act - should not throw
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    // =========================================================================
    // EXISTING AUTHENTICATION TESTS
    // =========================================================================
    @Nested
    @DisplayName("Existing Authentication Tests")
    class ExistingAuthenticationTests {

        @Test
        @DisplayName("Should not override existing authentication")
        void doFilterInternal_existingAuth_doesNotOverride() throws Exception {
            // Arrange - pre-set authentication
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken existingAuth =
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            "existing-user", null, java.util.Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(existingAuth);

            when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
            when(jwtUtil.extractUserId(validToken)).thenReturn(testUserId);

            // Act
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // Assert - should keep existing authentication
            verify(filterChain).doFilter(request, response);
            verify(jwtUtil, never()).validateToken(anyString());
            
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getName()).isEqualTo("existing-user");
        }
    }
}
