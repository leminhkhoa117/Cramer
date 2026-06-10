package com.cramer.controller;

import com.cramer.config.JwtAuthFilter;
import com.cramer.config.SecurityConfig;
import com.cramer.dto.ChatMessageDTO;
import com.cramer.dto.ChatResponseDTO;
import com.cramer.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ChatController.
 * Tests AI chatbot endpoints including sending messages, history, and rate limiting.
 * 
 * @author Cramer Test Team
 * @since 2026-01-25
 */
@WebMvcTest(ChatController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
@DisplayName("ChatController Unit Tests")
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private com.cramer.util.JwtUtil jwtUtil;

    @MockitoBean
    private ChatService chatService;

    private UUID testUserId;
    private static final UUID DEFAULT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @BeforeEach
    void setUp() {
        testUserId = DEFAULT_USER_ID;
    }

    // =========================================================================
    // POST /api/chat TESTS
    // =========================================================================
    @Nested
    @DisplayName("POST /api/chat")
    class SendMessageTests {

        @Test
        @DisplayName("Should return 200 and AI response when message is valid")
        void sendMessage_valid_returns200() throws Exception {
            ChatResponseDTO response = ChatResponseDTO.success(
                "Hello! How can I help with your IELTS preparation?",
                19
            );

            when(chatService.sendMessage(eq(testUserId), eq("Hello, can you help me?"))).thenReturn(response);

            String requestBody = """
                {
                    "message": "Hello, can you help me?"
                }
                """;

            mockMvc.perform(post("/api/chat")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Hello! How can I help with your IELTS preparation?"))
                    .andExpect(jsonPath("$.remainingQuestions").value(19))
                    .andExpect(jsonPath("$.success").value(true));

            verify(chatService).sendMessage(eq(testUserId), eq("Hello, can you help me?"));
        }

        @Test
        @DisplayName("Should return 429 when rate limit exceeded")
        void sendMessage_rateLimited_returns429() throws Exception {
            ChatResponseDTO response = ChatResponseDTO.rateLimitExceeded();

            when(chatService.sendMessage(eq(testUserId), anyString())).thenReturn(response);

            String requestBody = """
                {
                    "message": "Another question"
                }
                """;

            mockMvc.perform(post("/api/chat")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.remainingQuestions").value(0));
        }

        @Test
        @DisplayName("Should return 500 when AI service fails")
        void sendMessage_serviceError_returns500() throws Exception {
            ChatResponseDTO response = ChatResponseDTO.error("AI service unavailable", 10);

            when(chatService.sendMessage(eq(testUserId), anyString())).thenReturn(response);

            String requestBody = """
                {
                    "message": "Test message"
                }
                """;

            mockMvc.perform(post("/api/chat")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error").value("AI service unavailable"));
        }

        @Test
        @DisplayName("Should return 400 when message is blank")
        void sendMessage_blankMessage_returns400() throws Exception {
            String requestBody = """
                {
                    "message": ""
                }
                """;

            mockMvc.perform(post("/api/chat")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());

            verify(chatService, never()).sendMessage(any(), anyString());
        }

        @Test
        @DisplayName("Should return 400 when message is null")
        void sendMessage_nullMessage_returns400() throws Exception {
            String requestBody = """
                {
                    "message": null
                }
                """;

            mockMvc.perform(post("/api/chat")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());

            verify(chatService, never()).sendMessage(any(), anyString());
        }

        @Test
        @DisplayName("Should return 403 when no JWT token")
        void sendMessage_unauthorized_returns403() throws Exception {
            String requestBody = """
                {
                    "message": "Hello"
                }
                """;

            mockMvc.perform(post("/api/chat")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isForbidden());

            verify(chatService, never()).sendMessage(any(), anyString());
        }
    }

    // =========================================================================
    // GET /api/chat/history TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/chat/history")
    class GetHistoryTests {

        @Test
        @DisplayName("Should return 200 and chat history")
        void getHistory_valid_returns200() throws Exception {
            ChatMessageDTO msg1 = ChatMessageDTO.builder()
                    .id(1L)
                    .role("user")
                    .content("How do I improve my speaking?")
                    .createdAt(OffsetDateTime.now().minusMinutes(5))
                    .build();

            ChatMessageDTO msg2 = ChatMessageDTO.builder()
                    .id(2L)
                    .role("assistant")
                    .content("Here are some tips for improving speaking...")
                    .createdAt(OffsetDateTime.now().minusMinutes(4))
                    .build();

            when(chatService.getHistory(eq(testUserId), eq(50))).thenReturn(List.of(msg1, msg2));

            mockMvc.perform(get("/api/chat/history")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].role").value("user"))
                    .andExpect(jsonPath("$[0].content").value("How do I improve my speaking?"))
                    .andExpect(jsonPath("$[1].role").value("assistant"));
        }

        @Test
        @DisplayName("Should return 200 with empty list when no history")
        void getHistory_empty_returns200() throws Exception {
            when(chatService.getHistory(eq(testUserId), anyInt())).thenReturn(List.of());

            mockMvc.perform(get("/api/chat/history")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("Should return 200 with custom limit")
        void getHistory_withLimit_returns200() throws Exception {
            ChatMessageDTO msg = ChatMessageDTO.builder()
                    .id(1L)
                    .role("user")
                    .content("Test message")
                    .createdAt(OffsetDateTime.now())
                    .build();

            when(chatService.getHistory(eq(testUserId), eq(10))).thenReturn(List.of(msg));

            mockMvc.perform(get("/api/chat/history")
                            .param("limit", "10")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].content").value("Test message"));

            verify(chatService).getHistory(eq(testUserId), eq(10));
        }

        @Test
        @DisplayName("Should cap limit at 100")
        void getHistory_exceededLimit_caps100() throws Exception {
            when(chatService.getHistory(eq(testUserId), eq(100))).thenReturn(List.of());

            mockMvc.perform(get("/api/chat/history")
                            .param("limit", "500")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk());

            verify(chatService).getHistory(eq(testUserId), eq(100));
        }

        @Test
        @DisplayName("Should return 403 when no JWT token")
        void getHistory_unauthorized_returns403() throws Exception {
            mockMvc.perform(get("/api/chat/history"))
                    .andExpect(status().isForbidden());

            verify(chatService, never()).getHistory(any(), anyInt());
        }
    }

    // =========================================================================
    // GET /api/chat/remaining TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/chat/remaining")
    class GetRemainingTests {

        @Test
        @DisplayName("Should return 200 with remaining count for limited user")
        void getRemaining_limited_returns200() throws Exception {
            when(chatService.getRemainingQuestions(testUserId)).thenReturn(15);

            mockMvc.perform(get("/api/chat/remaining")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.remaining").value(15))
                    .andExpect(jsonPath("$.unlimited").value(false));
        }

        @Test
        @DisplayName("Should return 200 with unlimited flag for premium user")
        void getRemaining_unlimited_returns200() throws Exception {
            when(chatService.getRemainingQuestions(testUserId)).thenReturn(-1);

            mockMvc.perform(get("/api/chat/remaining")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.remaining").value(-1))
                    .andExpect(jsonPath("$.unlimited").value(true));
        }

        @Test
        @DisplayName("Should return 200 with zero remaining")
        void getRemaining_zero_returns200() throws Exception {
            when(chatService.getRemainingQuestions(testUserId)).thenReturn(0);

            mockMvc.perform(get("/api/chat/remaining")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.remaining").value(0))
                    .andExpect(jsonPath("$.unlimited").value(false));
        }

        @Test
        @DisplayName("Should return 403 when no JWT token")
        void getRemaining_unauthorized_returns403() throws Exception {
            mockMvc.perform(get("/api/chat/remaining"))
                    .andExpect(status().isForbidden());

            verify(chatService, never()).getRemainingQuestions(any());
        }
    }

    // =========================================================================
    // DELETE /api/chat/history TESTS
    // =========================================================================
    @Nested
    @DisplayName("DELETE /api/chat/history")
    class ClearHistoryTests {

        @Test
        @DisplayName("Should return 200 and clear history successfully")
        void clearHistory_valid_returns200() throws Exception {
            when(chatService.clearHistory(testUserId)).thenReturn(25);

            mockMvc.perform(delete("/api/chat/history")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.deletedCount").value(25))
                    .andExpect(jsonPath("$.message").value("Chat history cleared successfully"));

            verify(chatService).clearHistory(testUserId);
        }

        @Test
        @DisplayName("Should return 200 when no history to clear")
        void clearHistory_empty_returns200() throws Exception {
            when(chatService.clearHistory(testUserId)).thenReturn(0);

            mockMvc.perform(delete("/api/chat/history")
                            .with(csrf())
                            .with(jwt().jwt(jwt -> jwt
                                    .subject(testUserId.toString())
                                    .claim("aud", "authenticated"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.deletedCount").value(0));
        }

        @Test
        @DisplayName("Should return 403 when no JWT token")
        void clearHistory_unauthorized_returns403() throws Exception {
            mockMvc.perform(delete("/api/chat/history")
                            .with(csrf()))
                    .andExpect(status().isForbidden());

            verify(chatService, never()).clearHistory(any());
        }

    }
}
