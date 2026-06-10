package com.cramer.service.implement;

import com.cramer.entity.Hashtag;
import com.cramer.entity.IeltsTest;
import com.cramer.entity.Section;
import com.cramer.entity.TestSet;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.repository.HashtagRepository;
import com.cramer.repository.IeltsTestRepository;
import com.cramer.repository.SectionRepository;
import com.cramer.repository.TestSetRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

final class AdminContentOperations {

    private static final Logger logger = LoggerFactory.getLogger(AdminContentOperations.class);

    private final JdbcTemplate jdbcTemplate;
    private final TestSetRepository testSetRepository;
    private final IeltsTestRepository ieltsTestRepository;
    private final SectionRepository sectionRepository;
    private final HashtagRepository hashtagRepository;
    private final ObjectMapper objectMapper;
    private final Runnable invalidateCache;

    AdminContentOperations(
            JdbcTemplate jdbcTemplate,
            TestSetRepository testSetRepository,
            IeltsTestRepository ieltsTestRepository,
            SectionRepository sectionRepository,
            HashtagRepository hashtagRepository,
            ObjectMapper objectMapper,
            Runnable invalidateCache) {
        this.jdbcTemplate = jdbcTemplate;
        this.testSetRepository = testSetRepository;
        this.ieltsTestRepository = ieltsTestRepository;
        this.sectionRepository = sectionRepository;
        this.hashtagRepository = hashtagRepository;
        this.objectMapper = objectMapper;
        this.invalidateCache = invalidateCache;
    }

    Integer parseTestNumber(Object value) {
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

    Long resolveTestId(Object testIdValue, String examSource, Integer testNumber) {
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

    String serializeSectionLayout(Object sectionLayout) {
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

    String serializeJsonValue(Object value) {
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

    Object parseSectionLayout(String rawLayout) {
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

    Map<String, Object> createSection(Map<String, Object> sectionData, String adminUserId) {
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

            invalidateCache.run();

            logger.info("Admin {} created section {}", adminUserId, sectionId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("sectionId", sectionId);
            result.put("message", "Đã tạo section mới");
            return result;

        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error creating section", e);
            throw new RuntimeException("Không thể tạo section: " + e.getMessage(), e);
        }
    }

    Map<String, Object> updateSection(Long sectionId, Map<String, Object> sectionData, String adminUserId) {
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

        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error updating section {}", sectionId, e);
            throw new RuntimeException("Không thể cập nhật section: " + e.getMessage(), e);
        }
    }

    Map<String, Object> getSectionById(Long sectionId) {
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

    Map<String, Object> createQuestion(Long sectionId, Map<String, Object> questionData, String adminUserId) {
        try {
            Integer maxQuestionNumber = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(MAX(question_number), 0) FROM public.questions WHERE section_id = ?",
                    Integer.class, sectionId);
            int nextQuestionNumber = (maxQuestionNumber != null ? maxQuestionNumber : 0) + 1;

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

            invalidateCache.run();

            logger.info("Admin {} created question {} in section {}", adminUserId, questionId, sectionId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("questionId", questionId);
            result.put("questionNumber", nextQuestionNumber);
            result.put("questionUid", questionUid);
            result.put("message", "Đã tạo câu hỏi mới");
            return result;

        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error creating question in section {}", sectionId, e);
            throw new RuntimeException("Không thể tạo câu hỏi: " + e.getMessage(), e);
        }
    }

    Map<String, Object> updateQuestion(Long questionId, Map<String, Object> questionData, String adminUserId) {
        try {
            List<String> setClauses = new ArrayList<>();
            List<Object> params = new ArrayList<>();

            if (questionData.containsKey("questionType")) {
                setClauses.add("question_type = ?");
                params.add(questionData.get("questionType"));
            }
            if (questionData.containsKey("questionContent")) {
                setClauses.add("question_content = ?::jsonb");
                params.add(serializeJsonValue(questionData.get("questionContent")));
            }
            if (questionData.containsKey("correctAnswer")) {
                setClauses.add("correct_answer = ?::jsonb");
                params.add(serializeJsonValue(questionData.get("correctAnswer")));
            }
            if (questionData.containsKey("explanation")) {
                setClauses.add("explanation = ?::jsonb");
                Object explanationValue = questionData.get("explanation");
                if (explanationValue == null) {
                    params.add(null);
                } else if (explanationValue instanceof String strVal) {
                    String trimmed = strVal.trim();
                    if (trimmed.isEmpty()) {
                        params.add(null);
                    } else if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                        params.add(trimmed);
                    } else {
                        params.add("\"" + trimmed.replace("\"", "\\\"") + "\"");
                    }
                } else {
                    params.add(serializeJsonValue(explanationValue));
                }
            }
            if (questionData.containsKey("imageUrl")) {
                setClauses.add("image_url = ?");
                params.add(questionData.get("imageUrl"));
            }
            if (questionData.containsKey("wordLimit")) {
                setClauses.add("word_limit = ?");
                params.add(questionData.get("wordLimit"));
            }

            if (setClauses.isEmpty()) {
                logger.warn("No fields to update for question {}", questionId);
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("updated", 0);
                result.put("message", "Không có thay đổi");
                return result;
            }

            String sql = "UPDATE public.questions SET " + String.join(", ", setClauses) + " WHERE id = ?";
            params.add(questionId);

            int updated = jdbcTemplate.update(sql, params.toArray());

            logger.info("Admin {} updated question {} - {} rows affected", adminUserId, questionId, updated);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("updated", updated);
            result.put("message", "Đã cập nhật câu hỏi");
            return result;

        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error updating question {}", questionId, e);
            throw new RuntimeException("Không thể cập nhật câu hỏi: " + e.getMessage(), e);
        }
    }

    void deleteQuestion(Long questionId, String adminUserId) {
        try {
            int deleted = jdbcTemplate.update("DELETE FROM public.questions WHERE id = ?", questionId);

            invalidateCache.run();

            logger.info("Admin {} deleted question {} - {} rows affected", adminUserId, questionId, deleted);

            if (deleted == 0) {
                throw new ResourceNotFoundException("Không tìm thấy câu hỏi với ID: " + questionId);
            }

        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error deleting question {}", questionId, e);
            throw new RuntimeException("Không thể xóa câu hỏi: " + e.getMessage(), e);
        }
    }

    Map<String, Object> getQuestionById(Long questionId) {
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

    Map<String, Object> updateTestStatus(String examSource, Integer testNumber, String status,
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

            invalidateCache.run();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("updated", updated);
            result.put("message", "Đã cập nhật trạng thái đề thi");
            return result;

        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error updating test status for {} Test {}", examSource, testNumber, e);
            throw new RuntimeException("Không thể cập nhật trạng thái: " + e.getMessage(), e);
        }
    }

    void deleteTest(String testId, String adminUserId) {
        try {
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

            String deleteUserAnswersSql = """
                        DELETE FROM public.user_answers
                        WHERE attempt_id IN (
                            SELECT id FROM public.test_attempts
                            WHERE exam_source = ? AND test_number = ?
                        )
                    """;
            jdbcTemplate.update(deleteUserAnswersSql, examSource, String.valueOf(testNumber));

            String deleteSubmissionsSql = """
                        DELETE FROM public.writing_submissions
                        WHERE attempt_id IN (
                            SELECT id FROM public.test_attempts
                            WHERE exam_source = ? AND test_number = ?
                        )
                    """;
            jdbcTemplate.update(deleteSubmissionsSql, examSource, String.valueOf(testNumber));

            String deleteVocabAttemptsSql = """
                        DELETE FROM public.vocabulary
                        WHERE source_test_id IN (
                            SELECT id FROM public.test_attempts
                            WHERE exam_source = ? AND test_number = ?
                        )
                    """;
            jdbcTemplate.update(deleteVocabAttemptsSql, examSource, String.valueOf(testNumber));

            String deleteVocabSectionsSql = """
                        DELETE FROM public.vocabulary
                        WHERE source_section_id IN (
                            SELECT id FROM public.sections
                            WHERE exam_source = ? AND test_number = ?
                        )
                    """;
            jdbcTemplate.update(deleteVocabSectionsSql, examSource, testNumber);

            jdbcTemplate.update("DELETE FROM public.test_attempts WHERE exam_source = ? AND test_number = ?",
                    examSource, String.valueOf(testNumber));

            String deleteQuestionsSql = """
                        DELETE FROM public.questions
                        WHERE section_id IN (
                            SELECT id FROM public.sections
                            WHERE exam_source = ? AND test_number = ?
                        )
                    """;
            jdbcTemplate.update(deleteQuestionsSql, examSource, testNumber);

            String deleteSectionsSql = "DELETE FROM public.sections WHERE exam_source = ? AND test_number = ?";
            int deletedSections = jdbcTemplate.update(deleteSectionsSql, examSource, testNumber);

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
            invalidateCache.run();

        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error deleting test {}", testId, e);
            throw new RuntimeException("Không thể xóa đề thi: " + e.getMessage(), e);
        }
    }

    Map<String, Object> createTest(Map<String, Object> testData, String adminUserId) {
        Long setId = ((Number) testData.get("setId")).longValue();
        Object testNumberObj = testData.get("testNumber");
        Integer testNumber = testNumberObj instanceof String ? Integer.parseInt((String) testNumberObj)
                : ((Number) testNumberObj).intValue();

        try {
            logger.info("Attempting to create test in set {} with number {}", setId, testNumber);

            TestSet testSet = testSetRepository.findById(setId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ đề ID: " + setId));

            Optional<IeltsTest> existingTest = ieltsTestRepository.findByTestSetIdAndTestNumber(setId, testNumber);

            if (existingTest.isPresent()) {
                logger.info("Test already exists: Set {} / Number {}", setId, testNumber);
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("testId", existingTest.get().getId());
                result.put("message", "Test already exists");
                return result;
            }

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

            if (testData.containsKey("hashtagIds")) {
                List<?> rawIds = (List<?>) testData.get("hashtagIds");
                if (rawIds != null && !rawIds.isEmpty()) {
                    List<Long> hashtagIds = rawIds.stream()
                            .map(id -> Long.valueOf(id.toString()))
                            .toList();
                    List<Hashtag> hashtags = hashtagRepository
                            .findAllById(Objects.requireNonNull(hashtagIds, "hashtagIds must not be null"));
                    test.setHashtags(new HashSet<>(hashtags));
                    hashtags.forEach(hashtag -> {
                        hashtag.incrementUseCount();
                        hashtagRepository.save(hashtag);
                    });

                    test = ieltsTestRepository.save(test);
                }
            }

            logger.info("Created new IeltsTest with ID: {}", test.getId());

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

            invalidateCache.run();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("testId", test.getId());
            result.put("setId", setId);
            result.put("testNumber", testNumber);
            result.put("message", "Đã tạo test mới thành công");
            return result;

        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error creating test in set {}/{}", setId, testNumber, e);
            throw new RuntimeException("Không thể tạo test: " + e.getMessage(), e);
        }
    }

    Map<String, Object> updateTest(Long testId, Map<String, Object> testData, String adminUserId) {
        logger.info("Updating test {}: {}", testId, testData);
        try {
            IeltsTest test = Objects.requireNonNull(
                    ieltsTestRepository.findById(Objects.requireNonNull(testId, "testId must not be null"))
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi ID: " + testId)),
                    "Test must not be null");

            if (testData.containsKey("nameVi"))
                test.setName((String) testData.get("nameVi"));
            if (testData.containsKey("nameEn"))
                test.setName((String) testData.get("nameEn"));
            if (testData.containsKey("description"))
                test.setDescription((String) testData.get("description"));

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

                Set<Hashtag> finalOldHashtags = oldHashtags;
                List<Hashtag> removed = oldHashtags.stream()
                        .filter(hashtag -> !newHashtags.contains(hashtag))
                        .toList();

                List<Hashtag> added = newHashtags.stream()
                        .filter(hashtag -> !finalOldHashtags.contains(hashtag))
                        .toList();

                removed.forEach(hashtag -> {
                    hashtag.decrementUseCount();
                    hashtagRepository.save(hashtag);
                });
                added.forEach(hashtag -> {
                    hashtag.incrementUseCount();
                    hashtagRepository.save(hashtag);
                });

                test.setHashtags(newHashtags);
            }

            Objects.requireNonNull(ieltsTestRepository.save(test), "Failed to save updated test");
            invalidateCache.run();

            return Map.of("success", true, "message", "Cập nhật đề thi thành công");
        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error updating test {}", testId, e);
            throw new RuntimeException("Lỗi khi cập nhật đề thi: " + e.getMessage(), e);
        }
    }

    Map<String, Object> createTestSet(Map<String, Object> setData, String adminUserId) {
        logger.info("Creating new test set: {}", setData);
        try {
            TestSet testSet = TestSet.builder()
                    .code((String) setData.get("code"))
                    .name((String) setData.get("name"))
                    .description((String) setData.get("description"))
                    .sourceType((String) setData.getOrDefault("sourceType", "custom"))
                    .displayOrder((Integer) setData.getOrDefault("displayOrder", 0))
                    .isPublished(false)
                    .createdBy(UUID.fromString(adminUserId))
                    .build();

            testSet = Objects.requireNonNull(testSetRepository.save(testSet), "Failed to save test set");
            invalidateCache.run();
            return Map.of("success", true, "id", testSet.getId());
        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error creating test set", e);
            throw new RuntimeException("Lỗi khi tạo bộ đề: " + e.getMessage(), e);
        }
    }

    Map<String, Object> updateTestSet(Long setId, Map<String, Object> setData, String adminUserId) {
        logger.info("Updating test set {}: {}", setId, setData);
        try {
            TestSet testSet = testSetRepository.findById(setId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ đề ID: " + setId));

            if (setData.containsKey("name"))
                testSet.setName((String) setData.get("name"));
            if (setData.containsKey("description"))
                testSet.setDescription((String) setData.get("description"));
            if (setData.containsKey("sourceType"))
                testSet.setSourceType((String) setData.get("sourceType"));
            if (setData.containsKey("displayOrder"))
                testSet.setDisplayOrder((Integer) setData.get("displayOrder"));

            Objects.requireNonNull(testSetRepository.save(testSet), "Failed to save updated testset");
            invalidateCache.run();
            return Map.of("success", true);
        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error updating test set", e);
            throw new RuntimeException("Lỗi khi cập nhật bộ đề: " + e.getMessage(), e);
        }
    }

    void deleteTestSet(Long setId, String adminUserId) {
        logger.info("Admin {} is deleting test set {}", adminUserId, setId);
        try {
            testSetRepository.deleteById(Objects.requireNonNull(setId, "setId must not be null"));
            invalidateCache.run();
            logger.info("Successfully deleted test set {}", setId);
        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error deleting test set", e);
            throw new RuntimeException("Lỗi khi xóa bộ đề: " + e.getMessage(), e);
        }
    }
}