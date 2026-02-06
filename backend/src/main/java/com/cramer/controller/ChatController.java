package com.cramer.controller;

import com.cramer.dto.ChatMessageDTO;
import com.cramer.dto.ChatRequestDTO;
import com.cramer.dto.ChatResponseDTO;
import com.cramer.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for AI chatbot functionality.
 * Powers the Floating Assistant widget in the frontend.
 * 
 * All endpoints require JWT authentication.
 * Rate limiting is enforced based on subscription tier:
 * - Free (Cramerie): 20 messages/day
 * - Cramerich: 100 messages/day
 * - Cramerous: Unlimited
 */
@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "AI Chatbot APIs for IELTS learning assistant")
public class ChatController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(
        summary = "Send message to AI assistant",
        description = "Send a message to the Cramer AI assistant and receive a response. " +
                      "Rate limited based on subscription tier."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Message processed successfully",
            content = @Content(schema = @Schema(implementation = ChatResponseDTO.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request (empty message)"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @PostMapping
    public ResponseEntity<ChatResponseDTO> sendMessage(
            @Valid @RequestBody ChatRequestDTO request,
            Authentication authentication) {
        
        UUID userId = getCurrentUserId(authentication);
        logger.info("📥 POST /api/chat - User: {}", userId);

        ChatResponseDTO response = chatService.sendMessage(userId, request.getMessage());
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else if (response.getRemainingQuestions() == 0 && response.getError() != null 
                   && response.getError().contains("hết lượt")) {
            // Rate limit exceeded - return 429
            return ResponseEntity.status(429).body(response);
        } else {
            // Other errors - return 500
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @Operation(
        summary = "Get chat history",
        description = "Retrieve recent chat messages for the authenticated user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Chat history retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    @GetMapping("/history")
    public ResponseEntity<List<ChatMessageDTO>> getHistory(
            @Parameter(description = "Maximum number of messages to return (default: 50)")
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication) {
        
        UUID userId = getCurrentUserId(authentication);
        logger.info("📥 GET /api/chat/history - User: {}, limit: {}", userId, limit);

        // Cap limit to prevent abuse
        int cappedLimit = Math.min(limit, 100);
        List<ChatMessageDTO> history = chatService.getHistory(userId, cappedLimit);
        
        return ResponseEntity.ok(history);
    }

    @Operation(
        summary = "Get remaining questions for today",
        description = "Check how many chat messages the user can send today based on their subscription"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Remaining count retrieved",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    @GetMapping("/remaining")
    public ResponseEntity<Map<String, Object>> getRemainingQuestions(Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        logger.info("📥 GET /api/chat/remaining - User: {}", userId);

        int remaining = chatService.getRemainingQuestions(userId);
        
        Map<String, Object> response = Map.of(
            "remaining", remaining,
            "unlimited", remaining < 0
        );
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Clear chat history",
        description = "Delete all chat messages for the authenticated user"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Chat history cleared",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    @DeleteMapping("/history")
    public ResponseEntity<Map<String, Object>> clearHistory(Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        logger.info("📥 DELETE /api/chat/history - User: {}", userId);

        int deleted = chatService.clearHistory(userId);
        
        Map<String, Object> response = Map.of(
            "success", true,
            "deletedCount", deleted,
            "message", "Chat history cleared successfully"
        );
        
        return ResponseEntity.ok(response);
    }
}
