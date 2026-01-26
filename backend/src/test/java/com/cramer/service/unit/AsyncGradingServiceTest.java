package com.cramer.service.unit;

import com.cramer.dto.GradingStatusDTO;
import com.cramer.entity.CreditTransaction;
import com.cramer.entity.Section;
import com.cramer.entity.TestAttempt;
import com.cramer.entity.WritingSubmission;
import com.cramer.repository.SectionRepository;
import com.cramer.repository.WritingSubmissionRepository;
import com.cramer.service.AsyncGradingService;
import com.cramer.service.CreditService;
import com.cramer.service.LLMGradingService;
import com.cramer.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AsyncGradingService.
 * Tests async grading workflow, billing, and error handling.
 * 
 * @author Cramer Test Team
 * @since 2026-01-25
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AsyncGradingService Unit Tests")
class AsyncGradingServiceTest {

    @Mock
    private WritingSubmissionRepository writingSubmissionRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private LLMGradingService llmGradingService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private CreditService creditService;

    private AsyncGradingService asyncGradingService;

    private UUID testUserId;
    private TestAttempt mockAttempt;
    private WritingSubmission mockSubmission1;
    private WritingSubmission mockSubmission2;
    private Section mockSection1;
    private Section mockSection2;

    @BeforeEach
    void setUp() {
        asyncGradingService = new AsyncGradingService(
                writingSubmissionRepository,
                sectionRepository,
                llmGradingService,
                subscriptionService,
                creditService
        );

        ReflectionTestUtils.setField(asyncGradingService, "deepSeekApiKey", "test-api-key");
        ReflectionTestUtils.setField(asyncGradingService, "deepSeekModel", "deepseek-chat");

        testUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockAttempt = new TestAttempt();
        mockAttempt.setId(1L);
        mockAttempt.setUserId(testUserId);
        mockAttempt.setExamSource("cam17");
        mockAttempt.setTestNumber("1");
        mockAttempt.setSkill("writing");
        mockAttempt.setStatus("IN_PROGRESS");

        mockSubmission1 = new WritingSubmission();
        mockSubmission1.setId(1L);
        mockSubmission1.setAttemptId(1L);
        mockSubmission1.setTaskNumber(1);
        mockSubmission1.setEssayText("Sample essay for Task 1");
        mockSubmission1.setGradingStatus("PENDING");

        mockSubmission2 = new WritingSubmission();
        mockSubmission2.setId(2L);
        mockSubmission2.setAttemptId(1L);
        mockSubmission2.setTaskNumber(2);
        mockSubmission2.setEssayText("Sample essay for Task 2");
        mockSubmission2.setGradingStatus("PENDING");

        mockSection1 = new Section();
        mockSection1.setId(1L);
        mockSection1.setExamSource("cam17");
        mockSection1.setTestNumber(1);
        mockSection1.setSkill("writing");
        mockSection1.setPartNumber(1);
        mockSection1.setPassageText("Task 1 prompt: Describe the chart...");

        mockSection2 = new Section();
        mockSection2.setId(2L);
        mockSection2.setExamSource("cam17");
        mockSection2.setTestNumber(1);
        mockSection2.setSkill("writing");
        mockSection2.setPartNumber(2);
        mockSection2.setPassageText("Task 2 prompt: Some people believe...");
    }

    // =========================================================================
    // SUCCESSFUL GRADING TESTS
    // =========================================================================
    @Nested
    @DisplayName("gradeSubmissionsAsync() Success Tests")
    class GradeSubmissionsSuccessTests {

        @Test
        @DisplayName("Should grade both tasks successfully with subscription")
        void gradeSubmissionsAsync_bothTasksSuccess_usesSubscription() throws Exception {
            List<WritingSubmission> submissions = List.of(mockSubmission1, mockSubmission2);

            GradingStatusDTO gradingStatus = new GradingStatusDTO();
            gradingStatus.setAllowed(true);
            gradingStatus.setLimit(10);
            gradingStatus.setUsed(2);
            gradingStatus.setRemaining(8);

            when(subscriptionService.checkAIGradingAllowed(testUserId)).thenReturn(gradingStatus);
            when(sectionRepository.findByExamSourceAndTestNumberAndSkill("cam17", 1, "writing"))
                    .thenReturn(List.of(mockSection1, mockSection2));

            doAnswer(invocation -> {
                WritingSubmission sub = invocation.getArgument(0);
                sub.setGradingStatus("COMPLETED");
                sub.setOverallBand(new BigDecimal("7.0"));
                return null;
            }).when(llmGradingService).gradeSubmission(
                    any(WritingSubmission.class), anyString(), any(), any(), anyString(), anyString());

            when(writingSubmissionRepository.save(any(WritingSubmission.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            asyncGradingService.gradeSubmissionsAsync(submissions, mockAttempt, testUserId);

            Thread.sleep(500);

            verify(subscriptionService).incrementAIGradingUsage(testUserId);
            verify(creditService, never()).spendCredits(any(), anyInt(), any(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should grade and charge Lua when subscription exceeded")
        void gradeSubmissionsAsync_subscriptionExceeded_chargesLua() throws Exception {
            List<WritingSubmission> submissions = List.of(mockSubmission1);

            GradingStatusDTO gradingStatus = new GradingStatusDTO();
            gradingStatus.setAllowed(false);
            gradingStatus.setCanUseExtraWithLua(true);
            gradingStatus.setLuaBalance(100);
            gradingStatus.setLimit(5);
            gradingStatus.setUsed(5);
            gradingStatus.setRemaining(0);

            when(subscriptionService.checkAIGradingAllowed(testUserId)).thenReturn(gradingStatus);
            when(sectionRepository.findByExamSourceAndTestNumberAndSkill("cam17", 1, "writing"))
                    .thenReturn(List.of(mockSection1));

            doAnswer(invocation -> {
                WritingSubmission sub = invocation.getArgument(0);
                sub.setGradingStatus("COMPLETED");
                sub.setOverallBand(new BigDecimal("6.5"));
                return null;
            }).when(llmGradingService).gradeSubmission(
                    any(WritingSubmission.class), anyString(), any(), any(), anyString(), anyString());

            when(writingSubmissionRepository.save(any(WritingSubmission.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            asyncGradingService.gradeSubmissionsAsync(submissions, mockAttempt, testUserId);

            Thread.sleep(500);

            verify(creditService).spendCredits(
                    eq(testUserId),
                    eq(20),
                    eq(CreditTransaction.Category.AI_GRADING),
                    contains("cam17"),
                    anyString()
            );
            verify(subscriptionService, never()).incrementAIGradingUsage(any());
        }
    }

    // =========================================================================
    // FAILURE HANDLING TESTS
    // =========================================================================
    @Nested
    @DisplayName("gradeSubmissionsAsync() Failure Tests")
    class GradeSubmissionsFailureTests {

        @Test
        @DisplayName("Should mark all as FAILED when API key not configured")
        void gradeSubmissionsAsync_noApiKey_marksAllFailed() throws Exception {
            ReflectionTestUtils.setField(asyncGradingService, "deepSeekApiKey", "");

            List<WritingSubmission> submissions = List.of(mockSubmission1, mockSubmission2);

            when(writingSubmissionRepository.save(any(WritingSubmission.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            asyncGradingService.gradeSubmissionsAsync(submissions, mockAttempt, testUserId);

            Thread.sleep(100);

            verify(writingSubmissionRepository, atLeast(2)).save(argThat(sub ->
                    "FAILED".equals(sub.getGradingStatus()) &&
                            sub.getAiFeedback() != null &&
                            sub.getAiFeedback().containsKey("error")
            ));
            verify(subscriptionService, never()).checkAIGradingAllowed(any());
        }

        @Test
        @DisplayName("Should mark all as FAILED when no quota and insufficient Lua")
        void gradeSubmissionsAsync_noQuotaNoLua_marksAllFailed() throws Exception {
            List<WritingSubmission> submissions = List.of(mockSubmission1);

            GradingStatusDTO gradingStatus = new GradingStatusDTO();
            gradingStatus.setAllowed(false);
            gradingStatus.setCanUseExtraWithLua(true);
            gradingStatus.setLuaBalance(10);
            gradingStatus.setLimit(5);
            gradingStatus.setUsed(5);
            gradingStatus.setRemaining(0);

            when(subscriptionService.checkAIGradingAllowed(testUserId)).thenReturn(gradingStatus);
            when(writingSubmissionRepository.save(any(WritingSubmission.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            asyncGradingService.gradeSubmissionsAsync(submissions, mockAttempt, testUserId);

            Thread.sleep(100);

            verify(writingSubmissionRepository, atLeast(1)).save(argThat(sub ->
                    "FAILED".equals(sub.getGradingStatus())
            ));
            verify(llmGradingService, never()).gradeSubmission(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should handle LLM grading error gracefully")
        void gradeSubmissionsAsync_llmError_marksAsFailed() throws Exception {
            List<WritingSubmission> submissions = new ArrayList<>();
            submissions.add(mockSubmission1);

            GradingStatusDTO gradingStatus = new GradingStatusDTO();
            gradingStatus.setAllowed(true);
            gradingStatus.setLimit(10);
            gradingStatus.setUsed(0);
            gradingStatus.setRemaining(10);

            when(subscriptionService.checkAIGradingAllowed(testUserId)).thenReturn(gradingStatus);
            when(sectionRepository.findByExamSourceAndTestNumberAndSkill("cam17", 1, "writing"))
                    .thenReturn(List.of(mockSection1));

            doThrow(new RuntimeException("LLM API connection failed"))
                    .when(llmGradingService).gradeSubmission(
                            any(WritingSubmission.class), anyString(), any(), any(), anyString(), anyString());

            when(writingSubmissionRepository.save(any(WritingSubmission.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            asyncGradingService.gradeSubmissionsAsync(submissions, mockAttempt, testUserId);

            Thread.sleep(500);

            verify(subscriptionService, never()).incrementAIGradingUsage(any());
            verify(creditService, never()).spendCredits(any(), anyInt(), any(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should mark all as FAILED if one task fails")
        void gradeSubmissionsAsync_oneTaskFails_marksAllFailed() throws Exception {
            List<WritingSubmission> submissions = new ArrayList<>();
            submissions.add(mockSubmission1);
            submissions.add(mockSubmission2);

            GradingStatusDTO gradingStatus = new GradingStatusDTO();
            gradingStatus.setAllowed(true);
            gradingStatus.setLimit(10);
            gradingStatus.setUsed(0);
            gradingStatus.setRemaining(10);

            when(subscriptionService.checkAIGradingAllowed(testUserId)).thenReturn(gradingStatus);
            when(sectionRepository.findByExamSourceAndTestNumberAndSkill("cam17", 1, "writing"))
                    .thenReturn(List.of(mockSection1, mockSection2));

            doAnswer(invocation -> {
                WritingSubmission sub = invocation.getArgument(0);
                if (sub.getTaskNumber() == 1) {
                    sub.setGradingStatus("COMPLETED");
                    sub.setOverallBand(new BigDecimal("7.0"));
                } else {
                    sub.setGradingStatus("FAILED");
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", "Task 2 grading failed");
                    sub.setAiFeedback(error);
                }
                return null;
            }).when(llmGradingService).gradeSubmission(
                    any(WritingSubmission.class), anyString(), any(), any(), anyString(), anyString());

            when(writingSubmissionRepository.save(any(WritingSubmission.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            asyncGradingService.gradeSubmissionsAsync(submissions, mockAttempt, testUserId);

            Thread.sleep(500);

            verify(subscriptionService, never()).incrementAIGradingUsage(any());
            verify(creditService, never()).spendCredits(any(), anyInt(), any(), anyString(), anyString());
        }
    }

    // =========================================================================
    // EDGE CASES TESTS
    // =========================================================================
    @Nested
    @DisplayName("gradeSubmissionsAsync() Edge Cases")
    class GradeSubmissionsEdgeCasesTests {

        @Test
        @DisplayName("Should handle empty submission list gracefully")
        void gradeSubmissionsAsync_emptyList_doesNothing() throws Exception {
            List<WritingSubmission> submissions = List.of();

            GradingStatusDTO gradingStatus = new GradingStatusDTO();
            gradingStatus.setAllowed(true);
            gradingStatus.setLimit(10);
            gradingStatus.setUsed(0);
            gradingStatus.setRemaining(10);

            when(subscriptionService.checkAIGradingAllowed(testUserId)).thenReturn(gradingStatus);
            when(sectionRepository.findByExamSourceAndTestNumberAndSkill("cam17", 1, "writing"))
                    .thenReturn(List.of());

            asyncGradingService.gradeSubmissionsAsync(submissions, mockAttempt, testUserId);

            Thread.sleep(100);

            verify(llmGradingService, never()).gradeSubmission(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should handle missing section gracefully")
        void gradeSubmissionsAsync_missingSection_gradesWithEmptyPrompt() throws Exception {
            List<WritingSubmission> submissions = List.of(mockSubmission1);

            GradingStatusDTO gradingStatus = new GradingStatusDTO();
            gradingStatus.setAllowed(true);
            gradingStatus.setLimit(10);
            gradingStatus.setUsed(0);
            gradingStatus.setRemaining(10);

            when(subscriptionService.checkAIGradingAllowed(testUserId)).thenReturn(gradingStatus);
            when(sectionRepository.findByExamSourceAndTestNumberAndSkill("cam17", 1, "writing"))
                    .thenReturn(List.of());

            doAnswer(invocation -> {
                WritingSubmission sub = invocation.getArgument(0);
                String prompt = invocation.getArgument(1);
                assertThat(prompt).isEmpty();
                sub.setGradingStatus("COMPLETED");
                return null;
            }).when(llmGradingService).gradeSubmission(
                    any(WritingSubmission.class), anyString(), any(), any(), anyString(), anyString());

            when(writingSubmissionRepository.save(any(WritingSubmission.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            asyncGradingService.gradeSubmissionsAsync(submissions, mockAttempt, testUserId);

            Thread.sleep(500);

            verify(llmGradingService).gradeSubmission(
                    any(WritingSubmission.class), eq(""), any(), any(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should handle usage tracking error gracefully")
        void gradeSubmissionsAsync_usageTrackingError_stillCompletes() throws Exception {
            List<WritingSubmission> submissions = List.of(mockSubmission1);

            GradingStatusDTO gradingStatus = new GradingStatusDTO();
            gradingStatus.setAllowed(true);
            gradingStatus.setLimit(10);
            gradingStatus.setUsed(0);
            gradingStatus.setRemaining(10);

            when(subscriptionService.checkAIGradingAllowed(testUserId)).thenReturn(gradingStatus);
            when(sectionRepository.findByExamSourceAndTestNumberAndSkill("cam17", 1, "writing"))
                    .thenReturn(List.of(mockSection1));

            doAnswer(invocation -> {
                WritingSubmission sub = invocation.getArgument(0);
                sub.setGradingStatus("COMPLETED");
                sub.setOverallBand(new BigDecimal("7.5"));
                return null;
            }).when(llmGradingService).gradeSubmission(
                    any(WritingSubmission.class), anyString(), any(), any(), anyString(), anyString());

            when(writingSubmissionRepository.save(any(WritingSubmission.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            doThrow(new RuntimeException("Database error"))
                    .when(subscriptionService).incrementAIGradingUsage(testUserId);

            asyncGradingService.gradeSubmissionsAsync(submissions, mockAttempt, testUserId);

            Thread.sleep(500);

            assertThat(mockSubmission1.getGradingStatus()).isEqualTo("COMPLETED");
        }
    }
}
