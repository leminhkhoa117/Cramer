package com.cramer.catalog.service;

import com.cramer.catalog.domain.Section;
import com.cramer.catalog.domain.SectionStatus;
import com.cramer.platform.common.ielts.Skill;
import com.cramer.catalog.repository.QuestionRepository;
import com.cramer.catalog.repository.SectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implements the published {@link ContentLookupPort} (SPEC-11 §5) over catalog repositories.
 * Only this class (inside catalog) touches the entities; consumers see records only.
 */
@Service
public class ContentLookupService implements ContentLookupPort {

    private final SectionRepository sections;
    private final QuestionRepository questions;

    public ContentLookupService(SectionRepository sections, QuestionRepository questions) {
        this.sections = sections;
        this.questions = questions;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectionRef> sectionsForTest(long testId, Skill skill) {
        return sections.findByTestIdAndSkillAndStatusOrderByPartNumberAsc(testId, skill, SectionStatus.PUBLISHED)
                .stream().map(this::toRef).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectionRef> sectionsForExam(String examSource, int testNumber, Skill skill) {
        return sections.findByExamSourceAndTestNumberAndSkillAndStatusOrderByPartNumberAsc(
                        examSource, testNumber, skill, SectionStatus.PUBLISHED)
                .stream().map(this::toRef).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradableQuestion> gradableQuestions(long sectionId) {
        return questions.findBySectionIdOrderByQuestionNumberAsc(sectionId)
                .stream()
                .map(q -> new GradableQuestion(q.getId(), q.getQuestionNumber(), q.getQuestionType(), q.getCorrectAnswer()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewSection> reviewContent(String examSource, int testNumber, Skill skill) {
        return sections.findByExamSourceAndTestNumberAndSkillAndStatusOrderByPartNumberAsc(
                        examSource, testNumber, skill, SectionStatus.PUBLISHED)
                .stream()
                .map(s -> new ReviewSection(
                        s.getId(), s.getPartNumber(), s.getPassageText(), s.getAudioUrl(),
                        s.getDisplayContentUrl(), s.getSectionLayout(), s.getImageDescription(),
                        questions.findBySectionIdOrderByQuestionNumberAsc(s.getId()).stream()
                                .map(q -> new ReviewQuestion(q.getId(), q.getQuestionNumber(), q.getQuestionUid(),
                                        q.getQuestionType(), q.getQuestionContent(), q.getCorrectAnswer(),
                                        q.getExplanation()))
                                .toList()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public int totalQuestions(String examSource, int testNumber, Skill skill) {
        return sections.findByExamSourceAndTestNumberAndSkillAndStatusOrderByPartNumberAsc(
                        examSource, testNumber, skill, SectionStatus.PUBLISHED)
                .stream()
                .mapToInt(s -> questions.findBySectionIdOrderByQuestionNumberAsc(s.getId()).size())
                .sum();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpeakingQuestionRef> speakingBank(long testId, int partNumber) {
        return sections.findByTestIdAndSkillAndStatusOrderByPartNumberAsc(testId, Skill.SPEAKING, SectionStatus.PUBLISHED)
                .stream()
                .filter(s -> partNumber <= 0 || (s.getPartNumber() != null && s.getPartNumber() == partNumber))
                .flatMap(s -> questions.findBySectionIdOrderByQuestionNumberAsc(s.getId()).stream()
                        .map(q -> new SpeakingQuestionRef(q.getId(), s.getPartNumber(), q.getQuestionUid(),
                                q.getQuestionContent())))
                .toList();
    }

    private SectionRef toRef(Section s) {
        return new SectionRef(s.getId(), s.getTestId(), s.getExamSource(), s.getTestNumber(), s.getSkill(), s.getPartNumber());
    }
}
