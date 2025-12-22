package com.cramer.service.implement;

import com.cramer.service.AdminContentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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

    // Simple in-memory cache for overview stats (expires after 5 minutes)
    private Map<String, Object> cachedOverview = null;
    private long overviewCacheTime = 0;
    private static final long CACHE_DURATION_MS = 5 * 60 * 1000; // 5 minutes

    @Override
    public List<Map<String, Object>> getTopicsWithTests(String search, String status) {
        try {
            // OPTIMIZED: Single query to get all test info with aggregated data
            String sql = """
                    WITH test_stats AS (
                        SELECT
                            s.exam_source,
                            s.test_number,
                            s.skill,
                            COUNT(DISTINCT s.id) as section_count,
                            COUNT(q.id) as question_count
                        FROM public.sections s
                        LEFT JOIN public.questions q ON q.section_id = s.id
                        GROUP BY s.exam_source, s.test_number, s.skill
                    ),
                    test_attempts_count AS (
                        SELECT
                            exam_source,
                            test_number,
                            COUNT(*) as attempt_count
                        FROM public.test_attempts
                        GROUP BY exam_source, test_number
                    ),
                    aggregated_tests AS (
                        SELECT
                            ts.exam_source,
                            ts.test_number,
                            COALESCE(ta.attempt_count, 0) as total_attempts,
                            MAX(CASE WHEN ts.skill = 'reading' THEN ts.question_count ELSE 0 END) as reading_questions,
                            MAX(CASE WHEN ts.skill = 'listening' THEN ts.question_count ELSE 0 END) as listening_questions,
                            MAX(CASE WHEN ts.skill = 'writing' THEN ts.question_count ELSE 0 END) as writing_questions,
                            MAX(CASE WHEN ts.skill = 'speaking' THEN ts.section_count ELSE 0 END) as speaking_sections
                        FROM test_stats ts
                        LEFT JOIN test_attempts_count ta
                            ON ts.exam_source = ta.exam_source
                            AND ts.test_number::text = ta.test_number
                        GROUP BY ts.exam_source, ts.test_number, ta.attempt_count
                    )
                    SELECT
                        exam_source,
                        test_number,
                        total_attempts,
                        reading_questions,
                        listening_questions,
                        writing_questions,
                        speaking_sections,
                        CASE
                            WHEN (reading_questions >= 40 OR listening_questions >= 40)
                                 AND (reading_questions > 0 OR listening_questions > 0)
                            THEN 'PUBLISHED'
                            WHEN reading_questions > 0 OR listening_questions > 0
                                 OR writing_questions > 0 OR speaking_sections > 0
                            THEN 'DRAFT'
                            ELSE 'DRAFT'
                        END as status
                    FROM aggregated_tests
                    ORDER BY exam_source, test_number
                    """;

            List<Map<String, Object>> rawTests = jdbcTemplate.queryForList(sql);

            // Group by exam_source (topic)
            Map<String, List<Map<String, Object>>> groupedBySource = new LinkedHashMap<>();

            for (Map<String, Object> row : rawTests) {
                String examSource = (String) row.get("exam_source");

                // Apply search filter
                if (search != null && !search.trim().isEmpty()) {
                    String displayName = formatDisplayName(examSource);
                    if (!displayName.toLowerCase().contains(search.toLowerCase()) &&
                            !examSource.toLowerCase().contains(search.toLowerCase())) {
                        continue;
                    }
                }

                // Apply status filter
                String testStatus = (String) row.get("status");
                if (status != null && !status.equals("ALL") && !status.equals(testStatus)) {
                    continue;
                }

                groupedBySource.computeIfAbsent(examSource, k -> new ArrayList<>()).add(row);
            }

            // Build topics list
            List<Map<String, Object>> topics = new ArrayList<>();
            int topicId = 1;

            for (Map.Entry<String, List<Map<String, Object>>> entry : groupedBySource.entrySet()) {
                String examSource = entry.getKey();
                List<Map<String, Object>> testRows = entry.getValue();

                Map<String, Object> topic = new HashMap<>();
                topic.put("id", topicId++);
                topic.put("source", examSource);
                topic.put("displayName", formatDisplayName(examSource));

                List<Map<String, Object>> tests = new ArrayList<>();
                int publishedCount = 0;

                for (Map<String, Object> row : testRows) {
                    Map<String, Object> test = buildTestFromRow(row);
                    tests.add(test);

                    if ("PUBLISHED".equals(test.get("status"))) {
                        publishedCount++;
                    }
                }

                topic.put("tests", tests);
                topic.put("testsCount", tests.size());
                topic.put("publishedTests", publishedCount);

                topics.add(topic);
            }

            return topics;

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
            // OPTIMIZED: Single query for all counts
            String sql = """
                    SELECT
                        (SELECT COUNT(DISTINCT exam_source) FROM public.sections) as total_topics,
                        (SELECT COUNT(*) FROM (SELECT DISTINCT exam_source, test_number FROM public.sections) sub) as total_tests,
                        (SELECT COUNT(*) FROM public.questions) as total_questions,
                        (SELECT COUNT(*) FROM public.test_attempts) as total_attempts
                    """;

            Map<String, Object> counts = jdbcTemplate.queryForMap(sql);

            overview.put("totalTopics", counts.get("total_topics"));
            overview.put("totalTests", counts.get("total_tests"));
            overview.put("publishedTests", counts.get("total_tests")); // All with data are published
            overview.put("draftTests", 0);
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
            test.put("status", completeSkills >= 2 ? "PUBLISHED" : "DRAFT");

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
                section.put("sectionLayout", rs.getString("section_layout"));
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
     * Build test info from aggregated row data
     */
    private Map<String, Object> buildTestFromRow(Map<String, Object> row) {
        Map<String, Object> test = new HashMap<>();

        String examSource = (String) row.get("exam_source");
        Integer testNumber = ((Number) row.get("test_number")).intValue();

        test.put("id", examSource + "-" + testNumber);
        test.put("examSource", examSource);
        test.put("testNumber", testNumber);
        test.put("name", "Test " + testNumber);
        test.put("status", row.get("status"));
        test.put("totalAttempts", row.get("total_attempts"));

        // Build skills
        Map<String, Object> skills = new HashMap<>();

        int readingQ = ((Number) row.getOrDefault("reading_questions", 0)).intValue();
        int listeningQ = ((Number) row.getOrDefault("listening_questions", 0)).intValue();
        int writingQ = ((Number) row.getOrDefault("writing_questions", 0)).intValue();
        int speakingS = ((Number) row.getOrDefault("speaking_sections", 0)).intValue();

        skills.put("reading", createSkillInfo(readingQ, determineSkillStatus("reading", 0, readingQ)));
        skills.put("listening", createSkillInfo(listeningQ, determineSkillStatus("listening", 0, listeningQ)));
        skills.put("writing", createSkillInfo(writingQ, determineSkillStatus("writing", 0, writingQ)));
        skills.put("speaking", createSkillInfo(speakingS, determineSkillStatus("speaking", speakingS, 0)));

        test.put("skills", skills);

        return test;
    }

    private Map<String, Object> createSkillInfo(int count, String status) {
        Map<String, Object> info = new HashMap<>();
        info.put("questionCount", count);
        info.put("status", status);
        return info;
    }

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

    /**
     * Format exam_source to display name
     */
    private String formatDisplayName(String examSource) {
        if (examSource == null)
            return "Unknown";

        if (examSource.toLowerCase().startsWith("cam")) {
            String number = examSource.substring(3);
            return "Cambridge IELTS " + number;
        }

        if (examSource.toLowerCase().startsWith("real")) {
            return "Real Tests";
        }

        return examSource.substring(0, 1).toUpperCase() + examSource.substring(1);
    }

    // =====================
    // CRUD METHODS
    // =====================

    @Override
    public Map<String, Object> createSection(Map<String, Object> sectionData, String adminUserId) {
        try {
            String sql = """
                    INSERT INTO public.sections (exam_source, test_number, skill, part_number, passage_text, audio_url, display_content_url, image_description, section_layout, status, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, 'DRAFT', NOW(), NOW())
                    RETURNING id
                    """;

            Long sectionId = jdbcTemplate.queryForObject(sql, Long.class,
                    sectionData.get("examSource"),
                    sectionData.get("testNumber"),
                    sectionData.get("skill"),
                    sectionData.get("partNumber"),
                    sectionData.get("passageText"),
                    sectionData.get("audioUrl"),
                    sectionData.get("displayContentUrl"),
                    sectionData.get("imageDescription"),
                    sectionData.get("sectionLayout") != null ? sectionData.get("sectionLayout").toString() : null);

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
                params.add(
                        sectionData.get("sectionLayout") != null ? sectionData.get("sectionLayout").toString() : null);
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
                    questionData.get("questionContent") != null ? questionData.get("questionContent").toString() : null,
                    questionData.get("correctAnswer") != null ? questionData.get("correctAnswer").toString() : null,
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
                params.add(questionData.get("questionContent") != null ? questionData.get("questionContent").toString()
                        : null);
            }
            if (questionData.containsKey("correctAnswer")) {
                sql.append(", correct_answer = ?::jsonb");
                params.add(questionData.get("correctAnswer") != null ? questionData.get("correctAnswer").toString()
                        : null);
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
            // Validate status
            if (!List.of("DRAFT", "PUBLISHED", "ARCHIVED").contains(status)) {
                throw new RuntimeException("Trạng thái không hợp lệ: " + status);
            }

            // Update all sections of this test
            int updated = jdbcTemplate.update(
                    "UPDATE public.sections SET status = ?, updated_at = NOW() WHERE exam_source = ? AND test_number = ?",
                    status, examSource, testNumber);

            logger.info("Admin {} updated status of {} Test {} to {} - {} sections affected",
                    adminUserId, examSource, testNumber, status, updated);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("sectionsUpdated", updated);
            result.put("status", status);
            result.put("message", String.format("Đã cập nhật trạng thái thành %s cho %d sections", status, updated));
            return result;

        } catch (Exception e) {
            logger.error("Error updating test status for {} Test {}", examSource, testNumber, e);
            throw new RuntimeException("Không thể cập nhật trạng thái: " + e.getMessage());
        }
    }

    @Override

    public Map<String, Object> createTest(Map<String, Object> testData, String adminUserId) {
        String examSource = (String) testData.get("examSource");
        Object testNumberObj = testData.get("testNumber");
        Integer testNumber = testNumberObj instanceof String ? Integer.parseInt((String) testNumberObj)
                : (Integer) testNumberObj;

        try {
            logger.info("Attempting to create test: {} / {}", examSource, testNumber);

            // Check if test already exists
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM public.sections WHERE exam_source = ? AND test_number = ?",
                    Integer.class, examSource, testNumber);

            if (count != null && count > 0) {
                logger.info("Test already exists: {} / {}", examSource, testNumber);
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("examSource", examSource);
                result.put("testNumber", testNumber);
                result.put("message", "Test already exists");
                return result;
            }

            // Create a dummy Reading Part 1 section
            String sql = """
                    INSERT INTO public.sections (exam_source, test_number, skill, part_number, passage_text, status, created_at, updated_at)
                    VALUES (?, ?, 'reading', 1, 'Placeholder passage for initialization', 'DRAFT', NOW(), NOW())
                    RETURNING id
                    """;

            Long sectionId = jdbcTemplate.queryForObject(sql, Long.class, examSource, testNumber);
            logger.info("Created placeholder section ID: {}", sectionId);

            // Verify insertion immediately
            Integer verifyCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM public.sections WHERE exam_source = ? AND test_number = ?",
                    Integer.class, examSource, testNumber);
            logger.info("Verification count after insert: {}", verifyCount);

            if (verifyCount == null || verifyCount == 0) {
                throw new RuntimeException("Insert appeared successful but verification failed (count=0)");
            }

            // Invalidate cache
            cachedOverview = null;

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("sectionId", sectionId);
            result.put("examSource", examSource);
            result.put("testNumber", testNumber);
            result.put("message", "Đã tạo test mới thành công");
            return result;

        } catch (Exception e) {
            logger.error("Error creating test {}/{}", examSource, testNumber, e);
            throw new RuntimeException("Không thể tạo test: " + e.getMessage());
        }
    }
}
