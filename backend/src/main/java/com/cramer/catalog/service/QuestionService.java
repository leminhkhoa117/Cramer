package com.cramer.catalog.service;

import com.cramer.catalog.domain.Question;
import com.cramer.platform.common.ielts.QuestionType;
import com.cramer.catalog.repository.QuestionRepository;
import com.cramer.catalog.web.dto.QuestionAdminView;
import com.cramer.catalog.web.dto.QuestionRequest;
import com.cramer.platform.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Admin question CRUD (SPEC-11 §4). Admin views include the answer key + explanation; delivery
 * never does (SPEC-04 §3).
 */
@Service
@Transactional
public class QuestionService {

    private final QuestionRepository questions;

    public QuestionService(QuestionRepository questions) {
        this.questions = questions;
    }

    @Transactional(readOnly = true)
    public QuestionAdminView get(Long id) {
        return QuestionAdminView.of(load(id));
    }

    @Transactional(readOnly = true)
    public List<QuestionAdminView> listBySection(Long sectionId) {
        return questions.findBySectionIdOrderByQuestionNumberAsc(sectionId).stream()
                .map(QuestionAdminView::of).toList();
    }

    public QuestionAdminView create(QuestionRequest req) {
        Question q = new Question();
        apply(q, req);
        return QuestionAdminView.of(questions.save(q));
    }

    public QuestionAdminView update(Long id, QuestionRequest req) {
        Question q = load(id);
        apply(q, req);
        return QuestionAdminView.of(questions.save(q));
    }

    public void delete(Long id) {
        questions.delete(load(id));
    }

    private void apply(Question q, QuestionRequest req) {
        q.setSectionId(req.sectionId());
        q.setQuestionNumber(req.questionNumber());
        q.setQuestionUid(req.questionUid());
        q.setQuestionType(QuestionType.from(req.questionType()));
        q.setQuestionContent(req.questionContent());
        q.setCorrectAnswer(req.correctAnswer());
        q.setExplanation(req.explanation());
        q.setImageUrl(req.imageUrl());
        q.setWordLimit(req.wordLimit());
    }

    private Question load(Long id) {
        return questions.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Question", id));
    }
}
