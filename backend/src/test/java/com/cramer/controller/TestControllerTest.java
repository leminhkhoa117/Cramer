package com.cramer.controller;

import com.cramer.config.JwtAuthFilter;
import com.cramer.config.SecurityConfig;
import com.cramer.dto.TestSectionDTO;
import com.cramer.service.TestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TestController.
 * Tests test data retrieval endpoints (SAFE mode - no answers).
 * 
 * @author Cramer Test Team
 * @since 2026-01-19
 */
@WebMvcTest(TestController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
@DisplayName("TestController Unit Tests")
class TestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private com.cramer.util.JwtUtil jwtUtil;

    @MockitoBean
    private TestService testService;

    private TestSectionDTO mockSection;
    private static final UUID DEFAULT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @BeforeEach
    void setUp() {
        mockSection = new TestSectionDTO();
        mockSection.setId(1L);
        mockSection.setExamSource("cambridge");
        mockSection.setTestNumber(1);
        mockSection.setSkill("reading");
        mockSection.setPartNumber(1);
        mockSection.setPassageText("This is a test passage about climate change...");
    }

    // =========================================================================
    // GET /api/tests/data TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/tests/data")
    class GetTestDataTests {

        @Test
        @DisplayName("Should return 200 and sections with valid parameters")
        void getTestData_valid_returns200() throws Exception {
            // Arrange
            when(testService.getSafeTest("cambridge", 1, "reading"))
                    .thenReturn(List.of(mockSection));

            // Act & Assert
            mockMvc.perform(get("/api/tests/data")
                            .param("source", "cambridge")
                            .param("test", "1")
                            .param("skill", "reading")
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(DEFAULT_USER_ID.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].examSource").value("cambridge"))
                    .andExpect(jsonPath("$[0].skill").value("reading"));

            verify(testService).getSafeTest("cambridge", 1, "reading");
        }

        @Test
        @DisplayName("Should return 404 when test data not found")
        void getTestData_noData_returns404() throws Exception {
            // Arrange
            when(testService.getSafeTest("nonexistent", 99, "reading"))
                    .thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/tests/data")
                            .param("source", "nonexistent")
                            .param("test", "99")
                            .param("skill", "reading")
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(DEFAULT_USER_ID.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when no JWT token provided")
        void getTestData_unauthorized_returns401() throws Exception {
            // Act & Assert
            // Endpoint /api/tests/data requires authentication per SecurityConfig
            // Expect 403 Forbidden (MockMvc standard for missing auth in verified config)
            mockMvc.perform(get("/api/tests/data")
                            .param("source", "cambridge")
                            .param("test", "1")
                            .param("skill", "reading"))
                    .andExpect(status().isForbidden());

            verify(testService, never()).getSafeTest(any(), anyInt(), any());
        }

        @Test
        @DisplayName("Should return 400 when source parameter is missing")
        void getTestData_missingSource_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/tests/data")
                            .param("test", "1")
                            .param("skill", "reading")
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(DEFAULT_USER_ID.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when test parameter is missing")
        void getTestData_missingTest_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/tests/data")
                            .param("source", "cambridge")
                            .param("skill", "reading")
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(DEFAULT_USER_ID.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when skill parameter is missing")
        void getTestData_missingSkill_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/tests/data")
                            .param("source", "cambridge")
                            .param("test", "1")
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(DEFAULT_USER_ID.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when test parameter is not a number")
        void getTestData_invalidTestNumber_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/tests/data")
                            .param("source", "cambridge")
                            .param("test", "abc")
                            .param("skill", "reading")
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(DEFAULT_USER_ID.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should verify SAFE mode - answers should not be exposed")
        void getTestData_verifySafeMode_noAnswers() throws Exception {
            // Arrange
            TestSectionDTO safeSection = new TestSectionDTO();
            safeSection.setId(1L);
            safeSection.setExamSource("cambridge");
            safeSection.setTestNumber(1);
            safeSection.setSkill("reading");
            safeSection.setQuestions(List.of()); // No answers exposed

            when(testService.getSafeTest("cambridge", 1, "reading"))
                    .thenReturn(List.of(safeSection));

            // Act & Assert
            mockMvc.perform(get("/api/tests/data")
                            .param("source", "cambridge")
                            .param("test", "1")
                            .param("skill", "reading")
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(DEFAULT_USER_ID.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isOk())
                    // Verify that the service method for SAFE data is called
                    .andExpect(jsonPath("$[0].id").value(1));

            verify(testService).getSafeTest("cambridge", 1, "reading");
            verify(testService, never()).getFullTest(any(), anyInt(), any());
        }
    }
}
