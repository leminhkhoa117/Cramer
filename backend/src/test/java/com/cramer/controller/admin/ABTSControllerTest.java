package com.cramer.controller.admin;

import com.cramer.config.JwtAuthFilter;
import com.cramer.config.SecurityConfig;
import com.cramer.dto.abts.*;
import com.cramer.exception.GlobalExceptionHandler;
import com.cramer.service.abts.ABTSService;
import com.cramer.service.abts.RefinementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ABTSController.
 * Tests AI-Based Test Generation System endpoints.
 * 
 * @author Cramer Test Team
 * @since 2026-01-31
 */
@WebMvcTest(ABTSController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
@DisplayName("ABTSController Unit Tests")
class ABTSControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ABTSService abtsService;

    @MockBean
    private RefinementService refinementService;

    @MockBean
    private com.cramer.util.JwtUtil jwtUtil;

    private static final String DEFAULT_ADMIN_ID = "550e8400-e29b-41d4-a716-446655440000";

    private GenerationRequestDTO validReadingRequest;

    @BeforeEach
    void setUp() {
        // Setup Reading request with all required fields
        validReadingRequest = new GenerationRequestDTO();
        validReadingRequest.setSkill(GenerationRequestDTO.SkillType.READING);
        validReadingRequest.setScope(GenerationRequestDTO.GenerationScope.SINGLE_PART);
        validReadingRequest.setTopic("Climate change effects on agriculture");
        validReadingRequest.setDifficulty(GenerationRequestDTO.DifficultyLevel.INTERMEDIATE);
        validReadingRequest.setExplanationLanguage(GenerationRequestDTO.ExplanationLanguage.VI);
    }

    private ResultActions performPost(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .header("X-User-Id", DEFAULT_ADMIN_ID)
                .with(jwt().jwt(jwtBuilder -> jwtBuilder
                        .subject(DEFAULT_ADMIN_ID)
                        .claim("aud", "authenticated"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    // =========================================================================
    // POST /api/admin/abts/generate/reading TESTS
    // =========================================================================
    @Nested
    @DisplayName("POST /api/admin/abts/generate/reading")
    class GenerateReadingTests {

        @Test
        @DisplayName("Should return 200 and generated reading content")
        void generateReading_valid_returns200() throws Exception {
            // Arrange
            GenerationResponseDTO response = new GenerationResponseDTO();
            response.setStatus(GenerationResponseDTO.GenerationStatus.SUCCESS);

            when(abtsService.generate(any(GenerationRequestDTO.class))).thenReturn(response);

            // Act & Assert
            performPost("/api/admin/abts/generate/reading", validReadingRequest)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"));

            verify(abtsService).generate(any(GenerationRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 200 with error when generation fails")
        void generateReading_failure_returnsError() throws Exception {
            // Arrange
            GenerationResponseDTO response = GenerationResponseDTO.error(
                    "API_ERROR", "OpenRouter API connection failed", true);

            when(abtsService.generate(any(GenerationRequestDTO.class))).thenReturn(response);

            // Act & Assert
            performPost("/api/admin/abts/generate/reading", validReadingRequest)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("FAILED"));
        }
    }

    // =========================================================================
    // POST /api/admin/abts/generate/listening TESTS
    // =========================================================================
    @Nested
    @DisplayName("POST /api/admin/abts/generate/listening")
    class GenerateListeningTests {

        @Test
        @DisplayName("Should return 200 and generated listening content")
        void generateListening_valid_returns200() throws Exception {
            // Arrange
            GenerationRequestDTO listeningRequest = new GenerationRequestDTO();
            listeningRequest.setSkill(GenerationRequestDTO.SkillType.LISTENING);
            listeningRequest.setScope(GenerationRequestDTO.GenerationScope.SINGLE_PART);
            listeningRequest.setTopic("University campus tour");
            listeningRequest.setDifficulty(GenerationRequestDTO.DifficultyLevel.INTERMEDIATE);
            listeningRequest.setExplanationLanguage(GenerationRequestDTO.ExplanationLanguage.VI);

            GenerationResponseDTO response = new GenerationResponseDTO();
            response.setStatus(GenerationResponseDTO.GenerationStatus.SUCCESS);

            when(abtsService.generate(any(GenerationRequestDTO.class))).thenReturn(response);

            // Act & Assert
            performPost("/api/admin/abts/generate/listening", listeningRequest)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"));
        }
    }

    // =========================================================================
    // POST /api/admin/abts/save TESTS
    // =========================================================================
    @Nested
    @DisplayName("POST /api/admin/abts/save")
    class SaveContentTests {

        @Test
        @DisplayName("Should return 200 and save confirmation")
        void saveContent_valid_returns200() throws Exception {
            // Arrange - Create a valid request with all required fields
            SaveContentRequestDTO request = new SaveContentRequestDTO();
            request.setSkill("READING");
            request.setExamSource("abts");
            request.setTestNumber("1");
            request.setPartNumber(1);
            // Create a minimal GeneratedContentDTO to satisfy @NotNull validation
            GeneratedContentDTO content = new GeneratedContentDTO();
            request.setContent(content);

            SaveContentResponseDTO response = SaveContentResponseDTO.success(
                    100L, "abts", Integer.valueOf(1), "READING", Integer.valueOf(1), Integer.valueOf(13));

            when(abtsService.saveContent(any(SaveContentRequestDTO.class), eq(DEFAULT_ADMIN_ID)))
                    .thenReturn(response);

            // Act & Assert
            performPost("/api/admin/abts/save", request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.sectionId").value(100));

            verify(abtsService).saveContent(any(SaveContentRequestDTO.class), eq(DEFAULT_ADMIN_ID));
        }

        @Test
        @DisplayName("Should return 200 with error when save fails")
        void saveContent_failure_returnsError() throws Exception {
            // Arrange - Create a valid request with all required fields
            SaveContentRequestDTO request = new SaveContentRequestDTO();
            request.setSkill("READING");
            request.setPartNumber(1);
            // Create a minimal GeneratedContentDTO to satisfy @NotNull validation
            GeneratedContentDTO content = new GeneratedContentDTO();
            request.setContent(content);

            SaveContentResponseDTO response = SaveContentResponseDTO.error("Duplicate section already exists");

            when(abtsService.saveContent(any(SaveContentRequestDTO.class), eq(DEFAULT_ADMIN_ID)))
                    .thenReturn(response);

            // Act & Assert
            performPost("/api/admin/abts/save", request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
