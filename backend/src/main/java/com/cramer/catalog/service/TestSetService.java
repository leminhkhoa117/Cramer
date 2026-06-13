package com.cramer.catalog.service;

import com.cramer.catalog.domain.TestSet;
import com.cramer.catalog.repository.TestRepository;
import com.cramer.catalog.repository.TestSetRepository;
import com.cramer.catalog.web.dto.CreateTestSetRequest;
import com.cramer.catalog.web.dto.TestSetView;
import com.cramer.platform.error.ResourceAlreadyExistsException;
import com.cramer.platform.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Admin CRUD + publish + reorder for test sets (SPEC-11 §4). One typed surface — no raw-SQL CMS,
 * no {@code 200 {success:false}} (SPEC-11 §4, SPEC-04 §2).
 */
@Service
@Transactional
public class TestSetService {

    private final TestSetRepository testSets;
    private final TestRepository tests;

    public TestSetService(TestSetRepository testSets, TestRepository tests) {
        this.testSets = testSets;
        this.tests = tests;
    }

    @Transactional(readOnly = true)
    public List<TestSetView> listAll() {
        return testSets.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .map(s -> TestSetView.of(s, tests.countBySetId(s.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public TestSetView getById(Long id) {
        return TestSetView.of(load(id), tests.countBySetId(id));
    }

    @Transactional(readOnly = true)
    public TestSetView getByCode(String code) {
        TestSet s = testSets.findByCode(code)
                .orElseThrow(() -> ResourceNotFoundException.of("TestSet", code));
        return TestSetView.of(s, tests.countBySetId(s.getId()));
    }

    public TestSetView create(CreateTestSetRequest req, UUID userId) {
        if (testSets.existsByCode(req.code())) {
            throw new ResourceAlreadyExistsException("TestSet code already exists: " + req.code());
        }
        TestSet s = new TestSet();
        s.setCode(req.code());
        s.setName(req.name());
        s.setDescription(req.description());
        s.setCoverImageUrl(req.coverImageUrl());
        s.setSourceType(req.sourceType() != null ? req.sourceType() : "custom");
        s.setIsPublished(req.isPublished() != null ? req.isPublished() : false);
        s.setDisplayOrder(req.displayOrder() != null ? req.displayOrder() : nextDisplayOrder());
        s.setCreatedBy(userId);
        return TestSetView.of(testSets.save(s), 0L);
    }

    public TestSetView update(Long id, CreateTestSetRequest req) {
        TestSet s = load(id);
        if (!s.getCode().equals(req.code()) && testSets.existsByCode(req.code())) {
            throw new ResourceAlreadyExistsException("TestSet code already exists: " + req.code());
        }
        s.setCode(req.code());
        s.setName(req.name());
        s.setDescription(req.description());
        s.setCoverImageUrl(req.coverImageUrl());
        if (req.sourceType() != null) {
            s.setSourceType(req.sourceType());
        }
        if (req.isPublished() != null) {
            s.setIsPublished(req.isPublished());
        }
        if (req.displayOrder() != null) {
            s.setDisplayOrder(req.displayOrder());
        }
        return TestSetView.of(testSets.save(s), tests.countBySetId(id));
    }

    public void delete(Long id) {
        TestSet s = load(id);
        testSets.delete(s);
    }

    public TestSetView publish(Long id, boolean publish) {
        TestSet s = load(id);
        s.setIsPublished(publish);
        return TestSetView.of(testSets.save(s), tests.countBySetId(id));
    }

    public void reorder(List<Long> orderedIds) {
        int order = 0;
        for (Long id : orderedIds) {
            TestSet s = load(id);
            s.setDisplayOrder(order++);
            testSets.save(s);
        }
    }

    private int nextDisplayOrder() {
        return testSets.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .map(TestSet::getDisplayOrder)
                .filter(o -> o != null)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(-1) + 1;
    }

    private TestSet load(Long id) {
        return testSets.findById(id).orElseThrow(() -> ResourceNotFoundException.of("TestSet", id));
    }
}
