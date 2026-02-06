package com.cramer.controller;

import com.cramer.dto.CheckEmailRequest;
import com.cramer.service.SupabaseAdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.cramer.config.JwtAuthFilter;
import com.cramer.config.SecurityConfig;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AuthController.
 * Tests email existence check endpoint (public, no auth required).
 *
 * @since 2026-01-19
 */
@org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest(AuthController.class)
@org.springframework.context.annotation.Import({SecurityConfig.class, JwtAuthFilter.class})
@DisplayName("AuthController Unit Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SupabaseAdminService supabaseAdminService;

    // JwtUtil is required for SecurityConfig which is loaded by @WebMvcTest
    @MockBean
    private com.cramer.util.JwtUtil jwtUtil;

    private static final String CHECK_EMAIL_URL = "/api/auth/check-email";

    @Nested
    @DisplayName("POST /api/auth/check-email")
    class CheckEmailTests {

        @Test
        @DisplayName("Should return exists=true when email exists in system")
        void checkEmail_validEmail_returnsExists() throws Exception {
            // Arrange
            String email = "existing@example.com";
            when(supabaseAdminService.checkEmailExists(email)).thenReturn(true);

            CheckEmailRequest request = new CheckEmailRequest();
            request.setEmail(email);

            // Act & Assert
            mockMvc.perform(post(CHECK_EMAIL_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exists").value(true));

            verify(supabaseAdminService).checkEmailExists(email);
        }

        @Test
        @DisplayName("Should return exists=false when email not registered")
        void checkEmail_unknownEmail_returnsNotExists() throws Exception {
            // Arrange
            String email = "newuser@example.com";
            when(supabaseAdminService.checkEmailExists(email)).thenReturn(false);

            CheckEmailRequest request = new CheckEmailRequest();
            request.setEmail(email);

            // Act & Assert
            mockMvc.perform(post(CHECK_EMAIL_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exists").value(false));

            verify(supabaseAdminService).checkEmailExists(email);
        }

        @Test
        @DisplayName("Should return 400 when request body is null/empty")
        void checkEmail_nullRequest_returnsBadRequest() throws Exception {
            // Act & Assert
            mockMvc.perform(post(CHECK_EMAIL_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verify(supabaseAdminService, never()).checkEmailExists(any());
        }

        @Test
        @DisplayName("Should return 400 when email is blank")
        void checkEmail_blankEmail_returnsBadRequest() throws Exception {
            // Arrange
            CheckEmailRequest request = new CheckEmailRequest();
            request.setEmail("   "); // blank email

            // Act & Assert
            mockMvc.perform(post(CHECK_EMAIL_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(supabaseAdminService, never()).checkEmailExists(any());
        }
    }
}
