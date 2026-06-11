package com.cramer.engagement.web;

import com.cramer.engagement.service.ChatService;
import com.cramer.engagement.web.dto.ChatMessageView;
import com.cramer.engagement.web.dto.ChatRequest;
import com.cramer.engagement.web.dto.ChatResponse;
import com.cramer.platform.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** AI assistant chat endpoints (SPEC-16 §2). */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chat;
    private final CurrentUser currentUser;

    public ChatController(ChatService chat, CurrentUser currentUser) {
        this.chat = chat;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return chat.chat(currentUser.requireUserId(), request.message());
    }

    @GetMapping("/history")
    public List<ChatMessageView> history(@RequestParam(defaultValue = "50") int limit) {
        return chat.history(currentUser.requireUserId(), limit);
    }

    @GetMapping("/remaining")
    public Map<String, Integer> remaining() {
        return Map.of("remaining", chat.remaining(currentUser.requireUserId()));
    }

    @DeleteMapping("/history")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear() {
        chat.clearHistory(currentUser.requireUserId());
    }
}
