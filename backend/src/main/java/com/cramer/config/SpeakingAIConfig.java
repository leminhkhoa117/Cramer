package com.cramer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for Speaking Practice AI APIs
 * - Groq: STT (Whisper) and LLM (Llama)
 * - ElevenLabs: TTS (Text-to-Speech)
 */
@Configuration
@Slf4j
public class SpeakingAIConfig {
    
    @Value("${groq.api.key:}")
    private String groqApiKey;
    
    @Value("${elevenlabs.api.key:}")
    private String elevenLabsApiKey;
    
    @Value("${groq.timeout-ms:30000}")
    private int timeoutMs;
    
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs * 2); // Read timeout is longer for audio generation
        
        RestTemplate restTemplate = new RestTemplate(factory);
        log.info("✅ RestTemplate configured with timeout: {}ms", timeoutMs);
        
        return restTemplate;
    }
    
    @Bean
    public boolean speakingAIConfigured() {
        boolean groqConfigured = groqApiKey != null && !groqApiKey.isBlank();
        boolean elevenLabsConfigured = elevenLabsApiKey != null && !elevenLabsApiKey.isBlank();
        
        if (groqConfigured) {
            log.info("✅ Groq API key is configured (STT + LLM)");
        } else {
            log.warn("⚠️ Groq API key is NOT configured. Speaking STT/LLM will not work.");
        }
        
        if (elevenLabsConfigured) {
            log.info("✅ ElevenLabs API key is configured (TTS)");
        } else {
            log.warn("⚠️ ElevenLabs API key is NOT configured. TTS will be disabled.");
        }
        
        return groqConfigured;
    }
}
