package com.cramer.catalog.service;

import com.cramer.catalog.domain.Question;
import com.cramer.catalog.domain.Section;
import com.cramer.catalog.domain.SectionStatus;
import com.cramer.platform.common.ielts.Skill;
import com.cramer.catalog.repository.QuestionRepository;
import com.cramer.catalog.repository.SectionRepository;
import com.cramer.catalog.web.dto.TestQuestionView;
import com.cramer.catalog.web.dto.TestSectionView;
import com.cramer.platform.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Builds answer-free test payloads for the test-taking UI (SPEC-11 §2). The mapping deliberately
 * never reads {@code correct_answer}/{@code explanation}, and the {@code …View} records have no
 * field to carry them — the answer-key safety rule (SPEC-04 §3) is enforced by construction.
 */
@Service
public class TestDeliveryService {

    private final SectionRepository sections;
    private final QuestionRepository questions;

    public TestDeliveryService(SectionRepository sections, QuestionRepository questions) {
        this.sections = sections;
        this.questions = questions;
    }

    @Transactional(readOnly = true)
    public List<TestSectionView> getTestData(String examSource, int testNumber, Skill skill) {
        List<Section> secs = sections.findByExamSourceAndTestNumberAndSkillAndStatusOrderByPartNumberAsc(
                examSource, testNumber, skill, SectionStatus.PUBLISHED);
        if (secs.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No published content for source=" + examSource + " test=" + testNumber + " skill=" + skill);
        }
        return secs.stream().map(this::toSectionView).toList();
    }

    private TestSectionView toSectionView(Section s) {
        List<TestQuestionView> views = questions.findBySectionIdOrderByQuestionNumberAsc(s.getId())
                .stream().map(TestDeliveryService::toQuestionView).toList();
        return new TestSectionView(
                s.getId(),
                s.getTestId(),
                s.getSkill() == null ? null : s.getSkill().dbValue(),
                s.getPartNumber(),
                s.getPassageText(),
                s.getAudioUrl(),
                s.getSectionLayout(),
                s.getDisplayContentUrl(),
                s.getImageDescription(),
                views);
    }

    /** Maps to the answer-free view; {@code correctAnswer}/{@code explanation} are never accessed. */
    private static TestQuestionView toQuestionView(Question q) {
        return new TestQuestionView(
                q.getId(),
                q.getQuestionNumber(),
                q.getQuestionUid(),
                q.getQuestionType() == null ? null : q.getQuestionType().name(),
                q.getQuestionContent(),
                q.getImageUrl(),
                q.getWordLimit());
    }
}
