package com.cramer.service.unit;

import com.cramer.dto.QuotaStatusDTO;
import com.cramer.entity.SkillQuota;
import com.cramer.entity.UserQuota;
import com.cramer.repository.SkillQuotaRepository;
import com.cramer.repository.UserQuotaRepository;
import com.cramer.service.implement.QuotaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for QuotaServiceImpl.
 * Tests quota tracking, cap checking, and increment operations.
 * 
 * @author Cramer Test Team
 * @since 2026-01-25
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QuotaService Unit Tests")
class QuotaServiceTest {

    @Mock
    private UserQuotaRepository userQuotaRepository;

    @Mock
    private SkillQuotaRepository skillQuotaRepository;

    @InjectMocks
    private QuotaServiceImpl quotaService;

    private UUID testUserId;
    private LocalDate currentMonth;
    private UserQuota mockUserQuota;
    private SkillQuota mockSkillQuota;

    @BeforeEach
    void setUp() {
        testUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        currentMonth = LocalDate.now().withDayOfMonth(1);

        mockUserQuota = UserQuota.builder()
                .id(1L)
                .userId(testUserId)
                .quotaMonth(currentMonth)
                .attemptCount(10)
                .attemptAiCount(5)
                .build();

        mockSkillQuota = SkillQuota.builder()
                .id(1L)
                .userId(testUserId)
                .skill(SkillQuota.Skill.READING)
                .quotaMonth(currentMonth)
                .attemptCount(5)
                .attemptAiCount(1)
                .build();
    }

    // =========================================================================
    // CAN ATTEMPT TESTS
    // =========================================================================
    @Nested
    @DisplayName("canAttempt() Tests")
    class CanAttemptTests {

        @Test
        @DisplayName("Should return true when under all caps")
        void canAttempt_underCaps_returnsTrue() {
            when(userQuotaRepository.findByUserIdAndQuotaMonth(testUserId, currentMonth))
                    .thenReturn(Optional.of(mockUserQuota));
            when(skillQuotaRepository.findByUserIdAndSkillAndQuotaMonth(
                    testUserId, SkillQuota.Skill.READING, currentMonth))
                    .thenReturn(Optional.of(mockSkillQuota));

            boolean result = quotaService.canAttempt(testUserId, "reading", false);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true when no quota record exists")
        void canAttempt_noQuotaRecord_returnsTrue() {
            when(userQuotaRepository.findByUserIdAndQuotaMonth(testUserId, currentMonth))
                    .thenReturn(Optional.empty());
            when(skillQuotaRepository.findByUserIdAndSkillAndQuotaMonth(
                    testUserId, SkillQuota.Skill.READING, currentMonth))
                    .thenReturn(Optional.empty());

            boolean result = quotaService.canAttempt(testUserId, "reading", false);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when global cap is hit")
        void canAttempt_globalCapHit_returnsFalse() {
            mockUserQuota.setAttemptCount(UserQuota.GLOBAL_ATTEMPT_CAP);

            when(userQuotaRepository.findByUserIdAndQuotaMonth(testUserId, currentMonth))
                    .thenReturn(Optional.of(mockUserQuota));

            boolean result = quotaService.canAttempt(testUserId, "reading", false);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when local cap is hit")
        void canAttempt_localCapHit_returnsFalse() {
            mockSkillQuota.setAttemptCount(SkillQuota.LOCAL_ATTEMPT_CAP);

            when(userQuotaRepository.findByUserIdAndQuotaMonth(testUserId, currentMonth))
                    .thenReturn(Optional.of(mockUserQuota));
            when(skillQuotaRepository.findByUserIdAndSkillAndQuotaMonth(
                    testUserId, SkillQuota.Skill.READING, currentMonth))
                    .thenReturn(Optional.of(mockSkillQuota));

            boolean result = quotaService.canAttempt(testUserId, "reading", false);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should check AI caps when isAI is true")
        void canAttempt_aiMode_checksAiCaps() {
            mockUserQuota.setAttemptAiCount(UserQuota.GLOBAL_ATTEMPT_AI_CAP);

            when(userQuotaRepository.findByUserIdAndQuotaMonth(testUserId, currentMonth))
                    .thenReturn(Optional.of(mockUserQuota));

            boolean result = quotaService.canAttempt(testUserId, "writing", true);

            assertThat(result).isFalse();
        }
    }

    // =========================================================================
    // IS GLOBAL CAP HIT TESTS
    // =========================================================================
    @Nested
    @DisplayName("isGlobalCapHit() Tests")
    class IsGlobalCapHitTests {

        @Test
        @DisplayName("Should return false when under cap")
        void isGlobalCapHit_underCap_returnsFalse() {
            mockUserQuota.setAttemptCount(30);

            when(userQuotaRepository.findByUserIdAndQuotaMonth(testUserId, currentMonth))
                    .thenReturn(Optional.of(mockUserQuota));

            boolean result = quotaService.isGlobalCapHit(testUserId, false);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true when at cap")
        void isGlobalCapHit_atCap_returnsTrue() {
            mockUserQuota.setAttemptCount(60);

            when(userQuotaRepository.findByUserIdAndQuotaMonth(testUserId, currentMonth))
                    .thenReturn(Optional.of(mockUserQuota));

            boolean result = quotaService.isGlobalCapHit(testUserId, false);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when no quota exists")
        void isGlobalCapHit_noQuota_returnsFalse() {
            when(userQuotaRepository.findByUserIdAndQuotaMonth(testUserId, currentMonth))
                    .thenReturn(Optional.empty());

            boolean result = quotaService.isGlobalCapHit(testUserId, false);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should check AI cap when isAI is true")
        void isGlobalCapHit_aiMode_checksAiCap() {
            mockUserQuota.setAttemptAiCount(30);

            when(userQuotaRepository.findByUserIdAndQuotaMonth(testUserId, currentMonth))
                    .thenReturn(Optional.of(mockUserQuota));

            boolean result = quotaService.isGlobalCapHit(testUserId, true);

            assertThat(result).isTrue();
        }
    }

    // =========================================================================
    // IS LOCAL CAP HIT TESTS
    // =========================================================================
    @Nested
    @DisplayName("isLocalCapHit() Tests")
    class IsLocalCapHitTests {

        @Test
        @DisplayName("Should return false when under cap")
        void isLocalCapHit_underCap_returnsFalse() {
            mockSkillQuota.setAttemptCount(10);

            when(skillQuotaRepository.findByUserIdAndSkillAndQuotaMonth(
                    testUserId, SkillQuota.Skill.READING, currentMonth))
                    .thenReturn(Optional.of(mockSkillQuota));

            boolean result = quotaService.isLocalCapHit(testUserId, "READING", false);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true when at cap")
        void isLocalCapHit_atCap_returnsTrue() {
            mockSkillQuota.setAttemptCount(20);

            when(skillQuotaRepository.findByUserIdAndSkillAndQuotaMonth(
                    testUserId, SkillQuota.Skill.READING, currentMonth))
                    .thenReturn(Optional.of(mockSkillQuota));

            boolean result = quotaService.isLocalCapHit(testUserId, "READING", false);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when no quota exists")
        void isLocalCapHit_noQuota_returnsFalse() {
            when(skillQuotaRepository.findByUserIdAndSkillAndQuotaMonth(
                    testUserId, SkillQuota.Skill.LISTENING, currentMonth))
                    .thenReturn(Optional.empty());

            boolean result = quotaService.isLocalCapHit(testUserId, "LISTENING", false);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should check AI cap when isAI is true")
        void isLocalCapHit_aiMode_checksAiCap() {
            mockSkillQuota.setAttemptAiCount(3);

            when(skillQuotaRepository.findByUserIdAndSkillAndQuotaMonth(
                    testUserId, SkillQuota.Skill.WRITING, currentMonth))
                    .thenReturn(Optional.of(SkillQuota.builder()
                            .skill(SkillQuota.Skill.WRITING)
                            .attemptAiCount(3)
                            .build()));

            boolean result = quotaService.isLocalCapHit(testUserId, "WRITING", true);

            assertThat(result).isTrue();
        }
    }

    // =========================================================================
    // INCREMENT ATTEMPT TESTS
    // =========================================================================
    @Nested
    @DisplayName("incrementAttempt() Tests")
    class IncrementAttemptTests {

        @Test
        @DisplayName("Should increment regular attempt counts")
        void incrementAttempt_regular_incrementsBothCounts() {
            when(userQuotaRepository.findByUserIdAndQuotaMonth(testUserId, currentMonth))
                    .thenReturn(Optional.of(mockUserQuota));
            when(skillQuotaRepository.findByUserIdAndSkillAndQuotaMonth(
                    testUserId, SkillQuota.Skill.READING, currentMonth))
                    .thenReturn(Optional.of(mockSkillQuota));

            quotaService.incrementAttempt(testUserId, "reading", false);

            verify(userQuotaRepository).incrementAttemptCount(testUserId, currentMonth);
            verify(skillQuotaRepository).incrementAttemptCount(
                    testUserId, SkillQuota.Skill.READING, currentMonth);
        }

        @Test
        @DisplayName("Should increment AI attempt counts when isAI is true")
        void incrementAttempt_ai_incrementsAiCounts() {
            when(userQuotaRepository.findByUserIdAndQuotaMonth(testUserId, currentMonth))
                    .thenReturn(Optional.of(mockUserQuota));
            when(skillQuotaRepository.findByUserIdAndSkillAndQuotaMonth(
                    testUserId, SkillQuota.Skill.WRITING, currentMonth))
                    .thenReturn(Optional.of(SkillQuota.builder()
                            .skill(SkillQuota.Skill.WRITING)
                            .attemptCount(0)
                            .attemptAiCount(0)
                            .build()));

            quotaService.incrementAttempt(testUserId, "writing", true);

            verify(userQuotaRepository).incrementAttemptAiCount(testUserId, currentMonth);
            verify(skillQuotaRepository).incrementAttemptAiCount(
                    testUserId, SkillQuota.Skill.WRITING, currentMonth);
        }

        @Test
        @DisplayName("Should create quota records if not exists")
        void incrementAttempt_noQuota_createsRecords() {
            when(userQuotaRepository.findByUserIdAndQuotaMonth(testUserId, currentMonth))
                    .thenReturn(Optional.empty());
            when(userQuotaRepository.save(any(UserQuota.class)))
                    .thenAnswer(invocation -> {
                        UserQuota q = invocation.getArgument(0);
                        q.setId(1L);
                        return q;
                    });

            when(skillQuotaRepository.findByUserIdAndSkillAndQuotaMonth(
                    testUserId, SkillQuota.Skill.READING, currentMonth))
                    .thenReturn(Optional.empty());
            when(skillQuotaRepository.save(any(SkillQuota.class)))
                    .thenAnswer(invocation -> {
                        SkillQuota q = invocation.getArgument(0);
                        q.setId(1L);
                        return q;
                    });

            quotaService.incrementAttempt(testUserId, "reading", false);

            verify(userQuotaRepository).save(any(UserQuota.class));
            verify(skillQuotaRepository).save(any(SkillQuota.class));
        }
    }

    // =========================================================================
    // GET QUOTA STATUS TESTS
    // =========================================================================
    @Nested
    @DisplayName("getQuotaStatus() Tests")
    class GetQuotaStatusTests {

        @Test
        @DisplayName("Should return quota status DTO")
        void getQuotaStatus_hasData_returnsDTO() {
            when(userQuotaRepository.findByUserIdAndQuotaMonth(eq(testUserId), any(LocalDate.class)))
                    .thenReturn(Optional.of(mockUserQuota));
            when(skillQuotaRepository.findAllByUserIdAndQuotaMonth(eq(testUserId), any(LocalDate.class)))
                    .thenReturn(List.of(mockSkillQuota));

            QuotaStatusDTO result = quotaService.getQuotaStatus(testUserId);

            assertThat(result).isNotNull();
            assertThat(result.getGlobalAttempt()).isEqualTo(10);
            assertThat(result.getGlobalAttemptAI()).isEqualTo(5);
        }
    }
}
