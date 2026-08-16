package com.cramer.catalog.service;

import com.cramer.catalog.domain.TestSet;
import com.cramer.catalog.repository.SectionRepository;
import com.cramer.catalog.repository.TestSetRepository;
import com.cramer.catalog.web.dto.TestSetView;
import com.cramer.platform.config.CacheConfig;
import com.cramer.platform.error.ResourceNotFoundException;
import com.cramer.platform.web.PageResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-only course browsing for end users (SPEC-11 §3). Only published content is visible.
 */
@Service
@Transactional(readOnly = true)
public class CourseQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final SectionRepository sections;
    private final TestSetRepository testSets;

    public CourseQueryService(SectionRepository sections, TestSetRepository testSets) {
        this.sections = sections;
        this.testSets = testSets;
    }

    /** Distinct published exam sources, paged (SPEC-11 §3). */
    public PageResponse<String> listCourses(int page, int size, String search) {
        int capped = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        String term = (search == null || search.isBlank()) ? null : search.trim();
        Page<String> result = sections.findDistinctPublishedExamSources(term, PageRequest.of(Math.max(0, page), capped));
        return new PageResponse<>(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    /** Published test sets, full projection (SPEC-11 §3). */
    @Cacheable(CacheConfig.CACHE_COURSES)
    public List<TestSetView> listPublishedSets() {
        return testSets.findByIsPublishedTrueOrderByDisplayOrderAscIdAsc().stream()
                .map(TestSetView::of).toList();
    }

    /** Published test numbers for an exam source (SPEC-11 §3). */
    @Cacheable(CacheConfig.CACHE_COURSES)
    public List<Integer> testsForCourse(String course) {
        return sections.findDistinctPublishedTestNumbers(course);
    }

    /** Published test-set details by code, else 404 (SPEC-11 §3). */
    @Cacheable(CacheConfig.CACHE_COURSES)
    public TestSetView setDetails(String code) {
        TestSet s = testSets.findByCode(code)
                .filter(ts -> Boolean.TRUE.equals(ts.getIsPublished()))
                .orElseThrow(() -> ResourceNotFoundException.of("Course", code));
        return TestSetView.of(s);
    }
}
