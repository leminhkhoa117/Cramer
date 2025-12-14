package com.cramer.service.implement;

import com.cramer.config.LLMConfig;
import com.cramer.dto.ChatMessageDTO;
import com.cramer.dto.ChatResponseDTO;
import com.cramer.entity.ChatMessage;
import com.cramer.entity.ChatbotUsage;
import com.cramer.repository.ChatMessageRepository;
import com.cramer.repository.ChatbotUsageRepository;
import com.cramer.service.ChatService;
import com.cramer.service.SubscriptionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of ChatService.
 * Provides AI chatbot functionality using DeepSeek API.
 */
@Service
@Transactional
public class ChatServiceImpl implements ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);

    // DeepSeek API configuration
    private static final String CHAT_MODEL = "deepseek-chat";
    private static final String CHAT_ENDPOINT = "/chat/completions";
    private static final int CONTEXT_MESSAGE_LIMIT = 10;
    private static final int MAX_TOKENS = 500;

    // System prompt for the assistant
    private static final String SYSTEM_PROMPT = """
        Bạn là Cramer, trợ lý học IELTS thông minh của ứng dụng Cramer. Bạn:
        - Trả lời bằng tiếng Việt (trừ khi được hỏi bằng tiếng Anh)
        - Nhiệt tình, động viên, không phán xét
        - Giúp giải thích từ vựng, ngữ pháp tiếng Anh
        - Đưa ra lời khuyên học tập IELTS
        - Hướng dẫn sử dụng các tính năng của app
        - Trả lời ngắn gọn, súc tích (tối đa 200 từ)
        - Có thể dùng emoji để thân thiện hơn 🌻
        
        Một số tính năng của app Cramer:
        - Luyện thi Reading, Listening, Writing với đề thi Cambridge IELTS thực
        - Chấm bài Writing bằng AI với phản hồi chi tiết
        - Sổ tay từ vựng để lưu và ôn từ mới
        - Hệ thống Lúa (credit) để sử dụng các tính năng premium
        """;

    private final ChatMessageRepository messageRepository;
    private final ChatbotUsageRepository usageRepository;
    private final SubscriptionService subscriptionService;
    private final LLMConfig llmConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ChatServiceImpl(
            ChatMessageRepository messageRepository,
            ChatbotUsageRepository usageRepository,
            SubscriptionService subscriptionService,
            LLMConfig llmConfig) {
        this.messageRepository = messageRepository;
        this.usageRepository = usageRepository;
        this.subscriptionService = subscriptionService;
        this.llmConfig = llmConfig;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ChatResponseDTO sendMessage(UUID userId, String message) {
        logger.info("💬 Processing chat message from user: {}", userId);

        // Step 1: Check monthly limit (using the new monthly tracking)
        int remaining = getRemainingQuestions(userId);

        if (remaining == 0) {
            logger.info("⛔ User {} has exceeded monthly chat limit", userId);
            return ChatResponseDTO.rateLimitExceeded();
        }

        // Step 2: Check if API key is available
        if (!llmConfig.hasApiKey()) {
            logger.error("❌ No DeepSeek API key configured for chat service");
            return ChatResponseDTO.error(
                    "Dịch vụ chat tạm thời không khả dụng. Vui lòng thử lại sau.",
                    remaining
            );
        }

        try {
            // Step 3: Save user message
            ChatMessage userMessage = ChatMessage.userMessage(userId, message);
            messageRepository.save(userMessage);

            // Step 4: Build conversation context
            List<Map<String, String>> conversationHistory = buildConversationContext(userId);

            // Step 5: Call DeepSeek API
            String response = callDeepSeekApi(conversationHistory, message);

            // Step 6: Save assistant message
            ChatMessage assistantMessage = ChatMessage.assistantMessage(userId, response, 0);
            messageRepository.save(assistantMessage);

            // Step 7: Increment usage (monthly tracking in UserSubscription)
            subscriptionService.incrementChatUsage(userId);
            // Also keep daily tracking for analytics
            incrementUsage(userId);

            // Step 8: Calculate new remaining
            int newRemaining = remaining < 0 ? -1 : remaining - 1;

            logger.info("✅ Chat response sent successfully to user: {}", userId);
            return ChatResponseDTO.success(response, newRemaining);

        } catch (Exception e) {
            logger.error("❌ Failed to process chat message for user {}: {}", userId, e.getMessage(), e);
            return ChatResponseDTO.error(
                    "Xin lỗi, mình đang gặp trục trặc. Bạn thử lại sau nhé! 🙏",
                    remaining
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getHistory(UUID userId, int limit) {
        logger.info("📜 Fetching chat history for user: {}, limit: {}", userId, limit);

        return messageRepository.findRecentByUserId(userId, PageRequest.of(0, limit))
                .stream()
                .map(ChatMessageDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public int getRemainingQuestions(UUID userId) {
        // Use monthly tracking from UserSubscription
        return subscriptionService.getRemainingChatMessages(userId);
    }

    @Override
    public int clearHistory(UUID userId) {
        logger.info("🗑️ Clearing chat history for user: {}", userId);
        int deleted = messageRepository.deleteByUserId(userId);
        logger.info("✅ Deleted {} messages for user: {}", deleted, userId);
        return deleted;
    }

    /**
     * Build conversation context from recent messages.
     */
    private List<Map<String, String>> buildConversationContext(UUID userId) {
        List<ChatMessage> recentMessages = messageRepository.findRecentByUserId(
                userId, 
                PageRequest.of(0, CONTEXT_MESSAGE_LIMIT)
        );

        // Reverse to get chronological order (oldest first)
        Collections.reverse(recentMessages);

        List<Map<String, String>> messages = new ArrayList<>();

        // Add system prompt
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", SYSTEM_PROMPT);
        messages.add(systemMessage);

        // Add conversation history
        for (ChatMessage msg : recentMessages) {
            Map<String, String> chatMsg = new HashMap<>();
            chatMsg.put("role", msg.getRole());
            chatMsg.put("content", msg.getContent());
            messages.add(chatMsg);
        }

        return messages;
    }

    /**
     * Call DeepSeek API with conversation context.
     */
    private String callDeepSeekApi(List<Map<String, String>> conversationHistory, String userMessage) {
        String apiUrl = llmConfig.getBaseUrl() + CHAT_ENDPOINT;

        // Add current user message to history
        Map<String, String> currentMessage = new HashMap<>();
        currentMessage.put("role", "user");
        currentMessage.put("content", userMessage);
        conversationHistory.add(currentMessage);

        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", CHAT_MODEL);
        requestBody.put("messages", conversationHistory);
        requestBody.put("max_tokens", MAX_TOKENS);
        requestBody.put("temperature", 0.7);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(llmConfig.getApiKey());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            logger.debug("📤 Sending request to DeepSeek API");
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("DeepSeek API returned error status: {}", response.getStatusCode());
                throw new RuntimeException("DeepSeek API error: " + response.getStatusCode());
            }

            // Parse response
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            JsonNode choices = jsonResponse.get("choices");

            if (choices == null || choices.isEmpty()) {
                logger.error("DeepSeek API returned empty choices");
                throw new RuntimeException("Empty response from DeepSeek API");
            }

            String content = choices.get(0).get("message").get("content").asText();
            logger.debug("📥 Received response from DeepSeek API: {} chars", content.length());

            return content;

        } catch (RestClientException e) {
            logger.error("Failed to call DeepSeek API: {}", e.getMessage());
            throw new RuntimeException("Failed to call DeepSeek API: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error processing DeepSeek API response: {}", e.getMessage());
            throw new RuntimeException("Error processing API response: " + e.getMessage(), e);
        }
    }

    /**
     * Increment daily usage for a user.
     */
    private void incrementUsage(UUID userId) {
        LocalDate today = LocalDate.now();

        ChatbotUsage usage = usageRepository.findTodayUsage(userId)
                .orElseGet(() -> {
                    ChatbotUsage newUsage = ChatbotUsage.builder()
                            .userId(userId)
                            .usageDate(today)
                            .messagesUsed(0)
                            .build();
                    return usageRepository.save(newUsage);
                });

        usage.incrementUsage();
        usageRepository.save(usage);
    }
}
