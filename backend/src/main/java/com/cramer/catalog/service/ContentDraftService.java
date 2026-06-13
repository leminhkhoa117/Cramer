package com.cramer.catalog.service;

import com.cramer.catalog.domain.Question;
import com.cramer.catalog.domain.Section;
import com.cramer.catalog.domain.SectionStatus;
import com.cramer.catalog.domain.Test;
import com.cramer.catalog.domain.TestSet;
import com.cramer.catalog.repository.QuestionRepository;
import com.cramer.catalog.repository.SectionRepository;
import com.cramer.catalog.repository.TestRepository;
import com.cramer.catalog.repository.TestSetRepository;
import com.cramer.platform.common.ielts.QuestionType;
import com.cramer.platform.common.ielts.Skill;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements {@link ContentDraftPort} (SPEC-24 §4): persists generated content into the catalog
 * hierarchy as <strong>draft</strong>. Resolves/creates the set + test, then for each section
 * upserts by {@code (test_id, skill, part_number)} and replaces its questions.
 *
 * <p>Draft discipline (SPEC-24 §4.3): new sets/tests are unpublished; sections are {@code DRAFT};
 * saving never publishes. {@code question_uid = {setCode}_{testNumber}_{skillInitial}_q{number}}.
 */
@Service
@Transactional
public class ContentDraftService implements ContentDraftPort {

    private static final String DEFAULT_SET_CODE = "ai_generated";

    private final TestSetRepository testSets;
    private final TestRepository tests;
    private final SectionRepository sections;
    private final QuestionRepository questions;

    public ContentDraftService(TestSetRepository testSets, TestRepository tests,
                               SectionRepository sections, QuestionRepository questions) {
        this.testSets = testSets;
        this.tests = tests;
        this.sections = sections;
        this.questions = questions;
    }

    @Override
    public SaveDraftResult saveDraft(SaveDraftCommand cmd) {
        TestSet set = resolveSet(cmd);
        Test test = resolveTest(cmd, set);

        List<Long> sectionIds = new ArrayList<>();
        int questionCount = 0;
        for (DraftSection ds : cmd.sections()) {
            Section section = sections.findByTestIdAndSkillAndPartNumber(test.getId(), ds.skill(), ds.partNumber())
                    .orElseGet(Section::new);
            section.setTestId(test.getId());
            section.setSkill(ds.skill());
            section.setPartNumber(ds.partNumber());
            section.setPassageText(ds.passageText());
            section.setAudioUrl(ds.audioUrl());
            section.setSectionLayout(ds.sectionLayout());
            section.setImageDescription(ds.imageDescription());
            section.setDisplayContentUrl(ds.displayContentUrl());
            section.setStatus(SectionStatus.DRAFT); // never publish on save
            Section saved = sections.save(section);
            sectionIds.add(saved.getId());

            // Replace this section's questions.
            questions.deleteBySectionId(saved.getId());
            for (DraftQuestion dq : ds.questions()) {
                Question q = new Question();
                q.setSectionId(saved.getId());
                q.setQuestionNumber(dq.questionNumber());
                q.setQuestionUid(uid(set.getCode(), test.getTestNumber(), ds.skill(), dq.questionNumber()));
                q.setQuestionType(parseType(dq.questionType()));
                q.setQuestionContent(dq.questionContent());
                q.setCorrectAnswer(dq.correctAnswer());
                q.setExplanation(dq.explanation());
                q.setWordLimit(dq.wordLimit());
                q.setImageUrl(dq.imageUrl());
                questions.save(q);
                questionCount++;
            }
        }
        return new SaveDraftResult(set.getId(), set.getCode(), test.getId(), test.getTestNumber(),
                sectionIds, questionCount);
    }

    private TestSet resolveSet(SaveDraftCommand cmd) {
        if (cmd.setId() != null) {
            return testSets.findById(cmd.setId())
                    .orElseThrow(() -> com.cramer.platform.error.ResourceNotFoundException.of("TestSet", cmd.setId()));
        }
        String code = (cmd.setCode() == null || cmd.setCode().isBlank()) ? DEFAULT_SET_CODE : cmd.setCode().trim();
        return testSets.findByCode(code).orElseGet(() -> {
            TestSet s = new TestSet();
            s.setCode(code);
            s.setName(code.equals(DEFAULT_SET_CODE) ? "AI Generated" : code);
            s.setSourceType("ai_generated");
            s.setIsPublished(false); // draft discipline
            s.setDisplayOrder(0);
            return testSets.save(s);
        });
    }

    private Test resolveTest(SaveDraftCommand cmd, TestSet set) {
        if (cmd.testId() != null) {
            return tests.findById(cmd.testId())
                    .orElseThrow(() -> com.cramer.platform.error.ResourceNotFoundException.of("Test", cmd.testId()));
        }
        int number = cmd.testNumber() != null ? cmd.testNumber() : tests.maxTestNumber(set.getId()) + 1;
        return tests.findBySetIdAndTestNumber(set.getId(), number).orElseGet(() -> {
            Test t = new Test();
            t.setSetId(set.getId());
            t.setTestNumber(number);
            t.setName("AI Test " + number);
            t.setIsPublished(false); // draft discipline
            t.setIsAiGenerated(true);
            t.setGenerationMetadata(cmd.generationMetadata());
            return tests.save(t);
        });
    }

    private static String uid(String setCode, int testNumber, Skill skill, int questionNumber) {
        return setCode + "_" + testNumber + "_" + Character.toLowerCase(skill.name().charAt(0)) + "_q" + questionNumber;
    }

    private static QuestionType parseType(String raw) {
        try {
            return QuestionType.from(raw);
        } catch (Exception e) {
            return null;
        }
    }
}
