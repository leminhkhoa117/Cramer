package com.cramer.service.unit;

import com.cramer.dto.BillingResultDTO;
import com.cramer.dto.WritingReviewDTO;
import com.cramer.dto.WritingSubmissionDTO;
import com.cramer.entity.Section;
import com.cramer.entity.TestAttempt;
import com.cramer.entity.WritingSubmission;
import com.cramer.repository.SectionRepository;
import com.cramer.repository.TestAttemptRepository;
import com.cramer.repository.WritingSubmissionRepository;
import com.cramer.service.AsyncGradingService;
import com.cramer.service.LLMGradingService;
import com.cramer.service.QuotaBillingService;
import com.cramer.service.WritingSubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WritingSubmissionService.
 * Tests essay submission, grading workflow, and status tracking.
 * 
 * @author Cramer Test Team
 * @since 2026-01-15
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WritingSubmissionService Unit Tests")
class WritingSubmissionServiceTest {

    @Mock
    private WritingSubmissionRepository writingSubmissionRepository;

    @Mock
    private TestAttemptRepository testAttemptRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private LLMGradingService llmGradingService;

    @Mock
    private AsyncGradingService asyncGradingService;

    @Mock
    private QuotaBillingService quotaBillingService;

    @InjectMocks
    private WritingSubmissionService writingSubmissionService;

    private UUID testUserId;
    private Long testAttemptId;
    private TestAttempt testAttempt;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testAttemptId = 1L;

        testAttempt = new TestAttempt();
        testAttempt.setId(testAttemptId);
        testAttempt.setUserId(testUserId);
        testAttempt.setExamSource("cambridge");
        testAttempt.setTestNumber("17");
        testAttempt.setSkill("writing");
        testAttempt.setStatus("IN_PROGRESS");
        testAttempt.setStartedAt(OffsetDateTime.now().minusMinutes(30));
    }

    // =========================================================================
    // SAVE DRAFT TESTS
    // =========================================================================
    @Nested
    @DisplayName("saveDraft() Tests")
    class SaveDraftTests {

        @Test
        @DisplayName("Should save new draft for task")
        void saveDraft_newDraft_savedSuccessfully() {
            // Arrange
            String essayText = "This is my essay about climate change...";
            when(testAttemptRepository.findById(testAttemptId))
                    .thenReturn(Optional.of(testAttempt));
            when(writingSubmissionRepository.findByAttemptIdAndTaskNumber(testAttemptId, 2))
                    .thenReturn(Optional.empty());
            when(writingSubmissionRepository.save(any(WritingSubmission.class)))
                    .thenAnswer(invocation -> {
                        WritingSubmission saved = invocation.getArgument(0);
                        saved.setId(100L);
                        return saved;
                    });

            // Act
            WritingSubmissionDTO result = writingSubmissionService.saveDraft(
                    testAttemptId, 2, essayText, testUserId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getEssayText()).isEqualTo(essayText);
            assertThat(result.getTaskNumber()).isEqualTo(2);
            verify(writingSubmissionRepository).save(any(WritingSubmission.class));
        }

        @Test
        @DisplayName("Should update existing draft")
        void saveDraft_existingDraft_updatedSuccessfully() {
            // Arrange
            WritingSubmission existingSubmission = new WritingSubmission();
            existingSubmission.setId(100L);
            existingSubmission.setAttemptId(testAttemptId);
            existingSubmission.setUserId(testUserId);
            existingSubmission.setTaskNumber(2);
            existingSubmission.setEssayText("Old essay text");

            String newEssayText = "Updated essay about technology...";

            when(testAttemptRepository.findById(testAttemptId))
                    .thenReturn(Optional.of(testAttempt));
            when(writingSubmissionRepository.findByAttemptIdAndTaskNumber(testAttemptId, 2))
                    .thenReturn(Optional.of(existingSubmission));
            when(writingSubmissionRepository.save(any(WritingSubmission.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            WritingSubmissionDTO result = writingSubmissionService.saveDraft(
                    testAttemptId, 2, newEssayText, testUserId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getEssayText()).isEqualTo(newEssayText);
        }

        @Test
        @DisplayName("Should throw exception when attempt not found")
        void saveDraft_attemptNotFound_throwsException() {
            // Arrange
            when(testAttemptRepository.findById(testAttemptId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() ->
                    writingSubmissionService.saveDraft(testAttemptId, 2, "Essay", testUserId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Attempt not found");
        }

        @Test
        @DisplayName("Should throw exception when user unauthorized")
        void saveDraft_unauthorized_throwsException() {
            // Arrange
            UUID differentUserId = UUID.randomUUID();
            when(testAttemptRepository.findById(testAttemptId))
                    .thenReturn(Optional.of(testAttempt));

            // Act & Assert
            assertThatThrownBy(() ->
                    writingSubmissionService.saveDraft(testAttemptId, 2, "Essay", differentUserId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unauthorized");
        }
    }

    // =========================================================================
    // GET GRADING STATUS TESTS
    // =========================================================================
    @Nested
    @DisplayName("getGradingStatus() Tests")
    class GetGradingStatusTests {

        @Test
        @DisplayName("Should return COMPLETED status when all tasks graded")
        void getGradingStatus_allCompleted_returnsCompleted() {
            // Arrange
            WritingSubmission task1 = createSubmission(1, "COMPLETED");
            WritingSubmission task2 = createSubmission(2, "COMPLETED");

            when(testAttemptRepository.findById(testAttemptId))
                    .thenReturn(Optional.of(testAttempt));
            when(writingSubmissionRepository.findByAttemptId(testAttemptId))
                    .thenReturn(List.of(task1, task2));

            // Act
            Map<String, Object> status = writingSubmissionService.getGradingStatus(testAttemptId, testUserId);

            // Assert
            assertThat(status.get("status")).isEqualTo("COMPLETED");
            assertThat(status.get("totalTasks")).isEqualTo(2);
            assertThat(status.get("completedTasks")).isEqualTo(2L);
        }

        @Test
        @DisplayName("Should return GRADING status when tasks still grading")
        void getGradingStatus_stillGrading_returnsGrading() {
            // Arrange
            WritingSubmission task1 = createSubmission(1, "COMPLETED");
            WritingSubmission task2 = createSubmission(2, "GRADING");

            when(testAttemptRepository.findById(testAttemptId))
                    .thenReturn(Optional.of(testAttempt));
            when(writingSubmissionRepository.findByAttemptId(testAttemptId))
                    .thenReturn(List.of(task1, task2));

            // Act
            Map<String, Object> status = writingSubmissionService.getGradingStatus(testAttemptId, testUserId);

            // Assert
            assertThat(status.get("status")).isEqualTo("GRADING");
        }

        @Test
        @DisplayName("Should return PARTIAL_FAILURE when some tasks failed")
        void getGradingStatus_someFailed_returnsPartialFailure() {
            // Arrange
            WritingSubmission task1 = createSubmission(1, "COMPLETED");
            WritingSubmission task2 = createSubmission(2, "FAILED");

            when(testAttemptRepository.findById(testAttemptId))
                    .thenReturn(Optional.of(testAttempt));
            when(writingSubmissionRepository.findByAttemptId(testAttemptId))
                    .thenReturn(List.of(task1, task2));

            // Act
            Map<String, Object> status = writingSubmissionService.getGradingStatus(testAttemptId, testUserId);

            // Assert
            assertThat(status.get("status")).isEqualTo("PARTIAL_FAILURE");
            assertThat(status.get("failedTasks")).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should return PENDING when no tasks grading yet")
        void getGradingStatus_pending_returnsPending() {
            // Arrange
            WritingSubmission task1 = createSubmission(1, "PENDING");
            WritingSubmission task2 = createSubmission(2, "PENDING");

            when(testAttemptRepository.findById(testAttemptId))
                    .thenReturn(Optional.of(testAttempt));
            when(writingSubmissionRepository.findByAttemptId(testAttemptId))
                    .thenReturn(List.of(task1, task2));

            // Act
            Map<String, Object> status = writingSubmissionService.getGradingStatus(testAttemptId, testUserId);

            // Assert
            assertThat(status.get("status")).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("Should throw exception when unauthorized")
        void getGradingStatus_unauthorized_throwsException() {
            // Arrange
            UUID differentUserId = UUID.randomUUID();
            when(testAttemptRepository.findById(testAttemptId))
                    .thenReturn(Optional.of(testAttempt));

            // Act & Assert
            assertThatThrownBy(() ->
                    writingSubmissionService.getGradingStatus(testAttemptId, differentUserId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unauthorized");
        }
    }

    // =========================================================================
    // GET WRITING REVIEW TESTS
    // =========================================================================
    @Nested
    @DisplayName("getWritingReview() Tests")
    class GetWritingReviewTests {

        @Test
        @DisplayName("Should return full review with all task details")
        void getWritingReview_completed_returnsFullReview() {
            // Arrange
            testAttempt.setStatus("COMPLETED");
            testAttempt.setCompletedAt(OffsetDateTime.now());

            WritingSubmission task1 = createGradedSubmission(1, new BigDecimal("6.5"));
            WritingSubmission task2 = createGradedSubmission(2, new BigDecimal("7.0"));

            when(testAttemptRepository.findById(testAttemptId))
                    .thenReturn(Optional.of(testAttempt));
            when(writingSubmissionRepository.findByAttemptId(testAttemptId))
                    .thenReturn(List.of(task1, task2));
            when(sectionRepository.findByExamSourceAndTestNumberAndSkill(anyString(), anyInt(), eq("writing")))
                    .thenReturn(List.of());

            // Act
            WritingReviewDTO review = writingSubmissionService.getWritingReview(testAttemptId, testUserId);

            // Assert
            assertThat(review).isNotNull();
            assertThat(review.getAttemptId()).isEqualTo(testAttemptId);
            assertThat(review.getTasks()).hasSize(2);
            assertThat(review.getStatus()).isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("Should calculate duration correctly")
        void getWritingReview_withDuration_calculatesCorrectly() {
            // Arrange
            OffsetDateTime startTime = OffsetDateTime.now().minusMinutes(45);
            OffsetDateTime endTime = OffsetDateTime.now();
            testAttempt.setStartedAt(startTime);
            testAttempt.setCompletedAt(endTime);
            testAttempt.setStatus("COMPLETED");

            when(testAttemptRepository.findById(testAttemptId))
                    .thenReturn(Optional.of(testAttempt));
            when(writingSubmissionRepository.findByAttemptId(testAttemptId))
                    .thenReturn(List.of());
            when(sectionRepository.findByExamSourceAndTestNumberAndSkill(anyString(), anyInt(), eq("writing")))
                    .thenReturn(List.of());

            // Act
            WritingReviewDTO review = writingSubmissionService.getWritingReview(testAttemptId, testUserId);

            // Assert
            assertThat(review.getDuration()).isGreaterThan(2600L); // ~45 minutes in seconds
            assertThat(review.getDuration()).isLessThan(2800L);
        }
    }

    // =========================================================================
    // SUBMIT FOR GRADING TESTS
    // =========================================================================
    @Nested
    @DisplayName("submitForGrading() Tests")
    class SubmitForGradingTests {

        @Test
        @DisplayName("Should submit essays and start async grading when quota allowed")
        void submitForGrading_quotaAllowed_startsGrading() {
            // Arrange
            Map<Integer, String> essays = new HashMap<>();
            essays.put(1, "Task 1 essay about maps...");
            essays.put(2, "Task 2 essay about education...");

            BillingResultDTO quotaOk = BillingResultDTO.allowed();

            when(testAttemptRepository.findById(testAttemptId))
                    .thenReturn(Optional.of(testAttempt));
            when(testAttemptRepository.findByUserIdAndExamSourceAndTestNumberAndSkillAndStatus(
                    any(), any(), any(), any(), eq("IN_PROGRESS")))
                    .thenReturn(List.of());
            when(quotaBillingService.processAttemptBilling(testUserId, "WRITING", true))
                    .thenReturn(quotaOk);
            when(writingSubmissionRepository.findByAttemptIdAndTaskNumber(anyLong(), anyInt()))
                    .thenReturn(Optional.empty());
            when(writingSubmissionRepository.save(any(WritingSubmission.class)))
                    .thenAnswer(invocation -> {
                        WritingSubmission s = invocation.getArgument(0);
                        s.setId((long) (s.getTaskNumber() * 100));
                        return s;
                    });
            when(testAttemptRepository.save(any(TestAttempt.class)))
                    .thenReturn(testAttempt);

            // Act
            Map<String, Object> result = writingSubmissionService.submitForGrading(
                    testAttemptId, essays, testUserId);

            // Assert
            assertThat(result.get("status")).isEqualTo("GRADING_STARTED");
            assertThat(result.get("submissionCount")).isEqualTo(2);
            verify(writingSubmissionRepository, times(2)).save(any(WritingSubmission.class));
        }

        @Test
        @DisplayName("Should mark as FAILED when quota exceeded")
        void submitForGrading_quotaExceeded_marksAsFailed() {
            // Arrange
            Map<Integer, String> essays = new HashMap<>();
            essays.put(1, "Task 1 essay...");

            BillingResultDTO quotaBlocked = BillingResultDTO.blockedGlobal(20);

            when(testAttemptRepository.findById(testAttemptId))
                    .thenReturn(Optional.of(testAttempt));
            when(testAttemptRepository.findByUserIdAndExamSourceAndTestNumberAndSkillAndStatus(
                    any(), any(), any(), any(), eq("IN_PROGRESS")))
                    .thenReturn(List.of());
            when(quotaBillingService.processAttemptBilling(testUserId, "WRITING", true))
                    .thenReturn(quotaBlocked);
            when(writingSubmissionRepository.findByAttemptIdAndTaskNumber(anyLong(), anyInt()))
                    .thenReturn(Optional.empty());
            when(writingSubmissionRepository.save(any(WritingSubmission.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(testAttemptRepository.save(any(TestAttempt.class)))
                    .thenReturn(testAttempt);

            // Act
            Map<String, Object> result = writingSubmissionService.submitForGrading(
                    testAttemptId, essays, testUserId);

            // Assert
            assertThat(result.get("status")).isEqualTo("COMPLETED");
            assertThat(result.get("message").toString()).contains("quota");
            // Verify async grading was NOT called
            verify(asyncGradingService, never()).gradeSubmissionsAsync(any(), any(), any());
        }

        @Test
        @DisplayName("Should cancel stale IN_PROGRESS attempts before submitting")
        void submitForGrading_withStaleAttempts_cancelsStale() {
            // Arrange
            TestAttempt staleAttempt = new TestAttempt();
            staleAttempt.setId(999L);
            staleAttempt.setStatus("IN_PROGRESS");

            Map<Integer, String> essays = new HashMap<>();
            essays.put(1, "Essay text");

            when(testAttemptRepository.findById(testAttemptId))
                    .thenReturn(Optional.of(testAttempt));
            when(testAttemptRepository.findByUserIdAndExamSourceAndTestNumberAndSkillAndStatus(
                    any(), any(), any(), any(), eq("IN_PROGRESS")))
                    .thenReturn(List.of(staleAttempt));
            when(quotaBillingService.processAttemptBilling(any(), any(), anyBoolean()))
                    .thenReturn(BillingResultDTO.allowed());
            when(writingSubmissionRepository.findByAttemptIdAndTaskNumber(anyLong(), anyInt()))
                    .thenReturn(Optional.empty());
            when(writingSubmissionRepository.save(any(WritingSubmission.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(testAttemptRepository.save(any(TestAttempt.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            writingSubmissionService.submitForGrading(testAttemptId, essays, testUserId);

            // Assert - stale attempt should be cancelled
            verify(testAttemptRepository).save(argThat(attempt ->
                    attempt.getId().equals(999L) && "CANCELLED".equals(attempt.getStatus())));
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================
    private WritingSubmission createSubmission(int taskNumber, String status) {
        WritingSubmission submission = new WritingSubmission();
        submission.setId((long) (taskNumber * 100));
        submission.setAttemptId(testAttemptId);
        submission.setUserId(testUserId);
        submission.setTaskNumber(taskNumber);
        submission.setEssayText("Essay for task " + taskNumber);
        submission.setGradingStatus(status);
        return submission;
    }

    private WritingSubmission createGradedSubmission(int taskNumber, BigDecimal band) {
        WritingSubmission submission = createSubmission(taskNumber, "COMPLETED");
        submission.setOverallBand(band);
        submission.setGradedAt(OffsetDateTime.now());

        Map<String, Object> bandScores = new HashMap<>();
        bandScores.put("taskAchievement", band);
        bandScores.put("coherenceCohesion", band);
        bandScores.put("lexicalResource", band);
        bandScores.put("grammaticalRange", band);
        submission.setBandScores(bandScores);

        return submission;
    }
}
