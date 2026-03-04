package com.cramer.service;

import com.cramer.dto.SpeakingQuestionDTO;
import com.cramer.service.abts.OpenRouterClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for AI-driven follow-up question selection.
 * 
 * Analyzes conversation context and selects the most appropriate
 * follow-up question from a pre-approved question bank.
 * 
 * Uses Gemini Flash Lite for fast, cost-effective inference.
 * 
 * @since 2026-02-06 - Speaking Feature Enhancement
 */
@Service
public class FollowUpSelectionService {

    private static final Logger logger = LoggerFactory.getLogger(FollowUpSelectionService.class);

    // Fast model for real-time selection
    private static final String SELECTION_MODEL = "google/gemini-2.5-flash-lite";

    private static final String SELECTION_PROMPT = """
        You are an IELTS Speaking examiner selecting follow-up questions.
        
        Based on the candidate's previous answer, select the MOST appropriate follow-up question
        from the provided question bank. Consider:
        
        1. Relevance to what the candidate just said
        2. Natural conversation flow
        3. Opportunity to demonstrate vocabulary/grammar
        4. Avoiding repetition of themes already covered
        
        CONTEXT:
        Part: %d
        Topic: %s
        Previous Question: %s
        Candidate's Answer: %s
        
        Questions already asked: %s
        
        AVAILABLE FOLLOW-UP QUESTIONS:
        %s
        
        Respond with ONLY the question number (1, 2, 3, etc.) of the best follow-up.
        If the candidate's answer was too short or unclear, select a simpler/more general question.
        """;

    private final OpenRouterClient openRouterClient;
    private final SpeakingQuestionService questionService;

    @Value("${speaking.followup.enabled:true}")
    private boolean followUpEnabled;

    public FollowUpSelectionService(OpenRouterClient openRouterClient,
                                    SpeakingQuestionService questionService) {
        this.openRouterClient = openRouterClient;
        this.questionService = questionService;
    }

    /**
     * Select the best follow-up question based on conversation context.
     * 
     * @param topicId Topic ID for the session
     * @param part Current part number
     * @param previousQuestion The question just asked
     * @param candidateAnswer The candidate's transcript
     * @param askedQuestionIds List of question IDs already asked
     * @return The selected follow-up question, or null if none available
     */
    public SpeakingQuestionDTO selectFollowUp(
            Long topicId,
            int part,
            String previousQuestion,
            String candidateAnswer,
            List<Long> askedQuestionIds) {
        
        if (!followUpEnabled) {
            logger.warn("Follow-up selection is disabled");
            return null;
        }

        logger.info("Selecting follow-up question for part {} topic {}", part, topicId);

        // Get available follow-up questions for this part
        List<SpeakingQuestionDTO> availableQuestions = getAvailableFollowUps(topicId, part, askedQuestionIds);
        
        if (availableQuestions.isEmpty()) {
            logger.warn("No available follow-up questions for part {}", part);
            return null;
        }

        // If only one question available, return it
        if (availableQuestions.size() == 1) {
            return availableQuestions.get(0);
        }

        // Use AI to select the best one
        try {
            int selectedIndex = selectWithAI(
                topicId,
                part,
                previousQuestion,
                candidateAnswer,
                askedQuestionIds,
                availableQuestions
            );

            if (selectedIndex >= 0 && selectedIndex < availableQuestions.size()) {
                SpeakingQuestionDTO selected = availableQuestions.get(selectedIndex);
                logger.info("Selected follow-up question: {} (index {})", selected.getText(), selectedIndex);
                return selected;
            }

            // Fallback to first available
            logger.warn("AI selection returned invalid index {}, using first available", selectedIndex);
            return availableQuestions.get(0);

        } catch (Exception e) {
            logger.error("AI selection failed, using fallback: {}", e.getMessage());
            return availableQuestions.get(0);
        }
    }

    /**
     * Get available follow-up questions that haven't been asked yet.
     */
    private List<SpeakingQuestionDTO> getAvailableFollowUps(Long topicId, int part, List<Long> askedQuestionIds) {
        // Get all questions for this part
        String mode = "PART_" + part;
        List<SpeakingQuestionDTO> allQuestions = questionService.getQuestionsForSession(topicId, mode);

        // Filter out already asked questions
        List<SpeakingQuestionDTO> available = new ArrayList<>();
        for (SpeakingQuestionDTO q : allQuestions) {
            if (!askedQuestionIds.contains(q.getId())) {
                available.add(q);
            }
        }

        return available;
    }

    /**
     * Use AI to select the most appropriate follow-up question.
     */
    private int selectWithAI(
            Long topicId,
            int part,
            String previousQuestion,
            String candidateAnswer,
            List<Long> askedQuestionIds,
            List<SpeakingQuestionDTO> availableQuestions) {
        
        // Get topic name
        String topicName = "General";
        try {
            topicName = questionService.getTopicById(topicId).getNameEn();
        } catch (Exception e) {
            logger.warn("Could not get topic name for {}", topicId);
        }

        // Format already asked questions
        String askedList = askedQuestionIds.isEmpty() 
            ? "None"
            : askedQuestionIds.toString();

        // Format available questions
        StringBuilder questionsFormatted = new StringBuilder();
        for (int i = 0; i < availableQuestions.size(); i++) {
            questionsFormatted.append(i + 1)
                .append(". ")
                .append(availableQuestions.get(i).getText())
                .append("\n");
        }

        // Build prompt
        String prompt = String.format(
            SELECTION_PROMPT,
            part,
            topicName,
            previousQuestion,
            candidateAnswer != null ? candidateAnswer : "[No transcript available]",
            askedList,
            questionsFormatted.toString()
        );

        // Call AI
        OpenRouterClient.OpenRouterResponse response = openRouterClient.callChatCompletion(
            SELECTION_MODEL,
            "You are an IELTS Speaking examiner selecting follow-up questions.",
            prompt,
            null // No JSON schema needed
        );

        String content = response.getContent();
        if (content == null || content.isBlank()) {
            logger.warn("Empty AI response for follow-up selection");
            return 0;
        }

        // Parse response - expecting just a number
        try {
            // Extract first number from response
            String numberStr = content.trim().replaceAll("[^0-9]", "");
            if (!numberStr.isEmpty()) {
                int selected = Integer.parseInt(numberStr) - 1; // Convert to 0-based index
                return Math.max(0, Math.min(selected, availableQuestions.size() - 1));
            }
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse AI selection response: {}", content);
        }

        return 0;
    }

    /**
     * Check if follow-up selection is enabled.
     */
    public boolean isEnabled() {
        return followUpEnabled;
    }

    /**
     * Manually select a random follow-up question (fallback).
     */
    public SpeakingQuestionDTO selectRandomFollowUp(Long topicId, int part, List<Long> askedQuestionIds) {
        List<SpeakingQuestionDTO> available = getAvailableFollowUps(topicId, part, askedQuestionIds);
        
        if (available.isEmpty()) {
            return null;
        }

        // Simple random selection
        int randomIndex = (int) (Math.random() * available.size());
        return available.get(randomIndex);
    }
}
