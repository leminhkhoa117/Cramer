package com.cramer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

/**
 * Service for audio transcription using Groq Whisper API.
 *
 * Groq provides fast, affordable Whisper-based speech-to-text transcription.
 * API Reference: https://console.groq.com/docs/speech-text
 */
@Service
public class GroqWhisperService {

    private static final Logger logger = LoggerFactory.getLogger(GroqWhisperService.class);
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/audio/transcriptions";
    private static final String DEFAULT_MODEL = "whisper-large-v3";
    private static final int MAX_FILE_SIZE_MB = 25; // Groq limit

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.whisper.model:whisper-large-v3}")
    private String whisperModel;

    private final RestTemplate restTemplate;

    public GroqWhisperService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Check if Groq Whisper service is configured and available.
     *
     * @return true if API key is configured
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Transcribe audio from a public URL using Groq Whisper API.
     *
     * Process:
     * 1. Download audio file from URL (e.g., Supabase Storage)
     * 2. Send audio to Groq Whisper API
     * 3. Return transcribed text
     *
     * @param audioUrl Public URL to the audio file
     * @return Transcribed text, or null if transcription fails
     */
    public String transcribeAudio(String audioUrl) {
        if (!isConfigured()) {
            logger.warn("Groq API key not configured. Skipping transcription.");
            return null;
        }

        if (audioUrl == null || audioUrl.isBlank()) {
            logger.warn("Audio URL is empty. Skipping transcription.");
            return null;
        }

        try {
            logger.info("Starting transcription for audio: {}", audioUrl);

            // Step 1: Download audio file from URL
            byte[] audioData = downloadAudioFile(audioUrl);
            if (audioData == null || audioData.length == 0) {
                logger.error("Failed to download audio file from: {}", audioUrl);
                return null;
            }

            // Check file size
            int fileSizeMB = audioData.length / (1024 * 1024);
            if (fileSizeMB > MAX_FILE_SIZE_MB) {
                logger.error("Audio file too large: {}MB (max: {}MB)", fileSizeMB, MAX_FILE_SIZE_MB);
                return null;
            }

            // Step 2: Send to Groq Whisper API
            String transcript = callGroqWhisperApi(audioData, extractFileName(audioUrl));

            if (transcript != null && !transcript.isBlank()) {
                logger.info("Transcription successful. Length: {} chars", transcript.length());
                return transcript.trim();
            }

            logger.warn("Transcription returned empty result");
            return null;

        } catch (Exception e) {
            logger.error("Transcription failed for URL {}: {}", audioUrl, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Download audio file from a public URL.
     */
    private byte[] downloadAudioFile(String url) {
        try {
            logger.debug("Downloading audio from: {}", url);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    URI.create(url),
                    HttpMethod.GET,
                    null,
                    byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.debug("Downloaded {} bytes", response.getBody().length);
                return response.getBody();
            }

            logger.error("Failed to download audio. Status: {}", response.getStatusCode());
            return null;

        } catch (Exception e) {
            logger.error("Error downloading audio: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Call Groq Whisper API with audio data.
     *
     * @param audioData Raw audio bytes
     * @param fileName Original filename for content type detection
     * @return Transcribed text
     */
    private String callGroqWhisperApi(byte[] audioData, String fileName) {
        try {
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(apiKey);

            // Prepare multipart body
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // Add audio file
            ByteArrayResource fileResource = new ByteArrayResource(audioData) {
                @Override
                public String getFilename() {
                    return fileName != null ? fileName : "audio.webm";
                }
            };
            body.add("file", fileResource);

            // Add model parameter
            body.add("model", whisperModel != null ? whisperModel : DEFAULT_MODEL);

            // Add language hint for better accuracy (optional)
            body.add("language", "en"); // IELTS is in English

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            logger.debug("Calling Groq Whisper API with model: {}", whisperModel);

            // Make API call
            ResponseEntity<Map> response = restTemplate.exchange(
                    GROQ_API_URL,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object text = response.getBody().get("text");
                if (text != null) {
                    return text.toString();
                }
            }

            logger.error("Groq API error. Status: {}, Body: {}",
                    response.getStatusCode(), response.getBody());
            return null;

        } catch (Exception e) {
            logger.error("Groq Whisper API call failed: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extract filename from URL.
     */
    private String extractFileName(String url) {
        try {
            String path = URI.create(url).getPath();
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < path.length() - 1) {
                return path.substring(lastSlash + 1);
            }
            return "audio.webm";
        } catch (Exception e) {
            return "audio.webm";
        }
    }
}
