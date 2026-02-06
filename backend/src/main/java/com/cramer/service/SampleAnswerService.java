package com.cramer.service;

import com.cramer.dto.SpeakingQuestionDTO;
import com.cramer.dto.SpeakingTranscriptDTO;
import com.cramer.service.abts.OpenRouterClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for generating sample answers at Band 6 and Band 7-8 levels.
 * 
 * Creates model answers for IELTS Speaking questions to help candidates
 * understand what a good answer looks like at different band levels.
 * 
 * Uses Gemini 2.5 Flash for fast, quality answer generation.
 * 
 * @since 2026-02-06 - Speaking Feature Enhancement
 */
@Service
public class SampleAnswerService {

    private static final Logger logger = LoggerFactory.getLogger(SampleAnswerService.class);

    // Model for sample generation - balance of quality and speed
    private static final String SAMPLE_MODEL = "google/gemini-2.5-flash-lite";

    private static final String SAMPLE_PROMPT_BAND_6 = """
        You are an IELTS Speaking examiner generating a Band 6 sample answer.
        
        Question: %s
        Part: %d
        Topic: %s
        
        Generate a natural, Band 6 level answer that:
        - Is appropriate length for the part (Part 1: 3-4 sentences, Part 2: 90-120 seconds when spoken, Part 3: 4-6 sentences)
        - Uses mostly common vocabulary with occasional less common words
        - Has generally correct grammar with some errors
        - Shows some attempts to develop ideas but may lack depth
        - Has generally fluent delivery with some hesitation
        - Includes some fillers (um, uh, like) for naturalness
        
        Candidate's actual answer for reference (if available): %s
        
        Generate ONLY the sample answer text, nothing else.
        """;

    private static final String SAMPLE_PROMPT_BAND_8 = """
        You are an IELTS Speaking examiner generating a Band 7-8 sample answer.
        
        Question: %s
        Part: %d
        Topic: %s
        
        Generate a natural, Band 7-8 level answer that:
        - Is well-developed and complete for the part
        - Uses a wide range of vocabulary including idiomatic expressions
        - Has accurate and flexible grammar with complex structures
        - Explores ideas in depth with clear logical structure
        - Flows naturally with minimal hesitation
        - Sounds authentic, not robotic
        
        Candidate's actual answer for reference (if available): %s
        
        Generate ONLY the sample answer text, nothing else.
        """;

    private final OpenRouterClient openRouterClient;
    private final SpeakingQuestionService questionService;

    @Value("${speaking.samples.enabled:true}")
    private boolean samplesEnabled;

    public SampleAnswerService(OpenRouterClient openRouterClient,
                               SpeakingQuestionService questionService) {
        this.openRouterClient = openRouterClient;
        this.questionService = questionService;
    }

    /**
     * Generate sample answers for a list of questions.
     * Returns samples for both Band 6 and Band 7-8.
     * 
     * @param questions List of questions with optional candidate transcripts
     * @param topicName Topic name for context
     * @return Map of questionId -> { band6: string, band8: string }
     */
    public Map<Long, Map<String, String>> generateSamplesForQuestions(
            List<SpeakingQuestionDTO> questions,
            String topicName,
            Map<Long, String> candidateAnswers) {
        
        if (!samplesEnabled) {
            logger.warn("Sample answer generation is disabled");
            return Map.of();
        }

        Map<Long, Map<String, String>> results = new HashMap<>();

        for (SpeakingQuestionDTO question : questions) {
            try {
                String candidateAnswer = candidateAnswers != null 
                    ? candidateAnswers.getOrDefault(question.getId(), null) 
                    : null;

                String band6 = generateBand6Sample(question, topicName, candidateAnswer);
                String band8 = generateBand8Sample(question, topicName, candidateAnswer);

                Map<String, String> samples = new HashMap<>();
                samples.put("band6", band6);
                samples.put("band8", band8);
                results.put(question.getId(), samples);

                logger.info("Generated samples for question {}", question.getId());
            } catch (Exception e) {
                logger.error("Failed to generate samples for question {}: {}", 
                            question.getId(), e.getMessage());
            }
        }

        return results;
    }

    /**
     * Generate a Band 6 sample answer.
     */
    public String generateBand6Sample(SpeakingQuestionDTO question, String topicName, String candidateAnswer) {
        String prompt = String.format(
            SAMPLE_PROMPT_BAND_6,
            question.getText(),
            question.getPart(),
            topicName != null ? topicName : "General",
            candidateAnswer != null ? candidateAnswer : "[No candidate answer]"
        );

        try {
            OpenRouterClient.OpenRouterResponse response = openRouterClient.callChatCompletion(
                SAMPLE_MODEL,
                "You are an IELTS Speaking examiner generating sample answers.",
                prompt,
                null // No JSON schema needed
            );

            String content = response.getContent();
            if (content != null && !content.isBlank()) {
                return cleanSampleText(content);
            }
        } catch (Exception e) {
            logger.error("Band 6 sample generation failed: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Generate a Band 7-8 sample answer.
     */
    public String generateBand8Sample(SpeakingQuestionDTO question, String topicName, String candidateAnswer) {
        String prompt = String.format(
            SAMPLE_PROMPT_BAND_8,
            question.getText(),
            question.getPart(),
            topicName != null ? topicName : "General",
            candidateAnswer != null ? candidateAnswer : "[No candidate answer]"
        );

        try {
            OpenRouterClient.OpenRouterResponse response = openRouterClient.callChatCompletion(
                SAMPLE_MODEL,
                "You are an IELTS Speaking examiner generating sample answers.",
                prompt,
                null // No JSON schema needed
            );

            String content = response.getContent();
            if (content != null && !content.isBlank()) {
                return cleanSampleText(content);
            }
        } catch (Exception e) {
            logger.error("Band 8 sample generation failed: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Async version for generating samples in background.
     */
    @Async
    public CompletableFuture<Map<Long, Map<String, String>>> generateSamplesAsync(
            List<SpeakingQuestionDTO> questions,
            String topicName,
            Map<Long, String> candidateAnswers) {
        
        Map<Long, Map<String, String>> results = generateSamplesForQuestions(
            questions, topicName, candidateAnswers
        );
        
        return CompletableFuture.completedFuture(results);
    }

    /**
     * Generate samples for a completed session.
     * 
     * @param sessionId Session ID
     * @param transcripts List of transcripts from the session
     * @return Map of questionId -> sample answers
     */
    public Map<Long, Map<String, String>> generateSamplesForSession(
            Long sessionId,
            Long topicId,
            List<SpeakingTranscriptDTO> transcripts) {
        
        if (!samplesEnabled) {
            return Map.of();
        }

        // Get topic name
        String topicName = "General";
        try {
            topicName = questionService.getTopicById(topicId).getNameEn();
        } catch (Exception e) {
            logger.warn("Could not get topic name for {}", topicId);
        }

        // Build candidate answers map
        Map<Long, String> candidateAnswers = new HashMap<>();
        List<SpeakingQuestionDTO> questions = new ArrayList<>();

        for (SpeakingTranscriptDTO transcript : transcripts) {
            Long questionId = transcript.getQuestionId();
            candidateAnswers.put(questionId, transcript.getTranscriptText());

            // Create minimal question DTO
            SpeakingQuestionDTO q = new SpeakingQuestionDTO();
            q.setId(questionId);
            q.setPart(transcript.getPart());
            q.setText(transcript.getQuestionText()); // If available
            questions.add(q);
        }

        return generateSamplesForQuestions(questions, topicName, candidateAnswers);
    }

    /**
     * Clean up the generated sample text.
     */
    private String cleanSampleText(String text) {
        if (text == null) return null;

        // Remove quotes if wrapped
        text = text.trim();
        if (text.startsWith("\"") && text.endsWith("\"")) {
            text = text.substring(1, text.length() - 1);
        }

        // Remove "Sample answer:" prefix if present
        text = text.replaceFirst("(?i)^(sample answer:|answer:)\\s*", "");

        return text.trim();
    }

    /**
     * Check if sample generation is enabled.
     */
    public boolean isEnabled() {
        return samplesEnabled;
    }
}
