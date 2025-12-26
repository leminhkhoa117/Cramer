package com.cramer.controller.admin;

import com.cramer.dto.testhierarchy.*;
import com.cramer.entity.Section;
import com.cramer.service.HashtagService;
import com.cramer.service.SectionService;
import com.cramer.service.TestManagementService;
import com.cramer.service.TestSetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin controller for managing test hierarchy (TestSets, Tests, Hashtags).
 * All endpoints require admin authentication.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class TestHierarchyController {

    private static final Logger logger = LoggerFactory.getLogger(TestHierarchyController.class);

    private final TestSetService testSetService;
    private final TestManagementService testManagementService;
    private final HashtagService hashtagService;
    private final SectionService sectionService;

    // ==================== TEST SETS ====================

    /**
     * Get all test sets.
     */
    @GetMapping("/test-sets")
    public ResponseEntity<List<TestSetDTO>> getAllTestSets() {
        logger.info("GET /api/admin/test-sets");
        List<TestSetDTO> testSets = testSetService.getAllTestSets();
        return ResponseEntity.ok(testSets);
    }

    /**
     * Get test set by ID with tests.
     */
    @GetMapping("/test-sets/{id}")
    public ResponseEntity<TestSetDetailDTO> getTestSetById(@PathVariable Long id) {
        logger.info("GET /api/admin/test-sets/{}", id);
        TestSetDetailDTO testSet = testSetService.getTestSetById(id);
        return ResponseEntity.ok(testSet);
    }

    /**
     * Get test set by code with tests.
     */
    @GetMapping("/test-sets/code/{code}")
    public ResponseEntity<TestSetDetailDTO> getTestSetByCode(@PathVariable String code) {
        logger.info("GET /api/admin/test-sets/code/{}", code);
        TestSetDetailDTO testSet = testSetService.getTestSetByCode(code);
        return ResponseEntity.ok(testSet);
    }

    /**
     * Create a new test set.
     */
    @PostMapping("/test-sets")
    public ResponseEntity<TestSetDTO> createTestSet(
            @Valid @RequestBody CreateTestSetRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        logger.info("POST /api/admin/test-sets - code: {}", request.getCode());
        UUID userId = UUID.fromString(jwt.getSubject());
        TestSetDTO created = testSetService.createTestSet(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update a test set.
     */
    @PutMapping("/test-sets/{id}")
    public ResponseEntity<TestSetDTO> updateTestSet(
            @PathVariable Long id,
            @Valid @RequestBody CreateTestSetRequest request) {
        logger.info("PUT /api/admin/test-sets/{}", id);
        TestSetDTO updated = testSetService.updateTestSet(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a test set.
     */
    @DeleteMapping("/test-sets/{id}")
    public ResponseEntity<Void> deleteTestSet(@PathVariable Long id) {
        logger.info("DELETE /api/admin/test-sets/{}", id);
        testSetService.deleteTestSet(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Publish a test set.
     */
    @PostMapping("/test-sets/{id}/publish")
    public ResponseEntity<TestSetDTO> publishTestSet(@PathVariable Long id) {
        logger.info("POST /api/admin/test-sets/{}/publish", id);
        TestSetDTO updated = testSetService.publishTestSet(id, true);
        return ResponseEntity.ok(updated);
    }

    /**
     * Unpublish a test set.
     */
    @PostMapping("/test-sets/{id}/unpublish")
    public ResponseEntity<TestSetDTO> unpublishTestSet(@PathVariable Long id) {
        logger.info("POST /api/admin/test-sets/{}/unpublish", id);
        TestSetDTO updated = testSetService.publishTestSet(id, false);
        return ResponseEntity.ok(updated);
    }

    /**
     * Reorder test sets.
     */
    @PostMapping("/test-sets/reorder")
    public ResponseEntity<Void> reorderTestSets(@RequestBody List<Long> orderedIds) {
        logger.info("POST /api/admin/test-sets/reorder - {} items", orderedIds.size());
        testSetService.reorderTestSets(orderedIds);
        return ResponseEntity.ok().build();
    }

    // ==================== TESTS ====================

    /**
     * Get all tests in a test set.
     */
    @GetMapping("/test-sets/{setId}/tests")
    public ResponseEntity<List<TestSummaryDTO>> getTestsBySetId(@PathVariable Long setId) {
        logger.info("GET /api/admin/test-sets/{}/tests", setId);
        List<TestSummaryDTO> tests = testManagementService.getTestsBySetId(setId);
        return ResponseEntity.ok(tests);
    }

    /**
     * Get test by ID with full details.
     */
    @GetMapping("/tests/{id}")
    public ResponseEntity<TestDetailDTO> getTestById(@PathVariable Long id) {
        logger.info("GET /api/admin/tests/{}", id);
        TestDetailDTO test = testManagementService.getTestById(id);
        return ResponseEntity.ok(test);
    }

    /**
     * Get test by set code and test number.
     */
    @GetMapping("/tests/lookup")
    public ResponseEntity<TestDetailDTO> getTestBySetCodeAndNumber(
            @RequestParam String setCode,
            @RequestParam Integer testNumber) {
        logger.info("GET /api/admin/tests/lookup?setCode={}&testNumber={}", setCode, testNumber);
        TestDetailDTO test = testManagementService.getTestBySetCodeAndNumber(setCode, testNumber);
        return ResponseEntity.ok(test);
    }

    /**
     * Create a new test in a test set.
     */
    @PostMapping("/test-sets/{setId}/tests")
    public ResponseEntity<TestSummaryDTO> createTest(
            @PathVariable Long setId,
            @Valid @RequestBody CreateTestRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        logger.info("POST /api/admin/test-sets/{}/tests", setId);
        UUID userId = UUID.fromString(jwt.getSubject());
        TestSummaryDTO created = testManagementService.createTest(setId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update a test.
     */
    @PutMapping("/tests/{id}")
    public ResponseEntity<TestSummaryDTO> updateTest(
            @PathVariable Long id,
            @Valid @RequestBody CreateTestRequest request) {
        logger.info("PUT /api/admin/tests/{}", id);
        TestSummaryDTO updated = testManagementService.updateTest(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a test.
     */
    @DeleteMapping("/tests/{id}")
    public ResponseEntity<Void> deleteTest(@PathVariable Long id) {
        logger.info("DELETE /api/admin/tests/{}", id);
        testManagementService.deleteTest(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Publish a test.
     */
    @PostMapping("/tests/{id}/publish")
    public ResponseEntity<TestSummaryDTO> publishTest(@PathVariable Long id) {
        logger.info("POST /api/admin/tests/{}/publish", id);
        TestSummaryDTO updated = testManagementService.publishTest(id, true);
        return ResponseEntity.ok(updated);
    }

    /**
     * Unpublish a test.
     */
    @PostMapping("/tests/{id}/unpublish")
    public ResponseEntity<TestSummaryDTO> unpublishTest(@PathVariable Long id) {
        logger.info("POST /api/admin/tests/{}/unpublish", id);
        TestSummaryDTO updated = testManagementService.publishTest(id, false);
        return ResponseEntity.ok(updated);
    }

    /**
     * Update hashtags for a test.
     */
    @PutMapping("/tests/{id}/hashtags")
    public ResponseEntity<TestSummaryDTO> updateTestHashtags(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTestHashtagsRequest request) {
        logger.info("PUT /api/admin/tests/{}/hashtags", id);
        TestSummaryDTO updated = testManagementService.updateTestHashtags(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Duplicate a test.
     */
    @PostMapping("/tests/{id}/duplicate")
    public ResponseEntity<TestSummaryDTO> duplicateTest(
            @PathVariable Long id,
            @RequestParam Integer newTestNumber) {
        logger.info("POST /api/admin/tests/{}/duplicate?newTestNumber={}", id, newTestNumber);
        TestSummaryDTO duplicated = testManagementService.duplicateTest(id, newTestNumber);
        return ResponseEntity.status(HttpStatus.CREATED).body(duplicated);
    }

    /**
     * Get sections for a test and skill.
     */
    @GetMapping("/tests/{id}/sections")
    public ResponseEntity<List<Section>> getTestSections(
            @PathVariable Long id,
            @RequestParam String skill) {
        logger.info("GET /api/admin/tests/{}/sections?skill={}", id, skill);
        List<Section> sections = sectionService.getSectionsByTestIdAndSkill(id, skill);
        return ResponseEntity.ok(sections);
    }

    // ==================== HASHTAGS ====================

    /**
     * Get all active hashtags.
     */
    @GetMapping("/hashtags")
    public ResponseEntity<List<HashtagDTO>> getAllHashtags() {
        logger.info("GET /api/admin/hashtags");
        List<HashtagDTO> hashtags = hashtagService.getAllHashtags();
        return ResponseEntity.ok(hashtags);
    }

    /**
     * Get hashtags by category.
     */
    @GetMapping("/hashtags/category/{category}")
    public ResponseEntity<List<HashtagDTO>> getHashtagsByCategory(@PathVariable String category) {
        logger.info("GET /api/admin/hashtags/category/{}", category);
        List<HashtagDTO> hashtags = hashtagService.getHashtagsByCategory(category);
        return ResponseEntity.ok(hashtags);
    }

    /**
     * Search hashtags.
     */
    @GetMapping("/hashtags/search")
    public ResponseEntity<List<HashtagDTO>> searchHashtags(@RequestParam String q) {
        logger.info("GET /api/admin/hashtags/search?q={}", q);
        List<HashtagDTO> hashtags = hashtagService.searchHashtags(q);
        return ResponseEntity.ok(hashtags);
    }

    /**
     * Get popular hashtags.
     */
    @GetMapping("/hashtags/popular")
    public ResponseEntity<List<HashtagDTO>> getPopularHashtags(
            @RequestParam(defaultValue = "10") int limit) {
        logger.info("GET /api/admin/hashtags/popular?limit={}", limit);
        List<HashtagDTO> hashtags = hashtagService.getPopularHashtags(limit);
        return ResponseEntity.ok(hashtags);
    }

    /**
     * Get distinct hashtag categories.
     */
    @GetMapping("/hashtags/categories")
    public ResponseEntity<List<String>> getHashtagCategories() {
        logger.info("GET /api/admin/hashtags/categories");
        List<String> categories = hashtagService.getDistinctCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * Create a new hashtag.
     */
    @PostMapping("/hashtags")
    public ResponseEntity<HashtagDTO> createHashtag(@Valid @RequestBody CreateHashtagRequest request) {
        logger.info("POST /api/admin/hashtags - code: {}", request.getCode());
        HashtagDTO created = hashtagService.createHashtag(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update a hashtag.
     */
    @PutMapping("/hashtags/{id}")
    public ResponseEntity<HashtagDTO> updateHashtag(
            @PathVariable Long id,
            @Valid @RequestBody CreateHashtagRequest request) {
        logger.info("PUT /api/admin/hashtags/{}", id);
        HashtagDTO updated = hashtagService.updateHashtag(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a hashtag (soft delete).
     */
    @DeleteMapping("/hashtags/{id}")
    public ResponseEntity<Void> deleteHashtag(@PathVariable Long id) {
        logger.info("DELETE /api/admin/hashtags/{}", id);
        hashtagService.deleteHashtag(id);
        return ResponseEntity.noContent().build();
    }
}
