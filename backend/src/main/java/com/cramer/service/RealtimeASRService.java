package com.cramer.service;

import com.cramer.service.abts.OpenRouterClient;
import com.cramer.service.abts.OpenRouterClient.AudioInput;
import com.cramer.service.abts.OpenRouterClient.AudioPart;
import com.cramer.service.abts.OpenRouterClient.ContentPart;
import com.cramer.service.abts.OpenRouterClient.TextPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Service for real-time audio transcription using Gemini 2.5 Flash Lite.
 * 
 * Uses OpenRouter to access Gemini's multimodal capabilities for
 * speech-to-text transcription. Optimized for low latency and cost.
 * 
 * Model mapping (from speaking_session_foundations_vi.md):
 * - Post-turn ASR/transcript: gemini-2.5-flash-lite
 * - ASR refinement: gemini-2.5-flash / gemini-2.5-pro
 * 
 * @since 2026-02-06 - Speaking Feature Enhancement
 */
@Service
public class RealtimeASRService {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeASRService.class);

    // Gemini Flash Lite for cost-effective ASR
    private static final String ASR_MODEL = "google/gemini-2.5-flash-lite";
    
    // Alternative models for more accurate transcription
    private static final String ASR_HIGH_ACCURACY_MODEL = "google/gemini-2.5-flash";

    private static final String TRANSCRIPTION_PROMPT = """
        You are a professional speech-to-text transcription system.
        Transcribe the following audio accurately. The speaker is taking an IELTS Speaking test.
        
        Guidelines:
        - Transcribe exactly what is spoken, including filler words (um, uh, like, you know)
        - Mark unclear speech with [unclear]
        - Do not add punctuation unless there's a clear pause
        - Do not correct grammar - transcribe as spoken
        - If the audio is silent or inaudible, respond with [silence]
        
        Return ONLY the transcribed text, nothing else.
        """;

    private final OpenRouterClient openRouterClient;
    private final GroqWhisperService groqWhisperService;

    @Value("${speaking.asr.provider:openrouter}")
    private String asrProvider;

    @Value("${speaking.asr.enabled:true}")
    private boolean asrEnabled;

    public RealtimeASRService(OpenRouterClient openRouterClient, 
                              GroqWhisperService groqWhisperService) {
        this.openRouterClient = openRouterClient;
        this.groqWhisperService = groqWhisperService;
    }

    /**
     * Check if ASR service is configured and available.
     */
    public boolean isConfigured() {
        if (!asrEnabled) return false;
        
        if ("groq".equalsIgnoreCase(asrProvider)) {
            return groqWhisperService.isConfigured();
        }
        return true; // OpenRouter is always available if API key is set
    }

    /**
     * Transcribe audio data to text.
     * 
     * @param audioData Raw audio bytes (WebM/Opus, MP3, WAV, etc.)
     * @param audioFormat Audio format (e.g., "webm", "mp3", "wav")
     * @return Transcribed text, or null if transcription fails
     */
    public String transcribe(byte[] audioData, String audioFormat) {
        if (!asrEnabled) {
            logger.warn("ASR is disabled. Skipping transcription.");
            return null;
        }

        if (audioData == null || audioData.length == 0) {
            logger.warn("Empty audio data provided");
            return "[silence]";
        }

        logger.info("Transcribing audio: {} bytes, format: {}, provider: {}", 
                    audioData.length, audioFormat, asrProvider);

        try {
            if ("groq".equalsIgnoreCase(asrProvider)) {
                return transcribeWithGroq(audioData, audioFormat);
            } else {
                return transcribeWithGemini(audioData, audioFormat);
            }
        } catch (Exception e) {
            logger.error("Transcription failed: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Transcribe audio from a URL.
     * 
     * @param audioUrl Public URL to the audio file
     * @return Transcribed text
     */
    public String transcribeFromUrl(String audioUrl) {
        if (!asrEnabled) {
            logger.warn("ASR is disabled. Skipping transcription.");
            return null;
        }

        if ("groq".equalsIgnoreCase(asrProvider)) {
            return groqWhisperService.transcribeAudio(audioUrl);
        } else {
            // Download and transcribe with Gemini
            return transcribeUrlWithGemini(audioUrl);
        }
    }

    /**
     * Refine an existing transcript using a higher-accuracy model.
     * Useful for post-session transcript cleanup.
     * 
     * @param audioData Audio bytes
     * @param audioFormat Audio format
     * @param roughTranscript Initial transcription to refine
     * @return Refined transcript with better accuracy
     */
    public String refineTranscript(byte[] audioData, String audioFormat, String roughTranscript) {
        if (!asrEnabled || audioData == null || audioData.length == 0) {
            return roughTranscript;
        }

        String refinementPrompt = """
            You are refining an automatic speech transcript. The original transcription may have errors.
            
            Original transcript: %s
            
            Listen carefully to the audio and provide a corrected, more accurate transcription.
            Fix obvious misheard words while keeping the natural speech patterns (fillers, pauses).
            Return ONLY the refined transcript, nothing else.
            """.formatted(roughTranscript);

        try {
            String base64Audio = Base64.getEncoder().encodeToString(audioData);
            List<ContentPart> contentParts = new ArrayList<>();
            contentParts.add(new TextPart(refinementPrompt));
            contentParts.add(new AudioPart(new AudioInput(base64Audio, normalizeFormat(audioFormat))));

            OpenRouterClient.OpenRouterResponse response = openRouterClient.callWithAudio(
                refinementPrompt,
                contentParts,
                ASR_HIGH_ACCURACY_MODEL, // Use higher accuracy model for refinement
                null,
                "refined_transcript"
            );

            String refined = response.getContent();
            if (refined != null && !refined.isBlank()) {
                return cleanTranscript(refined);
            }
        } catch (Exception e) {
            logger.warn("Transcript refinement failed, returning original: {}", e.getMessage());
        }

        return roughTranscript;
    }

    /**
     * Transcribe using Gemini via OpenRouter.
     * Uses multimodal API with audio input.
     */
    private String transcribeWithGemini(byte[] audioData, String audioFormat) {
        try {
            // Encode audio as base64
            String base64Audio = Base64.getEncoder().encodeToString(audioData);

            // Build content parts
            List<ContentPart> contentParts = new ArrayList<>();
            contentParts.add(new TextPart(TRANSCRIPTION_PROMPT));
            contentParts.add(new AudioPart(new AudioInput(base64Audio, normalizeFormat(audioFormat))));

            // Call OpenRouter with audio
            OpenRouterClient.OpenRouterResponse response = openRouterClient.callWithAudio(
                TRANSCRIPTION_PROMPT,
                contentParts,
                ASR_MODEL,
                null, // No JSON schema needed
                "transcript"
            );

            String transcript = response.getContent();
            if (transcript != null && !transcript.isBlank()) {
                transcript = cleanTranscript(transcript);
                logger.info("Gemini transcription successful: {} chars", transcript.length());
                return transcript;
            }

            logger.warn("Gemini transcription returned empty result");
            return "[silence]";

        } catch (Exception e) {
            logger.error("Gemini transcription failed: {}", e.getMessage());
            
            // Try Groq as fallback
            if (groqWhisperService.isConfigured()) {
                logger.info("Falling back to Groq Whisper");
                return transcribeWithGroq(audioData, audioFormat);
            }
            
            throw new RuntimeException("Transcription failed: " + e.getMessage(), e);
        }
    }

    /**
     * Transcribe from URL using Gemini.
     * Downloads audio and uses multimodal API.
     */
    private String transcribeUrlWithGemini(String audioUrl) {
        try {
            // Download audio
            byte[] audioData = downloadAudio(audioUrl);
            if (audioData == null) {
                logger.error("Failed to download audio from: {}", audioUrl);
                return null;
            }

            // Detect format from URL
            String format = detectFormatFromUrl(audioUrl);
            
            return transcribeWithGemini(audioData, format);

        } catch (Exception e) {
            logger.error("Failed to transcribe from URL: {}", e.getMessage());
            
            // Fallback to Groq
            if (groqWhisperService.isConfigured()) {
                return groqWhisperService.transcribeAudio(audioUrl);
            }
            return null;
        }
    }

    /**
     * Transcribe using Groq Whisper (URL-based only).
     * Note: Groq Whisper doesn't support raw bytes, so we return null
     * and let the caller fall back to URL-based transcription.
     */
    @SuppressWarnings("unused")
    private String transcribeWithGroq(byte[] audioData, String audioFormat) {
        // Groq Whisper requires URL, so this is a simplified implementation
        // The audioData and audioFormat params are kept for interface consistency
        logger.warn("Groq transcription from raw bytes not supported. Upload to storage first.");
        logger.debug("Audio data size: {} bytes, format: {}", audioData.length, audioFormat);
        return null;
    }

    /**
     * Download audio from URL.
     */
    private byte[] downloadAudio(String url) {
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            return null;
        } catch (Exception e) {
            logger.error("Failed to download audio: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Normalize audio format string for API.
     */
    private String normalizeFormat(String format) {
        if (format == null) return "webm";
        
        return switch (format.toLowerCase()) {
            case "webm", "audio/webm" -> "webm";
            case "mp3", "audio/mpeg", "audio/mp3" -> "mp3";
            case "wav", "audio/wav", "audio/wave" -> "wav";
            case "ogg", "audio/ogg" -> "ogg";
            case "m4a", "audio/m4a" -> "m4a";
            default -> format.toLowerCase();
        };
    }

    /**
     * Detect audio format from URL.
     */
    private String detectFormatFromUrl(String url) {
        if (url == null) return "webm";
        
        String lower = url.toLowerCase();
        if (lower.contains(".mp3")) return "mp3";
        if (lower.contains(".wav")) return "wav";
        if (lower.contains(".ogg")) return "ogg";
        if (lower.contains(".m4a")) return "m4a";
        if (lower.contains(".webm")) return "webm";
        
        return "webm"; // Default
    }

    /**
     * Clean up transcript text.
     */
    private String cleanTranscript(String transcript) {
        if (transcript == null) return null;
        
        // Remove common prefixes from AI responses
        transcript = transcript.trim();
        
        // Remove quotes if the whole transcript is quoted
        if (transcript.startsWith("\"") && transcript.endsWith("\"")) {
            transcript = transcript.substring(1, transcript.length() - 1);
        }
        
        return transcript.trim();
    }

    /**
     * Get the ASR provider configuration.
     */
    public String getProvider() {
        return asrProvider;
    }

    /**
     * Check if real-time ASR is available.
     * Real-time requires OpenRouter (Gemini) - Groq is batch only.
     */
    public boolean isRealtimeAvailable() {
        return asrEnabled && !"groq".equalsIgnoreCase(asrProvider);
    }
}
