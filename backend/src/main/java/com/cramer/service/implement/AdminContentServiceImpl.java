package com.cramer.service.implement;

import com.cramer.service.AdminContentService;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.repository.*;
import com.cramer.entity.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.Objects;

/**
 * Admin Content Service Implementation - Xử lý logic quản lý nội dung đề thi
 * cho Admin CMS
 * 
 * OPTIMIZED VERSION: Sử dụng batch queries và caching để giảm số lượng database
 * calls
 */
@Service
public class AdminContentServiceImpl implements AdminContentService {

    private static final Logger logger = LoggerFactory.getLogger(AdminContentServiceImpl.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestSetRepository testSetRepository;

    @Autowired
    private IeltsTestRepository ieltsTestRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private HashtagRepository hashtagRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // Simple in-memory cache for overview stats (expires after 5 minutes)
    private Map<String, Object> cachedOverview = null;
    private long overviewCacheTime = 0;
    private static final long CACHE_DURATION_MS = 5 * 60 * 1000; // 5 minutes

    private AdminContentOperations contentOperations() {
        return new AdminContentOperations(
                jdbcTemplate,
                testSetRepository,
                ieltsTestRepository,
                sectionRepository,
                hashtagRepository,
                objectMapper,
                this::invalidateOverviewCache);
    }

    private void invalidateOverviewCache() {
        cachedOverview = null;
    }

    @Override
    public List<Map<String, Object>> getTopicsWithTests(String search, String status) {
        try {
            logger.info("Fetching hierarchical topics with tests. Search: {}, Status: {}", search, status);

            List<TestSet> testSets;
            if (search != null && !search.trim().isEmpty()) {
                testSets = testSetRepository.searchByCodeOrName(search);
            } else {
                testSets = testSetRepository.findAllByOrderByDisplayOrderAsc();
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (TestSet testSet : testSets) {
                Map<String, Object> setMap = new HashMap<>();
                setMap.put("id", testSet.getId());
                setMap.put("code", testSet.getCode());
                setMap.put("name", testSet.getName());
                setMap.put("description", testSet.getDescription());
                setMap.put("coverImageUrl", testSet.getCoverImageUrl());
                setMap.put("isPublished", testSet.getIsPublished());
                // Note: Hashtags are now at the IeltsTest level, not TestSet level
                setMap.put("hashtags", new ArrayList<>());

                List<Map<String, Object>> testsList = new ArrayList<>();
                for (IeltsTest test : testSet.getTests()) {
                    // Filter by status if provided
                    String testStatus = test.getIsPublished() ? "PUBLISHED" : "DRAFT";
                    if (status != null && !status.equals("ALL") && !status.equalsIgnoreCase(testStatus)) {
                        continue;
                    }

                    Map<String, Object> testMap = new HashMap<>();
                    testMap.put("id", test.getId());
                    testMap.put("testNumber", test.getTestNumber());
                    testMap.put("nameVi", test.getName());
                    testMap.put("nameEn", test.getName());
                    testMap.put("difficulty", test.getDifficulty());
                    testMap.put("isPublished", test.getIsPublished());
                    testMap.put("isAiGenerated", test.getIsAiGenerated());

                    // Add hashtags
                    List<Map<String, Object>> hashtags = new ArrayList<>();
                    for (Hashtag h : test.getHashtags()) {
                        Map<String, Object> hMap = new HashMap<>();
                        hMap.put("id", h.getId());
                        hMap.put("code", h.getCode());
                        hMap.put("nameVi", h.getName());
                        hMap.put("icon", h.getIcon());
                        hMap.put("color", h.getColor());
                        hashtags.add(hMap);
                    }
                    testMap.put("hashtags", hashtags);

                    // Add skill stats
                    Map<String, Integer> skillCounts = test.getSkillSectionCounts();
                    testMap.put("skills", skillCounts);

                    testsList.add(testMap);
                }

                setMap.put("tests", testsList);
                setMap.put("testsCount", testsList.size());

                if (!testsList.isEmpty() || (search == null || search.trim().isEmpty())) {
                    result.add(setMap);
                }
            }

            return result;

        } catch (Exception e) {
            logger.error("Error fetching topics with tests", e);
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Object> getContentOverview() {
        // Check cache first
        long now = System.currentTimeMillis();
        if (cachedOverview != null && (now - overviewCacheTime) < CACHE_DURATION_MS) {
            return cachedOverview;
        }

        Map<String, Object> overview = new HashMap<>();

        try {
            // OPTIMIZED: Single query for all counts from the new unified tables
            String sql = """
                    SELECT
                        (SELECT COUNT(*) FROM public.test_sets) as total_topics,
                        (SELECT COUNT(*) FROM public.tests) as total_tests,
                        (SELECT COUNT(*) FROM public.tests WHERE is_published = true) as published_tests,
                        (SELECT COUNT(*) FROM public.tests WHERE is_published = false) as draft_tests,
                        (SELECT COUNT(*) FROM public.questions) as total_questions,
                        (SELECT COUNT(*) FROM public.test_attempts) as total_attempts
                    """;

            Map<String, Object> counts = jdbcTemplate.queryForMap(sql);

            overview.put("totalTopics", counts.get("total_topics"));
            overview.put("totalTests", counts.get("total_tests"));
            overview.put("publishedTests", counts.get("published_tests"));
            overview.put("draftTests", counts.get("draft_tests"));
            overview.put("reviewTests", 0);
            overview.put("totalQuestions", counts.get("total_questions"));
            overview.put("totalAttempts", counts.get("total_attempts"));

            // Cache the result
            cachedOverview = overview;
            overviewCacheTime = now;

        } catch (Exception e) {
            logger.error("Error fetching content overview", e);
            overview.put("totalTopics", 0);
            overview.put("totalTests", 0);
            overview.put("publishedTests", 0);
            overview.put("draftTests", 0);
            overview.put("reviewTests", 0);
            overview.put("totalQuestions", 0);
            overview.put("totalAttempts", 0);
        }

        return overview;
    }

    @Override
    public Map<String, Object> getTestDetails(String examSource, Integer testNumber) {
        try {
            logger.info("Fetching details for test: source='{}', number={}", examSource, testNumber);

            // OPTIMIZED: Single query for test details
            String sql = """
                    WITH skill_stats AS (
                        SELECT
                            s.skill,
                            COUNT(DISTINCT s.id) as section_count,
                            COUNT(q.id) as question_count
                        FROM public.sections s
                        LEFT JOIN public.questions q ON q.section_id = s.id
                        WHERE s.exam_source = ? AND s.test_number = ?
                        GROUP BY s.skill
                    )
                    SELECT
                        skill,
                        section_count,
                        question_count
                    FROM skill_stats
                    """;

            List<Map<String, Object>> skillRows = jdbcTemplate.queryForList(sql, examSource, testNumber);
            logger.info("Found {} skill rows for {}/{}", skillRows.size(), examSource, testNumber);

            if (skillRows.isEmpty()) {
                // Secondary check: Does the test exist at all?
                Integer checkCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM public.sections WHERE exam_source = ? AND test_number = ?",
                        Integer.class, examSource, testNumber);
                logger.info("Double check count for {}/{}: {}", examSource, testNumber, checkCount);

                return null;
            }

            Map<String, Object> test = new HashMap<>();
            test.put("id", examSource + "-" + testNumber);
            test.put("examSource", examSource);
            test.put("testNumber", testNumber);
            test.put("name", "Test " + testNumber);
            Long resolvedTestId = contentOperations().resolveTestId(null, examSource, testNumber);
            if (resolvedTestId != null) {
                test.put("testId", resolvedTestId);
            }

            // Build skills map
            Map<String, Object> skills = new HashMap<>();
            String[] skillList = { "reading", "listening", "writing", "speaking" };

            for (String skill : skillList) {
                Map<String, Object> skillInfo = new HashMap<>();
                skillInfo.put("sectionsCount", 0);
                skillInfo.put("questionCount", 0);
                skillInfo.put("status", "empty");
                skills.put(skill, skillInfo);
            }

            int completeSkills = 0;
            for (Map<String, Object> row : skillRows) {
                String skill = (String) row.get("skill");
                int sectionCount = ((Number) row.get("section_count")).intValue();
                int questionCount = ((Number) row.get("question_count")).intValue();

                Map<String, Object> skillInfo = new HashMap<>();
                skillInfo.put("sectionsCount", sectionCount);
                skillInfo.put("questionCount", questionCount);

                String status = determineSkillStatus(skill, sectionCount, questionCount);
                skillInfo.put("status", status);

                if ("complete".equals(status)) {
                    completeSkills++;
                }

                skills.put(skill, skillInfo);
            }

            test.put("skills", skills);

            String status = completeSkills >= 2 ? "PUBLISHED" : "DRAFT";
            try {
                Boolean isPublished = jdbcTemplate.queryForObject(
                        """
                                SELECT t.is_published
                                FROM public.tests t
                                JOIN public.test_sets s ON t.set_id = s.id
                                WHERE s.code = ? AND t.test_number = ?
                                """,
                        Boolean.class,
                        examSource,
                        testNumber);
                if (isPublished != null) {
                    status = isPublished ? "PUBLISHED" : "DRAFT";
                }
            } catch (Exception e) {
                logger.debug("No published status found for {}/{}: {}", examSource, testNumber, e.getMessage());
            }

            test.put("status", status);

            // Get attempt count
            Long attempts = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM public.test_attempts WHERE exam_source = ? AND test_number = ?",
                    Long.class, examSource, String.valueOf(testNumber));
            test.put("totalAttempts", attempts != null ? attempts : 0);

            logger.info("Successfully built test details for {}/{}", examSource, testNumber);
            return test;

        } catch (Exception e) {
            logger.error("Error fetching test details for {} Test {}", examSource, testNumber, e);
            return null;
        }
    }

    @Override
    public List<Map<String, Object>> getSections(String examSource, Integer testNumber, String skill) {
        try {
            Long testId = contentOperations().resolveTestId(null, examSource, testNumber);
            if (testId != null) {
                jdbcTemplate.update(
                        """
                                UPDATE public.sections
                                SET test_id = ?
                                WHERE exam_source = ? AND test_number = ? AND test_id IS NULL
                                """,
                        testId,
                        examSource,
                        testNumber);
            }

            String sql = """
                    SELECT
                        s.id,
                        s.exam_source,
                        s.test_number,
                        s.skill,
                        s.part_number,
                        s.passage_text,
                        s.audio_url,
                        s.display_content_url,
                        s.image_description,
                        s.section_layout,
                        s.status,
                        COUNT(q.id) as question_count
                    FROM public.sections s
                    LEFT JOIN public.questions q ON q.section_id = s.id
                    WHERE s.exam_source = ? AND s.test_number = ? AND s.skill = ?
                    GROUP BY s.id, s.exam_source, s.test_number, s.skill, s.part_number,
                             s.passage_text, s.audio_url, s.display_content_url,
                             s.image_description, s.section_layout, s.status
                    ORDER BY s.part_number ASC
                    """;

            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Map<String, Object> section = new HashMap<>();
                section.put("id", rs.getLong("id"));
                section.put("examSource", rs.getString("exam_source"));
                section.put("testNumber", rs.getInt("test_number"));
                section.put("skill", rs.getString("skill"));
                section.put("partNumber", rs.getInt("part_number"));
                section.put("passageText", rs.getString("passage_text"));
                section.put("audioUrl", rs.getString("audio_url"));
                section.put("displayContentUrl", rs.getString("display_content_url"));
                section.put("imageDescription", rs.getString("image_description"));
                section.put("sectionLayout", contentOperations().parseSectionLayout(rs.getString("section_layout")));
                section.put("status", rs.getString("status"));
                section.put("questionCount", rs.getInt("question_count"));
                return section;
            }, examSource, testNumber, skill);

        } catch (Exception e) {
            logger.error("Error fetching sections for {} Test {} {}", examSource, testNumber, skill, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Map<String, Object>> getQuestionsBySection(Long sectionId) {
        try {
            String sql = """
                    SELECT
                        id,
                        section_id,
                        question_number,
                        question_uid,
                        question_type,
                        question_content,
                        correct_answer,
                        explanation,
                        image_url,
                        word_limit
                    FROM public.questions
                    WHERE section_id = ?
                    ORDER BY question_number ASC
                    """;

            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Map<String, Object> question = new HashMap<>();
                question.put("id", rs.getLong("id"));
                question.put("sectionId", rs.getLong("section_id"));
                question.put("questionNumber", rs.getInt("question_number"));
                question.put("questionUid", rs.getString("question_uid"));
                question.put("questionType", rs.getString("question_type"));
                question.put("questionContent", rs.getString("question_content"));
                question.put("correctAnswer", rs.getString("correct_answer"));
                question.put("explanation", rs.getString("explanation"));
                question.put("imageUrl", rs.getString("image_url"));
                question.put("wordLimit", rs.getObject("word_limit"));
                return question;
            }, sectionId);

        } catch (Exception e) {
            logger.error("Error fetching questions for section {}", sectionId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Map<String, Object>> getRecentActivities(int limit) {
        List<Map<String, Object>> activities = new ArrayList<>();

        try {
            // Get from user_activities and admin_audit_log combined
            String sql = """
                    (
                        SELECT
                            ua.id::text as id,
                            ua.activity_type as type,
                            ua.title as description,
                            p.username as admin_email,
                            ua.created_at
                        FROM public.user_activities ua
                        LEFT JOIN public.profiles p ON ua.user_id = p.id
                        WHERE ua.activity_type IN ('TEST_COMPLETED', 'VOCAB_SAVED')
                        ORDER BY ua.created_at DESC
                        LIMIT ?
                    )
                    UNION ALL
                    (
                        SELECT
                            aal.id::text as id,
                            aal.action as type,
                            aal.description,
                            aal.admin_email,
                            aal.created_at
                        FROM public.admin_audit_log aal
                        ORDER BY aal.created_at DESC
                        LIMIT ?
                    )
                    ORDER BY created_at DESC
                    LIMIT ?
                    """;

            activities = jdbcTemplate.queryForList(sql, limit, limit, limit);

        } catch (Exception e) {
            logger.error("Error fetching recent activities", e);
            // Fallback to test attempts
            try {
                String fallbackSql = """
                        SELECT
                            ta.id::text as id,
                            'TEST_COMPLETED' as type,
                            CONCAT(p.username, ' đã hoàn thành ', ta.exam_source, ' Test ', ta.test_number) as description,
                            p.username as admin_email,
                            ta.started_at as created_at
                        FROM public.test_attempts ta
                        LEFT JOIN public.profiles p ON ta.user_id = p.id
                        ORDER BY ta.started_at DESC
                        LIMIT ?
                        """;
                activities = jdbcTemplate.queryForList(fallbackSql, limit);
            } catch (Exception e2) {
                logger.error("Fallback also failed", e2);
            }
        }

        return activities;
    }

    // =====================
    // HELPER METHODS
    // =====================

    /**
     * Determine skill completion status
     */
    private String determineSkillStatus(String skill, int sectionCount, int questionCount) {
        if (questionCount == 0 && sectionCount == 0) {
            return "empty";
        }

        switch (skill) {
            case "reading":
                return questionCount >= 40 ? "complete" : "draft";
            case "listening":
                return questionCount >= 40 ? "complete" : "draft";
            case "writing":
                return questionCount >= 2 ? "complete" : "draft";
            case "speaking":
                return sectionCount >= 3 ? "complete" : "draft";
            default:
                return questionCount > 0 ? "draft" : "empty";
        }
    }

    // =====================
    // CRUD METHODS
    // =====================

    @Override
    public Map<String, Object> createSection(Map<String, Object> sectionData, String adminUserId) {
        return contentOperations().createSection(sectionData, adminUserId);
    }

    @Override
    public Map<String, Object> updateSection(Long sectionId, Map<String, Object> sectionData, String adminUserId) {
        return contentOperations().updateSection(sectionId, sectionData, adminUserId);
    }

    @Override
    public Map<String, Object> getSectionById(Long sectionId) {
        return contentOperations().getSectionById(sectionId);
    }

    @Override
    public Map<String, Object> createQuestion(Long sectionId, Map<String, Object> questionData, String adminUserId) {
        return contentOperations().createQuestion(sectionId, questionData, adminUserId);
    }

    @Override
    public Map<String, Object> updateQuestion(Long questionId, Map<String, Object> questionData, String adminUserId) {
        return contentOperations().updateQuestion(questionId, questionData, adminUserId);
    }

    @Override
    public void deleteQuestion(Long questionId, String adminUserId) {
        contentOperations().deleteQuestion(questionId, adminUserId);
    }

    @Override
    public Map<String, Object> getQuestionById(Long questionId) {
        return contentOperations().getQuestionById(questionId);
    }

    @Override
    public Map<String, Object> updateTestStatus(String examSource, Integer testNumber, String status,
            String adminUserId) {
        return contentOperations().updateTestStatus(examSource, testNumber, status, adminUserId);
    }

    @Override
    public void deleteTest(String testId, String adminUserId) {
        contentOperations().deleteTest(testId, adminUserId);
    }

    @Override
    public Map<String, Object> createTest(Map<String, Object> testData, String adminUserId) {
        return contentOperations().createTest(testData, adminUserId);
    }

    @Override
    @Transactional
    public Map<String, Object> updateTest(Long testId, Map<String, Object> testData, String adminUserId) {
        return contentOperations().updateTest(testId, testData, adminUserId);
    }

    @Override
    @Transactional
    public Map<String, Object> createTestSet(Map<String, Object> setData, String adminUserId) {
        return contentOperations().createTestSet(setData, adminUserId);
    }

    @Override
    @Transactional
    public Map<String, Object> updateTestSet(Long setId, Map<String, Object> setData, String adminUserId) {
        return contentOperations().updateTestSet(setId, setData, adminUserId);
    }

    @Override
    @Transactional
    public void deleteTestSet(Long setId, String adminUserId) {
        contentOperations().deleteTestSet(setId, adminUserId);
    }
}
