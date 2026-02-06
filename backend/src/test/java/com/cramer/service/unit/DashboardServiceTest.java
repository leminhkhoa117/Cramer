package com.cramer.service.unit;

import com.cramer.dto.*;
import com.cramer.entity.*;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.repository.*;
import com.cramer.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DashboardService.
 * Tests dashboard summary building, target saving, and statistics calculation.
 * 
 * @author Cramer Test Team
 * @since 2026-01-25
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService Unit Tests")
class DashboardServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private TargetRepository targetRepository;

    @Mock
    private TestAttemptRepository testAttemptRepository;

    @Mock
    private UserAnswerRepository userAnswerRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private WritingSubmissionRepository writingSubmissionRepository;

    @Mock
    private TestSetRepository testSetRepository;

    @Mock
    private IeltsTestRepository ieltsTestRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private UUID testUserId;
    private Profile mockProfile;
    private Target mockTarget;

    @BeforeEach
    void setUp() {
        testUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockProfile = new Profile();
        mockProfile.setId(testUserId);
        mockProfile.setUsername("testuser");
        mockProfile.setFullName("Test User");

        mockTarget = new Target();
        mockTarget.setId(UUID.randomUUID());
        mockTarget.setUserId(testUserId);
        mockTarget.setExamName("IELTS Academic");
        mockTarget.setExamDate(LocalDate.of(2026, 6, 15));
        mockTarget.setListening(7.0);
        mockTarget.setReading(7.5);
        mockTarget.setWriting(6.5);
        mockTarget.setSpeaking(7.0);
    }

    // =========================================================================
    // BUILD DASHBOARD SUMMARY TESTS
    // =========================================================================
    @Nested
    @DisplayName("buildDashboardSummary() Tests")
    class BuildDashboardSummaryTests {

        @Test
        @DisplayName("Should build dashboard summary for user with data")
        void buildDashboardSummary_hasData_returnsSummary() {
            when(profileRepository.findById(testUserId)).thenReturn(Optional.of(mockProfile));
            when(targetRepository.findByUserId(testUserId)).thenReturn(Optional.of(mockTarget));
            when(testAttemptRepository.findByUserId(testUserId)).thenReturn(List.of());
            when(userAnswerRepository.findByAttempt_UserId(testUserId)).thenReturn(List.of());

            DashboardSummaryDTO result = dashboardService.buildDashboardSummary(testUserId, 0, 10, null);

            assertThat(result).isNotNull();
            assertThat(result.getProfile()).isNotNull();
            assertThat(result.getProfile().getFullName()).isEqualTo("Test User");
            assertThat(result.getTarget()).isNotNull();
            assertThat(result.getTarget().examName()).isEqualTo("IELTS Academic");
        }

        @Test
        @DisplayName("Should build dashboard summary without target")
        void buildDashboardSummary_noTarget_returnsNullTarget() {
            when(profileRepository.findById(testUserId)).thenReturn(Optional.of(mockProfile));
            when(targetRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
            when(testAttemptRepository.findByUserId(testUserId)).thenReturn(List.of());
            when(userAnswerRepository.findByAttempt_UserId(testUserId)).thenReturn(List.of());

            DashboardSummaryDTO result = dashboardService.buildDashboardSummary(testUserId, 0, 10, null);

            assertThat(result).isNotNull();
            assertThat(result.getTarget()).isNull();
            assertThat(result.getGoals()).isEmpty();
        }

        @Test
        @DisplayName("Should throw when profile not found")
        void buildDashboardSummary_noProfile_throws() {
            when(profileRepository.findById(testUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> dashboardService.buildDashboardSummary(testUserId, 0, 10, null))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw when userId is null")
        void buildDashboardSummary_nullUserId_throws() {
            assertThatThrownBy(() -> dashboardService.buildDashboardSummary(null, 0, 10, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("userId must not be null");
        }

        @Test
        @DisplayName("Should build goals from target")
        void buildDashboardSummary_hasTarget_buildsGoals() {
            when(profileRepository.findById(testUserId)).thenReturn(Optional.of(mockProfile));
            when(targetRepository.findByUserId(testUserId)).thenReturn(Optional.of(mockTarget));
            when(testAttemptRepository.findByUserId(testUserId)).thenReturn(List.of());
            when(userAnswerRepository.findByAttempt_UserId(testUserId)).thenReturn(List.of());

            DashboardSummaryDTO result = dashboardService.buildDashboardSummary(testUserId, 0, 10, null);

            assertThat(result.getGoals()).hasSize(4);
            assertThat(result.getGoals())
                    .extracting(DashboardGoalDTO::getLabel)
                    .containsExactlyInAnyOrder("Listening", "Reading", "Writing", "Speaking");
        }
    }

    // =========================================================================
    // SAVE TARGET TESTS
    // =========================================================================
    @Nested
    @DisplayName("saveTarget() Tests")
    class SaveTargetTests {

        @Test
        @DisplayName("Should create new target when none exists")
        void saveTarget_noExisting_createsNew() {
            TargetDTO targetDTO = new TargetDTO(
                    "IELTS General",
                    LocalDate.of(2026, 8, 20),
                    6.5, 6.5, 6.0, 6.5
            );

            when(targetRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
            when(targetRepository.save(any(Target.class))).thenAnswer(invocation -> {
                Target t = invocation.getArgument(0);
                t.setId(UUID.randomUUID());
                return t;
            });

            TargetDTO result = dashboardService.saveTarget(testUserId, targetDTO);

            assertThat(result).isNotNull();
            assertThat(result.examName()).isEqualTo("IELTS General");
            verify(targetRepository).save(any(Target.class));
        }

        @Test
        @DisplayName("Should update existing target")
        void saveTarget_hasExisting_updates() {
            TargetDTO targetDTO = new TargetDTO(
                    "IELTS Academic Updated",
                    LocalDate.of(2026, 12, 1),
                    8.0, 8.0, 7.0, 7.5
            );

            when(targetRepository.findByUserId(testUserId)).thenReturn(Optional.of(mockTarget));
            when(targetRepository.save(any(Target.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TargetDTO result = dashboardService.saveTarget(testUserId, targetDTO);

            assertThat(result.examName()).isEqualTo("IELTS Academic Updated");
            assertThat(result.listening()).isEqualTo(8.0);
            verify(targetRepository).save(any(Target.class));
        }

        @Test
        @DisplayName("Should throw when userId is null")
        void saveTarget_nullUserId_throws() {
            TargetDTO targetDTO = new TargetDTO("IELTS", null, null, null, null, null);

            assertThatThrownBy(() -> dashboardService.saveTarget(null, targetDTO))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("User ID cannot be null");
        }

        @Test
        @DisplayName("Should throw when targetDTO is null")
        void saveTarget_nullTargetDTO_throws() {
            assertThatThrownBy(() -> dashboardService.saveTarget(testUserId, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Target data cannot be null");
        }
    }

    // =========================================================================
    // GET RECENT ACTIVITIES TESTS
    // =========================================================================
    @Nested
    @DisplayName("getRecentActivities() Tests")
    class GetRecentActivitiesTests {

        @Test
        @DisplayName("Should return recent activities sorted by date")
        void getRecentActivities_hasAnswers_returnsSorted() {
            Question q1 = new Question();
            q1.setId(1L);
            Question q2 = new Question();
            q2.setId(2L);

            TestAttempt attempt = new TestAttempt();
            attempt.setId(1L);

            UserAnswer answer1 = new UserAnswer();
            answer1.setId(1L);
            answer1.setQuestion(q1);
            answer1.setAttempt(attempt);
            answer1.setSubmittedAt(OffsetDateTime.now().minusMinutes(5));
            answer1.setCorrect(true);

            UserAnswer answer2 = new UserAnswer();
            answer2.setId(2L);
            answer2.setQuestion(q2);
            answer2.setAttempt(attempt);
            answer2.setSubmittedAt(OffsetDateTime.now().minusMinutes(1));
            answer2.setCorrect(false);

            List<RecentActivityDTO> result = dashboardService.getRecentActivities(List.of(answer1, answer2));

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getQuestionId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Should return empty list when no answers")
        void getRecentActivities_noAnswers_returnsEmpty() {
            List<RecentActivityDTO> result = dashboardService.getRecentActivities(List.of());
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when answers is null")
        void getRecentActivities_nullAnswers_returnsEmpty() {
            List<RecentActivityDTO> result = dashboardService.getRecentActivities(null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should limit to 10 recent activities")
        void getRecentActivities_moreThan10_limitsTo10() {
            List<UserAnswer> answers = new ArrayList<>();
            TestAttempt attempt = new TestAttempt();
            attempt.setId(1L);

            for (int i = 0; i < 15; i++) {
                Question q = new Question();
                q.setId((long) i);

                UserAnswer answer = new UserAnswer();
                answer.setId((long) i);
                answer.setQuestion(q);
                answer.setAttempt(attempt);
                answer.setSubmittedAt(OffsetDateTime.now().minusMinutes(i));
                answer.setCorrect(i % 2 == 0);
                answers.add(answer);
            }

            List<RecentActivityDTO> result = dashboardService.getRecentActivities(answers);

            assertThat(result).hasSize(10);
        }
    }
}
