package com.cramer.catalog.service;

import com.cramer.catalog.domain.Section;
import com.cramer.catalog.domain.SectionStatus;
import com.cramer.platform.common.ielts.Skill;
import com.cramer.catalog.repository.QuestionRepository;
import com.cramer.catalog.repository.SectionRepository;
import com.cramer.catalog.web.dto.SectionAdminView;
import com.cramer.catalog.web.dto.SectionRequest;
import com.cramer.platform.config.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import com.cramer.platform.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin section CRUD (SPEC-11 §4). Admin-gated; replaces the old generic, answer-leaking
 * section endpoints (SPEC-11 §2). Deleting a section removes its questions.
 */
@Service
@Transactional
public class SectionService {

    private final SectionRepository sections;
    private final QuestionRepository questions;

    public SectionService(SectionRepository sections, QuestionRepository questions) {
        this.sections = sections;
        this.questions = questions;
    }

    @Transactional(readOnly = true)
    public SectionAdminView get(Long id) {
        return SectionAdminView.of(load(id));
    }

        @CacheEvict(value = CacheConfig.CACHE_COURSES, allEntries = true)
    public SectionAdminView create(SectionRequest req) {
        Section s = new Section();
        apply(s, req);
        return SectionAdminView.of(sections.save(s));
    }

        @CacheEvict(value = CacheConfig.CACHE_COURSES, allEntries = true)
    public SectionAdminView update(Long id, SectionRequest req) {
        Section s = load(id);
        apply(s, req);
        return SectionAdminView.of(sections.save(s));
    }

        @CacheEvict(value = CacheConfig.CACHE_COURSES, allEntries = true)
    public void delete(Long id) {
        Section s = load(id);
        questions.deleteBySectionId(id);
        sections.delete(s);
    }

    private void apply(Section s, SectionRequest req) {
        s.setTestId(req.testId());
        s.setExamSource(req.examSource());
        s.setTestNumber(req.testNumber());
        s.setSkill(Skill.from(req.skill()));
        s.setPartNumber(req.partNumber());
        s.setPassageText(req.passageText());
        s.setAudioUrl(req.audioUrl());
        s.setSectionLayout(req.sectionLayout());
        s.setImageDescription(req.imageDescription());
        s.setDisplayContentUrl(req.displayContentUrl());
        s.setStatus(req.status() != null && !req.status().isBlank()
                ? SectionStatus.valueOf(req.status().trim().toUpperCase())
                : SectionStatus.DRAFT);
    }

    private Section load(Long id) {
        return sections.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Section", id));
    }
}
