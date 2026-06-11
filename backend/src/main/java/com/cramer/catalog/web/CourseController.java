package com.cramer.catalog.web;

import com.cramer.catalog.service.CourseQueryService;
import com.cramer.catalog.web.dto.TestSetView;
import com.cramer.platform.web.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public course browsing (SPEC-11 §3). Only published content is visible.
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseQueryService courses;

    public CourseController(CourseQueryService courses) {
        this.courses = courses;
    }

    @GetMapping
    public PageResponse<String> listCourses(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "6") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String search) {
        return courses.listCourses(page, size, search);
    }

    @GetMapping("/v2")
    public List<TestSetView> listPublishedSets() {
        return courses.listPublishedSets();
    }

    @GetMapping("/{course}/tests")
    public List<Integer> testsForCourse(@PathVariable String course) {
        return courses.testsForCourse(course);
    }

    @GetMapping("/{code}/details")
    public TestSetView details(@PathVariable String code) {
        return courses.setDetails(code);
    }
}
