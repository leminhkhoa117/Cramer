package com.cramer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing a chat message between user and AI assistant.
 * Messages are stored for context and history purposes.
 */
@Entity
@Table(name = "chat_messages", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Message role: "user", "assistant", or "system"
     */
    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Number of tokens used for this message (from API response).
     * Useful for tracking usage and costs.
     */
    @Column(name = "tokens_used")
    @Builder.Default
    private Integer tokensUsed = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Role constants
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_SYSTEM = "system";

    /**
     * Create a user message.
     */
    public static ChatMessage userMessage(UUID userId, String content) {
        return ChatMessage.builder()
                .userId(userId)
                .role(ROLE_USER)
                .content(content)
                .build();
    }

    /**
     * Create an assistant message.
     */
    public static ChatMessage assistantMessage(UUID userId, String content, int tokensUsed) {
        return ChatMessage.builder()
                .userId(userId)
                .role(ROLE_ASSISTANT)
                .content(content)
                .tokensUsed(tokensUsed)
                .build();
    }
}
