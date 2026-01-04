package com.cramer.service;

import com.cramer.dto.testhierarchy.*;
import com.cramer.entity.TestSet;
import com.cramer.entity.IeltsTest;
import com.cramer.exception.OperationNotAllowedException;
import com.cramer.exception.ResourceAlreadyExistsException;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.repository.TestSetRepository;
import com.cramer.repository.IeltsTestRepository;
import com.cramer.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing TestSet entities.
 * Handles CRUD operations and business logic for test sets.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TestSetService {

    private static final Logger logger = LoggerFactory.getLogger(TestSetService.class);

    private final TestSetRepository testSetRepository;
    private final IeltsTestRepository ieltsTestRepository;
    private final SectionRepository sectionRepository;

    /**
     * Get all test sets with counts.
     * 
     * @return list of test sets with test counts
     */
    @Transactional(readOnly = true)
    public List<TestSetDTO> getAllTestSets() {
        logger.info("Fetching all test sets");
        return testSetRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get test set by ID with tests.
     * 
     * @param id the test set ID
     * @return detailed test set DTO with tests
     */
    @Transactional(readOnly = true)
    public TestSetDetailDTO getTestSetById(Long id) {
        logger.info("Fetching test set by ID: {}", id);
        TestSet testSet = Objects.requireNonNull(testSetRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("TestSet", "id", id)));
        return toDetailDTO(testSet);
    }

    /**
     * Get test set by code with tests.
     * 
     * @param code the test set code
     * @return detailed test set DTO with tests
     */
    @Transactional(readOnly = true)
    public TestSetDetailDTO getTestSetByCode(String code) {
        logger.info("Fetching test set by code: {}", code);
        TestSet testSet = testSetRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("TestSet", "code", code));
        return toDetailDTO(testSet);
    }

    /**
     * Get test set by code (returns Optional for controller use).
     * 
     * @param code the test set code
     * @return optional TestSetDTO
     */
    @Transactional(readOnly = true)
    public java.util.Optional<TestSetDTO> getByCode(String code) {
        logger.info("Fetching test set by code (optional): {}", code);
        return testSetRepository.findByCode(code).map(this::toDTO);
    }

    /**
     * Create a new test set.
     * 
     * @param request creation request
     * @param userId  creator user ID
     * @return created test set DTO
     */
    public TestSetDTO createTestSet(CreateTestSetRequest request, UUID userId) {
        logger.info("Creating new test set with code: {}", request.getCode());

        // Check if code already exists
        if (testSetRepository.existsByCode(request.getCode())) {
            throw new ResourceAlreadyExistsException("TestSet", "code", request.getCode());
        }

        TestSet testSet = new TestSet();
        testSet.setCode(request.getCode());
        testSet.setName(request.getName());
        testSet.setDescription(request.getDescription());
        testSet.setCoverImageUrl(request.getCoverImageUrl());
        testSet.setSourceType(request.getSourceType() != null ? request.getSourceType() : "custom");
        testSet.setIsPublished(request.getIsPublished() != null ? request.getIsPublished() : false);
        testSet.setCreatedBy(userId);

        // Set display order to max + 1 if not provided
        if (request.getDisplayOrder() != null) {
            testSet.setDisplayOrder(request.getDisplayOrder());
        } else {
            int maxOrder = testSetRepository.findMaxDisplayOrder();
            testSet.setDisplayOrder(maxOrder + 1);
        }

        TestSet saved = testSetRepository.save(testSet);
        logger.info("Created test set with ID: {}", saved.getId());

        return toDTO(saved);
    }

    /**
     * Update an existing test set.
     * 
     * @param id      test set ID
     * @param request update request
     * @return updated test set DTO
     */
    public TestSetDTO updateTestSet(Long id, CreateTestSetRequest request) {
        logger.info("Updating test set ID: {}", id);

        TestSet testSet = Objects.requireNonNull(testSetRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("TestSet", "id", id)));

        // Check if code changed and if new code already exists
        if (!testSet.getCode().equals(request.getCode()) &&
                testSetRepository.existsByCode(request.getCode())) {
            throw new ResourceAlreadyExistsException("TestSet", "code", request.getCode());
        }

        testSet.setCode(request.getCode());
        testSet.setName(request.getName());
        testSet.setDescription(request.getDescription());
        testSet.setCoverImageUrl(request.getCoverImageUrl());
        testSet.setSourceType(request.getSourceType() != null ? request.getSourceType() : "custom");

        if (request.getIsPublished() != null) {
            testSet.setIsPublished(request.getIsPublished());
        }
        if (request.getDisplayOrder() != null) {
            testSet.setDisplayOrder(request.getDisplayOrder());
        }

        TestSet saved = testSetRepository.save(testSet);
        logger.info("Updated test set ID: {}", saved.getId());

        return toDTO(saved);
    }

    /**
     * Delete a test set.
     * 
     * @param id test set ID
     * @throws OperationNotAllowedException if test set is a system set
     */
    public void deleteTestSet(Long id) {
        logger.info("Deleting test set ID: {}", id);

        TestSet testSet = Objects.requireNonNull(testSetRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("TestSet", "id", id)));

        // Cascade delete will handle tests
        testSetRepository.delete(testSet);
        logger.info("Deleted test set ID: {}", id);
    }

    /**
     * Publish or unpublish a test set.
     * 
     * @param id      test set ID
     * @param publish true to publish, false to unpublish
     * @return updated test set DTO
     */
    public TestSetDTO publishTestSet(Long id, boolean publish) {
        logger.info("{} test set ID: {}", publish ? "Publishing" : "Unpublishing", id);

        TestSet testSet = Objects.requireNonNull(testSetRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("TestSet", "id", id)));

        testSet.setIsPublished(publish);
        TestSet saved = testSetRepository.save(testSet);

        logger.info("Test set ID: {} is now {}", id, publish ? "published" : "unpublished");
        return toDTO(saved);
    }

    /**
     * Reorder test sets by providing ordered list of IDs.
     * 
     * @param orderedIds list of test set IDs in desired order
     */
    public void reorderTestSets(List<Long> orderedIds) {
        logger.info("Reordering {} test sets", orderedIds.size());

        for (int i = 0; i < orderedIds.size(); i++) {
            testSetRepository.updateDisplayOrder(orderedIds.get(i), i);
        }

        logger.info("Reordered test sets successfully");
    }

    /**
     * Convert TestSet entity to DTO with counts.
     */
    private TestSetDTO toDTO(TestSet entity) {
        return TestSetDTO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .coverImageUrl(entity.getCoverImageUrl())
                .sourceType(entity.getSourceType())
                .isPublished(entity.getIsPublished())
                .displayOrder(entity.getDisplayOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .testCount(testSetRepository.countTestsByTestSetId(entity.getId()))
                .publishedTestCount(testSetRepository.countPublishedTestsByTestSetId(entity.getId()))
                .build();
    }

    /**
     * Convert TestSet entity to detail DTO with tests.
     */
    private TestSetDetailDTO toDetailDTO(TestSet entity) {
        List<IeltsTest> tests = ieltsTestRepository.findByTestSetIdOrderByTestNumberAsc(entity.getId());

        List<TestSummaryDTO> testDTOs = tests.stream()
                .map(this::toTestSummaryDTO)
                .collect(Collectors.toList());

        return TestSetDetailDTO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .coverImageUrl(entity.getCoverImageUrl())
                .sourceType(entity.getSourceType())
                .isPublished(entity.getIsPublished())
                .displayOrder(entity.getDisplayOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .testCount((long) tests.size())
                .publishedTestCount(tests.stream().filter(IeltsTest::getIsPublished).count())
                .tests(testDTOs)
                .build();
    }

    /**
     * Convert IeltsTest entity to summary DTO.
     */
    private TestSummaryDTO toTestSummaryDTO(IeltsTest test) {
        List<HashtagDTO> hashtagDTOs = test.getHashtags().stream()
                .map(h -> HashtagDTO.builder()
                        .id(h.getId())
                        .code(h.getCode())
                        .name(h.getName())
                        .name(h.getName())
                        .category(h.getCategory())
                        .icon(h.getIcon())
                        .color(h.getColor())
                        .build())
                .collect(Collectors.toList());

        // Query section counts by skill
        Map<String, Long> skillCounts = new java.util.HashMap<>();
        List<Object[]> rawCounts = sectionRepository.countSectionsByTestIdGroupBySkill(test.getId());
        for (Object[] row : rawCounts) {
            String skill = (String) row[0];
            Long count = (Long) row[1];
            skillCounts.put(skill, count);
        }

        return TestSummaryDTO.builder()
                .id(test.getId())
                .setId(test.getSetId())
                .setCode(test.getSetCode())
                .setName(test.getTestSet() != null ? test.getTestSet().getName() : null)
                .testNumber(test.getTestNumber())
                .name(test.getName())
                .name(test.getName())
                .description(test.getDescription())
                .difficulty(test.getDifficulty())
                .estimatedTimeMinutes(test.getEstimatedTimeMinutes())
                .isPublished(test.getIsPublished())
                .isAiGenerated(test.getIsAiGenerated())
                .createdAt(test.getCreatedAt())
                .updatedAt(test.getUpdatedAt())
                .skillSectionCounts(skillCounts)
                .hashtags(hashtagDTOs)
                .build();
    }
}
