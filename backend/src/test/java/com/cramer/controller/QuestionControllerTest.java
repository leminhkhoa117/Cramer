package com.cramer.controller;

import com.cramer.config.TestSecurityConfig;
import com.cramer.dto.QuestionDTO;
import com.cramer.entity.Question;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.service.QuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for QuestionController.
 * Tests question CRUD operations.
 * 
 * @author Cramer Test Team
 * @since 2026-01-19
 */
import com.cramer.config.JwtAuthFilter;
import com.cramer.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(QuestionController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
@DisplayName("QuestionController Unit Tests")
class QuestionControllerTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private com.cramer.util.JwtUtil jwtUtil;

    @MockitoBean
    private QuestionService questionService;

    private Question mockQuestion;
    private static final java.util.UUID DEFAULT_USER_ID = java.util.UUID.fromString("00000000-0000-0000-0000-000000000000");

    @BeforeEach
    void setUp() {
        mockQuestion = new Question();
        mockQuestion.setId(1L);
        mockQuestion.setQuestionUid("Q001");
        mockQuestion.setQuestionType("MCQ");
        // Create simple JSON content
        ObjectMapper mapper = new ObjectMapper();
        mockQuestion.setQuestionContent(mapper.createObjectNode().put("text", "What is the main idea?"));
    }

    // =========================================================================
    // GET /api/questions TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/questions")
    class GetAllQuestionsTests {

        @Test
        @DisplayName("Should return 200 and list of questions")
        void getAllQuestions_returns200() throws Exception {
            // Arrange
            when(questionService.getAllQuestions()).thenReturn(List.of(mockQuestion));

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/questions")
                            .with(jwt().jwt(jwt -> jwt.subject(DEFAULT_USER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].questionUid").value("Q001"));
        }
    }

    // =========================================================================
    // GET /api/questions/{id} TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/questions/{id}")
    class GetQuestionByIdTests {

        @Test
        @DisplayName("Should return 200 and question when found")
        void getQuestionById_valid_returns200() throws Exception {
            // Arrange
            when(questionService.getQuestionById(1L)).thenReturn(Optional.of(mockQuestion));

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/questions/{id}", 1L)
                            .with(jwt().jwt(jwt -> jwt.subject(DEFAULT_USER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.questionContent.text").value("What is the main idea?"));
        }

        @Test
        @DisplayName("Should return 404 when question not found")
        void getQuestionById_notFound_returns404() throws Exception {
            // Arrange
            when(questionService.getQuestionById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/questions/{id}", 999L)
                            .with(jwt().jwt(jwt -> jwt.subject(DEFAULT_USER_ID.toString()))))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // GET /api/questions/section/{sectionId} TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/questions/section/{sectionId}")
    class GetQuestionsBySectionIdTests {

        @Test
        @DisplayName("Should return 200 and questions for section")
        void getQuestionsBySectionId_valid_returns200() throws Exception {
            // Arrange
            when(questionService.getQuestionsBySectionId(10L)).thenReturn(List.of(mockQuestion));

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/questions/section/{sectionId}", 10L)
                            .with(jwt().jwt(jwt -> jwt.subject(DEFAULT_USER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(1));
        }
    }

    // =========================================================================
    // GET /api/questions/uid/{questionUid} TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/questions/uid/{questionUid}")
    class GetQuestionByUidTests {

        @Test
        @DisplayName("Should return 200 and question when found by UID")
        void getQuestionByUid_valid_returns200() throws Exception {
            // Arrange
            when(questionService.getQuestionByUid("Q001")).thenReturn(Optional.of(mockQuestion));

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/questions/uid/{questionUid}", "Q001")
                            .with(jwt().jwt(jwt -> jwt.subject(DEFAULT_USER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.questionUid").value("Q001"));
        }

        @Test
        @DisplayName("Should return 404 when question UID not found")
        void getQuestionByUid_notFound_returns404() throws Exception {
            // Arrange
            when(questionService.getQuestionByUid("INVALID")).thenReturn(Optional.empty());

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/questions/uid/{questionUid}", "INVALID")
                            .with(jwt().jwt(jwt -> jwt.subject(DEFAULT_USER_ID.toString()))))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // GET /api/questions/type/{questionType} TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/questions/type/{questionType}")
    class GetQuestionsByTypeTests {

        @Test
        @DisplayName("Should return 200 and questions of specified type")
        void getQuestionsByType_valid_returns200() throws Exception {
            // Arrange
            when(questionService.getQuestionsByType("MCQ")).thenReturn(List.of(mockQuestion));

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/questions/type/{questionType}", "MCQ")
                            .with(jwt().jwt(jwt -> jwt.subject(DEFAULT_USER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].questionType").value("MCQ"));
        }
    }

    // =========================================================================
    // GET /api/questions/section/{sectionId}/type/{questionType} TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/questions/section/{sectionId}/type/{questionType}")
    class GetQuestionsBySectionAndTypeTests {

        @Test
        @DisplayName("Should return 200 and filtered questions")
        void getQuestionsBySectionAndType_valid_returns200() throws Exception {
            // Arrange
            when(questionService.getQuestionsBySectionAndType(10L, "MCQ")).thenReturn(List.of(mockQuestion));

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/questions/section/{sectionId}/type/{questionType}", 10L, "MCQ")
                            .with(jwt().jwt(jwt -> jwt.subject(DEFAULT_USER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    // =========================================================================
    // GET /api/questions/types TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/questions/types")
    class GetAllQuestionTypesTests {

        @Test
        @DisplayName("Should return 200 and list of question types")
        void getAllQuestionTypes_returns200() throws Exception {
            // Arrange
            when(questionService.getAllQuestionTypes()).thenReturn(List.of("MCQ", "TF_NG", "MATCHING", "FILL_BLANK"));

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/questions/types")
                            .with(jwt().jwt(jwt -> jwt.subject(DEFAULT_USER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0]").value("MCQ"));
        }
    }

    // =========================================================================
    // POST /api/questions TESTS
    // =========================================================================
    @Nested
    @DisplayName("POST /api/questions")
    class CreateQuestionTests {

        @Test
        @DisplayName("Should return 201 and created question")
        void createQuestion_valid_returns201() throws Exception {
            // Arrange
            when(questionService.createQuestion(any(Question.class))).thenReturn(mockQuestion);

            String requestBody = """
                {
                    "questionUid": "Q001",
                    "questionContent": { "text": "What is the main idea?" },
                    "questionType": "MCQ"
                }
                """;

            // Act & Assert
            mockMvc.perform(post("/api/questions")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(DEFAULT_USER_ID.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.questionUid").value("Q001"));
        }
    }

    // =========================================================================
    // PUT /api/questions/{id} TESTS
    // =========================================================================
    @Nested
    @DisplayName("PUT /api/questions/{id}")
    class UpdateQuestionTests {

        @Test
        @DisplayName("Should return 200 and updated question")
        void updateQuestion_valid_returns200() throws Exception {
            // Arrange
            mockQuestion.setQuestionType("TF");
            when(questionService.updateQuestion(eq(1L), any(Question.class))).thenReturn(mockQuestion);

            String requestBody = """
                {
                    "questionUid": "Q001",
                    "questionType": "TF"
                }
                """;

            // Act & Assert
            mockMvc.perform(put("/api/questions/{id}", 1L)
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(DEFAULT_USER_ID.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.questionType").value("TF"));
        }
    }

    // =========================================================================
    // DELETE /api/questions/{id} TESTS
    // =========================================================================
    @Nested
    @DisplayName("DELETE /api/questions/{id}")
    class DeleteQuestionTests {

        @Test
        @DisplayName("Should return 204 on successful deletion")
        void deleteQuestion_valid_returns204() throws Exception {
            // Arrange
            doNothing().when(questionService).deleteQuestion(1L);

            // Act & Assert
            mockMvc.perform(delete("/api/questions/{id}", 1L)
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(DEFAULT_USER_ID.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isNoContent());

            verify(questionService).deleteQuestion(1L);
        }
    }
}
