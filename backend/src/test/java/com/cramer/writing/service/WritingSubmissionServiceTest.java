package com.cramer.writing.service;

import com.cramer.assessment.service.AttemptWriteBackPort;
import com.cramer.assessment.service.AttemptWriteBackPort.AttemptContext;
import com.cramer.catalog.service.ContentLookupPort;
import com.cramer.platform.error.OperationNotAllowedException;
import com.cramer.platform.error.RateLimitExceededException;
import com.cramer.platform.ratelimit.RateLimiter;
import com.cramer.writing.domain.WritingStatus;
import com.cramer.writing.domain.WritingSubmission;
import com.cramer.writing.repository.WritingSubmissionRepository;
import com.cramer.writing.web.dto.WritingStatusResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WritingSubmissionServiceTest {

    @Mock WritingSubmissionRepository submissions;
    @Mock WritingGradingDispatcher dispatcher;
    @Mock ContentLookupPort content;
    @Mock AttemptWriteBackPort attemptWriteBack;
    @Mock RateLimiter rateLimiter;

    private WritingSubmissionService service() {
        lenient().when(submissions.save(any(WritingSubmission.class))).thenAnswer(inv -> inv.getArgument(0));
        return new WritingSubmissionService(submissions, dispatcher, new WritingBandCalculator(),
                content, attemptWriteBack, rateLimiter);
    }

    private AttemptContext ctx(UUID user) {
        return new AttemptContext(1L, user, "Cambridge 17", "1", "writing", "IN_PROGRESS");
    }

    @Test
    @DisplayName("submit rate-limit exceeded throws 429 before any work")
    void submitRateLimited() {
        UUID user = UUID.randomUUID();
        when(rateLimiter.tryConsume(user, RateLimiter.GRADING)).thenReturn(false);

        assertThatThrownBy(() -> service().submit(1L, Map.of(2, "essay"), user))
                .isInstanceOf(RateLimitExceededException.class);
        verify(attemptWriteBack, never()).completeForGrading(anyLong(), any());
    }

    @Test
    @DisplayName("submit completes the attempt, saves PENDING submissions, and dispatches grading")
    void submitHappyPath() {
        UUID user = UUID.randomUUID();
        when(rateLimiter.tryConsume(user, RateLimiter.GRADING)).thenReturn(true);
        when(attemptWriteBack.completeForGrading(1L, user)).thenReturn(ctx(user));
        when(submissions.findByAttemptIdOrderByTaskNumberAsc(1L)).thenReturn(List.of());

        WritingStatusResponse res = service().submit(1L, Map.of(1, "task one essay", 2, "task two essay"), user);

        verify(attemptWriteBack).completeForGrading(1L, user);
        verify(submissions).deleteByAttemptId(1L);
        // not transactional in the test → dispatch happens inline
        verify(dispatcher).gradeAttempt(eq(1L), eq(user), eq("Cambridge 17"), eq("1"));
        assertThat(res.attemptId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("regrade with no existing submissions is rejected (403)")
    void regradeNoSubmissions() {
        UUID user = UUID.randomUUID();
        when(rateLimiter.tryConsume(user, RateLimiter.GRADING)).thenReturn(true);
        when(attemptWriteBack.requireOwnedContext(1L, user)).thenReturn(ctx(user));
        when(submissions.findByAttemptIdOrderByTaskNumberAsc(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service().regrade(1L, user))
                .isInstanceOf(OperationNotAllowedException.class);
        verify(dispatcher, never()).gradeAttempt(anyLong(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("status reports COMPLETED when all tasks completed and none failed")
    void statusCompleted() {
        UUID user = UUID.randomUUID();
        when(attemptWriteBack.requireOwnedContext(1L, user)).thenReturn(ctx(user));
        when(submissions.findByAttemptIdOrderByTaskNumberAsc(1L)).thenReturn(List.of(
                task(1, WritingStatus.COMPLETED), task(2, WritingStatus.COMPLETED)));

        WritingStatusResponse res = service().status(1L, user);

        assertThat(res.overall()).isEqualTo("COMPLETED");
        assertThat(res.completed()).isEqualTo(2);
        assertThat(res.failed()).isZero();
    }

    @Test
    @DisplayName("status reports PARTIAL_FAILURE when all terminal but at least one failed")
    void statusPartialFailure() {
        UUID user = UUID.randomUUID();
        when(attemptWriteBack.requireOwnedContext(1L, user)).thenReturn(ctx(user));
        when(submissions.findByAttemptIdOrderByTaskNumberAsc(1L)).thenReturn(List.of(
                task(1, WritingStatus.COMPLETED), task(2, WritingStatus.FAILED)));

        WritingStatusResponse res = service().status(1L, user);

        assertThat(res.overall()).isEqualTo("PARTIAL_FAILURE");
    }

    private WritingSubmission task(int taskNumber, WritingStatus status) {
        WritingSubmission s = new WritingSubmission();
        s.setTaskNumber(taskNumber);
        s.setGradingStatus(status);
        return s;
    }
}
