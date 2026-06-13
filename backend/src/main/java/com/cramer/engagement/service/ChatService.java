package com.cramer.engagement.service;

import com.cramer.billing.service.ChatBillingPort;
import com.cramer.engagement.domain.ChatMessage;
import com.cramer.engagement.repository.ChatMessageRepository;
import com.cramer.engagement.web.dto.ChatMessageView;
import com.cramer.engagement.web.dto.ChatResponse;
import com.cramer.platform.common.json.Json;
import com.cramer.platform.error.QuotaExceededException;
import com.cramer.platform.integration.llm.DeepSeekClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * IELTS assistant chat (SPEC-16 §2). Billing is via {@link ChatBillingPort}, charged
 * <strong>after</strong> a successful reply (no charge on failure). The monthly subscription
 * counter is the source of truth.
 *
 * <p>Fixes: the user message is persisted <em>after</em> building the model context (no
 * duplicate-message bug); billing is post-success (not pre-call).
 */
@Service
public class ChatService {

    private static final int MAX_TOKENS = 500;
    private static final double TEMPERATURE = 0.7;
    private static final int HISTORY_CAP = 100;
    private static final String SYSTEM_PROMPT = """
            Bạn là trợ lý IELTS thân thiện của Cramer. Trả lời ngắn gọn, chính xác, bằng tiếng Việt
            (kèm thuật ngữ tiếng Anh khi cần). Tập trung vào luyện thi IELTS: từ vựng, ngữ pháp,
            chiến lược làm bài, và sửa lỗi. Nếu câu hỏi ngoài phạm vi IELTS, lịch sự đưa người dùng
            quay lại chủ đề học tập.
            """;

    private final ChatMessageRepository messages;
    private final DeepSeekClient deepSeek;
    private final ChatBillingPort billing;

    public ChatService(ChatMessageRepository messages, DeepSeekClient deepSeek, ChatBillingPort billing) {
        this.messages = messages;
        this.deepSeek = deepSeek;
        this.billing = billing;
    }

    @Transactional
    public ChatResponse chat(UUID userId, String userMessage) {
        if (!billing.canChat(userId)) {
            throw new QuotaExceededException("CHAT_LIMIT", "Monthly chat allowance exhausted and insufficient Lúa");
        }

        // Build context from prior history WITHOUT the current message (no duplication).
        String reply = callModel(userId, userMessage);

        // Persist both turns only after a successful reply.
        save(userId, "user", userMessage);
        save(userId, "assistant", reply);

        // Charge after success.
        billing.chargeChat(userId, "chat_" + System.currentTimeMillis() + "_" + userId);
        return new ChatResponse(reply, billing.remaining(userId));
    }

    @Transactional(readOnly = true)
    public List<ChatMessageView> history(UUID userId, int limit) {
        int capped = Math.min(Math.max(1, limit), HISTORY_CAP);
        return messages.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, capped))
                .stream().map(ChatMessageView::of).toList();
    }

    @Transactional(readOnly = true)
    public int remaining(UUID userId) {
        return billing.remaining(userId);
    }

    @Transactional
    public void clearHistory(UUID userId) {
        messages.deleteByUserId(userId);
    }

    private String callModel(UUID userId, String userMessage) {
        // Prior turns (ascending) form the context; the new message is appended once here.
        List<ChatMessage> prior = messages.findByUserIdOrderByCreatedAtAsc(userId);
        StringBuilder convo = new StringBuilder();
        List<ChatMessage> tail = prior.size() > 20 ? prior.subList(prior.size() - 20, prior.size()) : prior;
        for (ChatMessage m : tail) {
            convo.append(m.getRole()).append(": ").append(m.getContent()).append('\n');
        }
        convo.append("user: ").append(userMessage);

        JsonNode result = deepSeek.chatJson(null, SYSTEM_PROMPT,
                convo + "\n\nRespond as the assistant. Return JSON {\"reply\": \"...\"}.",
                TEMPERATURE, MAX_TOKENS);
        String reply = result.path("reply").asText("");
        return reply.isBlank() ? "Xin lỗi, mình chưa thể trả lời ngay lúc này." : reply;
    }

    private void save(UUID userId, String role, String content) {
        ChatMessage m = new ChatMessage();
        m.setUserId(userId);
        m.setRole(role);
        m.setContent(content);
        messages.save(m);
    }
}
