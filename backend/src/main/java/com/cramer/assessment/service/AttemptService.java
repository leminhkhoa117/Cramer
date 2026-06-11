package com.cramer.assessment.service;

import com.cramer.assessment.domain.Attempt;
import com.cramer.assessment.domain.AttemptStatus;
import com.cramer.assessment.domain.UserAnswer;
import com.cramer.assessment.repository.AttemptRepository;
import com.cramer.assessment.repository.UserAnswerRepository;
import com.cramer.assessment.web.dto.AnswerInput;
import com.cramer.assessment.web.dto.AnswerView;
import com.cramer.assessment.web.dto.AttemptResultResponse;
import com.cramer.assessment.web.dto.AttemptView;
import com.cramer.assessment.web.dto.SaveProgressRequest;
import com.cramer.assessment.web.dto.SubmitAnswersRequest;
import com.cramer.billing.service.AttemptBillingPort;
import com.cramer.catalog.service.ContentLookupPort;
import com.cramer.catalog.service.GradableQuestion;
import com.cramer.catalog.service.SectionRef;
import com.cramer.platform.common.ielts.BandScale;
import com.cramer.platform.common.ielts.Skill;
import com.cramer.platform.common.json.Json;
import com.cramer.platform.error.OperationNotAllowedException;
import com.cramer.platform.error.ResourceNotFoundException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Attempt lifecycle for Reading/Listening (and the shared attempt shell used by Writing/Speaking):
 * start/resume with locking + quota, save progress, submit + grade, cancel, regrade, delete
 * (SPEC-12 §3/§4). Scoring is delegated to {@link ScoringService}; content comes from
 * {@link ContentLookupPort}; quota from {@link AttemptBillingPort}.
 */
@Service
@Transactional
public class AttemptService {

    private final AttemptRepository attempts;
    private final UserAnswerRepository answers;
    private final ContentLookupPort content;
    private final ScoringService scoring;
    private final AttemptBillingPort billing;
    private final ObjectProvider<AttemptCleanupParticipant> cleanupParticipants;

    public AttemptService(AttemptRepository attempts,
                          UserAnswerRepository answers,
                          ContentLookupPort content,
                          ScoringService scoring,
                          AttemptBillingPort billing,
                          ObjectProvider<AttemptCleanupParticipant> cleanupParticipants) {
        this.attempts = attempts;
        this.answers = answers;
        this.content = content;
        this.scoring = scoring;
        this.billing = billing;
        this.cleanupParticipants = cleanupParticipants;
    }

    // --- Start / resume (SPEC-12 §3) ---

    public AttemptView start(String source, String testNumber, String skillRaw, UUID userId, boolean forceNew) {
        String src = requireText(source, "source");
        String test = requireText(testNumber, "test");
        Skill skill = Skill.from(skillRaw);
        String skillDb = skill.dbValue();

        List<Attempt> locked = attempts.lockByKey(userId, src, test, skillDb);
        List<Attempt> inProgress = locked.stream()
                .filter(a -> a.getStatus() == AttemptStatus.IN_PROGRESS).toList();

        if (forceNew) {
            inProgress.forEach(this::cancelInternal);
            return AttemptView.of(createNew(src, test, skill, skillDb, userId));
        }
        if (inProgress.size() > 1) {
            // keep the most recent (locked is ordered desc), cancel the rest
            inProgress.stream().skip(1).forEach(this::cancelInternal);
            return AttemptView.of(inProgress.get(0));
        }
        if (inProgress.size() == 1) {
            return AttemptView.of(inProgress.get(0));
        }
        Attempt latest = locked.isEmpty() ? null : locked.get(0);
        if (latest != null && latest.getStatus() == AttemptStatus.COMPLETED) {
            return AttemptView.of(latest); // read-only resume of the result
        }
        return AttemptView.of(createNew(src, test, skill, skillDb, userId));
    }

    private Attempt createNew(String source, String testNumber, Skill skill, String skillDb, UUID userId) {
        Attempt a = new Attempt();
        a.setUserId(userId);
        a.setExamSource(source);
        a.setTestNumber(testNumber);
        a.setSkill(skillDb);
        a.setStatus(AttemptStatus.IN_PROGRESS);
        a = attempts.save(a); // assigns id before charging
        // Reading/Listening charge at start; Writing bills at grading time (SPEC-12 §3, SPEC-13).
        // A 402 here rolls back this attempt insert (same transaction), so no orphan attempt remains.
        if (skill == Skill.READING || skill == Skill.LISTENING) {
            billing.chargeAttemptStart(userId, skill, "attempt_" + a.getId());
        }
        return a;
    }

    // --- Save progress (SPEC-12 §3) ---

    public AttemptView saveProgress(Long attemptId, UUID userId, SaveProgressRequest req) {
        Attempt a = requireOwned(attemptId, userId);
        requireInProgress(a);
        if (req.timeLeft() != null) {
            a.setTimeLeft(req.timeLeft());
        }
        if (req.currentPart() != null) {
            a.setCurrentPart(req.currentPart());
        }
        if (req.answers() != null) {
            answers.deleteByAttemptId(attemptId);
            for (AnswerInput in : req.answers()) {
                if (in.value() != null && !in.value().isBlank()) {
                    answers.save(newAnswer(a, in.questionId(), in.value(), null));
                }
            }
        }
        return AttemptView.of(attempts.save(a));
    }

    // --- Submit + grade (SPEC-12 §4) ---

    public AttemptResultResponse submit(Long attemptId, UUID userId, SubmitAnswersRequest req) {
        Attempt a = requireOwned(attemptId, userId);
        answers.deleteByAttemptId(attemptId);

        Skill skill = Skill.from(a.getSkill());
        Map<Long, GradableQuestion> bank = gradableBank(a, skill);
        int total = content.totalQuestions(a.getExamSource(), parseTestNumber(a), skill);

        int correct = 0;
        for (AnswerInput in : req.answers()) {
            if (in.value() == null || in.value().isBlank()) {
                continue;
            }
            GradableQuestion gq = bank.get(in.questionId());
            boolean isCorrect = gq != null && scoring.isCorrect(gq.questionType(), gq.correctAnswer(), in.value());
            if (isCorrect) {
                correct++;
            }
            answers.save(newAnswer(a, in.questionId(), in.value(), isCorrect));
        }

        a.setScore(correct);
        a.setStatus(AttemptStatus.COMPLETED);
        a.setCompletedAt(OffsetDateTime.now());
        attempts.save(a);

        Double band = isObjective(skill) ? BandScale.bandFor(correct) : null;
        return new AttemptResultResponse(a.getId(), a.getStatus().name(), correct, total, band, a.getCompletedAt());
    }

    // --- Regrade a completed attempt (SPEC-12 §3) ---

    public AttemptResultResponse regrade(Long attemptId, UUID userId) {
        Attempt a = requireOwned(attemptId, userId);
        if (a.getStatus() != AttemptStatus.COMPLETED) {
            throw new IllegalStateException("Only completed attempts can be regraded");
        }
        Skill skill = Skill.from(a.getSkill());
        Map<Long, GradableQuestion> bank = gradableBank(a, skill);
        int total = content.totalQuestions(a.getExamSource(), parseTestNumber(a), skill);

        int correct = 0;
        for (UserAnswer ua : answers.findByAttemptIdOrderByQuestionIdAsc(attemptId)) {
            GradableQuestion gq = bank.get(ua.getQuestionId());
            boolean isCorrect = gq != null && scoring.isCorrect(gq.questionType(), gq.correctAnswer(), ua.getUserAnswer());
            ua.setIsCorrect(isCorrect);
            answers.save(ua);
            if (isCorrect) {
                correct++;
            }
        }
        a.setScore(correct);
        attempts.save(a);
        Double band = isObjective(skill) ? BandScale.bandFor(correct) : null;
        return new AttemptResultResponse(a.getId(), a.getStatus().name(), correct, total, band, a.getCompletedAt());
    }

    // --- Cancel / resume / delete / answers ---

    /** Idempotent cancel (SPEC-12 §3): missing/cancelled/completed → success; in-progress → purge. */
    public void cancel(Long attemptId, UUID userId) {
        Optional<Attempt> found = attempts.findById(attemptId);
        if (found.isEmpty()) {
            return; // idempotent
        }
        Attempt a = found.get();
        requireOwner(a, userId);
        if (a.getStatus() == AttemptStatus.IN_PROGRESS) {
            cancelInternal(a);
        }
    }

    public AttemptView resume(Long attemptId, UUID userId) {
        Attempt a = requireOwned(attemptId, userId);
        requireInProgress(a);
        return AttemptView.of(a);
    }

    public void delete(Long attemptId, UUID userId) {
        Attempt a = requireOwned(attemptId, userId);
        cleanupParticipants.orderedStream().forEach(p -> p.beforeAttemptDeletion(attemptId));
        answers.deleteByAttemptId(attemptId);
        attempts.delete(a);
    }

    @Transactional(readOnly = true)
    public List<AnswerView> getAnswers(Long attemptId, UUID userId) {
        requireOwned(attemptId, userId);
        return answers.findByAttemptIdOrderByQuestionIdAsc(attemptId).stream()
                .map(ua -> new AnswerView(ua.getQuestionId(), ua.getAnswerContent(), ua.getUserAnswer(), ua.getIsCorrect()))
                .toList();
    }

    // --- Helpers ---

    private void cancelInternal(Attempt a) {
        cleanupParticipants.orderedStream().forEach(p -> p.beforeAttemptDeletion(a.getId()));
        answers.deleteByAttemptId(a.getId());
        a.setStatus(AttemptStatus.CANCELLED);
        attempts.save(a);
    }

    private Map<Long, GradableQuestion> gradableBank(Attempt a, Skill skill) {
        Map<Long, GradableQuestion> bank = new HashMap<>();
        for (SectionRef section : content.sectionsForExam(a.getExamSource(), parseTestNumber(a), skill)) {
            for (GradableQuestion gq : content.gradableQuestions(section.sectionId())) {
                bank.put(gq.questionId(), gq);
            }
        }
        return bank;
    }

    private UserAnswer newAnswer(Attempt a, Long questionId, String value, Boolean isCorrect) {
        ObjectNode node = Json.mapper().createObjectNode();
        node.put("value", value);
        UserAnswer ua = new UserAnswer();
        ua.setAttemptId(a.getId());
        ua.setQuestionId(questionId);
        ua.setUserId(a.getUserId());
        ua.setAnswerContent(node);
        ua.setUserAnswer(value);
        ua.setIsCorrect(isCorrect);
        return ua;
    }

    private static boolean isObjective(Skill skill) {
        return skill == Skill.READING || skill == Skill.LISTENING;
    }

    private int parseTestNumber(Attempt a) {
        try {
            return Integer.parseInt(a.getTestNumber().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Non-numeric test number: " + a.getTestNumber());
        }
    }

    private Attempt requireOwned(Long attemptId, UUID userId) {
        Attempt a = attempts.findById(attemptId).orElseThrow(() -> ResourceNotFoundException.of("Attempt", attemptId));
        requireOwner(a, userId);
        return a;
    }

    private void requireOwner(Attempt a, UUID userId) {
        if (!a.getUserId().equals(userId)) {
            throw new OperationNotAllowedException("Attempt does not belong to the current user");
        }
    }

    private void requireInProgress(Attempt a) {
        if (a.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new IllegalStateException("Attempt is not in progress");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
