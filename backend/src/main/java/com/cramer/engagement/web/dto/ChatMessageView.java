package com.cramer.engagement.web.dto;

import com.cramer.engagement.domain.ChatMessage;

import java.time.OffsetDateTime;

/** A chat history line (SPEC-16 §2). */
public record ChatMessageView(String role, String content, OffsetDateTime createdAt) {

    public static ChatMessageView of(ChatMessage m) {
        return new ChatMessageView(m.getRole(), m.getContent(), m.getCreatedAt());
    }
}
