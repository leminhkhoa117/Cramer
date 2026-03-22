package com.cramer.controller;

import com.cramer.config.JwtAuthFilter;
import com.cramer.config.SecurityConfig;
import com.cramer.dto.CreateSpeakingSessionDTO;
import com.cramer.dto.PageDTO;
import com.cramer.dto.SaveSpeakingTranscriptDTO;
import com.cramer.dto.SpeakingHistoryItemDTO;
import com.cramer.dto.SpeakingSessionDTO;
import com.cramer.dto.SpeakingTranscriptDTO;
import com.cramer.service.SpeakingSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpeakingController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
@DisplayName("SpeakingController Unit Tests")
class SpeakingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private com.cramer.util.JwtUtil jwtUtil;

    @MockBean
    private SpeakingSessionService speakingSessionService;

    private UUID testUserId;
    private ObjectNode questionSnapshot;

    @BeforeEach
    void setUp() {
        testUserId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        questionSnapshot = objectMapper.createObjectNode();
        questionSnapshot.put("schemaVersion", 1);
        questionSnapshot.put("partType", "PART_1");
        questionSnapshot.put("promptText", "What do you do on weekends?");
    }

    @Test
    @DisplayName("POST /api/speaking/sessions should return 201 with session payload")
    void createSession_validRequest_returns201() throws Exception {
        CreateSpeakingSessionDTO request = CreateSpeakingSessionDTO.builder()
                .sessionMode("FULL")
                .testId(32L)
                .accent("british")
                .speed(new BigDecimal("1.00"))
                .build();

        SpeakingSessionDTO response = SpeakingSessionDTO.builder()
                .sessionId(42L)
                .sessionMode("FULL")
                .testId(32L)
                .status("in_progress")
                .isFinalized(false)
                .luaCost(15)
                .accent("british")
                .speed(new BigDecimal("1.00"))
                .startedAt(OffsetDateTime.now())
                .sessionBlueprint(objectMapper.createObjectNode())
                .turns(List.of())
                .build();

        when(speakingSessionService.createSession(any(CreateSpeakingSessionDTO.class), eq(testUserId))).thenReturn(response);

        mockMvc.perform(post("/api/speaking/sessions")
                        .with(csrf())
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(42))
                .andExpect(jsonPath("$.status").value("in_progress"));
    }

    @Test
    @DisplayName("POST /api/speaking/sessions/{id}/transcripts should return saved transcript payload")
    void saveTranscript_validRequest_returns200() throws Exception {
        SaveSpeakingTranscriptDTO request = SaveSpeakingTranscriptDTO.builder()
                .sourceQuestionId(501L)
                .partNumber(1)
                .turnIndex(1)
                .questionSnapshot(questionSnapshot)
                .audioStoragePath("user-id/session-id/turn-001.webm")
                .transcriptText("I usually stay home.")
                .audioDurationSeconds(25)
                .transcriptConfidence(new BigDecimal("0.910"))
                .build();

        SpeakingTranscriptDTO response = SpeakingTranscriptDTO.builder()
                .transcriptId(9001L)
                .sessionId(42L)
                .turnIndex(1)
                .status("saved")
                .recordedAt(OffsetDateTime.now())
                .build();

        when(speakingSessionService.saveTranscript(eq(42L), any(SaveSpeakingTranscriptDTO.class), eq(testUserId))).thenReturn(response);

        mockMvc.perform(post("/api/speaking/sessions/{id}/transcripts", 42L)
                        .with(csrf())
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transcriptId").value(9001))
                .andExpect(jsonPath("$.status").value("saved"));
    }

    @Test
    @DisplayName("GET /api/speaking/history should return paginated user history")
    void getHistory_returnsPagedHistory() throws Exception {
        SpeakingHistoryItemDTO item = SpeakingHistoryItemDTO.builder()
                .sessionId(77L)
                .testId(32L)
                .sessionMode("FULL")
                .status("graded")
                .overallBand(new BigDecimal("6.5"))
                .createdAt(OffsetDateTime.now())
                .completedAt(OffsetDateTime.now())
                .build();

        PageDTO<SpeakingHistoryItemDTO> page = new PageDTO<>(List.of(item), 0, 20, 1, 1);
        when(speakingSessionService.getHistory(eq(testUserId), any(), eq("graded"))).thenReturn(page);

        mockMvc.perform(get("/api/speaking/history")
                        .with(authenticatedUser())
                        .param("page", "0")
                        .param("size", "20")
                        .param("status", "graded"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sessionId").value(77))
                .andExpect(jsonPath("$.content[0].status").value("graded"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("POST /api/speaking/sessions should return 403 without authentication")
    void createSession_withoutAuth_returns403() throws Exception {
        CreateSpeakingSessionDTO request = CreateSpeakingSessionDTO.builder()
                .sessionMode("FULL")
                .testId(32L)
                .accent("british")
                .speed(new BigDecimal("1.00"))
                .build();

        mockMvc.perform(post("/api/speaking/sessions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor authenticatedUser() {
        return jwt().jwt(jwt -> jwt.subject(testUserId.toString()).claim("aud", "authenticated"));
    }
}
