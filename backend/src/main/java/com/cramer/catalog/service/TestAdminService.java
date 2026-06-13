package com.cramer.catalog.service;

import com.cramer.catalog.domain.Difficulty;
import com.cramer.catalog.domain.SectionStatus;
import com.cramer.platform.common.ielts.Skill;
import com.cramer.catalog.domain.Test;
import com.cramer.catalog.domain.TestHashtag;
import com.cramer.catalog.domain.TestHashtagId;
import com.cramer.catalog.repository.SectionRepository;
import com.cramer.catalog.repository.TestHashtagRepository;
import com.cramer.catalog.repository.TestRepository;
import com.cramer.catalog.repository.TestSetRepository;
import com.cramer.catalog.web.dto.CreateTestRequest;
import com.cramer.catalog.web.dto.SectionAdminView;
import com.cramer.catalog.web.dto.TestView;
import com.cramer.catalog.web.dto.UpdateTestRequest;
import com.cramer.platform.error.OperationNotAllowedException;
import com.cramer.platform.error.ResourceAlreadyExistsException;
import com.cramer.platform.error.ResourceNotFoundException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Admin operations on tests (SPEC-11 §4): create (auto {@code test_number = max+1}), update,
 * publish/unpublish with section cascade, duplicate, hashtag replacement, and guarded delete.
 * No placeholder-section side effects (the old surprising behavior is removed, SPEC-11 §4).
 */
@Service
@Transactional
public class TestAdminService {

    private final TestRepository tests;
    private final SectionRepository sections;
    private final TestHashtagRepository testHashtags;
    private final HashtagService hashtagService;
    private final TestSetRepository testSets;
    private final ObjectProvider<TestDependencyGuard> dependencyGuard;

    public TestAdminService(TestRepository tests,
                            SectionRepository sections,
                            TestHashtagRepository testHashtags,
                            HashtagService hashtagService,
                            TestSetRepository testSets,
                            ObjectProvider<TestDependencyGuard> dependencyGuard) {
        this.tests = tests;
        this.sections = sections;
        this.testHashtags = testHashtags;
        this.hashtagService = hashtagService;
        this.testSets = testSets;
        this.dependencyGuard = dependencyGuard;
    }

    @Transactional(readOnly = true)
    public List<TestView> listBySet(Long setId) {
        return tests.findBySetIdOrderByTestNumberAsc(setId).stream().map(TestView::of).toList();
    }

    @Transactional(readOnly = true)
    public TestView getById(Long id) {
        return TestView.of(load(id), hashtagCodes(id));
    }

    @Transactional(readOnly = true)
    public TestView lookup(String setCode, int testNumber) {
        long setId = testSets.findByCode(setCode)
                .orElseThrow(() -> ResourceNotFoundException.of("TestSet", setCode))
                .getId();
        Test t = tests.findBySetIdAndTestNumber(setId, testNumber)
                .orElseThrow(() -> ResourceNotFoundException.of("Test", setCode + "#" + testNumber));
        return TestView.of(t, hashtagCodes(t.getId()));
    }

    public TestView create(Long setId, CreateTestRequest req) {
        if (!testSets.existsById(setId)) {
            throw ResourceNotFoundException.of("TestSet", setId);
        }
        int testNumber = req.testNumber() != null ? req.testNumber() : tests.maxTestNumber(setId) + 1;
        if (tests.findBySetIdAndTestNumber(setId, testNumber).isPresent()) {
            throw new ResourceAlreadyExistsException("Test number already exists in set: " + testNumber);
        }
        Test t = new Test();
        t.setSetId(setId);
        t.setTestNumber(testNumber);
        t.setName(req.name());
        t.setDescription(req.description());
        t.setDifficulty(Difficulty.fromOrDefault(req.difficulty()));
        t.setEstimatedTimeMinutes(req.estimatedTimeMinutes() != null ? req.estimatedTimeMinutes() : 170);
        t.setIsPublished(req.isPublished() != null ? req.isPublished() : false);
        t.setIsAiGenerated(false);
        return TestView.of(tests.save(t));
    }

    public TestView update(Long id, UpdateTestRequest req) {
        Test t = load(id);
        if (req.testNumber() != null && !req.testNumber().equals(t.getTestNumber())) {
            if (tests.findBySetIdAndTestNumber(t.getSetId(), req.testNumber()).isPresent()) {
                throw new ResourceAlreadyExistsException("Test number already exists in set: " + req.testNumber());
            }
            t.setTestNumber(req.testNumber());
        }
        if (req.name() != null) {
            t.setName(req.name());
        }
        if (req.description() != null) {
            t.setDescription(req.description());
        }
        if (req.difficulty() != null) {
            t.setDifficulty(Difficulty.fromOrDefault(req.difficulty()));
        }
        if (req.estimatedTimeMinutes() != null) {
            t.setEstimatedTimeMinutes(req.estimatedTimeMinutes());
        }
        if (req.isPublished() != null) {
            t.setIsPublished(req.isPublished());
        }
        return TestView.of(tests.save(t), hashtagCodes(id));
    }

    /** Publish/unpublish a test and cascade its sections' status (SPEC-11 §4.1). */
    public TestView setPublished(Long id, boolean publish) {
        Test t = load(id);
        t.setIsPublished(publish);
        tests.save(t);
        SectionStatus status = publish ? SectionStatus.PUBLISHED : SectionStatus.DRAFT;
        sections.updateStatusByTestId(id, status); // FK path covers all live sections
        return TestView.of(t, hashtagCodes(id));
    }

    /** Copy test metadata + hashtags into a new unpublished test (SPEC-11 §4.1). Sections are not
     * copied unless {@code includeSections} is set (default off). */
    public TestView duplicate(Long id, boolean includeSections) {
        Test src = load(id);
        Test copy = new Test();
        copy.setSetId(src.getSetId());
        copy.setTestNumber(tests.maxTestNumber(src.getSetId()) + 1);
        copy.setName(src.getName() == null ? null : src.getName() + " (copy)");
        copy.setDescription(src.getDescription());
        copy.setDifficulty(src.getDifficulty());
        copy.setEstimatedTimeMinutes(src.getEstimatedTimeMinutes());
        copy.setIsPublished(false);
        copy.setIsAiGenerated(src.getIsAiGenerated());
        Test saved = tests.save(copy);
        // copy hashtags
        for (TestHashtag th : testHashtags.findByIdTestId(id)) {
            TestHashtag c = new TestHashtag();
            c.setId(new TestHashtagId(saved.getId(), th.getId().getHashtagId()));
            c.setIsPrimary(th.getIsPrimary());
            testHashtags.save(c);
        }
        if (includeSections) {
            throw new OperationNotAllowedException("Section duplication is not yet supported");
        }
        return TestView.of(saved, hashtagCodes(saved.getId()));
    }

    /** Replace the test's hashtag set with the given codes (find-or-create), max 20 (SPEC-11 §4.1). */
    public TestView replaceHashtags(Long id, List<String> codes) {
        load(id);
        List<com.cramer.catalog.domain.Hashtag> resolved = hashtagService.findOrCreateByCodes(codes);
        testHashtags.deleteByIdTestId(id);
        boolean first = true;
        for (com.cramer.catalog.domain.Hashtag h : resolved) {
            TestHashtag th = new TestHashtag();
            th.setId(new TestHashtagId(id, h.getId()));
            th.setIsPrimary(first);
            testHashtags.save(th);
            first = false;
        }
        return TestView.of(load(id), resolved.stream().map(com.cramer.catalog.domain.Hashtag::getCode).toList());
    }

    /** Delete a test; blocked with 409 if it has user data unless {@code force} (SPEC-11 §4.1). */
    public void delete(Long id, boolean force) {
        Test t = load(id);
        if (!force) {
            TestDependencyGuard guard = dependencyGuard.getIfAvailable();
            if (guard != null && guard.hasUserData(id)) {
                throw new OperationNotAllowedException(
                        "Test has user attempts/answers; delete blocked. Pass force=true to override.");
            }
        }
        // remove catalog-owned dependents
        for (var s : sections.findByTestId(id)) {
            sections.deleteById(s.getId());
        }
        testHashtags.deleteByIdTestId(id);
        tests.delete(t);
    }

    @Transactional(readOnly = true)
    public List<SectionAdminView> sectionsForTest(Long id, String skill) {
        load(id);
        var all = sections.findByTestIdOrderByPartNumberAsc(id);
        if (skill != null && !skill.isBlank()) {
            Skill s = Skill.from(skill);
            all = all.stream().filter(sec -> sec.getSkill() == s).toList();
        }
        return all.stream().map(SectionAdminView::of).toList();
    }

    private List<String> hashtagCodes(Long testId) {
        List<Long> ids = testHashtags.findByIdTestId(testId).stream()
                .map(th -> th.getId().getHashtagId())
                .toList();
        return hashtagService.codesByIds(ids);
    }

    private Test load(Long id) {
        return tests.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Test", id));
    }
}
