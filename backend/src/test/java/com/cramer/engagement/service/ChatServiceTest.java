package com.cramer.engagement.service;

import com.cramer.billing.service.ChatBillingPort;
import com.cramer.engagement.domain.ChatMessage;
import com.cramer.engagement.repository.ChatMessageRepository;
import com.cramer.engagement.web.dto.ChatResponse;
import com.cramer.platform.error.QuotaExceededException;
import com.cramer.platform.integration.llm.DeepSeekClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock ChatMessageRepository messages;
    @Mock DeepSeekClient deepSeek;
    @Mock ChatBillingPort billing;

    private ChatService service() {
        lenient().when(messages.findByUserIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        lenient().when(messages.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));
        return new ChatService(messages, deepSeek, billing);
    }

    @Test
    @DisplayName("chat blocked when allowance exhausted -> 402, no LLM call, no charge")
    void blockedWhenExhausted() {
        UUID user = UUID.randomUUID();
        when(billing.canChat(user)).thenReturn(false);

        assertThatThrownBy(() -> service().chat(user, "hello"))
                .isInstanceOf(QuotaExceededException.class);
        verify(deepSeek, never()).chatJson(any(), any(), any(), anyDouble(), anyInt());
        verify(billing, never()).chargeChat(any(), anyString());
    }

    @Test
    @DisplayName("successful chat persists both turns and charges AFTER the reply")
    void chargesAfterSuccess() throws Exception {
        UUID user = UUID.randomUUID();
        when(billing.canChat(user)).thenReturn(true);
        when(deepSeek.chatJson(any(), any(), any(), anyDouble(), anyInt()))
                .thenReturn(mapper.readTree("{\"reply\":\"Chào bạn!\"}"));
        when(billing.remaining(user)).thenReturn(19);

        ChatResponse res = service().chat(user, "xin chào");

        assertThat(res.reply()).isEqualTo("Chào bạn!");
        assertThat(res.remaining()).isEqualTo(19);
        // both user + assistant messages saved
        verify(messages, org.mockito.Mockito.times(2)).save(any(ChatMessage.class));
        verify(billing).chargeChat(any(), anyString());
    }
}
