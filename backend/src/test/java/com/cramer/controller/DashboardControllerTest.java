package com.cramer.controller;

import com.cramer.config.JwtAuthFilter;
import com.cramer.config.SecurityConfig;
import com.cramer.dto.DashboardSummaryDTO;
import com.cramer.dto.TargetDTO;
import com.cramer.service.DashboardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for DashboardController.
 * Tests dashboard summary and target saving endpoints.
 * 
 * @author Cramer Test Team
 * @since 2026-01-25
 */
@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
@DisplayName("DashboardController Unit Tests")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private com.cramer.util.JwtUtil jwtUtil;

    @MockBean
    private DashboardService dashboardService;

    private UUID testUserId;
    private DashboardSummaryDTO mockSummary;
    private static final UUID DEFAULT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @BeforeEach
    void setUp() {
        testUserId = DEFAULT_USER_ID;
        mockSummary = new DashboardSummaryDTO();
    }

    // =========================================================================
    // GET /api/dashboard/summary TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/dashboard/summary")
    class GetSummaryTests {

        @Test
        @DisplayName("Should return 200 and dashboard summary")
        void getSummary_valid_returns200() throws Exception {
            when(dashboardService.buildDashboardSummary(eq(testUserId), eq(0), eq(3), isNull()))
                    .thenReturn(mockSummary);

            mockMvc.perform(get("/api/dashboard/summary")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk());

            verify(dashboardService).buildDashboardSummary(eq(testUserId), eq(0), eq(3), isNull());
        }

        @Test
        @DisplayName("Should return 200 with pagination params")
        void getSummary_withPagination_returns200() throws Exception {
            when(dashboardService.buildDashboardSummary(eq(testUserId), eq(1), eq(5), isNull()))
                    .thenReturn(mockSummary);

            mockMvc.perform(get("/api/dashboard/summary")
                            .param("page", "1")
                            .param("size", "5")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk());

            verify(dashboardService).buildDashboardSummary(eq(testUserId), eq(1), eq(5), isNull());
        }

        @Test
        @DisplayName("Should return 200 with search filter")
        void getSummary_withSearch_returns200() throws Exception {
            when(dashboardService.buildDashboardSummary(eq(testUserId), eq(0), eq(3), eq("cam17")))
                    .thenReturn(mockSummary);

            mockMvc.perform(get("/api/dashboard/summary")
                            .param("search", "cam17")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk());

            verify(dashboardService).buildDashboardSummary(eq(testUserId), eq(0), eq(3), eq("cam17"));
        }

        @Test
        @DisplayName("Should return error when size exceeds max limit")
        void getSummary_largeSize_returnsError() throws Exception {
            mockMvc.perform(get("/api/dashboard/summary")
                            .param("size", "100")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().is5xxServerError());

            verify(dashboardService, never()).buildDashboardSummary(any(), anyInt(), anyInt(), any());
        }

        @Test
        @DisplayName("Should return 403 when no JWT token")
        void getSummary_unauthorized_returns403() throws Exception {
            mockMvc.perform(get("/api/dashboard/summary"))
                    .andExpect(status().isForbidden());

            verify(dashboardService, never()).buildDashboardSummary(any(), anyInt(), anyInt(), any());
        }
    }

    // =========================================================================
    // POST /api/dashboard/target TESTS
    // =========================================================================
    @Nested
    @DisplayName("POST /api/dashboard/target")
    class SaveTargetTests {

        @Test
        @DisplayName("Should return 200 and saved target")
        void saveTarget_valid_returns200() throws Exception {
            TargetDTO inputTarget = new TargetDTO(
                    "IELTS Academic",
                    LocalDate.of(2026, 6, 15),
                    7.0, 7.5, 6.5, 7.0
            );

            when(dashboardService.saveTarget(eq(testUserId), any(TargetDTO.class)))
                    .thenReturn(inputTarget);

            String requestBody = """
                {
                    "examName": "IELTS Academic",
                    "examDate": "2026-06-15",
                    "listening": 7.0,
                    "reading": 7.5,
                    "writing": 6.5,
                    "speaking": 7.0
                }
                """;

            mockMvc.perform(post("/api/dashboard/target")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.examName").value("IELTS Academic"))
                    .andExpect(jsonPath("$.listening").value(7.0));

            verify(dashboardService).saveTarget(eq(testUserId), any(TargetDTO.class));
        }

        @Test
        @DisplayName("Should return 200 with minimal target data")
        void saveTarget_minimalData_returns200() throws Exception {
            TargetDTO inputTarget = new TargetDTO(
                    "IELTS General",
                    null,
                    null, null, null, null
            );

            when(dashboardService.saveTarget(eq(testUserId), any(TargetDTO.class)))
                    .thenReturn(inputTarget);

            String requestBody = """
                {
                    "examName": "IELTS General"
                }
                """;

            mockMvc.perform(post("/api/dashboard/target")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.examName").value("IELTS General"));
        }

        @Test
        @DisplayName("Should return 400 when examName is blank")
        void saveTarget_blankExamName_returns400() throws Exception {
            String requestBody = """
                {
                    "examName": "",
                    "listening": 7.0
                }
                """;

            mockMvc.perform(post("/api/dashboard/target")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());

            verify(dashboardService, never()).saveTarget(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when band score exceeds 9")
        void saveTarget_invalidBandScore_returns400() throws Exception {
            String requestBody = """
                {
                    "examName": "IELTS Academic",
                    "listening": 10.0
                }
                """;

            mockMvc.perform(post("/api/dashboard/target")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());

            verify(dashboardService, never()).saveTarget(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when band score is negative")
        void saveTarget_negativeBandScore_returns400() throws Exception {
            String requestBody = """
                {
                    "examName": "IELTS Academic",
                    "reading": -1.0
                }
                """;

            mockMvc.perform(post("/api/dashboard/target")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());

            verify(dashboardService, never()).saveTarget(any(), any());
        }

        @Test
        @DisplayName("Should return 403 when no JWT token")
        void saveTarget_unauthorized_returns403() throws Exception {
            String requestBody = """
                {
                    "examName": "IELTS Academic"
                }
                """;

            mockMvc.perform(post("/api/dashboard/target")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isForbidden());

            verify(dashboardService, never()).saveTarget(any(), any());
        }
    }
}
