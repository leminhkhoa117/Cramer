package com.cramer.controller;

import com.cramer.service.CourseService;
import com.cramer.service.TestSetService;
import com.cramer.dto.testhierarchy.TestSetDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@Tag(name = "Courses API", description = "API for browsing courses and tests")
public class CourseController {

    private final CourseService courseService;
    private final TestSetService testSetService;

    @Autowired
    public CourseController(CourseService courseService, TestSetService testSetService) {
        this.courseService = courseService;
        this.testSetService = testSetService;
    }

    @GetMapping
    public ResponseEntity<com.cramer.dto.PageDTO<String>> getAllCourses(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "6") int size,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String search) {
        return ResponseEntity.ok(courseService.getCourses(page, size, search));
    }

    /**
     * New endpoint that returns full TestSetDTO objects (with name, description,
     * etc.)
     */
    @GetMapping("/v2")
    public ResponseEntity<List<TestSetDTO>> getAllCoursesV2() {
        // Only return published test sets for user-facing page
        List<TestSetDTO> allSets = testSetService.getAllTestSets();
        List<TestSetDTO> publishedSets = allSets.stream()
                .filter(ts -> Boolean.TRUE.equals(ts.getIsPublished()))
                .toList();
        return ResponseEntity.ok(publishedSets);
    }

    @GetMapping("/{courseName}/tests")
    public ResponseEntity<List<Integer>> getTestsByCourse(@PathVariable String courseName) {
        List<Integer> tests = courseService.getTestsForCourse(courseName);
        return ResponseEntity.ok(tests);
    }

    @GetMapping("/{courseCode}/details")
    public ResponseEntity<TestSetDTO> getCourseDetails(@PathVariable String courseCode) {
        return testSetService.getByCode(courseCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
