package com.cramer.service.implement;

import com.cramer.service.AdminContentService;
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
                setMap.put("isSystem", testSet.getIsSystem());

                // Add hashtags for Test Set
                List<Map<String, Object>> setHashtags = new ArrayList<>();
                for (Hashtag h : testSet.getHashtags()) {
                    Map<String, Object> hMap = new HashMap<>();
                    hMap.put("id", h.getId());
                    hMap.put("code", h.getCode());
                    hMap.put("nameVi", h.getName());
                    hMap.put("icon", h.getIcon());
                    hMap.put("color", h.getColor());
                    setHashtags.add(hMap);
                }
                setMap.put("hashtags", setHashtags);

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
            Long resolvedTestId = resolveTestId(null, examSource, testNumber);
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
            Long testId = resolveTestId(null, examSource, testNumber);
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
                section.put("sectionLayout", parseSectionLayout(rs.getString("section_layout")));
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

    private Integer parseTestNumber(Object value) {
        if (value == null)
            return null;
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long resolveTestId(Object testIdValue, String examSource, Integer testNumber) {
        if (testIdValue != null) {
            try {
                return Long.valueOf(testIdValue.toString());
            } catch (NumberFormatException e) {
                logger.warn("Invalid testId value: {}", testIdValue);
            }
        }

        if (examSource == null || testNumber == null) {
            return null;
        }

        try {
            return jdbcTemplate.queryForObject(
                    """
                            SELECT t.id
                            FROM public.tests t
                            JOIN public.test_sets s ON t.set_id = s.id
                            WHERE s.code = ? AND t.test_number = ?
                            """,
                    Long.class,
                    examSource,
                    testNumber);
        } catch (Exception e) {
            logger.warn("Failed to resolve test_id for {}/{}: {}", examSource, testNumber, e.getMessage());
            return null;
        }
    }

    private String serializeSectionLayout(Object sectionLayout) {
        if (sectionLayout == null) {
            return null;
        }
        if (sectionLayout instanceof String raw) {
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                objectMapper.readTree(trimmed);
                return trimmed;
            } catch (Exception e) {
                throw new IllegalArgumentException("sectionLayout must be valid JSON");
            }
        }
        try {
            return objectMapper.writeValueAsString(sectionLayout);
        } catch (Exception e) {
            throw new IllegalArgumentException("sectionLayout must be valid JSON");
        }
    }

    private String serializeJsonValue(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("JSON value must not be null");
        }
        if (value instanceof String raw) {
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("JSON value must not be empty");
            }
            try {
                objectMapper.readTree(trimmed);
                return trimmed;
            } catch (Exception e) {
                throw new IllegalArgumentException("JSON value must be valid JSON");
            }
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON value must be valid JSON");
        }
    }

    private Object parseSectionLayout(String rawLayout) {
        if (rawLayout == null || rawLayout.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(rawLayout);
            return node;
        } catch (Exception e) {
            logger.warn("Failed to parse section_layout JSON: {}", e.getMessage());
            return rawLayout;
        }
    }

    // =====================
    // CRUD METHODS
    // =====================

    @Override
    public Map<String, Object> createSection(Map<String, Object> sectionData, String adminUserId) {
        try {
            String sql = """
                    INSERT INTO public.sections (test_id, exam_source, test_number, skill, part_number, passage_text, audio_url, display_content_url, image_description, section_layout, status, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, 'DRAFT', NOW(), NOW())
                    RETURNING id
                    """;

            String examSource = Objects.toString(sectionData.get("examSource"), null);
            Integer testNumber = parseTestNumber(sectionData.get("testNumber"));
            Long testId = resolveTestId(sectionData.get("testId"), examSource, testNumber);
            String sectionLayoutJson = serializeSectionLayout(sectionData.get("sectionLayout"));

            Long sectionId = jdbcTemplate.queryForObject(sql, Long.class,
                    testId,
                    examSource,
                    testNumber,
                    sectionData.get("skill"),
                    sectionData.get("partNumber"),
                    sectionData.get("passageText"),
                    sectionData.get("audioUrl"),
                    sectionData.get("displayContentUrl"),
                    sectionData.get("imageDescription"),
                    sectionLayoutJson);

            // Invalidate cache
            cachedOverview = null;

            logger.info("Admin {} created section {}", adminUserId, sectionId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("sectionId", sectionId);
            result.put("message", "Đã tạo section mới");
            return result;

        } catch (Exception e) {
            logger.error("Error creating section", e);
            throw new RuntimeException("Không thể tạo section: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> updateSection(Long sectionId, Map<String, Object> sectionData, String adminUserId) {
        try {
            StringBuilder sql = new StringBuilder("UPDATE public.sections SET updated_at = NOW()");
            List<Object> params = new ArrayList<>();

            if (sectionData.containsKey("passageText")) {
                sql.append(", passage_text = ?");
                params.add(sectionData.get("passageText"));
            }
            if (sectionData.containsKey("audioUrl")) {
                sql.append(", audio_url = ?");
                params.add(sectionData.get("audioUrl"));
            }
            if (sectionData.containsKey("displayContentUrl")) {
                sql.append(", display_content_url = ?");
                params.add(sectionData.get("displayContentUrl"));
            }
            if (sectionData.containsKey("imageDescription")) {
                sql.append(", image_description = ?");
                params.add(sectionData.get("imageDescription"));
            }
            if (sectionData.containsKey("sectionLayout")) {
                sql.append(", section_layout = ?::jsonb");
                params.add(serializeSectionLayout(sectionData.get("sectionLayout")));
            }
            if (sectionData.containsKey("status")) {
                sql.append(", status = ?");
                params.add(sectionData.get("status"));
            }

            sql.append(" WHERE id = ?");
            params.add(sectionId);

            int updated = jdbcTemplate.update(Objects.requireNonNull(sql.toString()),
                    Objects.requireNonNull(params.toArray()));

            logger.info("Admin {} updated section {} - {} rows affected", adminUserId, sectionId, updated);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("updated", updated);
            result.put("message", "Đã cập nhật section");
            return result;

        } catch (Exception e) {
            logger.error("Error updating section {}", sectionId, e);
            throw new RuntimeException("Không thể cập nhật section: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getSectionById(Long sectionId) {
        try {
            String sql = """
                    SELECT
                        id, exam_source, test_number, skill, part_number,
                        passage_text, audio_url, display_content_url, image_description,
                        section_layout, status, created_at, updated_at
                    FROM public.sections
                    WHERE id = ?
                    """;

            return jdbcTemplate.queryForMap(sql, sectionId);

        } catch (Exception e) {
            logger.error("Error fetching section {}", sectionId, e);
            return null;
        }
    }

    @Override
    public Map<String, Object> createQuestion(Long sectionId, Map<String, Object> questionData, String adminUserId) {
        try {
            // Get next question number for this section
            Integer maxQuestionNumber = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(MAX(question_number), 0) FROM public.questions WHERE section_id = ?",
                    Integer.class, sectionId);
            int nextQuestionNumber = (maxQuestionNumber != null ? maxQuestionNumber : 0) + 1;

            // Generate question UID
            Map<String, Object> sectionInfo = getSectionById(sectionId);
            String questionUid = String.format("%s_%d_%s_%d_q%d",
                    sectionInfo.get("exam_source"),
                    sectionInfo.get("test_number"),
                    sectionInfo.get("skill"),
                    sectionInfo.get("part_number"),
                    nextQuestionNumber);

            String sql = """
                    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer, explanation, image_url, word_limit)
                    VALUES (?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?)
                    RETURNING id
                    """;

            Long questionId = jdbcTemplate.queryForObject(sql, Long.class,
                    sectionId,
                    nextQuestionNumber,
                    questionUid,
                    questionData.get("questionType"),
                    serializeJsonValue(questionData.get("questionContent")),
                    serializeJsonValue(questionData.get("correctAnswer")),
                    questionData.get("explanation"),
                    questionData.get("imageUrl"),
                    questionData.get("wordLimit"));

            // Invalidate cache
            cachedOverview = null;

            logger.info("Admin {} created question {} in section {}", adminUserId, questionId, sectionId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("questionId", questionId);
            result.put("questionNumber", nextQuestionNumber);
            result.put("questionUid", questionUid);
            result.put("message", "Đã tạo câu hỏi mới");
            return result;

        } catch (Exception e) {
            logger.error("Error creating question in section {}", sectionId, e);
            throw new RuntimeException("Không thể tạo câu hỏi: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> updateQuestion(Long questionId, Map<String, Object> questionData, String adminUserId) {
        try {
            StringBuilder sql = new StringBuilder("UPDATE public.questions SET id = id"); // id = id is a no-op
            List<Object> params = new ArrayList<>();

            if (questionData.containsKey("questionType")) {
                sql.append(", question_type = ?");
                params.add(questionData.get("questionType"));
            }
            if (questionData.containsKey("questionContent")) {
                sql.append(", question_content = ?::jsonb");
                params.add(serializeJsonValue(questionData.get("questionContent")));
            }
            if (questionData.containsKey("correctAnswer")) {
                sql.append(", correct_answer = ?::jsonb");
                params.add(serializeJsonValue(questionData.get("correctAnswer")));
            }
            if (questionData.containsKey("explanation")) {
                sql.append(", explanation = ?");
                params.add(questionData.get("explanation"));
            }
            if (questionData.containsKey("imageUrl")) {
                sql.append(", image_url = ?");
                params.add(questionData.get("imageUrl"));
            }
            if (questionData.containsKey("wordLimit")) {
                sql.append(", word_limit = ?");
                params.add(questionData.get("wordLimit"));
            }

            sql.append(" WHERE id = ?");
            params.add(questionId);

            int updated = jdbcTemplate.update(Objects.requireNonNull(sql.toString()),
                    Objects.requireNonNull(params.toArray()));

            logger.info("Admin {} updated question {} - {} rows affected", adminUserId, questionId, updated);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("updated", updated);
            result.put("message", "Đã cập nhật câu hỏi");
            return result;

        } catch (Exception e) {
            logger.error("Error updating question {}", questionId, e);
            throw new RuntimeException("Không thể cập nhật câu hỏi: " + e.getMessage());
        }
    }

    @Override
    public void deleteQuestion(Long questionId, String adminUserId) {
        try {
            int deleted = jdbcTemplate.update("DELETE FROM public.questions WHERE id = ?", questionId);

            // Invalidate cache
            cachedOverview = null;

            logger.info("Admin {} deleted question {} - {} rows affected", adminUserId, questionId, deleted);

            if (deleted == 0) {
                throw new RuntimeException("Không tìm thấy câu hỏi với ID: " + questionId);
            }

        } catch (Exception e) {
            logger.error("Error deleting question {}", questionId, e);
            throw new RuntimeException("Không thể xóa câu hỏi: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getQuestionById(Long questionId) {
        try {
            String sql = """
                    SELECT
                        id, section_id, question_number, question_uid, question_type,
                        question_content, correct_answer, explanation, image_url, word_limit
                    FROM public.questions
                    WHERE id = ?
                    """;

            return jdbcTemplate.queryForMap(sql, questionId);

        } catch (Exception e) {
            logger.error("Error fetching question {}", questionId, e);
            return null;
        }
    }

    @Override
    public Map<String, Object> updateTestStatus(String examSource, Integer testNumber, String status,
            String adminUserId) {
        try {
            int updated = jdbcTemplate.update(
                    "UPDATE public.sections SET status = ?, updated_at = NOW() WHERE exam_source = ? AND test_number = ?",
                    status, examSource, testNumber);

            boolean publish = "PUBLISHED".equalsIgnoreCase(status);
            jdbcTemplate.update(
                    """
                            UPDATE public.tests
                            SET is_published = ?
                            WHERE test_number = ?
                              AND set_id = (SELECT id FROM public.test_sets WHERE code = ?)
                            """,
                    publish,
                    testNumber,
                    examSource);

            logger.info("Admin {} updated status to {} for {} Test {} - {} sections affected",
                    adminUserId, status, examSource, testNumber, updated);

            // Invalidate cache
            cachedOverview = null;

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("updated", updated);
            result.put("message", "Đã cập nhật trạng thái đề thi");
            return result;

        } catch (Exception e) {
            logger.error("Error updating test status for {} Test {}", examSource, testNumber, e);
            throw new RuntimeException("Không thể cập nhật trạng thái: " + e.getMessage());
        }
    }

    @Override
    public void deleteTest(String testId, String adminUserId) {
        try {
            // Parse composite ID "examSource-testNumber"
            // Example: "cam17-1" -> examSource="cam17", testNumber=1
            int lastHyphenIndex = testId.lastIndexOf('-');
            if (lastHyphenIndex == -1) {
                throw new IllegalArgumentException("ID đề thi không hợp lệ: " + testId);
            }

            String examSource = testId.substring(0, lastHyphenIndex);
            String testNumberStr = testId.substring(lastHyphenIndex + 1);
            int testNumber;

            try {
                testNumber = Integer.parseInt(testNumberStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Số đề thi không hợp lệ: " + testNumberStr);
            }

            // 1. Delete dependent user answers
            String deleteUserAnswersSql = """
                        DELETE FROM public.user_answers
                        WHERE attempt_id IN (
                            SELECT id FROM public.test_attempts
                            WHERE exam_source = ? AND test_number = ?
                        )
                    """;
            jdbcTemplate.update(deleteUserAnswersSql, examSource, String.valueOf(testNumber));

            // 2. Delete writing submissions
            String deleteSubmissionsSql = """
                        DELETE FROM public.writing_submissions
                        WHERE attempt_id IN (
                            SELECT id FROM public.test_attempts
                            WHERE exam_source = ? AND test_number = ?
                        )
                    """;
            jdbcTemplate.update(deleteSubmissionsSql, examSource, String.valueOf(testNumber));

            // 3. Delete vocabulary (linked to attempts)
            String deleteVocabAttemptsSql = """
                        DELETE FROM public.vocabulary
                        WHERE source_test_id IN (
                            SELECT id FROM public.test_attempts
                            WHERE exam_source = ? AND test_number = ?
                        )
                    """;
            jdbcTemplate.update(deleteVocabAttemptsSql, examSource, String.valueOf(testNumber));

            // 4. Delete vocabulary (linked to sections)
            String deleteVocabSectionsSql = """
                        DELETE FROM public.vocabulary
                        WHERE source_section_id IN (
                            SELECT id FROM public.sections
                            WHERE exam_source = ? AND test_number = ?
                        )
                    """;
            jdbcTemplate.update(deleteVocabSectionsSql, examSource, testNumber);

            // 5. Delete test attempts
            jdbcTemplate.update("DELETE FROM public.test_attempts WHERE exam_source = ? AND test_number = ?",
                    examSource, String.valueOf(testNumber));

            // 6. Delete all questions in sections of this test
            String deleteQuestionsSql = """
                        DELETE FROM public.questions
                        WHERE section_id IN (
                            SELECT id FROM public.sections
                            WHERE exam_source = ? AND test_number = ?
                        )
                    """;
            jdbcTemplate.update(deleteQuestionsSql, examSource, testNumber);

            // 7. Delete all sections of this test
            String deleteSectionsSql = "DELETE FROM public.sections WHERE exam_source = ? AND test_number = ?";
            int deletedSections = jdbcTemplate.update(deleteSectionsSql, examSource, testNumber);

            // 8. Delete from tests table if exists
            try {
                jdbcTemplate.update("""
                            DELETE FROM public.test_hashtags
                            WHERE test_id IN (
                                SELECT id FROM public.tests
                                WHERE test_number = ? AND set_id IN (
                                    SELECT id FROM public.test_sets WHERE code = ?
                                )
                            )
                        """, testNumber, examSource);

                jdbcTemplate.update("""
                            DELETE FROM public.tests
                            WHERE test_number = ? AND set_id IN (
                                SELECT id FROM public.test_sets WHERE code = ?
                            )
                        """, testNumber, examSource);
            } catch (Exception e) {
                logger.warn("Could not delete from tests table for {}/{}: {}", examSource, testNumber, e.getMessage());
            }

            logger.info("Admin {} deleted test {} ({} sections)", adminUserId, testId, deletedSections);
            cachedOverview = null;

        } catch (Exception e) {
            logger.error("Error deleting test {}", testId, e);
            throw new RuntimeException("Không thể xóa đề thi: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> createTest(Map<String, Object> testData, String adminUserId) {
        Long setId = ((Number) testData.get("setId")).longValue();
        Object testNumberObj = testData.get("testNumber");
        Integer testNumber = testNumberObj instanceof String ? Integer.parseInt((String) testNumberObj)
                : ((Number) testNumberObj).intValue();

        try {
            logger.info("Attempting to create test in set {} with number {}", setId, testNumber);

            TestSet testSet = testSetRepository.findById(setId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ đề ID: " + setId));

            // Check if test already exists in this set
            Optional<IeltsTest> existingTest = ieltsTestRepository.findByTestSetIdAndTestNumber(setId, testNumber);

            if (existingTest.isPresent()) {
                logger.info("Test already exists: Set {} / Number {}", setId, testNumber);
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("testId", existingTest.get().getId());
                result.put("message", "Test already exists");
                return result;
            }

            // Create new IeltsTest entry
            IeltsTest test = IeltsTest.builder()
                    .testSet(testSet)
                    .testNumber(testNumber)
                    .name("Bài thi " + testNumber)
                    .name("Test " + testNumber)
                    .isPublished(false)
                    .isAiGenerated(false)
                    .createdBy(UUID.fromString(adminUserId))
                    .build();

            test = Objects.requireNonNull(ieltsTestRepository.save(test), "Failed to save test");

            // Handle hashtags if present
            if (testData.containsKey("hashtagIds")) {
                List<?> rawIds = (List<?>) testData.get("hashtagIds");
                if (rawIds != null && !rawIds.isEmpty()) {
                    List<Long> hashtagIds = rawIds.stream()
                            .map(id -> Long.valueOf(id.toString()))
                            .toList();
                    List<Hashtag> hashtags = hashtagRepository
                            .findAllById(Objects.requireNonNull(hashtagIds, "hashtagIds must not be null"));
                    test.setHashtags(new HashSet<>(hashtags));
                    hashtags.forEach(h -> {
                        h.incrementUseCount();
                        hashtagRepository.save(h);
                    });

                    // Save again to persist relationship
                    test = ieltsTestRepository.save(test);
                }
            }

            logger.info("Created new IeltsTest with ID: {}", test.getId());

            // Create a dummy Reading Part 1 section to initialize the test data
            Section placeholder = Section.builder()
                    .ieltsTest(test)
                    .examSource(testSet.getCode())
                    .testNumber(testNumber)
                    .skill("reading")
                    .partNumber(1)
                    .passageText("Placeholder passage for initialization")
                    .status("DRAFT")
                    .build();

            Objects.requireNonNull(sectionRepository.save(placeholder), "Failed to save placeholder section");
            logger.info("Created placeholder section for first skill");

            // Invalidate cache
            cachedOverview = null;

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("testId", test.getId());
            result.put("setId", setId);
            result.put("testNumber", testNumber);
            result.put("message", "Đã tạo test mới thành công");
            return result;

        } catch (Exception e) {
            logger.error("Error creating test in set {}/{}", setId, testNumber, e);
            throw new RuntimeException("Không thể tạo test: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Map<String, Object> updateTest(Long testId, Map<String, Object> testData, String adminUserId) {
        logger.info("Updating test {}: {}", testId, testData);
        try {
            IeltsTest test = Objects.requireNonNull(
                    ieltsTestRepository.findById(Objects.requireNonNull(testId, "testId must not be null"))
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy đề thi ID: " + testId)),
                    "Test must not be null");

            if (testData.containsKey("nameVi"))
                test.setName((String) testData.get("nameVi"));
            if (testData.containsKey("nameEn"))
                test.setName((String) testData.get("nameEn"));
            if (testData.containsKey("description"))
                test.setDescription((String) testData.get("description"));
            // Add other fields as needed

            if (testData.containsKey("hashtagIds")) {
                Set<Hashtag> oldHashtags = test.getHashtags();
                if (oldHashtags == null)
                    oldHashtags = new HashSet<>();

                List<?> rawIds = (List<?>) testData.get("hashtagIds");
                List<Long> newIds = rawIds == null ? new ArrayList<>()
                        : rawIds.stream().map(id -> Long.valueOf(id.toString())).toList();
                List<Hashtag> newHashtagsList = hashtagRepository
                        .findAllById(Objects.requireNonNull(newIds, "newIds must not be null"));
                Set<Hashtag> newHashtags = new HashSet<>(newHashtagsList);

                // Calculate removed
                Set<Hashtag> finalOldHashtags = oldHashtags;
                List<Hashtag> removed = oldHashtags.stream()
                        .filter(h -> !newHashtags.contains(h))
                        .toList();

                // Calculate added
                List<Hashtag> added = newHashtags.stream()
                        .filter(h -> !finalOldHashtags.contains(h))
                        .toList();

                // Update counts
                removed.forEach(h -> {
                    h.decrementUseCount();
                    hashtagRepository.save(h);
                });
                added.forEach(h -> {
                    h.incrementUseCount();
                    hashtagRepository.save(h);
                });

                test.setHashtags(newHashtags);
            }

            test = Objects.requireNonNull(ieltsTestRepository.save(test), "Failed to save updated test");
            cachedOverview = null; // Invalidate cache

            return Map.of("success", true, "message", "Cập nhật đề thi thành công");
        } catch (Exception e) {
            logger.error("Error updating test {}", testId, e);
            throw new RuntimeException("Lỗi khi cập nhật đề thi: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Map<String, Object> createTestSet(Map<String, Object> setData, String adminUserId) {
        logger.info("Creating new test set: {}", setData);
        try {
            TestSet testSet = TestSet.builder()
                    .code((String) setData.get("code"))
                    .name((String) setData.get("name"))
                    .description((String) setData.get("description"))
                    .sourceType((String) setData.getOrDefault("sourceType", "custom"))
                    .displayOrder((Integer) setData.getOrDefault("displayOrder", 0))
                    .isPublished(false)
                    .isSystem(false)
                    .createdBy(UUID.fromString(adminUserId))
                    .build();

            if (setData.containsKey("hashtagIds")) {
                List<?> rawIds = (List<?>) setData.get("hashtagIds");
                if (rawIds != null && !rawIds.isEmpty()) {
                    List<Long> hashtagIds = rawIds.stream()
                            .map(id -> Long.valueOf(id.toString()))
                            .toList();
                    List<Hashtag> hashtags = hashtagRepository
                            .findAllById(Objects.requireNonNull(hashtagIds, "hashtagIds must not be null"));
                    testSet.setHashtags(hashtags);
                    hashtags.forEach(h -> {
                        h.incrementUseCount();
                        hashtagRepository.save(h);
                    });
                }
            }

            testSet = Objects.requireNonNull(testSetRepository.save(testSet), "Failed to save test set");
            cachedOverview = null;
            return Map.of("success", true, "id", testSet.getId());
        } catch (Exception e) {
            logger.error("Error creating test set", e);
            throw new RuntimeException("Lỗi khi tạo bộ đề: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Map<String, Object> updateTestSet(Long setId, Map<String, Object> setData, String adminUserId) {
        logger.info("Updating test set {}: {}", setId, setData);
        try {
            TestSet testSet = testSetRepository.findById(setId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ đề ID: " + setId));

            if (setData.containsKey("name"))
                testSet.setName((String) setData.get("name"));
            if (setData.containsKey("description"))
                testSet.setDescription((String) setData.get("description"));
            if (setData.containsKey("sourceType"))
                testSet.setSourceType((String) setData.get("sourceType"));
            if (setData.containsKey("displayOrder"))
                testSet.setDisplayOrder((Integer) setData.get("displayOrder"));

            if (setData.containsKey("hashtagIds")) {
                List<Hashtag> oldHashtags = new ArrayList<>(testSet.getHashtags());

                List<?> rawIds = (List<?>) setData.get("hashtagIds");
                List<Long> newIds = rawIds == null ? new ArrayList<>()
                        : rawIds.stream().map(id -> Long.valueOf(id.toString())).toList();
                List<Hashtag> newHashtags = hashtagRepository
                        .findAllById(Objects.requireNonNull(newIds, "newIds must not be null"));

                // Calculate removed
                List<Hashtag> removed = oldHashtags.stream()
                        .filter(h -> !newHashtags.contains(h))
                        .toList();

                // Calculate added
                List<Hashtag> added = newHashtags.stream()
                        .filter(h -> !oldHashtags.contains(h))
                        .toList();

                // Update counts
                removed.forEach(h -> {
                    h.decrementUseCount();
                    hashtagRepository.save(h);
                });
                added.forEach(h -> {
                    h.incrementUseCount();
                    hashtagRepository.save(h);
                });

                testSet.setHashtags(newHashtags);
            }

            Objects.requireNonNull(testSetRepository.save(testSet), "Failed to save updated testset");
            cachedOverview = null;
            return Map.of("success", true);
        } catch (Exception e) {
            logger.error("Error updating test set", e);
            throw new RuntimeException("Lỗi khi cập nhật bộ đề: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteTestSet(Long setId, String adminUserId) {
        logger.info("Admin {} is deleting test set {}", adminUserId, setId);
        try {
            testSetRepository.deleteById(Objects.requireNonNull(setId, "setId must not be null"));
            cachedOverview = null;
            logger.info("Successfully deleted test set {}", setId);
        } catch (Exception e) {
            logger.error("Error deleting test set", e);
            throw new RuntimeException("Lỗi khi xóa bộ đề: " + e.getMessage());
        }
    }
}
