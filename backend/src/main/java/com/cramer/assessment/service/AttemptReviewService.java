package com.cramer.assessment.service;

import com.cramer.assessment.domain.Attempt;
import com.cramer.assessment.domain.AttemptStatus;
import com.cramer.assessment.domain.UserAnswer;
import com.cramer.assessment.repository.AttemptRepository;
import com.cramer.assessment.repository.UserAnswerRepository;
import com.cramer.assessment.web.dto.AttemptReviewView;
import com.cramer.assessment.web.dto.QuestionReviewView;
import com.cramer.assessment.web.dto.SectionReviewView;
import com.cramer.catalog.service.ContentLookupPort;
import com.cramer.catalog.service.ReviewQuestion;
import com.cramer.catalog.service.ReviewSection;
import com.cramer.platform.common.ielts.BandScale;
import com.cramer.platform.common.ielts.Skill;
import com.cramer.platform.error.OperationNotAllowedException;
import com.cramer.platform.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds the owner-only attempt review (SPEC-12 §5): attempt metadata + score overlaid with the
 * authored content (passage, correct answers, explanations) from {@link ContentLookupPort}. This
 * is the only user-facing surface that exposes answer keys (SPEC-04 §3).
 */
@Service
@Transactional(readOnly = true)
public class AttemptReviewService {

    private final AttemptRepository attempts;
    private final UserAnswerRepository answers;
    private final ContentLookupPort content;

    public AttemptReviewService(AttemptRepository attempts, UserAnswerRepository answers, ContentLookupPort content) {
        this.attempts = attempts;
        this.answers = answers;
        this.content = content;
    }

    public AttemptReviewView review(Long attemptId, UUID userId) {
        Attempt a = attempts.findById(attemptId)
                .orElseThrow(() -> ResourceNotFoundException.of("Attempt", attemptId));
        if (!a.getUserId().equals(userId)) {
            throw new OperationNotAllowedException("Attempt does not belong to the current user");
        }

        Map<Long, UserAnswer> byQuestion = new HashMap<>();
        for (UserAnswer ua : answers.findByAttemptIdOrderByQuestionIdAsc(attemptId)) {
            byQuestion.put(ua.getQuestionId(), ua);
        }

        Skill skill = Skill.from(a.getSkill());
        List<ReviewSection> sections = content.reviewContent(a.getExamSource(), parseTestNumber(a), skill);

        List<SectionReviewView> sectionViews = new ArrayList<>();
        List<QuestionReviewView> flat = new ArrayList<>();
        int total = 0;
        for (ReviewSection s : sections) {
            List<QuestionReviewView> qViews = new ArrayList<>();
            for (ReviewQuestion q : s.questions()) {
                UserAnswer ua = byQuestion.get(q.questionId());
                QuestionReviewView qv = new QuestionReviewView(
                        q.questionId(), q.questionNumber(), q.questionUid(),
                        q.questionType() == null ? null : q.questionType().name(),
                        q.questionContent(),
                        ua == null ? null : ua.getUserAnswer(),
                        q.correctAnswer(),
                        ua == null ? null : ua.getIsCorrect(),
                        q.explanation());
                qViews.add(qv);
                flat.add(qv);
                total++;
            }
            sectionViews.add(new SectionReviewView(
                    s.sectionId(), s.partNumber(), s.passageText(), s.audioUrl(),
                    s.displayContentUrl(), s.sectionLayout(), s.imageDescription(), qViews));
        }

        Double band = (skill == Skill.READING || skill == Skill.LISTENING) && a.getScore() != null
                ? BandScale.bandFor(a.getScore()) : null;
        Long duration = (a.getStartedAt() != null && a.getCompletedAt() != null)
                ? Duration.between(a.getStartedAt(), a.getCompletedAt()).getSeconds() : null;

        return new AttemptReviewView(
                a.getId(), a.getExamSource(), a.getTestNumber(), a.getSkill(),
                a.getStatus() == null ? null : a.getStatus().name(),
                a.getScore(), total, band, a.getStartedAt(), a.getCompletedAt(), duration, flat, sectionViews);
    }

    private int parseTestNumber(Attempt a) {
        try {
            return Integer.parseInt(a.getTestNumber().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Non-numeric test number: " + a.getTestNumber());
        }
    }
}
