package com.cramer.assessment.service;

import com.cramer.assessment.domain.Attempt;
import com.cramer.assessment.domain.AttemptStatus;
import com.cramer.assessment.repository.AttemptRepository;
import com.cramer.assessment.repository.UserAnswerRepository;
import com.cramer.assessment.web.dto.AnswerInput;
import com.cramer.assessment.web.dto.AttemptResultResponse;
import com.cramer.assessment.web.dto.AttemptView;
import com.cramer.assessment.web.dto.SubmitAnswersRequest;
import com.cramer.billing.service.AttemptBillingPort;
import com.cramer.catalog.service.ContentLookupPort;
import com.cramer.catalog.service.GradableQuestion;
import com.cramer.catalog.service.SectionRef;
import com.cramer.platform.common.ielts.QuestionType;
import com.cramer.platform.common.ielts.Skill;
import com.cramer.platform.error.OperationNotAllowedException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttemptServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock AttemptRepository attempts;
    @Mock UserAnswerRepository answers;
    @Mock ContentLookupPort content;
    @Mock AttemptBillingPort billing;
    @Mock ObjectProvider<AttemptCleanupParticipant> cleanup;

    private AttemptService service() {
        lenient().when(cleanup.orderedStream()).thenAnswer(inv -> Stream.empty());
        lenient().when(attempts.save(any(Attempt.class))).thenAnswer(inv -> {
            Attempt a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(500L);
            }
            return a;
        });
        return new AttemptService(attempts, answers, content, new ScoringService(), billing, cleanup);
    }

    private JsonNode json(String raw) {
        try {
            return mapper.readTree(raw);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Attempt attempt(long id, UUID user, String skill, AttemptStatus status) {
        Attempt a = new Attempt();
        a.setId(id);
        a.setUserId(user);
        a.setExamSource("Cambridge 17");
        a.setTestNumber("1");
        a.setSkill(skill);
        a.setStatus(status);
        return a;
    }

    @Test
    @DisplayName("start with no existing attempt charges Reading quota and creates IN_PROGRESS")
    void startNewReadingCharges() {
        UUID user = UUID.randomUUID();
        when(attempts.lockByKey(eq(user), eq("Cambridge 17"), eq("1"), eq("reading"))).thenReturn(List.of());

        AttemptView v = service().start("Cambridge 17", "1", "reading", user, false);

        assertThat(v.status()).isEqualTo("IN_PROGRESS");
        verify(billing).chargeAttemptStart(eq(user), eq(Skill.READING), anyString());
    }

    @Test
    @DisplayName("start resumes an existing in-progress attempt without charging again")
    void startResumesInProgress() {
        UUID user = UUID.randomUUID();
        Attempt existing = attempt(7L, user, "reading", AttemptStatus.IN_PROGRESS);
        when(attempts.lockByKey(eq(user), anyString(), anyString(), eq("reading"))).thenReturn(List.of(existing));

        AttemptView v = service().start("Cambridge 17", "1", "reading", user, false);

        assertThat(v.id()).isEqualTo(7L);
        verify(billing, never()).chargeAttemptStart(any(), any(), any());
        verify(attempts, never()).save(any());
    }

    @Test
    @DisplayName("start forceNew cancels the in-progress attempt and creates a new one")
    void startForceNew() {
        UUID user = UUID.randomUUID();
        Attempt existing = attempt(7L, user, "reading", AttemptStatus.IN_PROGRESS);
        when(attempts.lockByKey(eq(user), anyString(), anyString(), eq("reading"))).thenReturn(List.of(existing));

        service().start("Cambridge 17", "1", "reading", user, true);

        assertThat(existing.getStatus()).isEqualTo(AttemptStatus.CANCELLED);
        verify(answers).deleteByAttemptId(7L);
        verify(billing).chargeAttemptStart(eq(user), eq(Skill.READING), anyString());
    }

    @Test
    @DisplayName("submit grades answers, sets score + Reading band, and completes the attempt")
    void submitGrades() {
        UUID user = UUID.randomUUID();
        when(attempts.findById(1L)).thenReturn(Optional.of(attempt(1L, user, "reading", AttemptStatus.IN_PROGRESS)));
        when(content.sectionsForExam("Cambridge 17", 1, Skill.READING))
                .thenReturn(List.of(new SectionRef(100L, 9L, "Cambridge 17", 1, Skill.READING, 1)));
        when(content.gradableQuestions(100L)).thenReturn(List.of(
                new GradableQuestion(11L, 1, QuestionType.MULTIPLE_CHOICE, json("[\"B\"]")),
                new GradableQuestion(12L, 2, QuestionType.FILL_IN_BLANK, json("[\"cat\"]"))));
        when(content.totalQuestions("Cambridge 17", 1, Skill.READING)).thenReturn(2);

        AttemptResultResponse res = service().submit(1L, user, new SubmitAnswersRequest(List.of(
                new AnswerInput(11L, "b"), new AnswerInput(12L, "dog"))));

        assertThat(res.score()).isEqualTo(1);
        assertThat(res.totalQuestions()).isEqualTo(2);
        assertThat(res.bandScore()).isNotNull();
        assertThat(res.status()).isEqualTo("COMPLETED");
        verify(answers).deleteByAttemptId(1L);
    }

    @Test
    @DisplayName("submitting another user's attempt is forbidden (403)")
    void submitOtherUserForbidden() {
        UUID owner = UUID.randomUUID();
        UUID intruder = UUID.randomUUID();
        when(attempts.findById(1L)).thenReturn(Optional.of(attempt(1L, owner, "reading", AttemptStatus.IN_PROGRESS)));

        assertThatThrownBy(() -> service().submit(1L, intruder, new SubmitAnswersRequest(List.of())))
                .isInstanceOf(OperationNotAllowedException.class);
    }

    @Test
    @DisplayName("cancel on a missing attempt is an idempotent no-op")
    void cancelMissingIsNoop() {
        when(attempts.findById(99L)).thenReturn(Optional.empty());
        service().cancel(99L, UUID.randomUUID());
        verify(answers, never()).deleteByAttemptId(any());
    }
}
