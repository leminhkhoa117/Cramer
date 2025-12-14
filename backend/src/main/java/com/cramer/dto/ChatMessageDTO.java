package com.cramer.dto;

import com.cramer.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO for ChatMessage entity responses.
 * Used for displaying chat history to users.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDTO {

    private Long id;
    private String role;
    private String content;
    private OffsetDateTime createdAt;

    /**
     * Create a ChatMessageDTO from a ChatMessage entity.
     *
     * @param entity the ChatMessage entity
     * @return the corresponding ChatMessageDTO
     */
    public static ChatMessageDTO fromEntity(ChatMessage entity) {
        if (entity == null) {
            return null;
        }

        return ChatMessageDTO.builder()
                .id(entity.getId())
                .role(entity.getRole())
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
