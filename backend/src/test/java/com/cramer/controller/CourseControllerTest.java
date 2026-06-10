package com.cramer.controller;

import com.cramer.config.JwtAuthFilter;
import com.cramer.config.SecurityConfig;
import com.cramer.dto.PageDTO;
import com.cramer.dto.testhierarchy.TestSetDTO;
import com.cramer.service.CourseService;
import com.cramer.service.TestSetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for CourseController.
 * Tests course listing, test browsing, and course details endpoints.
 * All endpoints require JWT authentication.
 * 
 * @author Cramer Test Team
 * @since 2026-01-25
 */
@WebMvcTest(CourseController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
@DisplayName("CourseController Unit Tests")
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private com.cramer.util.JwtUtil jwtUtil;

    @MockitoBean
    private CourseService courseService;

    @MockitoBean
    private TestSetService testSetService;

    private TestSetDTO mockTestSet;
    private UUID testUserId;
    private static final UUID DEFAULT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @BeforeEach
    void setUp() {
        testUserId = DEFAULT_USER_ID;
        mockTestSet = TestSetDTO.builder()
                .id(1L)
                .code("cam17")
                .name("Cambridge IELTS 17")
                .description("Practice tests from Cambridge IELTS 17")
                .coverImageUrl("https://example.com/cam17.jpg")
                .sourceType("CAMBRIDGE")
                .isPublished(true)
                .displayOrder(1)
                .testCount(4L)
                .publishedTestCount(4L)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    // =========================================================================
    // GET /api/courses TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/courses")
    class GetAllCoursesTests {

        @Test
        @DisplayName("Should return 200 and paginated course list")
        void getAllCourses_valid_returns200() throws Exception {
            PageDTO<String> pageResult = new PageDTO<>(
                    List.of("cam17", "cam18", "cam19"),
                    0, 6, 3, 1
            );

            when(courseService.getCourses(eq(0), eq(6), isNull())).thenReturn(pageResult);

            mockMvc.perform(get("/api/courses")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0]").value("cam17"))
                    .andExpect(jsonPath("$.pageNumber").value(0))
                    .andExpect(jsonPath("$.totalElements").value(3));
        }

        @Test
        @DisplayName("Should return 200 with pagination params")
        void getAllCourses_withPagination_returns200() throws Exception {
            PageDTO<String> pageResult = new PageDTO<>(
                    List.of("cam19"),
                    1, 2, 5, 3
            );

            when(courseService.getCourses(eq(1), eq(2), isNull())).thenReturn(pageResult);

            mockMvc.perform(get("/api/courses")
                            .param("page", "1")
                            .param("size", "2")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pageNumber").value(1))
                    .andExpect(jsonPath("$.pageSize").value(2));

            verify(courseService).getCourses(eq(1), eq(2), isNull());
        }

        @Test
        @DisplayName("Should return 200 with search filter")
        void getAllCourses_withSearch_returns200() throws Exception {
            PageDTO<String> pageResult = new PageDTO<>(
                    List.of("cam17"),
                    0, 6, 1, 1
            );

            when(courseService.getCourses(eq(0), eq(6), eq("cam17"))).thenReturn(pageResult);

            mockMvc.perform(get("/api/courses")
                            .param("search", "cam17")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0]").value("cam17"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Should return 200 with empty list")
        void getAllCourses_empty_returns200() throws Exception {
            PageDTO<String> pageResult = new PageDTO<>(
                    List.of(),
                    0, 6, 0, 0
            );

            when(courseService.getCourses(anyInt(), anyInt(), any())).thenReturn(pageResult);

            mockMvc.perform(get("/api/courses")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("Should return error when size exceeds max limit")
        void getAllCourses_largeSize_returnsError() throws Exception {
            mockMvc.perform(get("/api/courses")
                            .param("size", "500")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().is5xxServerError());

            verify(courseService, never()).getCourses(anyInt(), anyInt(), any());
        }

        @Test
        @DisplayName("Should return 403 when no JWT token")
        void getAllCourses_unauthorized_returns403() throws Exception {
            mockMvc.perform(get("/api/courses"))
                    .andExpect(status().isForbidden());

            verify(courseService, never()).getCourses(anyInt(), anyInt(), any());
        }
    }

    // =========================================================================
    // GET /api/courses/v2 TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/courses/v2")
    class GetAllCoursesV2Tests {

        @Test
        @DisplayName("Should return 200 and list of TestSetDTO")
        void getAllCoursesV2_valid_returns200() throws Exception {
            when(testSetService.getAllTestSets()).thenReturn(List.of(mockTestSet));

            mockMvc.perform(get("/api/courses/v2")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].code").value("cam17"))
                    .andExpect(jsonPath("$[0].name").value("Cambridge IELTS 17"))
                    .andExpect(jsonPath("$[0].isPublished").value(true));
        }

        @Test
        @DisplayName("Should filter out unpublished test sets")
        void getAllCoursesV2_filterUnpublished_returns200() throws Exception {
            TestSetDTO unpublished = TestSetDTO.builder()
                    .id(2L)
                    .code("draft01")
                    .name("Draft Test Set")
                    .isPublished(false)
                    .build();

            when(testSetService.getAllTestSets()).thenReturn(List.of(mockTestSet, unpublished));

            mockMvc.perform(get("/api/courses/v2")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].code").value("cam17"));
        }

        @Test
        @DisplayName("Should return 200 with empty list")
        void getAllCoursesV2_empty_returns200() throws Exception {
            when(testSetService.getAllTestSets()).thenReturn(List.of());

            mockMvc.perform(get("/api/courses/v2")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("Should return 403 when no JWT token")
        void getAllCoursesV2_unauthorized_returns403() throws Exception {
            mockMvc.perform(get("/api/courses/v2"))
                    .andExpect(status().isForbidden());

            verify(testSetService, never()).getAllTestSets();
        }
    }

    // =========================================================================
    // GET /api/courses/{courseName}/tests TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/courses/{courseName}/tests")
    class GetTestsByCourseTests {

        @Test
        @DisplayName("Should return 200 and list of test numbers")
        void getTestsByCourse_valid_returns200() throws Exception {
            when(courseService.getTestsForCourse("cam17")).thenReturn(List.of(1, 2, 3, 4));

            mockMvc.perform(get("/api/courses/{courseName}/tests", "cam17")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0]").value(1))
                    .andExpect(jsonPath("$[1]").value(2))
                    .andExpect(jsonPath("$.length()").value(4));
        }

        @Test
        @DisplayName("Should return 200 with empty list when no tests")
        void getTestsByCourse_noTests_returns200() throws Exception {
            when(courseService.getTestsForCourse("newcourse")).thenReturn(List.of());

            mockMvc.perform(get("/api/courses/{courseName}/tests", "newcourse")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("Should return 403 when no JWT token")
        void getTestsByCourse_unauthorized_returns403() throws Exception {
            mockMvc.perform(get("/api/courses/{courseName}/tests", "cam17"))
                    .andExpect(status().isForbidden());

            verify(courseService, never()).getTestsForCourse(any());
        }
    }

    // =========================================================================
    // GET /api/courses/{courseCode}/details TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/courses/{courseCode}/details")
    class GetCourseDetailsTests {

        @Test
        @DisplayName("Should return 200 and course details")
        void getCourseDetails_valid_returns200() throws Exception {
            when(testSetService.getByCode("cam17")).thenReturn(Optional.of(mockTestSet));

            mockMvc.perform(get("/api/courses/{courseCode}/details", "cam17")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("cam17"))
                    .andExpect(jsonPath("$.name").value("Cambridge IELTS 17"))
                    .andExpect(jsonPath("$.description").value("Practice tests from Cambridge IELTS 17"))
                    .andExpect(jsonPath("$.testCount").value(4));
        }

        @Test
        @DisplayName("Should return 404 when course not found")
        void getCourseDetails_notFound_returns404() throws Exception {
            when(testSetService.getByCode("nonexistent")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/courses/{courseCode}/details", "nonexistent")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when no JWT token")
        void getCourseDetails_unauthorized_returns403() throws Exception {
            mockMvc.perform(get("/api/courses/{courseCode}/details", "cam17"))
                    .andExpect(status().isForbidden());

            verify(testSetService, never()).getByCode(any());
        }
    }
}
