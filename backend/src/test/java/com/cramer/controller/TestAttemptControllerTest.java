package com.cramer.controller;

import com.cramer.config.JwtAuthFilter;
import com.cramer.config.SecurityConfig;
import com.cramer.dto.AnswerSubmissionDTO;
import com.cramer.dto.SaveProgressDTO;
import com.cramer.dto.TestResultDTO;
import com.cramer.dto.TestReviewDTO;
import com.cramer.dto.UserAnswerDTO;
import com.cramer.entity.TestAttempt;
import com.cramer.service.TestAttemptService;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
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
 * Unit tests for TestAttemptController.
 * Tests test session lifecycle: start, submit, progress, cancel, review.
 * 
 * @author Cramer Test Team
 * @since 2026-01-19
 */
@WebMvcTest(TestAttemptController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
@DisplayName("TestAttemptController Unit Tests")
class TestAttemptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private com.cramer.util.JwtUtil jwtUtil;

    @MockitoBean
    private TestAttemptService testAttemptService;

    private UUID testUserId;
    private TestAttempt mockAttempt;
    private static final UUID DEFAULT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000"); // Standard UUID

    @BeforeEach
    void setUp() {
        testUserId = DEFAULT_USER_ID;

        mockAttempt = new TestAttempt();
        mockAttempt.setId(1L);
        mockAttempt.setUserId(testUserId);
        mockAttempt.setExamSource("cambridge");
        mockAttempt.setTestNumber("1");
        mockAttempt.setSkill("reading");
        mockAttempt.setStatus("IN_PROGRESS");
        mockAttempt.setStartedAt(OffsetDateTime.now());
        mockAttempt.setTimeLeft(3600);
    }

    // =========================================================================
    // POST /api/test-attempts/start TESTS
    // =========================================================================
    @Nested
    @DisplayName("POST /api/test-attempts/start")
    class StartAttemptTests {

        @Test
        @DisplayName("Should return 200 and create/resume attempt with valid params")
        void start_valid_returns200() throws Exception {
            // Arrange
            when(testAttemptService.startOrGetAttempt(
                    eq("cambridge"), eq("1"), eq("reading"), eq(testUserId), eq(false)))
                    .thenReturn(mockAttempt);

            // Act & Assert
            mockMvc.perform(post("/api/test-attempts/start")
                            .param("source", "cambridge")
                            .param("test", "1")
                            .param("skill", "reading")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.examSource").value("cambridge"))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

            verify(testAttemptService).startOrGetAttempt("cambridge", "1", "reading", testUserId, false);
        }

        @Test
        @DisplayName("Should return 401 when no JWT token provided")
        void start_unauthorized_returns401() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/test-attempts/start")
                            .param("source", "cambridge")
                            .param("test", "1")
                            .param("skill", "reading")
                            .with(csrf()))
                    .andExpect(status().isForbidden());

            verify(testAttemptService, never()).startOrGetAttempt(any(), any(), any(), any(), anyBoolean());
        }

        @Test
        @DisplayName("Should return 400 when source parameter is missing")
        void start_missingSource_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/test-attempts/start")
                            .param("test", "1")
                            .param("skill", "reading")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should create new attempt when forceNew=true")
        void start_forceNew_createsNew() throws Exception {
            // Arrange
            when(testAttemptService.startOrGetAttempt(
                    eq("cambridge"), eq("1"), eq("reading"), eq(testUserId), eq(true)))
                    .thenReturn(mockAttempt);

            // Act & Assert
            mockMvc.perform(post("/api/test-attempts/start")
                            .param("source", "cambridge")
                            .param("test", "1")
                            .param("skill", "reading")
                            .param("forceNew", "true")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isOk());

            verify(testAttemptService).startOrGetAttempt("cambridge", "1", "reading", testUserId, true);
        }
    }

    // =========================================================================
    // POST /api/test-attempts/{id}/submit TESTS
    // =========================================================================
    @Nested
    @DisplayName("POST /api/test-attempts/{id}/submit")
    class SubmitAttemptTests {

        @Test
        @DisplayName("Should return 200 and result when submission is valid")
        void submit_valid_returns200() throws Exception {
            // Arrange
            AnswerSubmissionDTO submission = new AnswerSubmissionDTO();
            submission.setAnswers(Map.of(1L, "A", 2L, "B", 3L, "True"));

            TestResultDTO result = new TestResultDTO();
            result.setAttemptId(1L);
            result.setScore(35);
            result.setTotalQuestions(40);

            when(testAttemptService.submitAttempt(eq(1L), anyMap(), eq(testUserId)))
                    .thenReturn(result);

            // Act & Assert
            mockMvc.perform(post("/api/test-attempts/1/submit")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(submission)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.attemptId").value(1))
                    .andExpect(jsonPath("$.score").value(35))
                    .andExpect(jsonPath("$.totalQuestions").value(40));

            verify(testAttemptService).submitAttempt(eq(1L), anyMap(), eq(testUserId));
        }

        @Test
        @DisplayName("Should return 401 when no JWT token provided")
        void submit_unauthorized_returns401() throws Exception {
            // Arrange
            AnswerSubmissionDTO submission = new AnswerSubmissionDTO();
            submission.setAnswers(Map.of(1L, "A"));

            // Act & Assert
            mockMvc.perform(post("/api/test-attempts/1/submit")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(submission)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 400 when answers are null")
        void submit_nullAnswers_returns400() throws Exception {
            // Arrange
            AnswerSubmissionDTO submission = new AnswerSubmissionDTO();
            submission.setAnswers(null);

            when(testAttemptService.submitAttempt(eq(1L), isNull(), eq(testUserId)))
                    .thenThrow(new IllegalArgumentException("Submission data cannot be null"));

            // Act & Assert
            mockMvc.perform(post("/api/test-attempts/1/submit")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(submission)))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // POST /api/test-attempts/{id}/progress TESTS
    // =========================================================================
    @Nested
    @DisplayName("POST /api/test-attempts/{id}/progress")
    class SaveProgressTests {

        @Test
        @DisplayName("Should return 200 when progress saved successfully")
        void saveProgress_valid_returns200() throws Exception {
            // Arrange
            SaveProgressDTO progress = new SaveProgressDTO();
            progress.setTimeLeft(1800);
            progress.setCurrentPart(2);
            progress.setAnswers(Map.of(1L, "A", 2L, "B"));

            doNothing().when(testAttemptService).saveProgress(eq(1L), any(SaveProgressDTO.class), eq(testUserId));

            // Act & Assert
            mockMvc.perform(post("/api/test-attempts/1/progress")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(progress)))
                    .andExpect(status().isOk());

            verify(testAttemptService).saveProgress(eq(1L), any(SaveProgressDTO.class), eq(testUserId));
        }

        @Test
        @DisplayName("Should return 401 when no JWT token provided")
        void saveProgress_unauthorized_returns401() throws Exception {
            // Arrange
            SaveProgressDTO progress = new SaveProgressDTO();
            progress.setTimeLeft(1800);

            // Act & Assert
            mockMvc.perform(post("/api/test-attempts/1/progress")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(progress)))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // POST /api/test-attempts/{id}/cancel TESTS
    // =========================================================================
    @Nested
    @DisplayName("POST /api/test-attempts/{id}/cancel")
    class CancelAttemptTests {

        @Test
        @DisplayName("Should return 200 when cancelling IN_PROGRESS attempt")
        void cancel_inProgress_returns200() throws Exception {
            // Arrange
            doNothing().when(testAttemptService).cancelAttempt(1L, testUserId);

            // Act & Assert
            mockMvc.perform(post("/api/test-attempts/1/cancel")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isOk());

            verify(testAttemptService).cancelAttempt(1L, testUserId);
        }

        @Test
        @DisplayName("Should return 401 when no JWT token provided")
        void cancel_unauthorized_returns401() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/test-attempts/1/cancel")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when user tries to cancel another's attempt")
        void cancel_idorViolation_returns403() throws Exception {
            // Arrange
            doThrow(new AccessDeniedException("Access denied"))
                    .when(testAttemptService).cancelAttempt(1L, testUserId);

            // Act & Assert
            mockMvc.perform(post("/api/test-attempts/1/cancel")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // POST /api/test-attempts/{id}/resume TESTS
    // =========================================================================
    @Nested
    @DisplayName("POST /api/test-attempts/{id}/resume")
    class ResumeAttemptTests {

        @Test
        @DisplayName("Should return 200 when resuming attempt")
        void resume_valid_returns200() throws Exception {
            // Arrange
            doNothing().when(testAttemptService).resumeAttempt(1L, testUserId);

            // Act & Assert
            mockMvc.perform(post("/api/test-attempts/1/resume")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isOk());

            verify(testAttemptService).resumeAttempt(1L, testUserId);
        }

        @Test
        @DisplayName("Should return 401 when no JWT token provided")
        void resume_unauthorized_returns401() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/test-attempts/1/resume")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // GET /api/test-attempts/{id}/answers TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/test-attempts/{id}/answers")
    class GetAnswersTests {

        @Test
        @DisplayName("Should return 200 and answers list")
        void getAnswers_valid_returns200() throws Exception {
            // Arrange
            UserAnswerDTO answer1 = new UserAnswerDTO();
            answer1.setQuestionId(1L);
            answer1.setUserAnswer("A");
            
            UserAnswerDTO answer2 = new UserAnswerDTO();
            answer2.setQuestionId(2L);
            answer2.setUserAnswer("B");

            when(testAttemptService.getAnswersForAttempt(1L, testUserId))
                    .thenReturn(List.of(answer1, answer2));

            // Act & Assert
            mockMvc.perform(get("/api/test-attempts/{id}/answers", 1L)
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].questionId").value(1))
                    .andExpect(jsonPath("$[0].userAnswer").value("A"));

            verify(testAttemptService).getAnswersForAttempt(1L, testUserId);
        }

        @Test
        @DisplayName("Should return 401 when no JWT token provided")
        void getAnswers_unauthorized_returns401() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/test-attempts/1/answers"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when accessing another user's answers")
        void getAnswers_idorViolation_returns403() throws Exception {
            // Arrange
            when(testAttemptService.getAnswersForAttempt(1L, testUserId))
                    .thenThrow(new AccessDeniedException("Access denied"));

            // Act & Assert
            mockMvc.perform(get("/api/test-attempts/{id}/answers", 1L)
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // GET /api/test-attempts/{id}/review TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/test-attempts/{id}/review")
    class GetReviewTests {

        @Test
        @DisplayName("Should return 200 and review DTO")
        void getReview_valid_returns200() throws Exception {
            // Arrange
            TestReviewDTO review = new TestReviewDTO();
            review.setAttemptId(1L);
            review.setScore(35);
            review.setTotalQuestions(40);
            review.setExamSource("cambridge");
            review.setTestNumber("1");

            when(testAttemptService.getTestReview(1L, testUserId)).thenReturn(review);

            // Act & Assert
            mockMvc.perform(get("/api/test-attempts/{id}/review", 1L)
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.attemptId").value(1))
                    .andExpect(jsonPath("$.score").value(35))
                    .andExpect(jsonPath("$.totalQuestions").value(40));

            verify(testAttemptService).getTestReview(1L, testUserId);
        }

        @Test
        @DisplayName("Should return 401 when no JWT token provided")
        void getReview_unauthorized_returns401() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/test-attempts/1/review"))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // DELETE /api/test-attempts/{id} TESTS
    // =========================================================================
    @Nested
    @DisplayName("DELETE /api/test-attempts/{id}")
    class DeleteAttemptTests {

        @Test
        @DisplayName("Should return 204 when deleting attempt")
        void delete_valid_returns204() throws Exception {
            // Arrange
            doNothing().when(testAttemptService).deleteAttempt(1L, testUserId);

            // Act & Assert
            mockMvc.perform(delete("/api/test-attempts/{id}", 1L)
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isNoContent());

            verify(testAttemptService).deleteAttempt(1L, testUserId);
        }

        @Test
        @DisplayName("Should return 401 when no JWT token provided")
        void delete_unauthorized_returns401() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/api/test-attempts/1")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when deleting another user's attempt")
        void delete_idorViolation_returns403() throws Exception {
            // Arrange
            doThrow(new AccessDeniedException("Access denied"))
                    .when(testAttemptService).deleteAttempt(1L, testUserId);

            // Act & Assert
            mockMvc.perform(delete("/api/test-attempts/{id}", 1L)
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // POST /api/test-attempts/{id}/regrade TESTS
    // =========================================================================
    @Nested
    @DisplayName("POST /api/test-attempts/{id}/regrade")
    class RegradeAttemptTests {

        @Test
        @DisplayName("Should return 200 and new result when re-grading")
        void regrade_valid_returns200() throws Exception {
            // Arrange
            TestResultDTO result = new TestResultDTO();
            result.setAttemptId(1L);
            result.setScore(36);
            result.setTotalQuestions(40);

            when(testAttemptService.regradeAttempt(1L, testUserId)).thenReturn(result);

            // Act & Assert
            mockMvc.perform(post("/api/test-attempts/1/regrade")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.score").value(36));

            verify(testAttemptService).regradeAttempt(1L, testUserId);
        }

        @Test
        @DisplayName("Should return 401 when no JWT token provided")
        void regrade_unauthorized_returns401() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/test-attempts/1/regrade")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }
}
