package com.cramer.service.abts;

import com.cramer.dto.abts.GeneratedContentDTO;
import com.cramer.dto.abts.SaveContentRequestDTO;
import com.cramer.dto.abts.SaveContentResponseDTO;
import com.cramer.entity.Hashtag;
import com.cramer.entity.IeltsTest;
import com.cramer.entity.Section;
import com.cramer.entity.TestSet;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.repository.HashtagRepository;
import com.cramer.repository.IeltsTestRepository;
import com.cramer.repository.SectionRepository;
import com.cramer.repository.TestSetRepository;
import com.cramer.service.HashtagService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.*;

final class AbtsContentSaver {

    private static final Logger logger = LoggerFactory.getLogger(AbtsContentSaver.class);

    private final JdbcTemplate jdbcTemplate;
    private final TestSetRepository testSetRepository;
    private final IeltsTestRepository ieltsTestRepository;
    private final HashtagRepository hashtagRepository;
    private final HashtagService hashtagService;
    private final SectionRepository sectionRepository;
    private final ObjectMapper objectMapper;

    AbtsContentSaver(
            JdbcTemplate jdbcTemplate,
            TestSetRepository testSetRepository,
            IeltsTestRepository ieltsTestRepository,
            HashtagRepository hashtagRepository,
            HashtagService hashtagService,
            SectionRepository sectionRepository,
            ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.testSetRepository = testSetRepository;
        this.ieltsTestRepository = ieltsTestRepository;
        this.hashtagRepository = hashtagRepository;
        this.hashtagService = hashtagService;
        this.sectionRepository = sectionRepository;
        this.objectMapper = objectMapper;
    }

    SaveContentResponseDTO saveContent(SaveContentRequestDTO request, String adminUserId) {
        logger.info("Saving ABTS content: skill={}, partNumber={}, setCode={}, testId={}",
                request.getSkill(), request.getPartNumber(), request.getSetCode(), request.getTestId());

        try {
            GeneratedContentDTO content = request.getContent();
            if (content == null) {
                return SaveContentResponseDTO.error("Nội dung trống");
            }

            UUID createdBy = null;
            if (adminUserId != null && !adminUserId.isBlank()) {
                try {
                    createdBy = UUID.fromString(adminUserId);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid admin user ID format: {}", adminUserId);
                }
            }

            TestSet testSet = resolveTestSet(request, createdBy);
            logger.info("Using TestSet: id={}, code={}", testSet.getId(), testSet.getCode());

            IeltsTest ieltsTest = resolveTest(request, testSet, createdBy);
            logger.info("Using IeltsTest: id={}, testNumber={}", ieltsTest.getId(), ieltsTest.getTestNumber());

            if (request.getHashtagCodes() != null && !request.getHashtagCodes().isEmpty()) {
                List<String> codes = request.getHashtagCodes();

                if (codes.size() > 20) {
                    return SaveContentResponseDTO.error("Quá nhiều hashtag: tối đa 20, đã nhập " + codes.size());
                }

                for (String code : codes) {
                    if (code == null || code.isBlank())
                        continue;
                    if (!code.matches("^[a-z0-9_-]{1,50}$")) {
                        return SaveContentResponseDTO.error(
                                "Định dạng mã hashtag không hợp lệ: '" + code + "'. " +
                                        "Mã chỉ được chứa 1-50 ký tự chữ thường, số, gạch dưới hoặc gạch ngang.");
                    }
                }

                Set<Hashtag> hashtags = hashtagService.findOrCreateByCodes(codes);
                ieltsTest.setHashtags(hashtags);
                hashtagService.incrementUseCounts(hashtags);
                logger.info("Associated {} hashtags with test", hashtags.size());
            } else if (request.getHashtagIds() != null && !request.getHashtagIds().isEmpty()) {
                List<Long> ids = request.getHashtagIds();

                if (ids.size() > 20) {
                    return SaveContentResponseDTO.error("Quá nhiều hashtag: tối đa 20, đã nhập " + ids.size());
                }

                Set<Hashtag> hashtags = new HashSet<>(
                        hashtagRepository.findAllById(Objects.requireNonNull(ids)));
                ieltsTest.setHashtags(hashtags);
                hashtagService.incrementUseCounts(hashtags);
                logger.info("Associated {} hashtags (by ID) with test", hashtags.size());
            }

            if (request.getGenerationConfig() != null) {
                Map<String, Object> metadata = buildGenerationMetadata(request);
                try {
                    JsonNode metadataJson = objectMapper.valueToTree(metadata);
                    ieltsTest.setGenerationMetadata(metadataJson);
                } catch (Exception e) {
                    logger.warn("Failed to set generation metadata: {}", e.getMessage());
                }
            }
            ieltsTest.setIsAiGenerated(true);

            ieltsTest = ieltsTestRepository.save(ieltsTest);

            long firstSectionId = -1;
            int totalQuestionsCreated = 0;
            String skillLower = request.getSkill().toLowerCase();

            List<SaveContentRequestDTO.PartSaveData> partsToSave = request.getPartsToSave();
            if (partsToSave != null && !partsToSave.isEmpty()) {
                logger.info("Processing multi-part save for {} parts", partsToSave.size());

                for (SaveContentRequestDTO.PartSaveData partData : partsToSave) {
                    GeneratedContentDTO partContent = partData.getContent();
                    if (partContent == null || partContent.getSection() == null)
                        continue;

                    Integer partNum = partData.getPartNumber();
                    if (partNum == null && partContent.getSection().getPartNumber() != null) {
                        partNum = partContent.getSection().getPartNumber();
                    }
                    if (partNum == null)
                        partNum = request.getPartNumber();

                    Optional<Section> existingSection = sectionRepository.findByIeltsTestIdAndSkillAndPartNumber(
                            ieltsTest.getId(), skillLower, partNum);

                    Section section;
                    if (existingSection.isPresent()) {
                        section = existingSection.get();
                        GeneratedContentDTO.GeneratedSectionDTO sectionData = partContent.getSection();
                        section.setPassageText(sectionData.getPassageText());
                        if (sectionData.getSectionLayout() != null) {
                            section.setSectionLayout(sectionData.getSectionLayout());
                        }
                        jdbcTemplate.update("DELETE FROM questions WHERE section_id = ?", section.getId());
                    } else {
                        section = createSection(request, partContent, ieltsTest);
                        section.setPartNumber(partNum);
                    }

                    section = sectionRepository.save(section);
                    if (firstSectionId == -1)
                        firstSectionId = section.getId();

                    int qCreated = createQuestions(partContent, section, ieltsTest);
                    totalQuestionsCreated += qCreated;
                }
            } else {
                Optional<Section> existingSection = sectionRepository.findByIeltsTestIdAndSkillAndPartNumber(
                        ieltsTest.getId(), skillLower, request.getPartNumber());

                Section section;
                if (existingSection.isPresent()) {
                    section = existingSection.get();
                    logger.info("Updating existing section ID: {} for test ID: {}", section.getId(), ieltsTest.getId());

                    if (content.getSection() != null) {
                        GeneratedContentDTO.GeneratedSectionDTO sectionData = content.getSection();
                        section.setPassageText(sectionData.getPassageText());
                        if (sectionData.getSectionLayout() != null) {
                            section.setSectionLayout(sectionData.getSectionLayout());
                        }
                    }

                    jdbcTemplate.update("DELETE FROM questions WHERE section_id = ?", section.getId());
                } else {
                    section = createSection(request, content, ieltsTest);
                }

                section = sectionRepository.save(section);
                logger.info("{} section with ID: {}, linked to test ID: {}",
                        existingSection.isPresent() ? "Updated" : "Created", section.getId(), ieltsTest.getId());

                totalQuestionsCreated = createQuestions(content, section, ieltsTest);
                firstSectionId = section.getId();
            }

            logger.info("Created total {} questions", totalQuestionsCreated);

            return SaveContentResponseDTO.success(
                    firstSectionId,
                    ieltsTest.getId(),
                    testSet.getId(),
                    ieltsTest.getName(),
                    testSet.getName(),
                    testSet.getCode(),
                    testSet.getCode(),
                    ieltsTest.getTestNumber(),
                    request.getSkill().toLowerCase(),
                    request.getPartNumber(),
                    totalQuestionsCreated);

        } catch (Exception e) {
            logger.error("Failed to save ABTS content: {}", e.getMessage(), e);
            return SaveContentResponseDTO.error("Lỗi cơ sở dữ liệu: " + e.getMessage());
        }
    }

    private TestSet resolveTestSet(SaveContentRequestDTO request, UUID createdBy) {
        if (request.getSetId() != null) {
            return testSetRepository.findById(Objects.requireNonNull(request.getSetId()))
                    .orElseThrow(() -> new ResourceNotFoundException("TestSet", "id", request.getSetId()));
        }

        String setCode = request.getSetCode();
        if (setCode == null || setCode.isEmpty()) {
            setCode = "ai_generated";
        }

        Optional<TestSet> existing = testSetRepository.findByCode(setCode);
        if (existing.isPresent()) {
            return existing.get();
        }

        String displayName = request.getSetNameVi();
        if (displayName == null || displayName.isBlank()) {
            displayName = formatCodeToName(setCode);
        }

        TestSet newSet = TestSet.builder()
                .code(setCode)
                .name(displayName)
                .sourceType("ai_generated")
                .isPublished(false)
                .displayOrder(testSetRepository.findMaxDisplayOrder() + 1)
                .createdBy(createdBy)
                .build();

        return testSetRepository.save(newSet);
    }

    private IeltsTest resolveTest(SaveContentRequestDTO request, TestSet testSet, UUID createdBy) {
        if (request.getTestId() != null) {
            return ieltsTestRepository.findById(Objects.requireNonNull(request.getTestId()))
                    .orElseThrow(() -> new ResourceNotFoundException("IeltsTest", "id", request.getTestId()));
        }

        Integer testNumber = null;
        if (request.getTestNumber() != null && !request.getTestNumber().isBlank()) {
            try {
                testNumber = Integer.parseInt(request.getTestNumber());
            } catch (NumberFormatException e) {
                // Will auto-generate below
            }
        }

        if (testNumber == null) {
            Integer maxTestNumber = ieltsTestRepository.findMaxTestNumberByTestSetId(testSet.getId());
            testNumber = (maxTestNumber != null ? maxTestNumber : 0) + 1;
        }

        Optional<IeltsTest> existingTest = ieltsTestRepository.findByTestSetIdAndTestNumber(testSet.getId(),
                testNumber);
        if (existingTest.isPresent()) {
            return existingTest.get();
        }

        String difficulty = "INTERMEDIATE";
        if (request.getDifficulty() != null && !request.getDifficulty().isBlank()) {
            difficulty = request.getDifficulty();
        } else {
            GeneratedContentDTO content = request.getContent();
            if (content != null && content.getMetadata() != null && content.getMetadata().getDifficulty() != null) {
                difficulty = content.getMetadata().getDifficulty();
            }
        }

        String topic = request.getTopic() != null ? request.getTopic() : "AI Generated Test";

        String nameVi = request.getTestNameVi();
        if (nameVi == null || nameVi.isBlank()) {
            nameVi = "AI Test " + testNumber + (topic != null ? " - " + topic : "");
        }

        String nameEn = request.getTestNameEn();
        if (nameEn == null || nameEn.isBlank()) {
            nameEn = "AI Generated Test " + testNumber;
        }

        IeltsTest newTest = IeltsTest.builder()
                .testSet(testSet)
                .testNumber(testNumber)
                .name(nameVi)
                .difficulty(difficulty)
                .isPublished(false)
                .isAiGenerated(true)
                .createdBy(createdBy)
                .build();

        return ieltsTestRepository.save(newTest);
    }

    private Section createSection(SaveContentRequestDTO request, GeneratedContentDTO content, IeltsTest ieltsTest) {
        Section section = new Section();
        section.setIeltsTest(ieltsTest);
        section.setExamSource(ieltsTest.getTestSet().getCode());
        section.setTestNumber(ieltsTest.getTestNumber());
        section.setSkill(request.getSkill().toLowerCase());
        section.setPartNumber(request.getPartNumber());
        section.setStatus("DRAFT");

        if (content.getSection() != null) {
            GeneratedContentDTO.GeneratedSectionDTO sectionData = content.getSection();
            section.setPassageText(sectionData.getPassageText());

            if (sectionData.getSectionLayout() != null) {
                section.setSectionLayout(sectionData.getSectionLayout());
            }
        }

        if (content.getFigureDescription() != null) {
            try {
                section.setImageDescription(objectMapper.writeValueAsString(content.getFigureDescription()));
            } catch (Exception e) {
                logger.warn("Failed to serialize figure_description: {}", e.getMessage());
            }
        }

        return section;
    }

    private int createQuestions(GeneratedContentDTO content, Section section, IeltsTest ieltsTest) {
        if (content.getQuestions() == null || content.getQuestions().isEmpty()) {
            return 0;
        }

        String insertQuestionSql = """
                INSERT INTO questions (section_id, question_number, question_uid, question_type,
                                      question_content, correct_answer, explanation, word_limit, image_url)
                VALUES (?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?, ?)
                """;

        String skillCode = section.getSkill().substring(0, 1);
        int questionsCreated = 0;

        for (GeneratedContentDTO.GeneratedQuestionDTO q : content.getQuestions()) {
            try {
                String questionUid = String.format("%s-t%d-%s-q%d",
                        ieltsTest.getTestSet().getCode().toLowerCase(),
                        ieltsTest.getTestNumber(),
                        skillCode,
                        q.getQuestionNumber());

                String questionContentJson = null;
                if (q.getQuestionContent() != null) {
                    questionContentJson = objectMapper.writeValueAsString(q.getQuestionContent());
                }

                String correctAnswerJson = null;
                if (q.getCorrectAnswer() != null) {
                    correctAnswerJson = objectMapper.writeValueAsString(q.getCorrectAnswer());
                }

                String explanationJson = null;
                if (q.getExplanation() != null) {
                    explanationJson = objectMapper.writeValueAsString(q.getExplanation());
                }

                jdbcTemplate.update(
                        insertQuestionSql,
                        section.getId(),
                        q.getQuestionNumber(),
                        questionUid,
                        q.getQuestionType(),
                        questionContentJson,
                        correctAnswerJson,
                        explanationJson,
                        q.getWordLimit(),
                        q.getImageUrl());

                questionsCreated++;
            } catch (Exception e) {
                logger.error("Failed to insert question {}: {}", q.getQuestionNumber(), e.getMessage());
            }
        }

        return questionsCreated;
    }

    private Map<String, Object> buildGenerationMetadata(SaveContentRequestDTO request) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("generated_by", "ABTS");
        metadata.put("version", "2.0.0");
        metadata.put("generated_at", Instant.now().toString());

        if (request.getGenerationConfig() != null) {
            metadata.put("generation_config", request.getGenerationConfig());
        }
        if (request.getTopic() != null) {
            metadata.put("topic", request.getTopic());
        }
        if (request.getHashtagCodes() != null) {
            metadata.put("hashtags", request.getHashtagCodes());
        }

        return metadata;
    }

    private String formatCodeToName(String code) {
        if (code == null || code.isEmpty()) {
            return code;
        }
        return Arrays.stream(code.split("[-_]"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .reduce((a, b) -> a + " " + b)
                .orElse(code);
    }
}