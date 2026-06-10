package com.cramer.controller;

import com.cramer.config.TestSecurityConfig;
import com.cramer.dto.FullSectionDTO;
import com.cramer.dto.SectionDTO;
import com.cramer.entity.Section;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.service.SectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for SectionController.
 * Tests section CRUD operations and queries.
 * 
 * @author Cramer Test Team
 * @since 2026-01-19
 */
import com.cramer.config.JwtAuthFilter;
import com.cramer.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(SectionController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
@DisplayName("SectionController Unit Tests")
class SectionControllerTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private com.cramer.util.JwtUtil jwtUtil;

    @MockitoBean
    private SectionService sectionService;


    private Section mockSection;
    private static final java.util.UUID DEFAULT_USER_ID = java.util.UUID.fromString("00000000-0000-0000-0000-000000000000");

    @BeforeEach
    void setUp() {
        mockSection = new Section();
        mockSection.setId(1L);
        mockSection.setExamSource("IELTS Cambridge 17");
        mockSection.setTestNumber(1);
        mockSection.setSkill("READING");
        mockSection.setPartNumber(1);
    }

    // =========================================================================
    // GET /api/sections TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/sections")
    class GetAllSectionsTests {

        @Test
        @DisplayName("Should return 200 and list of sections")
        void getAllSections_returns200() throws Exception {
            // Arrange
            when(sectionService.getAllSections()).thenReturn(List.of(mockSection));

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/sections")
                            .with(jwt().jwt(jwt -> jwt.subject(DEFAULT_USER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].examSource").value("IELTS Cambridge 17"));
        }
    }

    // =========================================================================
    // GET /api/sections/{id} TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/sections/{id}")
    class GetSectionByIdTests {

        @Test
        @DisplayName("Should return 200 and section when found")
        void getSectionById_valid_returns200() throws Exception {
            // Arrange
            when(sectionService.getSectionById(1L)).thenReturn(Optional.of(mockSection));

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/sections/{id}", 1L)
                            .with(jwt().jwt(jwt -> jwt.subject(DEFAULT_USER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.skill").value("READING"));
        }

        @Test
        @DisplayName("Should return 404 when section not found")
        void getSectionById_notFound_returns404() throws Exception {
            // Arrange
            when(sectionService.getSectionById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/sections/{id}", 999L)
                            .with(jwt().jwt(jwt -> jwt.subject(DEFAULT_USER_ID.toString()))))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // GET /api/sections/{id}/full TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/sections/{id}/full")
    class GetFullSectionByIdTests {

        @Test
        @DisplayName("Should return 200 and full section with questions")
        void getFullSectionById_valid_returns200() throws Exception {
            // Arrange
            FullSectionDTO fullSection = new FullSectionDTO();
            fullSection.setId(1L);
            fullSection.setExamSource("IELTS Cambridge 17");
            fullSection.setQuestions(List.of());

            when(sectionService.getFullSectionById(1L)).thenReturn(fullSection);

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/sections/{id}/full", 1L)
                            .with(jwt().jwt(jwt -> jwt.subject(DEFAULT_USER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.questions").isArray());
        }

        @Test
        @DisplayName("Should return 404 when section not found")
        void getFullSectionById_notFound_returns404() throws Exception {
            // Arrange
            when(sectionService.getFullSectionById(999L))
                    .thenThrow(new ResourceNotFoundException("Section", "id", 999L));

            // Act & Assert
            // Act & Assert
            mockMvc.perform(get("/api/sections/{id}/full", 999L)
                            .with(jwt().jwt(jwt -> jwt.subject(DEFAULT_USER_ID.toString()))))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // GET /api/sections/exam/{examSource} TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/sections/exam/{examSource}")
    class GetSectionsByExamSourceTests {

        @Test
        @DisplayName("Should return 200 and sections for exam source")
        void getSectionsByExamSource_valid_returns200() throws Exception {
            // Arrange
            when(sectionService.getSectionsByExamSource("IELTS Cambridge 17"))
                    .thenReturn(List.of(mockSection));

            // Act & Assert
            mockMvc.perform(get("/api/sections/exam/{examSource}", "IELTS Cambridge 17")
                            .with(jwt().jwt(jwt -> jwt.subject(DEFAULT_USER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].examSource").value("IELTS Cambridge 17"));
        }

        @Test
        @DisplayName("Should return 200 and empty list when no sections found")
        void getSectionsByExamSource_empty_returns200() throws Exception {
            // Arrange
            when(sectionService.getSectionsByExamSource("Unknown")).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/sections/exam/{examSource}", "Unknown")
                            .with(jwt().jwt(jwt -> jwt.subject(DEFAULT_USER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    // =========================================================================
    // GET /api/sections/exam/{examSource}/test/{testNumber} TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/sections/exam/{examSource}/test/{testNumber}")
    class GetSectionsByTestTests {

        @Test
        @DisplayName("Should return 200 and sections for specific test")
        void getSectionsByTest_valid_returns200() throws Exception {
            // Arrange
            when(sectionService.getSectionsByTest("IELTS Cambridge 17", 1))
                    .thenReturn(List.of(mockSection));

            // Act & Assert
            mockMvc.perform(get("/api/sections/exam/{examSource}/test/{testNumber}", 
                    "IELTS Cambridge 17", 1)
                            .with(jwt().jwt(jwt -> jwt.subject(DEFAULT_USER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].testNumber").value(1));
        }
    }

    // =========================================================================
    // POST /api/sections TESTS
    // =========================================================================
    @Nested
    @DisplayName("POST /api/sections")
    class CreateSectionTests {

        @Test
        @DisplayName("Should return 201 and created section")
        void createSection_valid_returns201() throws Exception {
            // Arrange
            when(sectionService.createSection(any(Section.class))).thenReturn(mockSection);

            String requestBody = """
                {
                    "examSource": "IELTS Cambridge 17",
                    "testNumber": 1,
                    "skill": "READING",
                    "partNumber": 1
                }
                """;

            // Act & Assert
            mockMvc.perform(post("/api/sections")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(DEFAULT_USER_ID.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.examSource").value("IELTS Cambridge 17"));
        }
    }

    // =========================================================================
    // PUT /api/sections/{id} TESTS
    // =========================================================================
    @Nested
    @DisplayName("PUT /api/sections/{id}")
    class UpdateSectionTests {

        @Test
        @DisplayName("Should return 200 and updated section")
        void updateSection_valid_returns200() throws Exception {
            // Arrange
            mockSection.setPartNumber(2);
            when(sectionService.updateSection(eq(1L), any(Section.class))).thenReturn(mockSection);

            String requestBody = """
                {
                    "examSource": "IELTS Cambridge 17",
                    "testNumber": 1,
                    "skill": "READING",
                    "partNumber": 2
                }
                """;

            // Act & Assert
            mockMvc.perform(put("/api/sections/{id}", 1L)
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(DEFAULT_USER_ID.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.partNumber").value(2));
        }
    }

    // =========================================================================
    // DELETE /api/sections/{id} TESTS
    // =========================================================================
    @Nested
    @DisplayName("DELETE /api/sections/{id}")
    class DeleteSectionTests {

        @Test
        @DisplayName("Should return 204 on successful deletion")
        void deleteSection_valid_returns204() throws Exception {
            // Arrange
            doNothing().when(sectionService).deleteSection(1L);

            // Act & Assert
            mockMvc.perform(delete("/api/sections/{id}", 1L)
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(DEFAULT_USER_ID.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isNoContent());

            verify(sectionService).deleteSection(1L);
        }
    }
}
