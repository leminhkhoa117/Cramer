package com.cramer.config;

import com.cramer.websocket.SpeakingWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for real-time Speaking sessions.
 * 
 * Enables WebSocket communication between frontend and backend,
 * which then connects to Gemini Live API for native audio conversation.
 * 
 * Flow:
 * Frontend <--WebSocket--> Backend <--WebSocket--> Gemini Live API
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SpeakingWebSocketHandler speakingWebSocketHandler;

    public WebSocketConfig(SpeakingWebSocketHandler speakingWebSocketHandler) {
        this.speakingWebSocketHandler = speakingWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Speaking session WebSocket endpoint
        // Frontend connects to ws://localhost:8080/ws/speaking/{sessionId}
        registry.addHandler(speakingWebSocketHandler, "/ws/speaking/{sessionId}")
                .setAllowedOrigins(
                    "http://localhost:3000",
                    "http://localhost:5173",
                    "https://cramer.vn"
                );
    }
}
