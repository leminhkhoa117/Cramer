package com.cramer.service;

import com.cramer.dto.ChatMessageDTO;
import com.cramer.dto.ChatResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for AI chatbot functionality.
 * Provides methods for sending messages to AI assistant and managing chat history.
 */
public interface ChatService {

    /**
     * Send a message to the AI assistant and get a response.
     * This method handles rate limiting, conversation context, and API calls.
     *
     * @param userId  the user's UUID
     * @param message the user's message
     * @return response from the AI assistant with remaining questions count
     */
    ChatResponseDTO sendMessage(UUID userId, String message);

    /**
     * Get chat history for a user.
     *
     * @param userId the user's UUID
     * @param limit  maximum number of messages to return
     * @return list of chat messages (newest first)
     */
    List<ChatMessageDTO> getHistory(UUID userId, int limit);

    /**
     * Get remaining questions for today.
     *
     * @param userId the user's UUID
     * @return remaining questions count (-1 for unlimited)
     */
    int getRemainingQuestions(UUID userId);

    /**
     * Clear chat history for a user.
     *
     * @param userId the user's UUID
     * @return number of messages deleted
     */
    int clearHistory(UUID userId);
}
