package com.cramer.service.unit;

import com.cramer.dto.PageDTO;
import com.cramer.repository.SectionRepository;
import com.cramer.service.CourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CourseService.
 * Tests course listing and test retrieval operations.
 * 
 * @author Cramer Test Team
 * @since 2026-01-25
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CourseService Unit Tests")
class CourseServiceTest {

    @Mock
    private SectionRepository sectionRepository;

    @InjectMocks
    private CourseService courseService;

    // =========================================================================
    // GET COURSES TESTS
    // =========================================================================
    @Nested
    @DisplayName("getCourses() Tests")
    class GetCoursesTests {

        @Test
        @DisplayName("Should return paginated courses")
        void getCourses_hasData_returnsPage() {
            List<String> courses = List.of("cambridge", "ielts-practice", "british-council");
            Pageable pageable = PageRequest.of(0, 10);
            Page<String> page = new PageImpl<>(courses, pageable, 3);

            when(sectionRepository.findDistinctExamSources(any(Pageable.class), isNull()))
                    .thenReturn(page);

            PageDTO<String> result = courseService.getCourses(0, 10, null);

            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent()).containsExactly("cambridge", "ielts-practice", "british-council");
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getTotalPages()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return empty page when no courses")
        void getCourses_noCourses_returnsEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<String> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(sectionRepository.findDistinctExamSources(any(Pageable.class), isNull()))
                    .thenReturn(emptyPage);

            PageDTO<String> result = courseService.getCourses(0, 10, null);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should filter courses by search term")
        void getCourses_withSearch_filtersResults() {
            List<String> courses = List.of("cambridge");
            Pageable pageable = PageRequest.of(0, 10);
            Page<String> page = new PageImpl<>(courses, pageable, 1);

            when(sectionRepository.findDistinctExamSources(any(Pageable.class), eq("cam")))
                    .thenReturn(page);

            PageDTO<String> result = courseService.getCourses(0, 10, "cam");

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent()).containsExactly("cambridge");
        }

        @Test
        @DisplayName("Should paginate correctly")
        void getCourses_pagination_worksCorrectly() {
            List<String> page2Courses = List.of("ielts-practice");
            Pageable pageable = PageRequest.of(1, 1);
            Page<String> page = new PageImpl<>(page2Courses, pageable, 3);

            when(sectionRepository.findDistinctExamSources(any(Pageable.class), isNull()))
                    .thenReturn(page);

            PageDTO<String> result = courseService.getCourses(1, 1, null);

            assertThat(result.getContent()).containsExactly("ielts-practice");
            assertThat(result.getPageNumber()).isEqualTo(1);
            assertThat(result.getPageSize()).isEqualTo(1);
            assertThat(result.getTotalPages()).isEqualTo(3);
        }
    }

    // =========================================================================
    // GET TESTS FOR COURSE TESTS
    // =========================================================================
    @Nested
    @DisplayName("getTestsForCourse() Tests")
    class GetTestsForCourseTests {

        @Test
        @DisplayName("Should return test numbers for course")
        void getTestsForCourse_hasTests_returnsTestNumbers() {
            when(sectionRepository.findDistinctTestNumbersByExamSource("cambridge"))
                    .thenReturn(List.of(1, 2, 3, 4));

            List<Integer> result = courseService.getTestsForCourse("cambridge");

            assertThat(result).containsExactly(1, 2, 3, 4);
        }

        @Test
        @DisplayName("Should return empty list for unknown course")
        void getTestsForCourse_unknownCourse_returnsEmpty() {
            when(sectionRepository.findDistinctTestNumbersByExamSource("unknown"))
                    .thenReturn(List.of());

            List<Integer> result = courseService.getTestsForCourse("unknown");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should call repository with correct course name")
        void getTestsForCourse_verifiesRepositoryCall() {
            when(sectionRepository.findDistinctTestNumbersByExamSource("british-council"))
                    .thenReturn(List.of(1, 2));

            courseService.getTestsForCourse("british-council");

            verify(sectionRepository).findDistinctTestNumbersByExamSource("british-council");
        }
    }
}
