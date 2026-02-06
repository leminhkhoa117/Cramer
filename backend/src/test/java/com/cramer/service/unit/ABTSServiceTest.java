package com.cramer.service.unit;

import com.cramer.config.OpenRouterConfig;
import com.cramer.dto.abts.*;
import com.cramer.dto.abts.GenerationRequestDTO.DifficultyLevel;
import com.cramer.dto.abts.GenerationRequestDTO.ExplanationLanguage;
import com.cramer.dto.abts.GenerationRequestDTO.GenerationScope;
import com.cramer.dto.abts.GenerationRequestDTO.SkillType;
import com.cramer.entity.TestSet;
import com.cramer.repository.HashtagRepository;
import com.cramer.repository.IeltsTestRepository;
import com.cramer.repository.SectionRepository;
import com.cramer.repository.TestSetRepository;
import com.cramer.service.HashtagService;
import com.cramer.service.abts.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ABTSService.
 * Tests core orchestration logic for AI-Based Test Generation.
 * 
 * Note: This service is highly complex with streaming and external API calls.
 * This file focuses on testable business logic - template management, status, etc.
 * Streaming generation methods are better suited for integration tests.
 * 
 * @author Cramer Test Team
 * @since 2026-01-31
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ABTSService Unit Tests")
class ABTSServiceTest {

    @Mock
    private OpenRouterConfig openRouterConfig;

    @Mock
    private OpenRouterClient openRouterClient;

    @Mock
    private PromptBuilderService promptBuilderService;

    @Mock
    private JsonValidatorService jsonValidatorService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private TestSetRepository testSetRepository;

    @Mock
    private IeltsTestRepository ieltsTestRepository;

    @Mock
    private HashtagRepository hashtagRepository;

    @Mock
    private HashtagService hashtagService;

    @Mock
    private SectionRepository sectionRepository;

    @InjectMocks
    private ABTSService abtsService;

    private GenerationRequestDTO validReadingRequest;

    @BeforeEach
    void setUp() {
        validReadingRequest = new GenerationRequestDTO();
        validReadingRequest.setSkill(SkillType.READING);
        validReadingRequest.setScope(GenerationScope.SINGLE_PART);
        validReadingRequest.setTopic("Climate change effects on agriculture");
        validReadingRequest.setDifficulty(DifficultyLevel.INTERMEDIATE);
        validReadingRequest.setExplanationLanguage(ExplanationLanguage.VI);
        validReadingRequest.setPartNumber(1);
    }

    // =========================================================================
    // GENERATE - BASIC VALIDATION TESTS
    // =========================================================================
    @Nested
    @DisplayName("generate() - Input Validation")
    class GenerateValidationTests {

        @Test
        @DisplayName("Should return error when API key is not configured")
        void generate_noApiKey_returnsAuthError() {
            // Arrange
            when(openRouterConfig.hasApiKey()).thenReturn(false);

            // Act
            GenerationResponseDTO result = abtsService.generate(validReadingRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(GenerationResponseDTO.GenerationStatus.FAILED);
            assertThat(result.getErrors()).isNotEmpty();
            assertThat(result.getErrors().get(0)).contains("API key");
        }

        @Test
        @DisplayName("Should return error for SPEAKING skill (not implemented)")
        void generate_speakingSkill_returnsNotImplemented() {
            // Arrange
            when(openRouterConfig.hasApiKey()).thenReturn(true);
            
            GenerationRequestDTO speakingRequest = new GenerationRequestDTO();
            speakingRequest.setSkill(SkillType.SPEAKING);

            // Act
            GenerationResponseDTO result = abtsService.generate(speakingRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(GenerationResponseDTO.GenerationStatus.FAILED);
        }
    }

    // =========================================================================
    // GET TEMPLATE CATEGORIES TESTS
    // =========================================================================
    @Nested
    @DisplayName("getTemplateCategories() - Topic Template Categories")
    class GetTemplateCategoriesTests {

        @Test
        @DisplayName("Should return categories from database when available")
        void getTemplateCategories_dbHasData_returnsCategoriesFromDb() {
            // Arrange
            List<Map<String, Object>> dbCategories = new ArrayList<>();
            Map<String, Object> category1 = new HashMap<>();
            category1.put("id", "science");
            category1.put("name", "Science & Technology");
            category1.put("icon", "flask");
            category1.put("template_count", 10L);
            dbCategories.add(category1);

            Map<String, Object> category2 = new HashMap<>();
            category2.put("id", "education");
            category2.put("name", "Education");
            category2.put("icon", "book");
            category2.put("template_count", 8L);
            dbCategories.add(category2);

            when(jdbcTemplate.queryForList(anyString())).thenReturn(dbCategories);

            // Act
            List<Map<String, Object>> result = abtsService.getTemplateCategories();

            // Assert
            assertThat(result).isNotEmpty();
            assertThat(result).hasSize(2);
            verify(jdbcTemplate).queryForList(anyString());
        }

        @Test
        @DisplayName("Should return fallback categories when database query fails")
        void getTemplateCategories_dbFails_returnsFallbackCategories() {
            // Arrange
            when(jdbcTemplate.queryForList(anyString())).thenThrow(new RuntimeException("DB connection failed"));

            // Act
            List<Map<String, Object>> result = abtsService.getTemplateCategories();

            // Assert
            assertThat(result).isNotEmpty();
            // Fallback categories should have at least 1 item
            assertThat(result.size()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Should return fallback when database returns empty list")
        void getTemplateCategories_dbEmpty_returnsFallback() {
            // Arrange
            when(jdbcTemplate.queryForList(anyString())).thenReturn(Collections.emptyList());

            // Act
            List<Map<String, Object>> result = abtsService.getTemplateCategories();

            // Assert
            assertThat(result).isNotEmpty();
        }
    }

    // =========================================================================
    // GET TEMPLATES BY CATEGORY TESTS
    // =========================================================================
    @Nested
    @DisplayName("getTemplatesByCategory() - Templates for Category")
    class GetTemplatesByCategoryTests {

        @Test
        @DisplayName("Should return templates for valid category")
        void getTemplatesByCategory_validCategory_returnsTemplates() {
            // Arrange
            List<Map<String, Object>> dbTemplates = new ArrayList<>();
            Map<String, Object> template = new HashMap<>();
            template.put("id", "climate_change");
            template.put("name", "Climate Change Impact");
            template.put("description", "Effects of global warming on ecosystems");
            template.put("use_count", 15);
            dbTemplates.add(template);

            when(jdbcTemplate.queryForList(anyString(), eq("science"))).thenReturn(dbTemplates);

            // Act
            List<Map<String, Object>> result = abtsService.getTemplatesByCategory("science");

            // Assert
            assertThat(result).isNotEmpty();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).get("id")).isEqualTo("climate_change");
            verify(jdbcTemplate).queryForList(anyString(), eq("science"));
        }

        @Test
        @DisplayName("Should return fallback templates when database fails")
        void getTemplatesByCategory_dbFails_returnsFallback() {
            // Arrange
            when(jdbcTemplate.queryForList(anyString(), anyString()))
                    .thenThrow(new RuntimeException("DB error"));

            // Act
            List<Map<String, Object>> result = abtsService.getTemplatesByCategory("science");

            // Assert
            assertThat(result).isNotEmpty();
        }
    }

    // =========================================================================
    // INCREMENT TEMPLATE USE COUNT TESTS
    // =========================================================================
    @Nested
    @DisplayName("incrementTemplateUseCount() - Usage Tracking")
    class IncrementTemplateUseCountTests {

        @Test
        @DisplayName("Should increment use count for valid template")
        void incrementTemplateUseCount_validTemplate_incrementsCount() {
            // Arrange
            when(jdbcTemplate.update(anyString(), anyString())).thenReturn(1);

            // Act
            abtsService.incrementTemplateUseCount("climate_change");

            // Assert
            verify(jdbcTemplate).update(anyString(), eq("climate_change"));
        }

        @Test
        @DisplayName("Should not throw when database update fails")
        void incrementTemplateUseCount_dbFails_doesNotThrow() {
            // Arrange
            when(jdbcTemplate.update(anyString(), anyString()))
                    .thenThrow(new RuntimeException("DB error"));

            // Act & Assert - should not throw
            abtsService.incrementTemplateUseCount("invalid_template");
        }
    }

    // =========================================================================
    // GET STATUS TESTS
    // =========================================================================
    @Nested
    @DisplayName("getStatus() - Service Status")
    class GetStatusTests {

        @Test
        @DisplayName("Should return service status with version info")
        void getStatus_returns_versionInfo() {
            // Arrange
            when(openRouterConfig.hasApiKey()).thenReturn(true);
            when(openRouterConfig.getBaseUrl()).thenReturn("https://openrouter.ai/api/v1");
            when(openRouterConfig.getGenerationModel()).thenReturn("google/gemini-2.0-flash-001");
            when(openRouterConfig.getRegenerationModel()).thenReturn("deepseek/deepseek-chat");
            when(openRouterConfig.isStreamingEnabled()).thenReturn(true);
            when(openRouterConfig.getTimeoutMs()).thenReturn(120000);

            // Act
            Map<String, Object> result = abtsService.getStatus();

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).containsKey("version");
            assertThat(result).containsKey("apiKeyConfigured");
            assertThat(result.get("apiKeyConfigured")).isEqualTo(true);
        }

        @Test
        @DisplayName("Should include model configuration in status")
        void getStatus_includesModelConfig() {
            // Arrange
            when(openRouterConfig.hasApiKey()).thenReturn(true);
            when(openRouterConfig.getBaseUrl()).thenReturn("https://openrouter.ai/api/v1");
            when(openRouterConfig.getGenerationModel()).thenReturn("google/gemini-2.0-flash-001");
            when(openRouterConfig.getRegenerationModel()).thenReturn("deepseek/deepseek-chat");
            when(openRouterConfig.isStreamingEnabled()).thenReturn(true);
            when(openRouterConfig.getTimeoutMs()).thenReturn(120000);

            // Act
            Map<String, Object> result = abtsService.getStatus();

            // Assert
            assertThat(result).containsKey("defaultGenerationModel");
            assertThat(result).containsKey("defaultRegenerationModel");
            assertThat(result.get("defaultGenerationModel")).isEqualTo("google/gemini-2.0-flash-001");
        }
    }

    // =========================================================================
    // GET AVAILABLE MODELS TESTS
    // =========================================================================
    @Nested
    @DisplayName("getAvailableModels() - AI Model List")
    class GetAvailableModelsTests {

        @Test
        @DisplayName("Should return models from OpenRouter client")
        void getAvailableModels_clientReturnsModels_returnsList() {
            // Arrange
            List<Map<String, Object>> mockModels = new ArrayList<>();
            Map<String, Object> model1 = new HashMap<>();
            model1.put("id", "google/gemini-2.0-flash-001");
            model1.put("name", "Gemini 2.0 Flash");
            model1.put("context_length", 128000);
            mockModels.add(model1);

            when(openRouterClient.fetchAvailableModels()).thenReturn(mockModels);

            // Act
            List<Map<String, Object>> result = abtsService.getAvailableModels();

            // Assert
            assertThat(result).isNotEmpty();
            verify(openRouterClient).fetchAvailableModels();
        }

        @Test
        @DisplayName("Should return fallback models when API returns null")
        void getAvailableModels_apiReturnsNull_returnsFallback() {
            // Arrange
            when(openRouterClient.fetchAvailableModels()).thenReturn(null);

            // Act
            List<Map<String, Object>> result = abtsService.getAvailableModels();

            // Assert
            assertThat(result).isNotEmpty();
            // Fallback should include several default models
            assertThat(result.size()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Should return fallback models when API returns empty list")
        void getAvailableModels_apiReturnsEmpty_returnsFallback() {
            // Arrange
            when(openRouterClient.fetchAvailableModels()).thenReturn(Collections.emptyList());

            // Act
            List<Map<String, Object>> result = abtsService.getAvailableModels();

            // Assert
            assertThat(result).isNotEmpty();
        }
    }

    // =========================================================================
    // SAVE CONTENT - VALIDATION TESTS
    // =========================================================================
    @Nested
    @DisplayName("saveContent() - Input Validation")
    class SaveContentValidationTests {

        @Test
        @DisplayName("Should return error when skill is null")
        void saveContent_nullSkill_returnsError() {
            // Arrange
            SaveContentRequestDTO request = new SaveContentRequestDTO();
            request.setPartNumber(1);
            request.setContent(new GeneratedContentDTO());
            // skill is null

            // Act
            SaveContentResponseDTO result = abtsService.saveContent(request, "admin-id");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isNotNull();
        }

        @Test
        @DisplayName("Should return error when content is null")
        void saveContent_nullContent_returnsError() {
            // Arrange
            SaveContentRequestDTO request = new SaveContentRequestDTO();
            request.setSkill("READING");
            request.setPartNumber(1);
            // content is null

            // Act
            SaveContentResponseDTO result = abtsService.saveContent(request, "admin-id");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("Should return error when partNumber is null")
        void saveContent_nullPartNumber_returnsError() {
            // Arrange
            SaveContentRequestDTO request = new SaveContentRequestDTO();
            request.setSkill("READING");
            request.setContent(new GeneratedContentDTO());
            // partNumber is null

            // Act
            SaveContentResponseDTO result = abtsService.saveContent(request, "admin-id");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
        }
    }

    // =========================================================================
    // SAVE CONTENT - TESTSET RESOLUTION TESTS
    // =========================================================================
    @Nested
    @DisplayName("saveContent() - TestSet Resolution")
    class SaveContentTestSetTests {

        @Test
        @DisplayName("Should find existing TestSet by setCode")
        void saveContent_existingSetCode_usesExistingTestSet() {
            // Arrange
            SaveContentRequestDTO request = new SaveContentRequestDTO();
            request.setSkill("READING");
            request.setPartNumber(1);
            request.setSetCode("ai_generated");
            request.setContent(new GeneratedContentDTO());

            TestSet existingTestSet = new TestSet();
            existingTestSet.setId(1L);
            existingTestSet.setCode("ai_generated");

            when(testSetRepository.findByCode("ai_generated"))
                    .thenReturn(Optional.of(existingTestSet));

            // Act
            abtsService.saveContent(request, "admin-id");

            // Assert
            verify(testSetRepository).findByCode("ai_generated");
        }
    }
}
