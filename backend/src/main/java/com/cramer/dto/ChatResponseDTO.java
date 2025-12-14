package com.cramer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for chat response from the AI assistant.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponseDTO {

    /**
     * The AI assistant's response message.
     */
    private String message;

    /**
     * Remaining questions/messages for today.
     * -1 indicates unlimited (Cramerous tier).
     */
    private int remainingQuestions;

    /**
     * Whether the request was successful.
     */
    private boolean success;

    /**
     * Optional error message if success is false.
     */
    private String error;

    /**
     * Create a successful response.
     */
    public static ChatResponseDTO success(String message, int remaining) {
        return ChatResponseDTO.builder()
                .message(message)
                .remainingQuestions(remaining)
                .success(true)
                .build();
    }

    /**
     * Create an error response.
     */
    public static ChatResponseDTO error(String errorMessage, int remaining) {
        return ChatResponseDTO.builder()
                .message(null)
                .error(errorMessage)
                .remainingQuestions(remaining)
                .success(false)
                .build();
    }

    /**
     * Create a rate limit exceeded response.
     */
    public static ChatResponseDTO rateLimitExceeded() {
        return ChatResponseDTO.builder()
                .message(null)
                .error("Bạn đã hết lượt hỏi hôm nay. Nâng cấp gói để có thêm lượt hỏi nhé! 🌻")
                .remainingQuestions(0)
                .success(false)
                .build();
    }
}
