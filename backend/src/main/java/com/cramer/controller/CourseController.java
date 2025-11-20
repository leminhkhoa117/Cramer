package com.cramer.controller;

import com.cramer.service.CourseService;
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

    @Autowired
    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ResponseEntity<com.cramer.dto.PageDTO<String>> getAllCourses(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "6") int size,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(courseService.getCourses(page, size, search));
    }

    @GetMapping("/{courseName}/tests")
    public ResponseEntity<List<Integer>> getTestsByCourse(@PathVariable String courseName) {
        List<Integer> tests = courseService.getTestsForCourse(courseName);
        return ResponseEntity.ok(tests);
    }
}
