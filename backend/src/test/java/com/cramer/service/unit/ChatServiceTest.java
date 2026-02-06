package com.cramer.service.unit;

import com.cramer.config.LLMConfig;
import com.cramer.dto.ChatMessageDTO;
import com.cramer.dto.ChatResponseDTO;
import com.cramer.entity.ChatMessage;
import com.cramer.entity.ChatbotUsage;
import com.cramer.repository.ChatMessageRepository;
import com.cramer.repository.ChatbotUsageRepository;
import com.cramer.service.ChatBillingService;
import com.cramer.service.SubscriptionService;
import com.cramer.service.implement.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChatServiceImpl.
 * Tests chat message handling, history retrieval, and quota checking.
 * 
 * @author Cramer Test Team
 * @since 2026-01-25
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService Unit Tests")
class ChatServiceTest {

    @Mock
    private ChatMessageRepository messageRepository;

    @Mock
    private ChatbotUsageRepository usageRepository;

    @Mock
    private ChatBillingService chatBillingService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private LLMConfig llmConfig;

    @InjectMocks
    private ChatServiceImpl chatService;

    private UUID testUserId;
    private ChatMessage mockUserMessage;
    private ChatMessage mockAssistantMessage;

    @BeforeEach
    void setUp() {
        testUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockUserMessage = ChatMessage.builder()
                .id(1L)
                .userId(testUserId)
                .role("user")
                .content("What is the meaning of ubiquitous?")
                .createdAt(OffsetDateTime.now().minusMinutes(5))
                .build();

        mockAssistantMessage = ChatMessage.builder()
                .id(2L)
                .userId(testUserId)
                .role("assistant")
                .content("Ubiquitous means existing everywhere at the same time.")
                .createdAt(OffsetDateTime.now().minusMinutes(4))
                .build();
    }

    // =========================================================================
    // SEND MESSAGE TESTS
    // =========================================================================
    @Nested
    @DisplayName("sendMessage() Tests")
    class SendMessageTests {

        @Test
        @DisplayName("Should return rate limited response when billing blocked")
        void sendMessage_billingBlocked_returnsRateLimited() {
            ChatBillingService.ChatBillingResult blockedResult = 
                    new ChatBillingService.ChatBillingResult(false, false, 0, 0, "Quota exceeded");

            when(chatBillingService.processChatBilling(testUserId)).thenReturn(blockedResult);

            ChatResponseDTO result = chatService.sendMessage(testUserId, "Hello");

            assertThat(result.isSuccess()).isFalse();
            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should return error when no API key configured")
        void sendMessage_noApiKey_returnsError() {
            ChatBillingService.ChatBillingResult allowedResult = 
                    new ChatBillingService.ChatBillingResult(true, false, 10, 0, null);

            when(chatBillingService.processChatBilling(testUserId)).thenReturn(allowedResult);
            when(llmConfig.hasApiKey()).thenReturn(false);

            ChatResponseDTO result = chatService.sendMessage(testUserId, "Hello");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError()).contains("tạm thời không khả dụng");
        }
    }

    // =========================================================================
    // GET HISTORY TESTS
    // =========================================================================
    @Nested
    @DisplayName("getHistory() Tests")
    class GetHistoryTests {

        @Test
        @DisplayName("Should return chat history")
        void getHistory_hasMessages_returnsHistory() {
            when(messageRepository.findRecentByUserId(testUserId, PageRequest.of(0, 50)))
                    .thenReturn(List.of(mockAssistantMessage, mockUserMessage));

            List<ChatMessageDTO> result = chatService.getHistory(testUserId, 50);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getRole()).isEqualTo("assistant");
            assertThat(result.get(1).getRole()).isEqualTo("user");
        }

        @Test
        @DisplayName("Should return empty list when no history")
        void getHistory_noMessages_returnsEmpty() {
            when(messageRepository.findRecentByUserId(testUserId, PageRequest.of(0, 50)))
                    .thenReturn(List.of());

            List<ChatMessageDTO> result = chatService.getHistory(testUserId, 50);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should respect limit parameter")
        void getHistory_withLimit_usesCorrectPageRequest() {
            when(messageRepository.findRecentByUserId(testUserId, PageRequest.of(0, 10)))
                    .thenReturn(List.of(mockUserMessage));

            chatService.getHistory(testUserId, 10);

            verify(messageRepository).findRecentByUserId(testUserId, PageRequest.of(0, 10));
        }
    }

    // =========================================================================
    // GET REMAINING QUESTIONS TESTS
    // =========================================================================
    @Nested
    @DisplayName("getRemainingQuestions() Tests")
    class GetRemainingQuestionsTests {

        @Test
        @DisplayName("Should return remaining questions from subscription service")
        void getRemainingQuestions_hasQuota_returnsRemaining() {
            when(subscriptionService.getRemainingChatMessages(testUserId)).thenReturn(25);

            int result = chatService.getRemainingQuestions(testUserId);

            assertThat(result).isEqualTo(25);
            verify(subscriptionService).getRemainingChatMessages(testUserId);
        }

        @Test
        @DisplayName("Should return -1 for unlimited users")
        void getRemainingQuestions_unlimitedUser_returnsNegativeOne() {
            when(subscriptionService.getRemainingChatMessages(testUserId)).thenReturn(-1);

            int result = chatService.getRemainingQuestions(testUserId);

            assertThat(result).isEqualTo(-1);
        }
    }

    // =========================================================================
    // CLEAR HISTORY TESTS
    // =========================================================================
    @Nested
    @DisplayName("clearHistory() Tests")
    class ClearHistoryTests {

        @Test
        @DisplayName("Should delete all messages and return count")
        void clearHistory_hasMessages_deletesThem() {
            when(messageRepository.deleteByUserId(testUserId)).thenReturn(15);

            int result = chatService.clearHistory(testUserId);

            assertThat(result).isEqualTo(15);
            verify(messageRepository).deleteByUserId(testUserId);
        }

        @Test
        @DisplayName("Should return 0 when no messages to delete")
        void clearHistory_noMessages_returnsZero() {
            when(messageRepository.deleteByUserId(testUserId)).thenReturn(0);

            int result = chatService.clearHistory(testUserId);

            assertThat(result).isEqualTo(0);
        }
    }
}
