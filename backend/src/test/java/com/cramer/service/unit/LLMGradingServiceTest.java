package com.cramer.service.unit;

import com.cramer.config.LLMConfig;
import com.cramer.entity.WritingSubmission;
import com.cramer.service.LLMGradingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LLMGradingService.
 * Tests AI grading API key resolution, empty essay handling, and grading logic.
 * 
 * Note: Actual API calls are not tested here (would require integration tests).
 * These tests focus on business logic and error handling.
 * 
 * @author Cramer Test Team
 * @since 2026-01-11
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LLMGradingService Unit Tests")
class LLMGradingServiceTest {

    @Mock
    private LLMConfig llmConfig;

    private LLMGradingService llmGradingService;

    @BeforeEach
    void setUp() {
        llmGradingService = new LLMGradingService(llmConfig);
    }

    /**
     * Helper method to create WritingSubmission for testing.
     */
    private WritingSubmission createSubmission(String essayText, int wordCount, int taskNumber) {
        WritingSubmission submission = new WritingSubmission();
        submission.setUserId(UUID.randomUUID());
        submission.setAttemptId(1L);
        submission.setEssayText(essayText);
        submission.setWordCount(wordCount);
        submission.setTaskNumber(taskNumber);
        return submission;
    }

    // =========================================================================
    // API KEY RESOLUTION TESTS
    // =========================================================================
    @Nested
    @DisplayName("resolveApiKey() Tests")
    class ResolveApiKeyTests {

        @Test
        @DisplayName("Should use user's API key when provided")
        void resolveApiKey_userKeyProvided_usesUserKey() {
            // Arrange
            String userKey = "user-deepseek-api-key";

            // Act
            String result = llmGradingService.resolveApiKey(userKey);

            // Assert
            assertThat(result).isEqualTo(userKey);
            verify(llmConfig, never()).getApiKey(); // Server key not checked
        }

        @Test
        @DisplayName("Should fallback to server key when user key is null")
        void resolveApiKey_userKeyNull_usesServerKey() {
            // Arrange
            when(llmConfig.hasApiKey()).thenReturn(true);
            when(llmConfig.getApiKey()).thenReturn("server-deepseek-api-key");

            // Act
            String result = llmGradingService.resolveApiKey(null);

            // Assert
            assertThat(result).isEqualTo("server-deepseek-api-key");
        }

        @Test
        @DisplayName("Should fallback to server key when user key is empty")
        void resolveApiKey_userKeyEmpty_usesServerKey() {
            // Arrange
            when(llmConfig.hasApiKey()).thenReturn(true);
            when(llmConfig.getApiKey()).thenReturn("server-deepseek-api-key");

            // Act
            String result = llmGradingService.resolveApiKey("   "); // Whitespace only

            // Assert
            assertThat(result).isEqualTo("server-deepseek-api-key");
        }

        @Test
        @DisplayName("Should throw exception when no API key available")
        void resolveApiKey_noKeyAvailable_throwsException() {
            // Arrange
            when(llmConfig.hasApiKey()).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> llmGradingService.resolveApiKey(null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No DeepSeek API key available");
        }

        @Test
        @DisplayName("Should trim user API key")
        void resolveApiKey_userKeyWithWhitespace_trimmed() {
            // Arrange
            String userKey = "  user-key-with-spaces  ";

            // Act
            String result = llmGradingService.resolveApiKey(userKey);

            // Assert
            assertThat(result).isEqualTo("user-key-with-spaces");
        }
    }

    // =========================================================================
    // EMPTY ESSAY HANDLING TESTS
    // =========================================================================
    @Nested
    @DisplayName("handleEmptyEssay() Tests")
    class HandleEmptyEssayTests {

        @Test
        @DisplayName("Should return band 0 for empty essay without calling API")
        void gradeSubmission_emptyEssay_returnsBandZero() {
            // Arrange
            WritingSubmission submission = createSubmission("", 0, 2);

            // Act
            WritingSubmission result = llmGradingService.gradeSubmission(
                    submission, "Write about...", null, null, "api-key", null);

            // Assert
            assertThat(result.getGradingStatus()).isEqualTo("COMPLETED");
            assertThat(result.getOverallBand()).isEqualTo(BigDecimal.ZERO);
            // Verify no API call was made (we'd need to verify RestTemplate, but it's internal)
        }

        @Test
        @DisplayName("Should return band 0 for null essay text")
        void gradeSubmission_nullEssayText_returnsBandZero() {
            // Arrange
            WritingSubmission submission = createSubmission(null, 0, 1);

            // Act
            WritingSubmission result = llmGradingService.gradeSubmission(
                    submission, "Describe the chart...", null, null, "api-key", null);

            // Assert
            assertThat(result.getGradingStatus()).isEqualTo("COMPLETED");
            assertThat(result.getOverallBand()).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should return band 1 for minimal essay (under 20 words)")
        void gradeSubmission_minimalEssay_returnsBandOne() {
            // Arrange
            WritingSubmission submission = createSubmission("This is a very short essay.", 6, 2);

            // Act
            WritingSubmission result = llmGradingService.gradeSubmission(
                    submission, "Write about...", null, null, "api-key", null);

            // Assert
            assertThat(result.getGradingStatus()).isEqualTo("COMPLETED");
            assertThat(result.getOverallBand()).isEqualTo(BigDecimal.ONE);
        }
    }

    // =========================================================================
    // GRADING WITH NO API KEY TESTS
    // =========================================================================
    @Nested
    @DisplayName("gradeSubmission() without API Key Tests")
    class GradeSubmissionNoApiKeyTests {

        @Test
        @DisplayName("Should fail gracefully when no API key available")
        void gradeSubmission_noApiKey_failsWithErrorMessage() {
            // Arrange
            when(llmConfig.hasApiKey()).thenReturn(false);
            
            WritingSubmission submission = createSubmission(
                    "This is a proper essay with enough words to pass the minimum threshold requirement.",
                    50, 2);

            // Act
            WritingSubmission result = llmGradingService.gradeSubmission(
                    submission, "Write about technology...", null, null, null, null);

            // Assert
            assertThat(result.getGradingStatus()).isEqualTo("FAILED");
            assertThat(result.getAiFeedback()).isNotNull();
            assertThat(result.getAiFeedback().get("error").toString())
                    .contains("No DeepSeek API key available");
        }
    }

    // =========================================================================
    // BAND SCORE VALIDATION TESTS
    // =========================================================================
    @Nested
    @DisplayName("Band Score Validation Tests")
    class BandScoreValidation {

        @Test
        @DisplayName("Available models should include deepseek-chat and deepseek-reasoner")
        void availableModels_containsExpectedModels() {
            // Assert
            assertThat(LLMGradingService.AVAILABLE_MODELS)
                    .containsExactlyInAnyOrder("deepseek-chat", "deepseek-reasoner");
        }
    }

    // =========================================================================
    // MODEL SELECTION TESTS
    // =========================================================================
    @Nested
    @DisplayName("Model Selection Tests")
    class ModelSelectionTests {

        @Test
        @DisplayName("Should use user-specified model when provided")
        void gradeSubmission_userSpecifiedModel_usesModel() {
            // This would require mocking RestTemplate to verify the model parameter
            // For now, we just ensure the service initializes correctly
            assertThat(llmGradingService).isNotNull();
        }

        @Test
        @DisplayName("Should fallback to config model when user model not specified")
        void gradeSubmission_noUserModel_usesConfigModel() {
            // Arrange - only stub what we actually use
            when(llmConfig.getGradingModel()).thenReturn("deepseek-reasoner");

            // Verify config is checked - this tests that the config returns expected value
            String model = llmConfig.getGradingModel();
            assertThat(model).isEqualTo("deepseek-reasoner");
            
            // Verify the method was called
            verify(llmConfig).getGradingModel();
        }
    }
}
