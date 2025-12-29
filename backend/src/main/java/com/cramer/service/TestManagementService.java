package com.cramer.service;

import com.cramer.dto.SectionDTO;
import com.cramer.dto.testhierarchy.*;
import com.cramer.entity.Hashtag;
import com.cramer.entity.IeltsTest;
import com.cramer.entity.Section;
import com.cramer.entity.TestSet;
import com.cramer.exception.ResourceAlreadyExistsException;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.repository.IeltsTestRepository;
import com.cramer.repository.SectionRepository;
import com.cramer.repository.TestSetRepository;
import com.cramer.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing IeltsTest entities.
 * Handles CRUD operations and business logic for individual tests.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TestManagementService {

    private static final Logger logger = LoggerFactory.getLogger(TestManagementService.class);

    private final IeltsTestRepository ieltsTestRepository;
    private final TestSetRepository testSetRepository;
    private final SectionRepository sectionRepository;
    private final HashtagService hashtagService;

    /**
     * Get all tests in a test set.
     * 
     * @param setId test set ID
     * @return list of test summaries
     */
    @Transactional(readOnly = true)
    public List<TestSummaryDTO> getTestsBySetId(Long setId) {
        logger.info("Fetching tests for test set ID: {}", setId);

        // Verify test set exists
        if (!testSetRepository.existsById(Objects.requireNonNull(setId))) {
            throw new ResourceNotFoundException("TestSet", "id", setId);
        }

        return ieltsTestRepository.findByTestSetIdOrderByTestNumberAsc(setId)
                .stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get test by ID with full details.
     * 
     * @param id test ID
     * @return detailed test DTO
     */
    public TestDetailDTO getTestById(Long id) {
        logger.info("Fetching test by ID: {}", id);
        IeltsTest test = Objects.requireNonNull(ieltsTestRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("IeltsTest", "id", id)));
        return toDetailDTO(test);
    }

    /**
     * Get test by set code and test number.
     * 
     * @param setCode    test set code
     * @param testNumber test number
     * @return detailed test DTO
     */
    public TestDetailDTO getTestBySetCodeAndNumber(String setCode, Integer testNumber) {
        logger.info("Fetching test by set code: {} and number: {}", setCode, testNumber);
        IeltsTest test = ieltsTestRepository.findBySetCodeAndTestNumber(setCode, testNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Test not found with setCode: %s and testNumber: %d", setCode, testNumber)));
        return toDetailDTO(test);
    }

    /**
     * Create a new test in a test set.
     * 
     * @param setId   test set ID
     * @param request creation request
     * @param userId  creator user ID
     * @return created test summary
     */
    public TestSummaryDTO createTest(Long setId, CreateTestRequest request, UUID userId) {
        logger.info("Creating new test in set ID: {}", setId);

        TestSet testSet = Objects.requireNonNull(testSetRepository.findById(Objects.requireNonNull(setId))
                .orElseThrow(() -> new ResourceNotFoundException("TestSet", "id", setId)));

        // Determine test number
        Integer testNumber = request.getTestNumber();
        if (testNumber == null) {
            Integer maxNumber = ieltsTestRepository.findMaxTestNumberByTestSetId(setId);
            testNumber = (maxNumber != null ? maxNumber : 0) + 1;
        } else {
            // Check if test number already exists
            if (ieltsTestRepository.existsByTestSetIdAndTestNumber(setId, testNumber)) {
                throw new ResourceAlreadyExistsException(
                        String.format("Test with number %d already exists in this set", testNumber));
            }
        }

        IeltsTest test = IeltsTest.builder()
                .testSet(testSet)
                .testNumber(testNumber)
                .name(request.getName())
                .name(request.getName())
                .description(request.getDescription())
                .difficulty(request.getDifficulty() != null ? request.getDifficulty() : "INTERMEDIATE")
                .estimatedTimeMinutes(
                        request.getEstimatedTimeMinutes() != null ? request.getEstimatedTimeMinutes() : 170)
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : false)
                .isAiGenerated(request.getIsAiGenerated() != null ? request.getIsAiGenerated() : false)
                .generationMetadata(request.getGenerationMetadata())
                .createdBy(userId)
                .build();

        // Handle hashtags
        if (request.getHashtagCodes() != null && !request.getHashtagCodes().isEmpty()) {
            Set<Hashtag> hashtags = hashtagService.findOrCreateByCodes(request.getHashtagCodes());
            test.setHashtags(hashtags);
            hashtagService.incrementUseCounts(hashtags);
        }

        IeltsTest saved = ieltsTestRepository.save(test);
        logger.info("Created test with ID: {}", saved.getId());

        return toSummaryDTO(saved);
    }

    /**
     * Update an existing test.
     * 
     * @param id      test ID
     * @param request update request
     * @return updated test summary
     */
    public TestSummaryDTO updateTest(Long id, CreateTestRequest request) {
        logger.info("Updating test ID: {}", id);

        IeltsTest test = Objects.requireNonNull(ieltsTestRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("IeltsTest", "id", id)));

        // Check if test number changed and if new number already exists
        if (request.getTestNumber() != null &&
                !request.getTestNumber().equals(test.getTestNumber()) &&
                ieltsTestRepository.existsByTestSetIdAndTestNumber(test.getSetId(), request.getTestNumber())) {
            throw new ResourceAlreadyExistsException(
                    String.format("Test with number %d already exists in this set", request.getTestNumber()));
        }

        if (request.getTestNumber() != null) {
            test.setTestNumber(request.getTestNumber());
        }
        if (request.getName() != null) {
            test.setName(request.getName());
        }
        if (request.getName() != null) {
            test.setName(request.getName());
        }
        if (request.getDescription() != null) {
            test.setDescription(request.getDescription());
        }
        if (request.getDifficulty() != null) {
            test.setDifficulty(request.getDifficulty());
        }
        if (request.getEstimatedTimeMinutes() != null) {
            test.setEstimatedTimeMinutes(request.getEstimatedTimeMinutes());
        }
        if (request.getIsPublished() != null) {
            test.setIsPublished(request.getIsPublished());
        }
        if (request.getIsAiGenerated() != null) {
            test.setIsAiGenerated(request.getIsAiGenerated());
        }
        if (request.getGenerationMetadata() != null) {
            test.setGenerationMetadata(request.getGenerationMetadata());
        }

        IeltsTest saved = Objects.requireNonNull(ieltsTestRepository.save(test));
        logger.info("Updated test ID: {}", saved.getId());

        return toSummaryDTO(saved);
    }

    /**
     * Delete a test.
     * 
     * @param id test ID
     */
    public void deleteTest(Long id) {
        logger.info("Deleting test ID: {}", id);

        IeltsTest test = Objects.requireNonNull(ieltsTestRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("IeltsTest", "id", id)));

        // Decrement hashtag use counts
        hashtagService.decrementUseCounts(test.getHashtags());

        // Delete the test (sections will be handled separately or via DB cascade)
        ieltsTestRepository.delete(test);
        logger.info("Deleted test ID: {}", id);
    }

    /**
     * Publish or unpublish a test.
     * 
     * @param id      test ID
     * @param publish true to publish, false to unpublish
     * @return updated test summary
     */
    public TestSummaryDTO publishTest(Long id, boolean publish) {
        logger.info("{} test ID: {}", publish ? "Publishing" : "Unpublishing", id);

        IeltsTest test = Objects.requireNonNull(ieltsTestRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("IeltsTest", "id", id)));

        test.setIsPublished(publish);
        IeltsTest saved = ieltsTestRepository.save(test);

        String sectionStatus = publish ? "PUBLISHED" : "DRAFT";
        int updatedSections = sectionRepository.updateStatusByTestId(id, sectionStatus);
        logger.info("Updated {} sections to status {}", updatedSections, sectionStatus);

        logger.info("Test ID: {} is now {}", id, publish ? "published" : "unpublished");
        return toSummaryDTO(saved);
    }

    /**
     * Update hashtags for a test.
     * 
     * @param testId  test ID
     * @param request update request with hashtag codes
     * @return updated test summary
     */
    public TestSummaryDTO updateTestHashtags(Long testId, UpdateTestHashtagsRequest request) {
        logger.info("Updating hashtags for test ID: {}", testId);

        IeltsTest test = Objects.requireNonNull(ieltsTestRepository.findById(Objects.requireNonNull(testId))
                .orElseThrow(() -> new ResourceNotFoundException("IeltsTest", "id", testId)));

        // Decrement old hashtag use counts
        hashtagService.decrementUseCounts(test.getHashtags());

        // Clear and set new hashtags
        test.getHashtags().clear();
        if (request.getHashtagCodes() != null && !request.getHashtagCodes().isEmpty()) {
            Set<Hashtag> newHashtags = hashtagService.findOrCreateByCodes(request.getHashtagCodes());
            test.setHashtags(newHashtags);
            hashtagService.incrementUseCounts(newHashtags);
        }

        IeltsTest saved = ieltsTestRepository.save(test);
        logger.info("Updated hashtags for test ID: {}", testId);

        return toSummaryDTO(saved);
    }

    /**
     * Duplicate a test with a new test number.
     * 
     * @param testId        original test ID
     * @param newTestNumber new test number
     * @return duplicated test summary
     */
    public TestSummaryDTO duplicateTest(Long testId, Integer newTestNumber) {
        logger.info("Duplicating test ID: {} with new number: {}", testId, newTestNumber);

        IeltsTest original = Objects.requireNonNull(ieltsTestRepository.findById(Objects.requireNonNull(testId))
                .orElseThrow(() -> new ResourceNotFoundException("IeltsTest", "id", testId)));

        // Check if new test number already exists
        if (ieltsTestRepository.existsByTestSetIdAndTestNumber(original.getSetId(), newTestNumber)) {
            throw new ResourceAlreadyExistsException(
                    String.format("Test with number %d already exists in this set", newTestNumber));
        }

        // Create duplicate
        IeltsTest duplicate = IeltsTest.builder()
                .testSet(original.getTestSet())
                .testNumber(newTestNumber)
                .name(original.getName() != null ? original.getName() + " (Copy)" : null)
                .name(original.getName() != null ? original.getName() + " (Copy)" : null)
                .description(original.getDescription())
                .difficulty(original.getDifficulty())
                .estimatedTimeMinutes(original.getEstimatedTimeMinutes())
                .isPublished(false) // Duplicates start unpublished
                .isAiGenerated(original.getIsAiGenerated())
                .generationMetadata(original.getGenerationMetadata())
                .build();

        // Copy hashtags
        if (original.getHashtags() != null && !original.getHashtags().isEmpty()) {
            duplicate.setHashtags(new HashSet<>(original.getHashtags()));
            hashtagService.incrementUseCounts(duplicate.getHashtags());
        }

        IeltsTest saved = ieltsTestRepository.save(duplicate);
        logger.info("Duplicated test ID: {} to new test ID: {}", testId, saved.getId());

        // Note: Sections are NOT duplicated - they need to be created separately
        // or a more complex duplication logic would be needed

        return toSummaryDTO(saved);
    }

    /**
     * Get section counts by skill for a test.
     */
    private Map<String, Long> getSkillSectionCounts(Long testId) {
        Map<String, Long> counts = new HashMap<>();
        String[] skills = { "reading", "listening", "writing", "speaking" };

        for (String skill : skills) {
            long count = ieltsTestRepository.countSectionsByTestIdAndSkill(testId, skill);
            if (count > 0) {
                counts.put(skill, count);
            }
        }

        return counts;
    }

    /**
     * Convert IeltsTest entity to summary DTO.
     */
    private TestSummaryDTO toSummaryDTO(IeltsTest test) {
        List<HashtagDTO> hashtagDTOs = test.getHashtags() != null ? test.getHashtags().stream()
                .map(h -> HashtagDTO.builder()
                        .id(h.getId())
                        .code(h.getCode())
                        .name(h.getName())
                        .name(h.getName())
                        .category(h.getCategory())
                        .icon(h.getIcon())
                        .color(h.getColor())
                        .build())
                .collect(Collectors.toList()) : Collections.emptyList();

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
                .skillSectionCounts(getSkillSectionCounts(test.getId()))
                .hashtags(hashtagDTOs)
                .build();
    }

    /**
     * Convert IeltsTest entity to detail DTO with sections.
     */
    private TestDetailDTO toDetailDTO(IeltsTest test) {
        // Get sections for this test
        String setCode = test.getSetCode();
        Integer testNumber = test.getTestNumber();

        List<Section> sections = new ArrayList<>(sectionRepository.findByIeltsTestId(test.getId()));
        List<Section> unlinkedSections = sectionRepository.findByExamSourceAndTestNumberAndIeltsTestIsNull(setCode,
                testNumber);
        if (!unlinkedSections.isEmpty()) {
            unlinkedSections.forEach(section -> section.setIeltsTest(test));
            sectionRepository.saveAll(unlinkedSections);
            sections.addAll(unlinkedSections);
        }
        if (sections.isEmpty()) {
            sections = sectionRepository.findByExamSourceAndTestNumber(setCode, testNumber);
        }

        // Group sections by skill
        Map<String, List<SectionDTO>> sectionsBySkill = sections.stream()
                .collect(Collectors.groupingBy(
                        Section::getSkill,
                        Collectors.mapping(EntityMapper::toDTO, Collectors.toList())));

        List<HashtagDTO> hashtagDTOs = test.getHashtags() != null ? test.getHashtags().stream()
                .map(h -> HashtagDTO.builder()
                        .id(h.getId())
                        .code(h.getCode())
                        .name(h.getName())
                        .name(h.getName())
                        .category(h.getCategory())
                        .icon(h.getIcon())
                        .color(h.getColor())
                        .build())
                .collect(Collectors.toList()) : Collections.emptyList();

        return TestDetailDTO.builder()
                .id(test.getId())
                .setId(test.getSetId())
                .setCode(setCode)
                .setName(test.getTestSet() != null ? test.getTestSet().getName() : null)
                .testNumber(testNumber)
                .name(test.getName())
                .name(test.getName())
                .description(test.getDescription())
                .difficulty(test.getDifficulty())
                .estimatedTimeMinutes(test.getEstimatedTimeMinutes())
                .isPublished(test.getIsPublished())
                .isAiGenerated(test.getIsAiGenerated())
                .generationMetadata(test.getGenerationMetadata())
                .createdAt(test.getCreatedAt())
                .updatedAt(test.getUpdatedAt())
                .skillSectionCounts(getSkillSectionCounts(test.getId()))
                .hashtags(hashtagDTOs)
                .sectionsBySkill(sectionsBySkill)
                .build();
    }
}
