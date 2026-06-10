package com.cramer.controller.admin;

import com.cramer.config.SecurityConfig;
import com.cramer.config.JwtAuthFilter;
import com.cramer.dto.testhierarchy.*;
import com.cramer.exception.GlobalExceptionHandler;
import com.cramer.service.HashtagService;
import com.cramer.service.SectionService;
import com.cramer.service.TestManagementService;
import com.cramer.service.TestSetService;
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
import org.springframework.jdbc.core.JdbcTemplate;
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
 * Unit tests for TestHierarchyController.
 * Tests test sets, tests, and hashtags management endpoints.
 * 
 * @author Cramer Test Team
 * @since 2026-01-31
 */
@WebMvcTest(TestHierarchyController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
@DisplayName("TestHierarchyController Unit Tests")
class TestHierarchyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

        @MockitoBean
    private TestSetService testSetService;

        @MockitoBean
    private TestManagementService testManagementService;

        @MockitoBean
    private HashtagService hashtagService;

        @MockitoBean
    private SectionService sectionService;

        @MockitoBean
    private JdbcTemplate jdbcTemplate;

        @MockitoBean
    private com.cramer.util.JwtUtil jwtUtil;

    private static final String DEFAULT_ADMIN_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final UUID DEFAULT_ADMIN_UUID = UUID.fromString(DEFAULT_ADMIN_ID);

    private TestSetDTO testSetDTO;
    private TestSetDetailDTO testSetDetailDTO;
    private HashtagDTO hashtagDTO;

    @BeforeEach
    void setUp() {
        // Setup TestSetDTO
        testSetDTO = TestSetDTO.builder()
                .id(1L)
                .code("cam17")
                .name("Cambridge IELTS 17")
                .description("Academic IELTS practice tests")
                .isPublished(true)
                .testCount(4L)
                .displayOrder(1)
                .build();

        // Setup TestSetDetailDTO
        testSetDetailDTO = TestSetDetailDTO.builder()
                .id(1L)
                .code("cam17")
                .name("Cambridge IELTS 17")
                .isPublished(true)
                .tests(Collections.emptyList())
                .build();

        // Setup HashtagDTO
        hashtagDTO = HashtagDTO.builder()
                .id(1L)
                .code("academic")
                .name("Academic")
                .category("type")
                .isActive(true)
                .useCount(25)
                .build();
    }

    private ResultActions performGet(String url, Object... uriVars) throws Exception {
        return mockMvc.perform(get(url, uriVars)
                .with(jwt().jwt(jwtBuilder -> jwtBuilder
                        .subject(DEFAULT_ADMIN_ID)
                        .claim("aud", "authenticated"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private ResultActions performPost(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .with(jwt().jwt(jwtBuilder -> jwtBuilder
                        .subject(DEFAULT_ADMIN_ID)
                        .claim("aud", "authenticated"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions performPut(String url, Object body) throws Exception {
        return mockMvc.perform(put(url)
                .with(jwt().jwt(jwtBuilder -> jwtBuilder
                        .subject(DEFAULT_ADMIN_ID)
                        .claim("aud", "authenticated"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions performDelete(String url) throws Exception {
        return mockMvc.perform(delete(url)
                .with(jwt().jwt(jwtBuilder -> jwtBuilder
                        .subject(DEFAULT_ADMIN_ID)
                        .claim("aud", "authenticated"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    // =========================================================================
    // TEST SETS ENDPOINTS
    // =========================================================================
    @Nested
    @DisplayName("Test Sets Endpoints")
    class TestSetsTests {

        @Test
        @DisplayName("GET /api/admin/test-sets - Should return all test sets")
        void getAllTestSets_valid_returns200() throws Exception {
            // Arrange
            when(testSetService.getAllTestSets()).thenReturn(Arrays.asList(testSetDTO));

            // Act & Assert
            performGet("/api/admin/test-sets")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].code").value("cam17"))
                    .andExpect(jsonPath("$[0].name").value("Cambridge IELTS 17"));

            verify(testSetService).getAllTestSets();
        }

        @Test
        @DisplayName("GET /api/admin/test-sets/{id} - Should return test set by ID")
        void getTestSetById_exists_returns200() throws Exception {
            // Arrange
            when(testSetService.getTestSetById(1L)).thenReturn(testSetDetailDTO);

            // Act & Assert
            performGet("/api/admin/test-sets/{id}", 1L)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.code").value("cam17"));

            verify(testSetService).getTestSetById(1L);
        }

        @Test
        @DisplayName("POST /api/admin/test-sets - Should create test set")
        void createTestSet_valid_returns201() throws Exception {
            // Arrange
            CreateTestSetRequest request = CreateTestSetRequest.builder()
                    .code("cam18")
                    .name("Cambridge IELTS 18")
                    .sourceType("cambridge")
                    .build();

            TestSetDTO created = TestSetDTO.builder()
                    .id(2L)
                    .code("cam18")
                    .name("Cambridge IELTS 18")
                    .build();

            when(testSetService.createTestSet(any(CreateTestSetRequest.class), any(UUID.class)))
                    .thenReturn(created);

            // Act & Assert
            performPost("/api/admin/test-sets", request)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("cam18"));
        }

        @Test
        @DisplayName("PUT /api/admin/test-sets/{id} - Should update test set")
        void updateTestSet_valid_returns200() throws Exception {
            // Arrange
            CreateTestSetRequest request = CreateTestSetRequest.builder()
                    .code("cam17")
                    .name("Cambridge IELTS 17 Updated")
                    .sourceType("cambridge")
                    .build();

            TestSetDTO updated = TestSetDTO.builder()
                    .id(1L)
                    .code("cam17")
                    .name("Cambridge IELTS 17 Updated")
                    .build();

            when(testSetService.updateTestSet(eq(1L), any(CreateTestSetRequest.class)))
                    .thenReturn(updated);

            // Act & Assert
            performPut("/api/admin/test-sets/1", request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Cambridge IELTS 17 Updated"));
        }
    }

    // =========================================================================
    // HASHTAGS ENDPOINTS
    // =========================================================================
    @Nested
    @DisplayName("Hashtags Endpoints")
    class HashtagsTests {

        @Test
        @DisplayName("GET /api/admin/hashtags - Should return all hashtags")
        void getAllHashtags_valid_returns200() throws Exception {
            // Arrange
            when(hashtagService.getAllHashtags()).thenReturn(Arrays.asList(hashtagDTO));

            // Act & Assert
            performGet("/api/admin/hashtags")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].code").value("academic"));

            verify(hashtagService).getAllHashtags();
        }

        @Test
        @DisplayName("POST /api/admin/hashtags - Should create hashtag")
        void createHashtag_valid_returns201() throws Exception {
            // Arrange
            CreateHashtagRequest request = CreateHashtagRequest.builder()
                    .code("general")
                    .name("General Training")
                    .category("type")
                    .build();

            HashtagDTO created = HashtagDTO.builder()
                    .id(2L)
                    .code("general")
                    .name("General Training")
                    .category("type")
                    .build();

            when(hashtagService.createHashtag(any(CreateHashtagRequest.class)))
                    .thenReturn(created);

            // Act & Assert
            performPost("/api/admin/hashtags", request)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("general"));
        }

        @Test
        @DisplayName("PUT /api/admin/hashtags/{id} - Should update hashtag")
        void updateHashtag_valid_returns200() throws Exception {
            // Arrange
            CreateHashtagRequest request = CreateHashtagRequest.builder()
                    .code("academic")
                    .name("Academic Updated")
                    .category("type")
                    .build();

            HashtagDTO updated = HashtagDTO.builder()
                    .id(1L)
                    .code("academic")
                    .name("Academic Updated")
                    .build();

            when(hashtagService.updateHashtag(eq(1L), any(CreateHashtagRequest.class)))
                    .thenReturn(updated);

            // Act & Assert
            performPut("/api/admin/hashtags/1", request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Academic Updated"));
        }

        @Test
        @DisplayName("DELETE /api/admin/hashtags/{id} - Should delete hashtag")
        void deleteHashtag_exists_returns204() throws Exception {
            // Arrange
            doNothing().when(hashtagService).deleteHashtag(1L);

            // Act & Assert
            performDelete("/api/admin/hashtags/1")
                    .andExpect(status().isNoContent());

            verify(hashtagService).deleteHashtag(1L);
        }
    }
}
