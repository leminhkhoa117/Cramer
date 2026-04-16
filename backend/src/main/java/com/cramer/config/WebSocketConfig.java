package com.cramer.config;

import com.cramer.websocket.SpeakingWebSocketAuthInterceptor;
import com.cramer.websocket.SpeakingWebSocketHandler;
import java.util.Objects;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Registers WebSocket endpoints used by the Speaking real-time flow.
 *
 * <p>The browser connects to {@code /ws/speaking/{sessionId}} and the handler
 * is responsible for validating session ownership, session state, and fallback
 * behavior when Gemini Live is unavailable.</p>
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SpeakingWebSocketAuthInterceptor speakingWebSocketAuthInterceptor;
    private final SpeakingWebSocketHandler speakingWebSocketHandler;
    private final SpeakingGeminiLiveProperties speakingGeminiLiveProperties;

    public WebSocketConfig(
        SpeakingWebSocketAuthInterceptor speakingWebSocketAuthInterceptor,
        SpeakingWebSocketHandler speakingWebSocketHandler,
        SpeakingGeminiLiveProperties speakingGeminiLiveProperties
    ) {
        this.speakingWebSocketAuthInterceptor =
            speakingWebSocketAuthInterceptor;
        this.speakingWebSocketHandler = speakingWebSocketHandler;
        this.speakingGeminiLiveProperties = speakingGeminiLiveProperties;
    }

    @Override
    public void registerWebSocketHandlers(
        @NonNull WebSocketHandlerRegistry registry
    ) {
        registry
            .addHandler(
                Objects.requireNonNull(speakingWebSocketHandler),
                "/ws/speaking/{sessionId}"
            )
            .addInterceptors(speakingWebSocketAuthInterceptor)
            .setAllowedOrigins(
                Objects.requireNonNull(
                    speakingGeminiLiveProperties.getAllowedOrigins()
                )
            );
    }
}
