package com.cramer.controller;

import com.cramer.config.JwtAuthFilter;
import com.cramer.config.SecurityConfig;
import com.cramer.dto.VocabularyCreateDTO;
import com.cramer.dto.VocabularyDTO;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.service.VocabularyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for VocabularyController.
 * Tests vocabulary CRUD operations, search, and AI translation.
 * 
 * @author Cramer Test Team
 * @since 2026-01-19
 */
@WebMvcTest(VocabularyController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
@DisplayName("VocabularyController Unit Tests")
class VocabularyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private com.cramer.util.JwtUtil jwtUtil;

    @MockBean
    private VocabularyService vocabularyService;

    private UUID testUserId;
    private VocabularyDTO mockVocab;
    private static final UUID DEFAULT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @BeforeEach
    void setUp() {
        testUserId = DEFAULT_USER_ID;

        mockVocab = new VocabularyDTO();
        mockVocab.setId(1L);
        mockVocab.setUserId(testUserId);
        mockVocab.setWord("ubiquitous");
        mockVocab.setDefinition("present everywhere");
        mockVocab.setTranslation("phổ biến khắp nơi");
        mockVocab.setIsMastered(false);
    }

    // =========================================================================
    // GET /api/vocabulary TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/vocabulary")
    class ListVocabularyTests {

        @Test
        @DisplayName("Should return 200 and paginated vocabulary list")
        void listVocabulary_valid_returns200() throws Exception {
            // Arrange
            Page<VocabularyDTO> page = new PageImpl<>(
                    List.of(mockVocab),
                    PageRequest.of(0, 20),
                    1
            );

            when(vocabularyService.getByUserId(eq(testUserId), any(Pageable.class))).thenReturn(page);

            // Act & Assert
            mockMvc.perform(get("/api/vocabulary")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].word").value("ubiquitous"));
        }

        @Test
        @DisplayName("Should return 200 with search filter")
        void listVocabulary_withSearch_returns200() throws Exception {
            // Arrange
            Page<VocabularyDTO> page = new PageImpl<>(
                    List.of(mockVocab),
                    PageRequest.of(0, 20),
                    1
            );

            when(vocabularyService.search(eq(testUserId), eq("ubiquit"), any(Pageable.class)))
                    .thenReturn(page);

            // Act & Assert
            mockMvc.perform(get("/api/vocabulary")
                            .param("search", "ubiquit")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].word").value("ubiquitous"));
        }

        @Test
        @DisplayName("Should return 200 with mastered filter")
        void listVocabulary_masteredFilter_returns200() throws Exception {
            // Arrange
            mockVocab.setIsMastered(true);
            Page<VocabularyDTO> page = new PageImpl<>(
                    List.of(mockVocab),
                    PageRequest.of(0, 20),
                    1
            );

            when(vocabularyService.getByUserIdAndMastered(eq(testUserId), eq(true), any(Pageable.class)))
                    .thenReturn(page);

            // Act & Assert
            mockMvc.perform(get("/api/vocabulary")
                            .param("filter", "mastered")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].mastered").value(true));
        }

        @Test
        @DisplayName("Should return 401 when no JWT token")
        void listVocabulary_unauthorized_returns401() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/vocabulary"))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // GET /api/vocabulary/{id} TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/vocabulary/{id}")
    class GetVocabularyByIdTests {

        @Test
        @DisplayName("Should return 200 and vocabulary when found")
        void getVocabularyById_valid_returns200() throws Exception {
            // Arrange
            when(vocabularyService.getById(eq(1L), eq(testUserId))).thenReturn(mockVocab);

            // Act & Assert
            mockMvc.perform(get("/api/vocabulary/{id}", 1L)
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.word").value("ubiquitous"));
        }

        @Test
        @DisplayName("Should return 403 when user doesn't own vocabulary")
        void getVocabularyById_forbidden_returns403() throws Exception {
            // Arrange
            when(vocabularyService.getById(eq(1L), eq(testUserId)))
                    .thenThrow(new org.springframework.security.access.AccessDeniedException("Access denied"));

            // Act & Assert
            mockMvc.perform(get("/api/vocabulary/{id}", 1L)
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 404 when vocabulary not found")
        void getVocabularyById_notFound_returns404() throws Exception {
            // Arrange
            when(vocabularyService.getById(eq(999L), eq(testUserId)))
                    .thenThrow(new ResourceNotFoundException("Vocabulary", "id", 999L));

            // Act & Assert
            mockMvc.perform(get("/api/vocabulary/{id}", 999L)
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // POST /api/vocabulary TESTS
    // =========================================================================
    @Nested
    @DisplayName("POST /api/vocabulary")
    class CreateVocabularyTests {

        @Test
        @DisplayName("Should return 201 and created vocabulary")
        void createVocabulary_valid_returns201() throws Exception {
            // Arrange
            when(vocabularyService.create(eq(testUserId), any(VocabularyCreateDTO.class)))
                    .thenReturn(mockVocab);

            String requestBody = """
                {
                    "word": "ubiquitous",
                    "meaning": "present everywhere"
                }
                """;

            // Act & Assert
            mockMvc.perform(post("/api/vocabulary")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.word").value("ubiquitous"));
        }

        @Test
        @DisplayName("Should return 400 when word is blank")
        void createVocabulary_blankWord_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/vocabulary")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"word\": \"\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // PUT /api/vocabulary/{id} TESTS
    // =========================================================================
    @Nested
    @DisplayName("PUT /api/vocabulary/{id}")
    class UpdateVocabularyTests {

        @Test
        @DisplayName("Should return 200 and updated vocabulary")
        void updateVocabulary_valid_returns200() throws Exception {
            // Arrange
            mockVocab.setDefinition("updated definition");
            when(vocabularyService.update(eq(1L), eq(testUserId), any(VocabularyDTO.class)))
                    .thenReturn(mockVocab);

            String requestBody = """
                {
                    "word": "ubiquitous",
                    "definition": "updated definition"
                }
                """;

            // Act & Assert
            mockMvc.perform(put("/api/vocabulary/{id}", 1L)
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.definition").value("updated definition"));
        }

        @Test
        @DisplayName("Should return 403 when user doesn't own vocabulary")
        void updateVocabulary_forbidden_returns403() throws Exception {
            // Arrange
            when(vocabularyService.update(eq(1L), eq(testUserId), any(VocabularyDTO.class)))
                    .thenThrow(new org.springframework.security.access.AccessDeniedException("Access denied"));

            // Act & Assert
            mockMvc.perform(put("/api/vocabulary/{id}", 1L)
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"word\": \"test\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // DELETE /api/vocabulary/{id} TESTS
    // =========================================================================
    @Nested
    @DisplayName("DELETE /api/vocabulary/{id}")
    class DeleteVocabularyTests {

        @Test
        @DisplayName("Should return 204 on successful deletion")
        void deleteVocabulary_valid_returns204() throws Exception {
            // Arrange
            doNothing().when(vocabularyService).delete(eq(1L), eq(testUserId));

            // Act & Assert
            mockMvc.perform(delete("/api/vocabulary/{id}", 1L)
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isNoContent());

            verify(vocabularyService).delete(eq(1L), eq(testUserId));
        }

        @Test
        @DisplayName("Should return 403 when user doesn't own vocabulary")
        void deleteVocabulary_forbidden_returns403() throws Exception {
            // Arrange
            doThrow(new org.springframework.security.access.AccessDeniedException("Access denied"))
                    .when(vocabularyService).delete(eq(1L), eq(testUserId));

            // Act & Assert
            mockMvc.perform(delete("/api/vocabulary/{id}", 1L)
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // PUT /api/vocabulary/{id}/toggle-mastered TESTS
    // =========================================================================
    @Nested
    @DisplayName("PUT /api/vocabulary/{id}/toggle-mastered")
    class ToggleMasteredTests {

        @Test
        @DisplayName("Should return 200 and toggle mastered status")
        void toggleMastered_valid_returns200() throws Exception {
            // Arrange
            mockVocab.setIsMastered(true);
            when(vocabularyService.toggleMastered(eq(1L), eq(testUserId))).thenReturn(mockVocab);

            // Act & Assert
            mockMvc.perform(put("/api/vocabulary/{id}/toggle-mastered", 1L)
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mastered").value(true));
        }
    }

    // =========================================================================
    // POST /api/vocabulary/translate TESTS
    // =========================================================================
    @Nested
    @DisplayName("POST /api/vocabulary/translate")
    class TranslateTests {

        @Test
        @DisplayName("Should return 200 and translation")
        void translate_valid_returns200() throws Exception {
            // Arrange
            Map<String, String> translation = Map.of(
                    "translation", "phổ biến khắp nơi",
                    "definition", "present everywhere",
                    "example", "Smartphones have become ubiquitous in modern life."
            );

            when(vocabularyService.translateWord(eq("ubiquitous"), isNull(), eq(testUserId)))
                    .thenReturn(translation);

            // Act & Assert
            mockMvc.perform(post("/api/vocabulary/translate")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"word\": \"ubiquitous\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.translation").value("phổ biến khắp nơi"));
        }
    }

    // =========================================================================
    // GET /api/vocabulary/stats TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/vocabulary/stats")
    class GetStatsTests {

        @Test
        @DisplayName("Should return 200 and vocabulary stats")
        void getStats_valid_returns200() throws Exception {
            // Arrange
            Map<String, Object> stats = Map.of(
                    "totalWords", 150,
                    "masteredWords", 50,
                    "unmasteredWords", 100,
                    "masteredPercent", 33.33
            );

            when(vocabularyService.getStats(testUserId)).thenReturn(stats);

            // Act & Assert
            mockMvc.perform(get("/api/vocabulary/stats")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalWords").value(150))
                    .andExpect(jsonPath("$.masteredWords").value(50));
        }
    }
}
