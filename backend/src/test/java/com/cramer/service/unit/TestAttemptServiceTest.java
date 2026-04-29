package com.cramer.service.unit;

import com.cramer.dto.BillingResultDTO;
import com.cramer.dto.SaveProgressDTO;
import com.cramer.dto.TestResultDTO;
import com.cramer.entity.Question;
import com.cramer.entity.Section;
import com.cramer.entity.TestAttempt;
import com.cramer.entity.UserAnswer;
import com.cramer.exception.QuotaExceededException;
import com.cramer.repository.QuestionRepository;
import com.cramer.repository.SectionRepository;
import com.cramer.repository.TestAttemptRepository;
import com.cramer.repository.UserAnswerRepository;
import com.cramer.repository.WritingSubmissionRepository;
import com.cramer.service.QuotaBillingService;
import com.cramer.service.TestAttemptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TestAttemptService.
 * Tests test session lifecycle: start, resume, save progress, submit.
 * 
 * @author Cramer Test Team
 * @since 2026-01-15
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TestAttemptService Unit Tests")
class TestAttemptServiceTest {

    @Mock
    private TestAttemptRepository testAttemptRepository;

    @Mock
    private UserAnswerRepository userAnswerRepository;

    @Mock
    private WritingSubmissionRepository writingSubmissionRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private QuotaBillingService quotaBillingService;

    // Use real ObjectMapper instead of mock to avoid null from createObjectNode()
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private jakarta.persistence.EntityManager entityManager;

    private TestAttemptService testAttemptService;

    private UUID testUserId;
    private String testSource;
    private String testNumber;
    private String testSkill;
    private TestAttempt existingAttempt;

    @BeforeEach
    void setUp() {
        // Manually construct service with all dependencies including EntityManager
        testAttemptService = new TestAttemptService(
                testAttemptRepository,
                userAnswerRepository,
                writingSubmissionRepository,
                questionRepository,
                sectionRepository,
                objectMapper,
                quotaBillingService
        );
        // Inject EntityManager using reflection
        try {
            java.lang.reflect.Field emField = TestAttemptService.class.getDeclaredField("entityManager");
            emField.setAccessible(true);
            emField.set(testAttemptService, entityManager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        testUserId = UUID.randomUUID();
        testSource = "cambridge";
        testNumber = "17";
        testSkill = "reading";

        existingAttempt = new TestAttempt();
        existingAttempt.setId(1L);
        existingAttempt.setUserId(testUserId);
        existingAttempt.setExamSource(testSource);
        existingAttempt.setTestNumber(testNumber);
        existingAttempt.setSkill(testSkill);
        existingAttempt.setStatus("IN_PROGRESS");
        existingAttempt.setStartedAt(OffsetDateTime.now().minusMinutes(20));
        existingAttempt.setTimeLeft(2400); // 40 minutes left
    }

    // =========================================================================
    // START OR GET ATTEMPT TESTS
    // =========================================================================
    @Nested
    @DisplayName("startOrGetAttempt() Tests")
    class StartOrGetAttemptTests {

        @Test
        @DisplayName("Should resume existing IN_PROGRESS attempt")
        void startOrGetAttempt_existingInProgress_resumesAttempt() {
            // Arrange
            when(testAttemptRepository.findAndLockByUserIdAndExamSourceAndTestNumberAndSkill(
                    testUserId, testSource, testNumber, testSkill))
                    .thenReturn(List.of(existingAttempt));

            // Act
            TestAttempt result = testAttemptService.startOrGetAttempt(
                    testSource, testNumber, testSkill, testUserId, false);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
            verify(testAttemptRepository, never()).save(any(TestAttempt.class));
        }

        @Test
        @DisplayName("Should create new attempt when none exists")
        void startOrGetAttempt_noExisting_createsNew() {
            // Arrange
            when(testAttemptRepository.findAndLockByUserIdAndExamSourceAndTestNumberAndSkill(
                    testUserId, testSource, testNumber, testSkill))
                    .thenReturn(List.of());
            when(quotaBillingService.processAttemptBilling(testUserId, testSkill.toUpperCase(), false))
                    .thenReturn(BillingResultDTO.allowed());
            when(testAttemptRepository.save(any(TestAttempt.class)))
                    .thenAnswer(invocation -> {
                        TestAttempt saved = invocation.getArgument(0);
                        saved.setId(99L);
                        return saved;
                    });

            // Act
            TestAttempt result = testAttemptService.startOrGetAttempt(
                    testSource, testNumber, testSkill, testUserId, false);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
            verify(testAttemptRepository).save(any(TestAttempt.class));
        }

        @Test
        @DisplayName("Should return COMPLETED attempt when forceNew=false")
        void startOrGetAttempt_completedNoForce_returnsCompleted() {
            // Arrange
            existingAttempt.setStatus("COMPLETED");
            existingAttempt.setCompletedAt(OffsetDateTime.now().minusDays(1));
            existingAttempt.setScore(8);

            when(testAttemptRepository.findAndLockByUserIdAndExamSourceAndTestNumberAndSkill(
                    testUserId, testSource, testNumber, testSkill))
                    .thenReturn(List.of(existingAttempt));

            // Act
            TestAttempt result = testAttemptService.startOrGetAttempt(
                    testSource, testNumber, testSkill, testUserId, false);

            // Assert
            assertThat(result.getStatus()).isEqualTo("COMPLETED");
            assertThat(result.getScore()).isEqualTo(8);
        }

        @Test
        @DisplayName("Should create new attempt when forceNew=true with COMPLETED")
        void startOrGetAttempt_completedWithForce_createsNew() {
            // Arrange
            existingAttempt.setStatus("COMPLETED");
            existingAttempt.setCompletedAt(OffsetDateTime.now().minusDays(1));

            when(testAttemptRepository.findAndLockByUserIdAndExamSourceAndTestNumberAndSkill(
                    testUserId, testSource, testNumber, testSkill))
                    .thenReturn(List.of(existingAttempt));
            when(quotaBillingService.processAttemptBilling(testUserId, testSkill.toUpperCase(), false))
                    .thenReturn(BillingResultDTO.allowed());
            when(testAttemptRepository.save(any(TestAttempt.class)))
                    .thenAnswer(invocation -> {
                        TestAttempt saved = invocation.getArgument(0);
                        saved.setId(99L);
                        return saved;
                    });

            // Act
            TestAttempt result = testAttemptService.startOrGetAttempt(
                    testSource, testNumber, testSkill, testUserId, true);

            // Assert
            assertThat(result.getId()).isEqualTo(99L);
            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
        }

        @Test
        @DisplayName("Should cancel stale IN_PROGRESS and create new when forceNew=true")
        void startOrGetAttempt_forceNewCancelsInProgress_createsNew() {
            // Arrange
            when(testAttemptRepository.findAndLockByUserIdAndExamSourceAndTestNumberAndSkill(
                    testUserId, testSource, testNumber, testSkill))
                    .thenReturn(List.of(existingAttempt)); // IN_PROGRESS
            when(quotaBillingService.processAttemptBilling(testUserId, testSkill.toUpperCase(), false))
                    .thenReturn(BillingResultDTO.allowed());
            when(testAttemptRepository.save(any(TestAttempt.class)))
                    .thenAnswer(invocation -> {
                        TestAttempt saved = invocation.getArgument(0);
                        if (saved.getId() != null && saved.getId().equals(1L)) {
                            // This is the cancelled attempt
                            return saved;
                        }
                        // New attempt
                        saved.setId(99L);
                        return saved;
                    });

            // Act
            TestAttempt result = testAttemptService.startOrGetAttempt(
                    testSource, testNumber, testSkill, testUserId, true);

            // Assert
            assertThat(result.getId()).isEqualTo(99L);
            // Verify old attempt was cancelled
            verify(testAttemptRepository).save(argThat(attempt ->
                    attempt.getId() != null && attempt.getId().equals(1L) &&
                            "CANCELLED".equals(attempt.getStatus())));
        }

        @Test
        @DisplayName("Should throw exception when userId is null")
        void startOrGetAttempt_nullUserId_throwsException() {
            // Act & Assert
            assertThatThrownBy(() ->
                    testAttemptService.startOrGetAttempt(testSource, testNumber, testSkill, null, false))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Should throw exception when source is empty")
        void startOrGetAttempt_emptySource_throwsException() {
            // Act & Assert
            assertThatThrownBy(() ->
                    testAttemptService.startOrGetAttempt("", testNumber, testSkill, testUserId, false))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Should create new attempt when latest is CANCELLED")
        void startOrGetAttempt_cancelledAttempt_createsNew() {
            // Arrange
            existingAttempt.setStatus("CANCELLED");

            when(testAttemptRepository.findAndLockByUserIdAndExamSourceAndTestNumberAndSkill(
                    testUserId, testSource, testNumber, testSkill))
                    .thenReturn(List.of(existingAttempt));
            when(quotaBillingService.processAttemptBilling(testUserId, testSkill.toUpperCase(), false))
                    .thenReturn(BillingResultDTO.allowed());
            when(testAttemptRepository.save(any(TestAttempt.class)))
                    .thenAnswer(invocation -> {
                        TestAttempt saved = invocation.getArgument(0);
                        saved.setId(99L);
                        return saved;
                    });

            // Act
            TestAttempt result = testAttemptService.startOrGetAttempt(
                    testSource, testNumber, testSkill, testUserId, false);

            // Assert
            assertThat(result.getId()).isEqualTo(99L);
            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
        }

        @Test
        @DisplayName("Should cancel multiple IN_PROGRESS and keep only latest")
        void startOrGetAttempt_multipleInProgress_cancelsOldOnes() {
            // Arrange
            TestAttempt oldAttempt1 = new TestAttempt();
            oldAttempt1.setId(2L);
            oldAttempt1.setUserId(testUserId);
            oldAttempt1.setStatus("IN_PROGRESS");
            oldAttempt1.setStartedAt(OffsetDateTime.now().minusDays(2));

            TestAttempt oldAttempt2 = new TestAttempt();
            oldAttempt2.setId(3L);
            oldAttempt2.setUserId(testUserId);
            oldAttempt2.setStatus("IN_PROGRESS");
            oldAttempt2.setStartedAt(OffsetDateTime.now().minusDays(1));

            // existingAttempt is the most recent (from setUp)
            when(testAttemptRepository.findAndLockByUserIdAndExamSourceAndTestNumberAndSkill(
                    testUserId, testSource, testNumber, testSkill))
                    .thenReturn(List.of(existingAttempt, oldAttempt2, oldAttempt1)); // Sorted by startedAt DESC

            when(testAttemptRepository.save(any(TestAttempt.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            TestAttempt result = testAttemptService.startOrGetAttempt(
                    testSource, testNumber, testSkill, testUserId, false);

            // Assert
            assertThat(result.getId()).isEqualTo(1L); // Most recent IN_PROGRESS
            // Verify old attempts were cancelled
            verify(testAttemptRepository).save(argThat(a -> a.getId().equals(2L) && "CANCELLED".equals(a.getStatus())));
            verify(testAttemptRepository).save(argThat(a -> a.getId().equals(3L) && "CANCELLED".equals(a.getStatus())));
        }
    }

    // =========================================================================
    // SAVE PROGRESS TESTS
    // =========================================================================
    @Nested
    @DisplayName("saveProgress() Tests")
    class SaveProgressTests {

        @Test
        @DisplayName("Should save answers and time left")
        void saveProgress_validData_savesSuccessfully() {
            // Arrange
            SaveProgressDTO progressDTO = new SaveProgressDTO();
            progressDTO.setTimeLeft(1800);
            progressDTO.setCurrentPart(2);
            progressDTO.setAnswers(Map.of(
                    1L, "A",
                    2L, "B",
                    3L, "TRUE"
            ));

            Question q1 = new Question();
            q1.setId(1L);
            Question q2 = new Question();
            q2.setId(2L);
            Question q3 = new Question();
            q3.setId(3L);

            when(testAttemptRepository.findById(1L))
                    .thenReturn(Optional.of(existingAttempt));
            when(testAttemptRepository.save(any(TestAttempt.class)))
                    .thenReturn(existingAttempt);
            doNothing().when(userAnswerRepository).deleteByAttemptId(anyLong());
            when(questionRepository.findAllById(anySet()))
                    .thenReturn(List.of(q1, q2, q3));
            when(userAnswerRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            testAttemptService.saveProgress(1L, progressDTO, testUserId);

            // Assert
            verify(testAttemptRepository).save(argThat(attempt ->
                    attempt.getTimeLeft() == 1800 && attempt.getCurrentPart() == 2));
            verify(userAnswerRepository).deleteByAttemptId(1L);
            verify(userAnswerRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should throw exception when attempt not found")
        void saveProgress_attemptNotFound_throwsException() {
            // Arrange
            when(testAttemptRepository.findById(999L))
                    .thenReturn(Optional.empty());

            SaveProgressDTO progressDTO = new SaveProgressDTO();

            // Act & Assert
            assertThatThrownBy(() ->
                    testAttemptService.saveProgress(999L, progressDTO, testUserId))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Should throw exception when user unauthorized")
        void saveProgress_unauthorized_throwsException() {
            // Arrange
            UUID differentUser = UUID.randomUUID();
            when(testAttemptRepository.findById(1L))
                    .thenReturn(Optional.of(existingAttempt));

            SaveProgressDTO progressDTO = new SaveProgressDTO();

            // Act & Assert
            assertThatThrownBy(() ->
                    testAttemptService.saveProgress(1L, progressDTO, differentUser))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Should not save when answers map is empty")
        void saveProgress_emptyAnswers_doesNotSaveAnswers() {
            // Arrange
            SaveProgressDTO progressDTO = new SaveProgressDTO();
            progressDTO.setTimeLeft(1800);
            progressDTO.setAnswers(Map.of());

            when(testAttemptRepository.findById(1L))
                    .thenReturn(Optional.of(existingAttempt));
            when(testAttemptRepository.save(any(TestAttempt.class)))
                    .thenReturn(existingAttempt);

            // Act
            testAttemptService.saveProgress(1L, progressDTO, testUserId);

            // Assert
            verify(testAttemptRepository).save(any(TestAttempt.class));
            verify(userAnswerRepository, never()).saveAll(anyList());
        }
    }

    // =========================================================================
    // CANCEL ATTEMPT TESTS
    // =========================================================================
    @Nested
    @DisplayName("cancelAttempt() Tests")
    class CancelAttemptTests {

        @Test
        @DisplayName("Should cancel IN_PROGRESS attempt and delete data")
        void cancelAttempt_inProgress_cancelsSuccessfully() {
            // Arrange
            when(testAttemptRepository.findById(1L))
                    .thenReturn(Optional.of(existingAttempt));
            doNothing().when(userAnswerRepository).deleteByAttemptId(1L);
            doNothing().when(writingSubmissionRepository).deleteByAttemptId(1L);
            doNothing().when(testAttemptRepository).deleteAttemptById(1L);

            // Act
            testAttemptService.cancelAttempt(1L, testUserId);

            // Assert
            verify(userAnswerRepository).deleteByAttemptId(1L);
            verify(writingSubmissionRepository).deleteByAttemptId(1L);
            verify(testAttemptRepository).deleteAttemptById(1L);
        }

        @Test
        @DisplayName("Should return silently when completed attempt (idempotent)")
        void cancelAttempt_completed_returnsSilently() {
            // Arrange
            existingAttempt.setStatus("COMPLETED");
            when(testAttemptRepository.findById(1L))
                    .thenReturn(Optional.of(existingAttempt));

            // Act - should not throw
            testAttemptService.cancelAttempt(1L, testUserId);

            // Assert - no deletion happens
            verify(userAnswerRepository, never()).deleteByAttemptId(anyLong());
            verify(testAttemptRepository, never()).deleteAttemptById(anyLong());
        }

        @Test
        @DisplayName("Should return silently when attempt not found (idempotent)")
        void cancelAttempt_notFound_returnsSilently() {
            // Arrange
            when(testAttemptRepository.findById(999L))
                    .thenReturn(Optional.empty());

            // Act - should not throw
            testAttemptService.cancelAttempt(999L, testUserId);

            // Assert - no deletion happens
            verify(userAnswerRepository, never()).deleteByAttemptId(anyLong());
        }

        @Test
        @DisplayName("Should throw exception when user unauthorized")
        void cancelAttempt_unauthorized_throwsException() {
            // Arrange
            UUID differentUser = UUID.randomUUID();
            when(testAttemptRepository.findById(1L))
                    .thenReturn(Optional.of(existingAttempt));

            // Act & Assert
            assertThatThrownBy(() ->
                    testAttemptService.cancelAttempt(1L, differentUser))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================
    private TestAttempt createTestAttempt(Long id, String status, String skill) {
        TestAttempt attempt = new TestAttempt();
        attempt.setId(id);
        attempt.setUserId(testUserId);
        attempt.setExamSource("cambridge");
        attempt.setTestNumber("17");
        attempt.setSkill(skill);
        attempt.setStatus(status);
        attempt.setStartedAt(OffsetDateTime.now().minusHours(id));
        if ("COMPLETED".equals(status)) {
            attempt.setCompletedAt(OffsetDateTime.now().minusMinutes(id * 30));
            attempt.setScore(6 + id.intValue());
        }
        return attempt;
    }
}
