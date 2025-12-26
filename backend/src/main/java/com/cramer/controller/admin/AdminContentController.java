package com.cramer.controller.admin;

import com.cramer.service.AdminContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin Content Controller - Quản lý nội dung đề thi (Admin CMS)
 * 
 * API endpoints cho module Quản lý Nội dung Đề thi trong Admin CMS.
 * Được bảo vệ bởi AdminAuthFilter - chỉ admin được phép truy cập.
 */
@RestController
@RequestMapping("/api/admin/content")
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:5173" })
public class AdminContentController {

    @Autowired
    private AdminContentService adminContentService;

    /**
     * Lấy danh sách topics với các tests
     * 
     * @param search Tìm kiếm theo tên topic/test
     * @param status Lọc theo trạng thái (DRAFT, PUBLISHED, ARCHIVED)
     */
    @GetMapping("/topics")
    public ResponseEntity<List<Map<String, Object>>> getTopics(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestHeader("X-User-Id") String adminUserId) {
        List<Map<String, Object>> topics = adminContentService.getTopicsWithTests(search, status);
        return ResponseEntity.ok(topics);
    }

    /**
     * Lấy thống kê tổng quan về nội dung
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getContentOverview(
            @RequestHeader("X-User-Id") String adminUserId) {
        Map<String, Object> overview = adminContentService.getContentOverview();
        return ResponseEntity.ok(overview);
    }

    /**
     * Lấy chi tiết một test cụ thể
     * 
     * @param examSource Nguồn đề (vd: cam17, cam18)
     * @param testNumber Số test (vd: 1, 2, 3, 4)
     */
    @GetMapping("/tests/{examSource}/{testNumber}")
    public ResponseEntity<Map<String, Object>> getTestDetails(
            @PathVariable String examSource,
            @PathVariable Integer testNumber,
            @RequestHeader("X-User-Id") String adminUserId) {
        Map<String, Object> testDetails = adminContentService.getTestDetails(examSource, testNumber);
        if (testDetails == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(testDetails);
    }

    /**
     * Lấy danh sách sections của một skill cụ thể
     * 
     * @param examSource Nguồn đề
     * @param testNumber Số test
     * @param skill      Kỹ năng (reading, listening, writing, speaking)
     */
    @GetMapping("/tests/{examSource}/{testNumber}/{skill}/sections")
    public ResponseEntity<List<Map<String, Object>>> getSections(
            @PathVariable String examSource,
            @PathVariable Integer testNumber,
            @PathVariable String skill,
            @RequestHeader("X-User-Id") String adminUserId) {
        List<Map<String, Object>> sections = adminContentService.getSections(examSource, testNumber, skill);
        return ResponseEntity.ok(sections);
    }

    /**
     * Lấy danh sách questions của một section
     * 
     * @param sectionId ID của section
     */
    @GetMapping("/sections/{sectionId}/questions")
    public ResponseEntity<List<Map<String, Object>>> getQuestions(
            @PathVariable Long sectionId,
            @RequestHeader("X-User-Id") String adminUserId) {
        List<Map<String, Object>> questions = adminContentService.getQuestionsBySection(sectionId);
        return ResponseEntity.ok(questions);
    }

    /**
     * Lấy hoạt động gần đây (placeholder - sẽ cần bảng admin_audit_log)
     */
    @GetMapping("/activities")
    public ResponseEntity<List<Map<String, Object>>> getRecentActivities(
            @RequestParam(defaultValue = "10") int limit,
            @RequestHeader("X-User-Id") String adminUserId) {
        List<Map<String, Object>> activities = adminContentService.getRecentActivities(limit);
        return ResponseEntity.ok(activities);
    }

    /**
     * Tạo section mới
     */
    @PostMapping("/sections")
    public ResponseEntity<Map<String, Object>> createSection(
            @RequestBody Map<String, Object> sectionData,
            @RequestHeader("X-User-Id") String adminUserId) {
        try {
            Map<String, Object> result = adminContentService.createSection(sectionData, adminUserId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Cập nhật section
     */
    @PutMapping("/sections/{sectionId}")
    public ResponseEntity<Map<String, Object>> updateSection(
            @PathVariable Long sectionId,
            @RequestBody Map<String, Object> sectionData,
            @RequestHeader("X-User-Id") String adminUserId) {
        try {
            Map<String, Object> result = adminContentService.updateSection(sectionId, sectionData, adminUserId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Tạo câu hỏi mới
     */
    @PostMapping("/sections/{sectionId}/questions")
    public ResponseEntity<Map<String, Object>> createQuestion(
            @PathVariable Long sectionId,
            @RequestBody Map<String, Object> questionData,
            @RequestHeader("X-User-Id") String adminUserId) {
        try {
            Map<String, Object> result = adminContentService.createQuestion(sectionId, questionData, adminUserId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Cập nhật câu hỏi
     */
    @PutMapping("/questions/{questionId}")
    public ResponseEntity<Map<String, Object>> updateQuestion(
            @PathVariable Long questionId,
            @RequestBody Map<String, Object> questionData,
            @RequestHeader("X-User-Id") String adminUserId) {
        try {
            Map<String, Object> result = adminContentService.updateQuestion(questionId, questionData, adminUserId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Xóa câu hỏi
     */
    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<Map<String, Object>> deleteQuestion(
            @PathVariable Long questionId,
            @RequestHeader("X-User-Id") String adminUserId) {
        try {
            adminContentService.deleteQuestion(questionId, adminUserId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa câu hỏi"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Cập nhật trạng thái của một test (tất cả sections trong test)
     * 
     * @param examSource Nguồn đề
     * @param testNumber Số test
     * @param status     Trạng thái mới (DRAFT, PUBLISHED, ARCHIVED)
     */
    @PatchMapping("/tests/{examSource}/{testNumber}/status")
    public ResponseEntity<Map<String, Object>> updateTestStatus(
            @PathVariable String examSource,
            @PathVariable Integer testNumber,
            @RequestBody Map<String, String> body,
            @RequestHeader("X-User-Id") String adminUserId) {
        try {
            String status = body.get("status");
            Map<String, Object> result = adminContentService.updateTestStatus(examSource, testNumber, status,
                    adminUserId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy chi tiết một section theo ID
     */
    @GetMapping("/sections/{sectionId}")
    public ResponseEntity<Map<String, Object>> getSectionById(
            @PathVariable Long sectionId,
            @RequestHeader("X-User-Id") String adminUserId) {
        Map<String, Object> section = adminContentService.getSectionById(sectionId);
        if (section == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(section);
    }

    /**
     * Lấy chi tiết một question theo ID
     */
    @GetMapping("/questions/{questionId}")
    public ResponseEntity<Map<String, Object>> getQuestionById(
            @PathVariable Long questionId,
            @RequestHeader("X-User-Id") String adminUserId) {
        Map<String, Object> question = adminContentService.getQuestionById(questionId);
        if (question == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(question);
    }

    /**
     * Tạo test mới
     */
    @PostMapping("/tests")
    public ResponseEntity<Map<String, Object>> createTest(
            @RequestBody Map<String, Object> testData,
            @RequestHeader("X-User-Id") String adminUserId) {
        try {
            Map<String, Object> result = adminContentService.createTest(testData, adminUserId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Cập nhật test
     */
    @PutMapping("/tests/{testId}")
    public ResponseEntity<Map<String, Object>> updateTest(
            @PathVariable Long testId,
            @RequestBody Map<String, Object> testData,
            @RequestHeader("X-User-Id") String adminUserId) {
        try {
            Map<String, Object> result = adminContentService.updateTest(testId, testData, adminUserId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Xóa test
     * 
     * @param testId ID của test
     */
    @DeleteMapping("/tests/{testId}")
    public ResponseEntity<Map<String, Object>> deleteTest(
            @PathVariable String testId,
            @RequestHeader("X-User-Id") String adminUserId) {
        try {
            adminContentService.deleteTest(testId, adminUserId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa đề thi"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Tạo bộ đề mới
     */
    @PostMapping("/test-sets")
    public ResponseEntity<Map<String, Object>> createTestSet(
            @RequestBody Map<String, Object> setData,
            @RequestHeader("X-User-Id") String adminUserId) {
        try {
            Map<String, Object> result = adminContentService.createTestSet(setData, adminUserId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Cập nhật bộ đề
     */
    @PutMapping("/test-sets/{setId}")
    public ResponseEntity<Map<String, Object>> updateTestSet(
            @PathVariable Long setId,
            @RequestBody Map<String, Object> setData,
            @RequestHeader("X-User-Id") String adminUserId) {
        try {
            Map<String, Object> result = adminContentService.updateTestSet(setId, setData, adminUserId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Xóa bộ đề
     */
    @DeleteMapping("/test-sets/{setId}")
    public ResponseEntity<Map<String, Object>> deleteTestSet(
            @PathVariable Long setId,
            @RequestHeader("X-User-Id") String adminUserId) {
        try {
            adminContentService.deleteTestSet(setId, adminUserId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa bộ đề"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
