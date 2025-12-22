package com.cramer.service;

import java.util.List;
import java.util.Map;

/**
 * Admin Content Service Interface - Xử lý logic quản lý nội dung đề thi cho
 * Admin CMS
 */
public interface AdminContentService {

    /**
     * Lấy danh sách topics với các tests
     * 
     * @param search Tìm kiếm (optional)
     * @param status Lọc theo trạng thái (optional)
     * @return Danh sách topics với thông tin chi tiết
     */
    List<Map<String, Object>> getTopicsWithTests(String search, String status);

    /**
     * Lấy thống kê tổng quan về nội dung
     * 
     * @return Map chứa các metric: totalTests, publishedTests, draftTests,
     *         totalAttempts, etc.
     */
    Map<String, Object> getContentOverview();

    /**
     * Lấy chi tiết một test cụ thể
     * 
     * @param examSource Nguồn đề (vd: cam17, cam18)
     * @param testNumber Số test
     * @return Chi tiết test bao gồm các skills
     */
    Map<String, Object> getTestDetails(String examSource, Integer testNumber);

    /**
     * Lấy danh sách sections của một skill
     * 
     * @param examSource Nguồn đề
     * @param testNumber Số test
     * @param skill      Kỹ năng
     * @return Danh sách sections
     */
    List<Map<String, Object>> getSections(String examSource, Integer testNumber, String skill);

    /**
     * Lấy danh sách questions của một section
     * 
     * @param sectionId ID của section
     * @return Danh sách questions
     */
    List<Map<String, Object>> getQuestionsBySection(Long sectionId);

    /**
     * Lấy hoạt động gần đây
     * 
     * @param limit Số lượng activities cần lấy
     * @return Danh sách activities
     */
    List<Map<String, Object>> getRecentActivities(int limit);

    /**
     * Tạo section mới
     */
    Map<String, Object> createSection(Map<String, Object> sectionData, String adminUserId);

    /**
     * Cập nhật section
     */
    Map<String, Object> updateSection(Long sectionId, Map<String, Object> sectionData, String adminUserId);

    /**
     * Lấy chi tiết section theo ID
     */
    Map<String, Object> getSectionById(Long sectionId);

    /**
     * Tạo câu hỏi mới
     */
    Map<String, Object> createQuestion(Long sectionId, Map<String, Object> questionData, String adminUserId);

    /**
     * Cập nhật câu hỏi
     */
    Map<String, Object> updateQuestion(Long questionId, Map<String, Object> questionData, String adminUserId);

    /**
     * Xóa câu hỏi
     */
    void deleteQuestion(Long questionId, String adminUserId);

    /**
     * Lấy chi tiết question theo ID
     */
    Map<String, Object> getQuestionById(Long questionId);

    /**
     * Cập nhật trạng thái của test (tất cả sections)
     */
    Map<String, Object> updateTestStatus(String examSource, Integer testNumber, String status, String adminUserId);

    /**
     * Tạo test mới
     */
    Map<String, Object> createTest(Map<String, Object> testData, String adminUserId);
}
