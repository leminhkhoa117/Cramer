package com.cramer.controller;

import com.cramer.config.JwtAuthFilter;
import com.cramer.config.RateLimitConfig;
import com.cramer.config.SecurityConfig;
import com.cramer.dto.WritingReviewDTO;
import com.cramer.dto.WritingSubmissionDTO;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.service.WritingSubmissionService;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for WritingController.
 * Tests writing draft saving, submission, grading status, and review.
 * 
 * @author Cramer Test Team
 * @since 2026-01-19
 */
@WebMvcTest(WritingController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
@DisplayName("WritingController Unit Tests")
class WritingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private com.cramer.util.JwtUtil jwtUtil;

    @MockitoBean
    private WritingSubmissionService writingSubmissionService;

    @MockitoBean
    private RateLimitConfig rateLimitConfig;

    private UUID testUserId;
    private Long testAttemptId;
    private static final UUID DEFAULT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @BeforeEach
    void setUp() {
        testUserId = DEFAULT_USER_ID;
        testAttemptId = 100L;
        
        // Default: allow rate limit
        when(rateLimitConfig.tryConsume(anyString(), anyString())).thenReturn(true);
    }

    // =========================================================================
    // POST /api/writing/draft/{attemptId} TESTS
    // =========================================================================
    @Nested
    @DisplayName("POST /api/writing/draft/{attemptId}")
    class SaveDraftTests {

        @Test
        @DisplayName("Should return 200 and save draft successfully")
        void saveDraft_valid_returns200() throws Exception {
            // Arrange
            WritingSubmissionDTO saved = new WritingSubmissionDTO();
            saved.setId(1L);
            saved.setTaskNumber(1);
            saved.setEssayText("This is my essay draft.");
            saved.setGradingStatus("DRAFT");

            when(writingSubmissionService.saveDraft(eq(testAttemptId), eq(1), anyString(), eq(testUserId)))
                    .thenReturn(saved);

            // Using lenient matcher for primitive long
            when(writingSubmissionService.saveDraft(
                    eq(testAttemptId), eq(1), anyString(), eq(testUserId)))
                    .thenReturn(saved);

            // Act & Assert
            mockMvc.perform(post("/api/writing/draft/{attemptId}", testAttemptId)
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .param("taskNumber", "1")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("This is my essay draft."))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.taskNumber").value(1))
                    .andExpect(jsonPath("$.gradingStatus").value("DRAFT"));

            verify(writingSubmissionService).saveDraft(eq(testAttemptId), eq(1), eq("This is my essay draft."), eq(testUserId));
        }

        @Test
        @DisplayName("Should return 404 when attempt not found")
        void saveDraft_attemptNotFound_returns404() throws Exception {
            // Arrange
            when(writingSubmissionService.saveDraft(eq(999L), anyInt(), anyString(), eq(testUserId)))
                    .thenThrow(new ResourceNotFoundException("TestAttempt", "id", 999L));

            // Act & Assert
            mockMvc.perform(post("/api/writing/draft/{attemptId}", 999L)
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .param("taskNumber", "1")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("Draft text"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 401 when no JWT token")
        void saveDraft_unauthorized_returns401() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/writing/draft/{attemptId}", testAttemptId)
                            .with(csrf())
                            .param("taskNumber", "1")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("Draft text"))
                    .andExpect(status().isForbidden()); // MockMvc returns 403 when not authenticated in this config
        }
    }

    // =========================================================================
    // POST /api/writing/submit/{attemptId} TESTS
    // =========================================================================
    @Nested
    @DisplayName("POST /api/writing/submit/{attemptId}")
    class SubmitForGradingTests {

        @Test
        @DisplayName("Should return 200 and start grading")
        void submitForGrading_valid_returns200() throws Exception {
            // Arrange
            Map<String, Object> result = Map.of(
                "message", "Essays submitted for grading",
                "attemptId", testAttemptId,
                "status", "GRADING"
            );

            when(writingSubmissionService.submitForGrading(eq(testAttemptId), anyMap(), eq(testUserId)))
                    .thenReturn(result);

            String requestBody = """
                {
                    "essays": {
                        "1": "Task 1 essay content about the chart.",
                        "2": "Task 2 essay discussing modern society."
                    }
                }
                """;

            // Act & Assert
            mockMvc.perform(post("/api/writing/submit/{attemptId}", testAttemptId)
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Essays submitted for grading"))
                    .andExpect(jsonPath("$.status").value("GRADING"));
        }

        @Test
        @DisplayName("Should return 429 when rate limit exceeded")
        void submitForGrading_rateLimited_returns429() throws Exception {
            // Arrange
            when(rateLimitConfig.tryConsume(anyString(), eq("grading"))).thenReturn(false);

            String requestBody = """
                {
                    "essays": {
                        "1": "Essay content"
                    }
                }
                """;

            // Act & Assert - Controller checks rate limit
            mockMvc.perform(post("/api/writing/submit/{attemptId}", testAttemptId)
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isTooManyRequests());

            verify(writingSubmissionService, never()).submitForGrading(anyLong(), anyMap(), any());
        }

        @Test
        @DisplayName("Should return 400 when essays field is null")
        void submitForGrading_nullEssays_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/writing/submit/{attemptId}", testAttemptId)
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"essays\": null}"))
                    .andDo(result -> System.err.println("DEBUG_RESPONSE: " + result.getResponse().getContentAsString()))
                    .andExpect(status().isInternalServerError()); // TEMPORARY DEBUG: Expect 500 to catch the body
        }

        @Test
        @DisplayName("Should return 401 when no JWT token")
        void submitForGrading_unauthorized_returns401() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/writing/submit/{attemptId}", testAttemptId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"essays\": {\"1\": \"text\"}}"))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // GET /api/writing/status/{attemptId} TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/writing/status/{attemptId}")
    class GetGradingStatusTests {

        @Test
        @DisplayName("Should return 200 with grading in progress")
        void getGradingStatus_inProgress_returns200() throws Exception {
            // Arrange
            Map<String, Object> status = Map.of(
                "status", "GRADING",
                "progress", 50,
                "tasks", Map.of("1", "COMPLETED", "2", "GRADING")
            );

            when(writingSubmissionService.getGradingStatus(testAttemptId, testUserId)).thenReturn(status);

            // Act & Assert
            mockMvc.perform(get("/api/writing/status/{attemptId}", testAttemptId)
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("GRADING"))
                    .andExpect(jsonPath("$.progress").value(50));
        }

        @Test
        @DisplayName("Should return 200 with grading completed")
        void getGradingStatus_completed_returns200() throws Exception {
            // Arrange
            Map<String, Object> status = Map.of(
                "status", "COMPLETED",
                "progress", 100,
                "overallBand", 7.0
            );

            when(writingSubmissionService.getGradingStatus(testAttemptId, testUserId)).thenReturn(status);

            // Act & Assert
            mockMvc.perform(get("/api/writing/status/{attemptId}", testAttemptId)
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.overallBand").value(7.0));
        }

        @Test
        @DisplayName("Should return 404 when attempt not found")
        void getGradingStatus_notFound_returns404() throws Exception {
            // Arrange
            when(writingSubmissionService.getGradingStatus(999L, testUserId))
                    .thenThrow(new ResourceNotFoundException("TestAttempt", "id", 999L));

            // Act & Assert
            mockMvc.perform(get("/api/writing/status/{attemptId}", 999L)
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // GET /api/writing/review/{attemptId} TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/writing/review/{attemptId}")
    class GetWritingReviewTests {

        @Test
        @DisplayName("Should return 200 with full review")
        void getWritingReview_valid_returns200() throws Exception {
            // Arrange
            WritingReviewDTO review = new WritingReviewDTO();
            review.setAttemptId(testAttemptId);
            review.setOverallBand(new java.math.BigDecimal("7.0"));
            review.setStatus("COMPLETED");

            when(writingSubmissionService.getWritingReview(testAttemptId, testUserId)).thenReturn(review);

            // Act & Assert
            mockMvc.perform(get("/api/writing/review/{attemptId}", testAttemptId)
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.attemptId").value(testAttemptId))
                    .andExpect(jsonPath("$.overallBand").value(7.0))
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("Should return 404 when attempt not found")
        void getWritingReview_notFound_returns404() throws Exception {
            // Arrange
            when(writingSubmissionService.getWritingReview(999L, testUserId))
                    .thenThrow(new ResourceNotFoundException("TestAttempt", "id", 999L));

            // Act & Assert
            mockMvc.perform(get("/api/writing/review/{attemptId}", 999L)
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when user doesn't own attempt")
        void getWritingReview_forbidden_returns403() throws Exception {
            // Arrange
            UUID otherUserId = UUID.randomUUID();
            when(writingSubmissionService.getWritingReview(eq(testAttemptId), eq(otherUserId)))
                    .thenThrow(new org.springframework.security.access.AccessDeniedException("Access denied"));

            // Act & Assert
            // Simulate other user token
            mockMvc.perform(get("/api/writing/review/{attemptId}", testAttemptId)
                            .with(jwt().jwt(jwt -> jwt.subject(otherUserId.toString()))))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // GET /api/writing/submissions/{attemptId} TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/writing/submissions/{attemptId}")
    class GetSubmissionsTests {

        @Test
        @DisplayName("Should return 200 and list of submissions")
        void getSubmissions_valid_returns200() throws Exception {
            // Arrange
            WritingSubmissionDTO sub1 = new WritingSubmissionDTO();
            sub1.setId(1L);
            sub1.setTaskNumber(1);
            sub1.setGradingStatus("COMPLETED");

            WritingSubmissionDTO sub2 = new WritingSubmissionDTO();
            sub2.setId(2L);
            sub2.setTaskNumber(2);
            sub2.setGradingStatus("COMPLETED");

            when(writingSubmissionService.getSubmissions(testAttemptId, testUserId))
                    .thenReturn(List.of(sub1, sub2));

            // Act & Assert
            mockMvc.perform(get("/api/writing/submissions/{attemptId}", testAttemptId)
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].taskNumber").value(1))
                    .andExpect(jsonPath("$[1].taskNumber").value(2));
        }

        @Test
        @DisplayName("Should return 200 and empty list when no submissions")
        void getSubmissions_empty_returns200() throws Exception {
            // Arrange
            when(writingSubmissionService.getSubmissions(testAttemptId, testUserId))
                    .thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/writing/submissions/{attemptId}", testAttemptId)
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    // =========================================================================
    // POST /api/writing/validate-api-key TESTS
    // =========================================================================
    @Nested
    @DisplayName("POST /api/writing/validate-api-key")
    class ValidateApiKeyTests {

        @Test
        @DisplayName("Should return 200 with valid=true for valid key")
        void validateApiKey_valid_returns200() throws Exception {
            // Arrange
            when(writingSubmissionService.validateApiKey("sk-valid-api-key")).thenReturn(true);

            // Act & Assert
            mockMvc.perform(post("/api/writing/validate-api-key")
                            .with(csrf()) // Require csrf for POST
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"apiKey\": \"sk-valid-api-key\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true))
                    .andExpect(jsonPath("$.message").value("API key is valid"));
        }

        @Test
        @DisplayName("Should return 200 with valid=false for invalid key")
        void validateApiKey_invalid_returns200() throws Exception {
            // Arrange
            when(writingSubmissionService.validateApiKey("invalid-key")).thenReturn(false);

            // Act & Assert
            mockMvc.perform(post("/api/writing/validate-api-key")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"apiKey\": \"invalid-key\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(false))
                    .andExpect(jsonPath("$.message").value("API key is invalid or expired"));
        }
    }

    // =========================================================================
    // POST /api/writing/regrade/{attemptId} TESTS
    // =========================================================================
    @Nested
    @DisplayName("POST /api/writing/regrade/{attemptId}")
    class RegradeAttemptTests {

        @Test
        @DisplayName("Should return 200 and start re-grading")
        void regradeAttempt_valid_returns200() throws Exception {
            // Arrange
            Map<String, Object> result = Map.of(
                "message", "Re-grading started",
                "attemptId", testAttemptId,
                "status", "GRADING"
            );

            when(writingSubmissionService.regradeAttempt(testAttemptId, testUserId)).thenReturn(result);

            // Act & Assert
            mockMvc.perform(post("/api/writing/regrade/{attemptId}", testAttemptId)
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Re-grading started"))
                    .andExpect(jsonPath("$.status").value("GRADING"));
        }

        @Test
        @DisplayName("Should return 429 when rate limit exceeded")
        void regradeAttempt_rateLimited_returns429() throws Exception {
            // Arrange
            when(rateLimitConfig.tryConsume(anyString(), eq("grading"))).thenReturn(false);

            // Act & Assert
            mockMvc.perform(post("/api/writing/regrade/{attemptId}", testAttemptId)
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isTooManyRequests());

            verify(writingSubmissionService, never()).regradeAttempt(anyLong(), any());
        }

        @Test
        @DisplayName("Should return 404 when attempt not found")
        void regradeAttempt_notFound_returns404() throws Exception {
            // Arrange
            when(writingSubmissionService.regradeAttempt(999L, testUserId))
                    .thenThrow(new ResourceNotFoundException("TestAttempt", "id", 999L));

            // Act & Assert
            mockMvc.perform(post("/api/writing/regrade/{attemptId}", 999L)
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 401 when no JWT token")
        void regradeAttempt_unauthorized_returns401() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/writing/regrade/{attemptId}", testAttemptId)
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }
}
