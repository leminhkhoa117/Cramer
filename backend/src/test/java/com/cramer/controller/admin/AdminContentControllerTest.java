package com.cramer.controller.admin;

import com.cramer.config.JwtAuthFilter;
import com.cramer.config.SecurityConfig;
import com.cramer.exception.GlobalExceptionHandler;
import com.cramer.service.AdminContentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AdminContentController.
 * Tests content management endpoints: topics, tests, sections, questions.
 * 
 * @author Cramer Test Team
 * @since 2026-01-31
 */
@WebMvcTest(AdminContentController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
@DisplayName("AdminContentController Unit Tests")
class AdminContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminContentService adminContentService;

    @MockBean
    private com.cramer.util.JwtUtil jwtUtil;

    private static final String DEFAULT_ADMIN_ID = "550e8400-e29b-41d4-a716-446655440000";

    private ResultActions performGet(String url, Object... uriVars) throws Exception {
        return mockMvc.perform(get(url, uriVars)
                .header("X-User-Id", DEFAULT_ADMIN_ID)
                .with(jwt().jwt(jwtBuilder -> jwtBuilder
                        .subject(DEFAULT_ADMIN_ID)
                        .claim("aud", "authenticated"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private ResultActions performPost(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .header("X-User-Id", DEFAULT_ADMIN_ID)
                .with(jwt().jwt(jwtBuilder -> jwtBuilder
                        .subject(DEFAULT_ADMIN_ID)
                        .claim("aud", "authenticated"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    // =========================================================================
    // GET /api/admin/content/topics TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/admin/content/topics")
    class GetTopicsTests {

        @Test
        @DisplayName("Should return 200 and topics list")
        void getTopics_valid_returns200() throws Exception {
            // Arrange
            List<Map<String, Object>> topics = new ArrayList<>();
            Map<String, Object> topic = new HashMap<>();
            topic.put("name", "Cambridge IELTS 17");
            topic.put("code", "cam17");
            topics.add(topic);

            when(adminContentService.getTopicsWithTests(isNull(), isNull())).thenReturn(topics);

            // Act & Assert
            performGet("/api/admin/content/topics")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].name").value("Cambridge IELTS 17"));

            verify(adminContentService).getTopicsWithTests(null, null);
        }
    }

    // =========================================================================
    // GET /api/admin/content/overview TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/admin/content/overview")
    class GetContentOverviewTests {

        @Test
        @DisplayName("Should return 200 and content overview")
        void getContentOverview_valid_returns200() throws Exception {
            // Arrange
            Map<String, Object> overview = new HashMap<>();
            overview.put("totalTests", 10L);
            overview.put("totalSections", 50L);
            overview.put("totalQuestions", 500L);

            when(adminContentService.getContentOverview()).thenReturn(overview);

            // Act & Assert
            performGet("/api/admin/content/overview")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalTests").value(10))
                    .andExpect(jsonPath("$.totalSections").value(50));
        }
    }

    // =========================================================================
    // GET /api/admin/content/tests/{examSource}/{testNumber} TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/admin/content/tests/{examSource}/{testNumber}")
    class GetTestDetailsTests {

        @Test
        @DisplayName("Should return 200 and test details when found")
        void getTestDetails_exists_returns200() throws Exception {
            // Arrange
            Map<String, Object> testDetails = new HashMap<>();
            testDetails.put("examSource", "cam17");
            testDetails.put("testNumber", 1);
            testDetails.put("name", "Test 1");

            when(adminContentService.getTestDetails("cam17", 1)).thenReturn(testDetails);

            // Act & Assert
            performGet("/api/admin/content/tests/{examSource}/{testNumber}", "cam17", 1)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.examSource").value("cam17"))
                    .andExpect(jsonPath("$.testNumber").value(1));
        }

        @Test
        @DisplayName("Should return 404 when test not found")
        void getTestDetails_notFound_returns404() throws Exception {
            // Arrange
            when(adminContentService.getTestDetails("nonexistent", 1)).thenReturn(null);

            // Act & Assert
            performGet("/api/admin/content/tests/{examSource}/{testNumber}", "nonexistent", 1)
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // GET /api/admin/content/sections/{sectionId}/questions TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/admin/content/sections/{sectionId}/questions")
    class GetQuestionsBySectionTests {

        @Test
        @DisplayName("Should return 200 and questions list")
        void getQuestions_valid_returns200() throws Exception {
            // Arrange
            List<Map<String, Object>> questions = new ArrayList<>();
            Map<String, Object> question = new HashMap<>();
            question.put("id", 1L);
            question.put("questionNumber", 1);
            question.put("type", "MULTIPLE_CHOICE");
            questions.add(question);

            when(adminContentService.getQuestionsBySection(1L)).thenReturn(questions);

            // Act & Assert
            performGet("/api/admin/content/sections/{sectionId}/questions", 1L)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(1));
        }
    }

    // =========================================================================
    // POST /api/admin/content/sections TESTS
    // =========================================================================
    @Nested
    @DisplayName("POST /api/admin/content/sections")
    class CreateSectionTests {

        @Test
        @DisplayName("Should return 200 and created section")
        void createSection_valid_returns200() throws Exception {
            // Arrange
            Map<String, Object> sectionData = new HashMap<>();
            sectionData.put("examSource", "cam17");
            sectionData.put("testNumber", 1);
            sectionData.put("skill", "reading");
            sectionData.put("partNumber", 1);

            Map<String, Object> result = new HashMap<>();
            result.put("id", 100L);
            result.put("success", true);

            when(adminContentService.createSection(anyMap(), eq(DEFAULT_ADMIN_ID))).thenReturn(result);

            // Act & Assert
            performPost("/api/admin/content/sections", sectionData)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(100));
        }
    }
}
