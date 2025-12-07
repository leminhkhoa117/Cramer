package com.cramer.service;

import com.cramer.entity.WritingSubmission;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Service for grading IELTS Writing essays using Gemini AI API.
 * Provides detailed feedback including band scores, corrections, and sample essays.
 * 
 * Uses Gemini 2.5 Flash for faster grading with higher rate limits.
 * Rate limits (Free Tier): 10 RPM for 2.5-flash, 2 RPM for 2.5-pro
 */
@Service
public class GeminiGradingService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiGradingService.class);
    
    // Using Gemini 2.5 Flash for higher rate limits (10 RPM vs 2 RPM for Pro)
    // Trade-off: Slightly less nuanced but still accurate for IELTS grading
    private static final String GEMINI_MODEL = "gemini-2.5-flash";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/" + GEMINI_MODEL + ":generateContent";
    
    // Minimum word thresholds for IELTS Writing
    private static final int TASK_1_MIN_WORDS = 150;
    private static final int TASK_2_MIN_WORDS = 250;
    private static final int MINIMUM_ESSAY_WORDS = 20; // Below this = band 0-1
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiGradingService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Grade a writing submission using Gemini AI.
     * 
     * @param submission The writing submission to grade
     * @param taskPrompt The original task prompt/question
     * @param taskImageUrl Optional image URL for Task 1 (charts, diagrams, maps)
     * @param apiKey User's Gemini API key
     * @return Updated submission with grading results
     */
    public WritingSubmission gradeSubmission(WritingSubmission submission, String taskPrompt, 
                                              String taskImageUrl, String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.error("No Gemini API key provided for grading");
            submission.setGradingStatus("FAILED");
            Map<String, Object> errorFeedback = new HashMap<>();
            errorFeedback.put("error", "No API key provided. Please add your Gemini API key in Profile settings.");
            submission.setAiFeedback(errorFeedback);
            return submission;
        }

        try {
            // Check for empty or minimal essay - return band 0-1 without calling API
            String essayText = submission.getEssayText();
            int wordCount = submission.getWordCount();
            
            if (essayText == null || essayText.trim().isEmpty()) {
                logger.warn("Empty essay submitted for grading");
                return handleEmptyEssay(submission);
            }
            
            if (wordCount < MINIMUM_ESSAY_WORDS) {
                logger.warn("Essay too short ({} words) - below minimum threshold", wordCount);
                return handleMinimalEssay(submission, wordCount);
            }
            
            submission.setGradingStatus("GRADING");
            
            // Call Gemini API with multimodal support
            String response = callGeminiApiWithImage(
                submission.getTaskNumber(),
                taskPrompt,
                essayText,
                wordCount,
                taskImageUrl,
                apiKey
            );
            
            // Parse and apply results
            parseAndApplyGradingResults(submission, response);
            
            submission.setGradingStatus("COMPLETED");
            submission.setGradedAt(OffsetDateTime.now());
            
            logger.info("Successfully graded submission {} with overall band {}", 
                       submission.getId(), submission.getOverallBand());
            
        } catch (Exception e) {
            logger.error("Failed to grade submission {}: {}", submission.getId(), e.getMessage(), e);
            submission.setGradingStatus("FAILED");
            Map<String, Object> errorFeedback = new HashMap<>();
            errorFeedback.put("error", "Grading failed: " + e.getMessage());
            submission.setAiFeedback(errorFeedback);
        }

        return submission;
    }

    /**
     * Handle empty essay - return band 0 without calling API.
     */
    private WritingSubmission handleEmptyEssay(WritingSubmission submission) {
        submission.setGradingStatus("COMPLETED");
        submission.setOverallBand(BigDecimal.ZERO);
        
        Map<String, Object> bandScores = new HashMap<>();
        String criterion = submission.getTaskNumber() == 1 ? "task_achievement" : "task_response";
        bandScores.put(criterion, 0.0);
        bandScores.put("coherence_cohesion", 0.0);
        bandScores.put("lexical_resource", 0.0);
        bandScores.put("grammatical_range_accuracy", 0.0);
        submission.setBandScores(bandScores);
        
        Map<String, Object> feedback = new HashMap<>();
        feedback.put("error", "Bài viết trống. Vui lòng viết bài để được chấm điểm.");
        Map<String, Object> feedbackSummary = new HashMap<>();
        feedbackSummary.put("strengths", Collections.emptyList());
        feedbackSummary.put("weaknesses", Arrays.asList("Không có nội dung bài viết"));
        feedbackSummary.put("improvement_tips", "Hãy viết bài hoàn chỉnh với đủ số từ yêu cầu.");
        feedback.put("feedback_summary", feedbackSummary);
        submission.setAiFeedback(feedback);
        submission.setGradedAt(OffsetDateTime.now());
        
        return submission;
    }

    /**
     * Handle minimal essay (under 20 words) - return band 1 without calling API.
     */
    private WritingSubmission handleMinimalEssay(WritingSubmission submission, int wordCount) {
        submission.setGradingStatus("COMPLETED");
        submission.setOverallBand(BigDecimal.ONE);
        
        Map<String, Object> bandScores = new HashMap<>();
        String criterion = submission.getTaskNumber() == 1 ? "task_achievement" : "task_response";
        bandScores.put(criterion, 1.0);
        bandScores.put("coherence_cohesion", 1.0);
        bandScores.put("lexical_resource", 1.0);
        bandScores.put("grammatical_range_accuracy", 1.0);
        submission.setBandScores(bandScores);
        
        Map<String, Object> feedback = new HashMap<>();
        Map<String, Object> feedbackSummary = new HashMap<>();
        feedbackSummary.put("strengths", Collections.emptyList());
        feedbackSummary.put("weaknesses", Arrays.asList(
            "Bài viết quá ngắn (" + wordCount + " từ)",
            "Không đủ nội dung để đánh giá"
        ));
        int minWords = submission.getTaskNumber() == 1 ? TASK_1_MIN_WORDS : TASK_2_MIN_WORDS;
        feedbackSummary.put("improvement_tips", 
            "Task " + submission.getTaskNumber() + " yêu cầu tối thiểu " + minWords + " từ. " +
            "Bài viết của bạn chỉ có " + wordCount + " từ.");
        feedback.put("feedback_summary", feedbackSummary);
        
        Map<String, String> criteriaComments = new HashMap<>();
        criteriaComments.put("task_achievement", "Bài viết quá ngắn, không thể đánh giá Task Achievement/Response.");
        criteriaComments.put("coherence_cohesion", "Không đủ nội dung để đánh giá tính mạch lạc và liên kết.");
        criteriaComments.put("lexical_resource", "Không đủ nội dung để đánh giá vốn từ vựng.");
        criteriaComments.put("grammatical_range", "Không đủ nội dung để đánh giá ngữ pháp.");
        feedback.put("criteria_comments", criteriaComments);
        
        submission.setAiFeedback(feedback);
        submission.setGradedAt(OffsetDateTime.now());
        
        return submission;
    }

    /**
     * Build the comprehensive IELTS grading system prompt with official band descriptors.
     */
    private String buildSystemPrompt(Integer taskNumber, int wordCount) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("# IELTS Writing Examiner System\n\n");
        prompt.append("You are a certified IELTS examiner with 15+ years of experience. ");
        prompt.append("Your role is to grade IELTS Writing essays accurately and fairly according to the official IELTS band descriptors.\n\n");
        
        // Grading philosophy - tolerant but accurate
        prompt.append("## Grading Philosophy\n");
        prompt.append("- Grade **accurately but tolerantly** - recognize that test-takers are under time pressure\n");
        prompt.append("- Focus on **what the student CAN do**, not just errors\n");
        prompt.append("- Minor slips that don't impede communication should NOT heavily penalize the score\n");
        prompt.append("- A score of 6.0-6.5 represents a competent user - this is a good, achievable score\n");
        prompt.append("- A score of 7.0+ requires consistent demonstration of the criteria across the essay\n");
        prompt.append("- Band 9 is extremely rare and requires near-native proficiency\n");
        prompt.append("- **Do NOT under-score** - if in doubt between two bands, give the higher one\n\n");
        
        // Word count context
        int minWords = taskNumber == 1 ? TASK_1_MIN_WORDS : TASK_2_MIN_WORDS;
        prompt.append("## Word Count Information\n");
        prompt.append("- **Submitted word count**: ").append(wordCount).append(" words\n");
        prompt.append("- **Minimum requirement**: ").append(minWords).append(" words\n");
        if (wordCount < minWords) {
            int deficit = minWords - wordCount;
            prompt.append("- **WARNING**: Essay is ").append(deficit).append(" words SHORT of minimum. ");
            prompt.append("This will affect Task Achievement/Response score, but other criteria should still be assessed fairly.\n");
        } else {
            prompt.append("- Word count requirement is MET.\n");
        }
        prompt.append("\n");
        
        // Official Band Descriptors (Band 3-9)
        if (taskNumber == 1) {
            prompt.append(getTask1BandDescriptors());
        } else {
            prompt.append(getTask2BandDescriptors());
        }
        
        return prompt.toString();
    }

    /**
     * Get official IELTS Task 1 band descriptors (bands 3-9).
     */
    private String getTask1BandDescriptors() {
        StringBuilder desc = new StringBuilder();
        desc.append("## Official IELTS Writing Task 1 Band Descriptors\n\n");
        
        // Band 9
        desc.append("### Band 9\n");
        desc.append("- **Task Achievement**: All requirements fully and appropriately satisfied. Extremely rare lapses in content.\n");
        desc.append("- **Coherence & Cohesion**: Message followed effortlessly. Cohesion rarely attracts attention. Skilful paragraphing.\n");
        desc.append("- **Lexical Resource**: Full flexibility and precise use. Very natural and sophisticated control. Minor errors extremely rare.\n");
        desc.append("- **Grammatical Range & Accuracy**: Wide range with full flexibility and control. Minor errors extremely rare.\n\n");
        
        // Band 8
        desc.append("### Band 8\n");
        desc.append("- **Task Achievement**: Covers all requirements appropriately and sufficiently. Key features skilfully selected and clearly presented. Occasional lapses.\n");
        desc.append("- **Coherence & Cohesion**: Message followed with ease. Information logically sequenced. Cohesion well managed. Occasional lapses.\n");
        desc.append("- **Lexical Resource**: Wide resource fluently and flexibly used. Skilful use of uncommon items. Occasional errors have minimal impact.\n");
        desc.append("- **Grammatical Range & Accuracy**: Wide range flexibly and accurately used. Majority error-free. Occasional non-systematic errors.\n\n");
        
        // Band 7
        desc.append("### Band 7\n");
        desc.append("- **Task Achievement**: Covers requirements. Content relevant and accurate with few omissions. Clear overview, appropriate categorisation.\n");
        desc.append("- **Coherence & Cohesion**: Information logically organised with clear progression. Some inaccuracies in cohesive devices.\n");
        desc.append("- **Lexical Resource**: Sufficient flexibility and precision. Some ability to use less common items. Few spelling/word form errors.\n");
        desc.append("- **Grammatical Range & Accuracy**: Variety of complex structures with some flexibility. Generally well controlled. Few errors persist.\n\n");
        
        // Band 6
        desc.append("### Band 6\n");
        desc.append("- **Task Achievement**: Focuses on requirements with appropriate format. Key features covered adequately. Some irrelevant or inaccurate details.\n");
        desc.append("- **Coherence & Cohesion**: Generally arranged coherently with clear overall progression. Some faulty cohesion. Some repetition.\n");
        desc.append("- **Lexical Resource**: Generally adequate for the task. Meaning generally clear despite restricted range or lack of precision.\n");
        desc.append("- **Grammatical Range & Accuracy**: Mix of simple and complex forms. Limited flexibility. Errors rarely impede communication.\n\n");
        
        // Band 5
        desc.append("### Band 5\n");
        desc.append("- **Task Achievement**: Generally addresses requirements. Key features not adequately covered. May focus too much on details.\n");
        desc.append("- **Coherence & Cohesion**: Organisation evident but not wholly logical. Sentences not fluently linked. Limited/overuse of cohesive devices.\n");
        desc.append("- **Lexical Resource**: Limited but minimally adequate. Simple vocabulary used accurately. Frequent lapses in appropriacy.\n");
        desc.append("- **Grammatical Range & Accuracy**: Limited and rather repetitive structures. Complex sentences tend to be faulty.\n\n");
        
        // Band 4
        desc.append("### Band 4\n");
        desc.append("- **Task Achievement**: Attempts to address task. Few key features selected. May be irrelevant or repetitive.\n");
        desc.append("- **Coherence & Cohesion**: Information not arranged coherently. No clear progression. Basic cohesive devices may be inaccurate.\n");
        desc.append("- **Lexical Resource**: Limited and inadequate for task. Basic vocabulary used repetitively. Errors may impede meaning.\n");
        desc.append("- **Grammatical Range & Accuracy**: Very limited range. Simple sentences predominate. Frequent errors may impede meaning.\n\n");
        
        // Band 3
        desc.append("### Band 3\n");
        desc.append("- **Task Achievement**: Does not address requirements (possibly misunderstanding). Limited information, may be irrelevant.\n");
        desc.append("- **Coherence & Cohesion**: No apparent logical organisation. Minimal use of cohesive devices. Little control of organisational features.\n");
        desc.append("- **Lexical Resource**: Inadequate. Very limited control. Errors predominate and may severely impede meaning.\n");
        desc.append("- **Grammatical Range & Accuracy**: Errors predominate. Little evidence of sentence forms except memorised phrases.\n\n");
        
        return desc.toString();
    }

    /**
     * Get official IELTS Task 2 band descriptors (bands 3-9).
     */
    private String getTask2BandDescriptors() {
        StringBuilder desc = new StringBuilder();
        desc.append("## Official IELTS Writing Task 2 Band Descriptors\n\n");
        
        // Band 9
        desc.append("### Band 9\n");
        desc.append("- **Task Response**: Prompt appropriately addressed and explored in depth. Clear, fully developed position. Ideas fully extended and well supported.\n");
        desc.append("- **Coherence & Cohesion**: Message followed effortlessly. Cohesion rarely attracts attention. Skilful paragraphing.\n");
        desc.append("- **Lexical Resource**: Full flexibility and precise use. Very natural and sophisticated control. Minor errors extremely rare.\n");
        desc.append("- **Grammatical Range & Accuracy**: Wide range with full flexibility and control. Minor errors extremely rare.\n\n");
        
        // Band 8
        desc.append("### Band 8\n");
        desc.append("- **Task Response**: Prompt appropriately and sufficiently addressed. Clear, well-developed position. Ideas relevant, well extended and supported.\n");
        desc.append("- **Coherence & Cohesion**: Message followed with ease. Information logically sequenced. Cohesion well managed.\n");
        desc.append("- **Lexical Resource**: Wide resource fluently and flexibly used. Skilful use of uncommon items. Occasional errors have minimal impact.\n");
        desc.append("- **Grammatical Range & Accuracy**: Wide range flexibly and accurately used. Majority error-free. Occasional non-systematic errors.\n\n");
        
        // Band 7
        desc.append("### Band 7\n");
        desc.append("- **Task Response**: Main parts of prompt appropriately addressed. Clear, developed position. Some tendency to over-generalise.\n");
        desc.append("- **Coherence & Cohesion**: Information logically organised with clear progression. Some inaccuracies in cohesive devices. Effective paragraphing.\n");
        desc.append("- **Lexical Resource**: Sufficient flexibility and precision. Some ability to use less common items. Few spelling/word form errors.\n");
        desc.append("- **Grammatical Range & Accuracy**: Variety of complex structures with some flexibility. Generally well controlled. Few errors persist.\n\n");
        
        // Band 6
        desc.append("### Band 6\n");
        desc.append("- **Task Response**: Main parts addressed but some more fully than others. Position relevant but conclusions may be unclear.\n");
        desc.append("- **Coherence & Cohesion**: Generally arranged coherently. Some faulty cohesion. Paragraphing may not always be logical.\n");
        desc.append("- **Lexical Resource**: Generally adequate for task. Meaning generally clear despite restricted range or lack of precision.\n");
        desc.append("- **Grammatical Range & Accuracy**: Mix of simple and complex forms. Limited flexibility. Errors rarely impede communication.\n\n");
        
        // Band 5
        desc.append("### Band 5\n");
        desc.append("- **Task Response**: Main parts incompletely addressed. Position expressed but development not always clear. Limited ideas, some irrelevant detail.\n");
        desc.append("- **Coherence & Cohesion**: Organisation evident but not wholly logical. Sentences not fluently linked. Paragraphing may be inadequate.\n");
        desc.append("- **Lexical Resource**: Limited but minimally adequate. Simple vocabulary used accurately. Frequent lapses in appropriacy.\n");
        desc.append("- **Grammatical Range & Accuracy**: Limited and rather repetitive structures. Complex sentences tend to be faulty.\n\n");
        
        // Band 4
        desc.append("### Band 4\n");
        desc.append("- **Task Response**: Tackled in minimal way or tangential. Position discernible but reader must work to find it. Ideas lack clarity.\n");
        desc.append("- **Coherence & Cohesion**: Information not arranged coherently. No clear progression. No paragraphing or unclear topics.\n");
        desc.append("- **Lexical Resource**: Limited and inadequate for task. Basic vocabulary used repetitively. Errors may impede meaning.\n");
        desc.append("- **Grammatical Range & Accuracy**: Very limited range. Simple sentences predominate. Frequent errors may impede meaning.\n\n");
        
        // Band 3
        desc.append("### Band 3\n");
        desc.append("- **Task Response**: No part of prompt adequately addressed or misunderstood. No relevant position. Few ideas, barely related to prompt.\n");
        desc.append("- **Coherence & Cohesion**: No apparent logical organisation. Minimal use of cohesive devices. Little control of organisational features.\n");
        desc.append("- **Lexical Resource**: Inadequate. Very limited control. Errors predominate and may severely impede meaning.\n");
        desc.append("- **Grammatical Range & Accuracy**: Errors predominate. Little evidence of sentence forms except memorised phrases.\n\n");
        
        return desc.toString();
    }

    /**
     * Build the user prompt with task details and essay.
     */
    private String buildUserPrompt(Integer taskNumber, String taskPrompt, String essay) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("## Task Prompt:\n");
        prompt.append(taskPrompt).append("\n\n");
        
        prompt.append("## Student's Essay:\n");
        prompt.append("```\n").append(essay).append("\n```\n\n");
        
        prompt.append("## Required Response Format\n");
        prompt.append("You MUST return a valid JSON object with this exact structure. ");
        prompt.append("Do NOT include any text outside the JSON. Do NOT use markdown code fences.\n\n");
        
        prompt.append("{\n");
        prompt.append("  \"band_scores\": {\n");
        if (taskNumber == 1) {
            prompt.append("    \"task_achievement\": <number 3.0-9.0, use 0.5 increments>,\n");
        } else {
            prompt.append("    \"task_response\": <number 3.0-9.0, use 0.5 increments>,\n");
        }
        prompt.append("    \"coherence_cohesion\": <number 3.0-9.0, use 0.5 increments>,\n");
        prompt.append("    \"lexical_resource\": <number 3.0-9.0, use 0.5 increments>,\n");
        prompt.append("    \"grammatical_range_accuracy\": <number 3.0-9.0, use 0.5 increments>\n");
        prompt.append("  },\n");
        prompt.append("  \"overall_band\": <calculated average of 4 criteria, rounded to nearest 0.5>,\n");
        prompt.append("  \"sentence_corrections\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"original\": \"<exact original sentence with error>\",\n");
        prompt.append("      \"corrected\": \"<corrected sentence>\",\n");
        prompt.append("      \"error_type\": \"<grammar|spelling|vocabulary|punctuation|coherence>\",\n");
        prompt.append("      \"explanation\": \"<brief explanation in Vietnamese>\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"paragraph_rewrites\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"paragraph_index\": <0-based index>,\n");
        prompt.append("      \"original\": \"<original paragraph>\",\n");
        prompt.append("      \"improved\": \"<improved paragraph at band+1 level>\",\n");
        prompt.append("      \"improvements_made\": [\"<improvement 1 in Vietnamese>\", \"<improvement 2>\"]\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"sample_essay_band_plus_one\": \"<complete rewritten essay that is 1 band higher>\",\n");
        prompt.append("  \"sample_essay_band_9\": \"<band 9 level model essay for this exact topic>\",\n");
        prompt.append("  \"feedback_summary\": {\n");
        prompt.append("    \"strengths\": [\"<strength 1 in Vietnamese>\", \"<strength 2>\", \"<strength 3>\"],\n");
        prompt.append("    \"weaknesses\": [\"<weakness 1 in Vietnamese>\", \"<weakness 2>\"],\n");
        prompt.append("    \"writing_approach\": \"<suggested approach in Vietnamese, 2-3 sentences>\",\n");
        prompt.append("    \"improvement_tips\": \"<actionable tips in Vietnamese, 2-3 sentences>\"\n");
        prompt.append("  },\n");
        prompt.append("  \"word_analysis\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"word\": \"<advanced/notable word or phrase used>\",\n");
        prompt.append("      \"definition\": \"<Vietnamese definition>\",\n");
        prompt.append("      \"context\": \"<how it was used in the essay>\",\n");
        prompt.append("      \"usage_quality\": \"<good|acceptable|incorrect>\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"criteria_comments\": {\n");
        if (taskNumber == 1) {
            prompt.append("    \"task_achievement\": \"<2-3 sentences explaining the Task Achievement score in Vietnamese>\",\n");
        } else {
            prompt.append("    \"task_achievement\": \"<2-3 sentences explaining the Task Response score in Vietnamese>\",\n");
        }
        prompt.append("    \"coherence_cohesion\": \"<2-3 sentences explaining the Coherence & Cohesion score in Vietnamese>\",\n");
        prompt.append("    \"lexical_resource\": \"<2-3 sentences explaining the Lexical Resource score in Vietnamese>\",\n");
        prompt.append("    \"grammatical_range\": \"<2-3 sentences explaining the Grammatical Range & Accuracy score in Vietnamese>\"\n");
        prompt.append("  }\n");
        prompt.append("}\n\n");
        
        prompt.append("## Grading Instructions\n");
        prompt.append("1. Read the essay carefully and assess against each criterion\n");
        prompt.append("2. For Task 1, carefully analyze the visual data (chart/diagram/map) to verify accuracy of descriptions\n");
        prompt.append("3. Provide at least 3-5 sentence corrections with clear explanations\n");
        prompt.append("4. Rewrite at least the introduction and one body paragraph\n");
        prompt.append("5. The sample essays should be realistic and relevant to the exact topic\n");
        prompt.append("6. All feedback text MUST be in Vietnamese for the student\n");
        prompt.append("7. Be encouraging - highlight what the student did well before weaknesses\n");
        prompt.append("8. Return ONLY the JSON object, no markdown fences or extra text\n");
        
        return prompt.toString();
    }

    /**
     * Call Gemini API with multimodal support (text + image for Task 1).
     */
    private String callGeminiApiWithImage(Integer taskNumber, String taskPrompt, String essay, 
                                           int wordCount, String imageUrl, String apiKey) {
        String url = GEMINI_API_URL + "?key=" + apiKey;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Build request with system instruction and user content
        Map<String, Object> requestBody = new HashMap<>();
        
        // System instruction (separate from content)
        Map<String, Object> systemInstruction = new HashMap<>();
        List<Map<String, Object>> systemParts = new ArrayList<>();
        Map<String, Object> systemTextPart = new HashMap<>();
        systemTextPart.put("text", buildSystemPrompt(taskNumber, wordCount));
        systemParts.add(systemTextPart);
        systemInstruction.put("parts", systemParts);
        requestBody.put("systemInstruction", systemInstruction);
        
        // User content
        Map<String, Object> userContent = new HashMap<>();
        userContent.put("role", "user");
        List<Map<String, Object>> parts = new ArrayList<>();
        
        // Add image for Task 1 if available
        if (taskNumber == 1 && imageUrl != null && !imageUrl.trim().isEmpty()) {
            try {
                Map<String, Object> imagePart = createImagePart(imageUrl);
                if (imagePart != null) {
                    parts.add(imagePart);
                    logger.info("Added image to grading request: {}", imageUrl);
                }
            } catch (Exception e) {
                logger.warn("Failed to add image to request, proceeding with text-only: {}", e.getMessage());
            }
        }
        
        // Add text prompt
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", buildUserPrompt(taskNumber, taskPrompt, essay));
        parts.add(textPart);
        
        userContent.put("parts", parts);
        List<Map<String, Object>> contents = new ArrayList<>();
        contents.add(userContent);
        requestBody.put("contents", contents);
        
        // Generation config optimized for accurate JSON output
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.3); // Lower for more consistent scoring
        generationConfig.put("topP", 0.9);
        generationConfig.put("topK", 40);
        generationConfig.put("maxOutputTokens", 16384); // Increased for detailed feedback
        generationConfig.put("responseMimeType", "application/json"); // Force JSON output
        requestBody.put("generationConfig", generationConfig);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class
            );
            
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Gemini API returned status: " + response.getStatusCode());
            }
            
            return response.getBody();
        } catch (Exception e) {
            logger.error("Gemini API call failed: {}", e.getMessage());
            throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
        }
    }

    /**
     * Create image part for multimodal request.
     * Downloads image and converts to base64 inline data.
     */
    private Map<String, Object> createImagePart(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            try (InputStream is = url.openStream()) {
                byte[] imageBytes = is.readAllBytes();
                String base64Data = Base64.getEncoder().encodeToString(imageBytes);
                
                // Determine MIME type from URL
                String mimeType = "image/png"; // Default
                String lowerUrl = imageUrl.toLowerCase();
                if (lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg")) {
                    mimeType = "image/jpeg";
                } else if (lowerUrl.contains(".gif")) {
                    mimeType = "image/gif";
                } else if (lowerUrl.contains(".webp")) {
                    mimeType = "image/webp";
                }
                
                Map<String, Object> imagePart = new HashMap<>();
                Map<String, Object> inlineData = new HashMap<>();
                inlineData.put("mimeType", mimeType);
                inlineData.put("data", base64Data);
                imagePart.put("inlineData", inlineData);
                
                return imagePart;
            }
        } catch (Exception e) {
            logger.error("Failed to download image from {}: {}", imageUrl, e.getMessage());
            return null;
        }
    }

    /**
     * Parse Gemini API response and apply grading results to submission.
     */
    private void parseAndApplyGradingResults(WritingSubmission submission, String apiResponse) 
            throws JsonProcessingException {
        
        JsonNode root = objectMapper.readTree(apiResponse);
        
        // Extract the generated text from Gemini response
        String generatedText = root
            .path("candidates").get(0)
            .path("content")
            .path("parts").get(0)
            .path("text").asText();
        
        // Clean up the response - remove markdown code blocks if present
        generatedText = generatedText.trim();
        if (generatedText.startsWith("```json")) {
            generatedText = generatedText.substring(7);
        }
        if (generatedText.startsWith("```")) {
            generatedText = generatedText.substring(3);
        }
        if (generatedText.endsWith("```")) {
            generatedText = generatedText.substring(0, generatedText.length() - 3);
        }
        generatedText = generatedText.trim();
        
        // Parse the grading JSON
        JsonNode gradingResult = objectMapper.readTree(generatedText);
        
        // Extract band scores
        JsonNode bandScoresNode = gradingResult.path("band_scores");
        Map<String, Object> bandScores = objectMapper.convertValue(bandScoresNode, Map.class);
        submission.setBandScores(bandScores);
        
        // Calculate and set overall band (rounded to nearest 0.5)
        double overallBandRaw = gradingResult.path("overall_band").asDouble();
        BigDecimal overallBand = roundToNearestHalf(overallBandRaw);
        submission.setOverallBand(overallBand);
        
        // Build AI feedback object
        Map<String, Object> aiFeedback = new HashMap<>();
        
        if (gradingResult.has("sentence_corrections")) {
            aiFeedback.put("sentence_corrections", 
                objectMapper.convertValue(gradingResult.path("sentence_corrections"), List.class));
        }
        
        if (gradingResult.has("paragraph_rewrites")) {
            aiFeedback.put("paragraph_rewrites", 
                objectMapper.convertValue(gradingResult.path("paragraph_rewrites"), List.class));
        }
        
        if (gradingResult.has("sample_essay_band_plus_one")) {
            aiFeedback.put("sample_essay_band_plus_one", 
                gradingResult.path("sample_essay_band_plus_one").asText());
        }
        
        if (gradingResult.has("sample_essay_band_9")) {
            aiFeedback.put("sample_essay_band_9", 
                gradingResult.path("sample_essay_band_9").asText());
        }
        
        if (gradingResult.has("feedback_summary")) {
            aiFeedback.put("feedback_summary", 
                objectMapper.convertValue(gradingResult.path("feedback_summary"), Map.class));
        }
        
        if (gradingResult.has("word_analysis")) {
            aiFeedback.put("word_analysis", 
                objectMapper.convertValue(gradingResult.path("word_analysis"), List.class));
        }
        
        if (gradingResult.has("criteria_comments")) {
            aiFeedback.put("criteria_comments", 
                objectMapper.convertValue(gradingResult.path("criteria_comments"), Map.class));
        }
        
        submission.setAiFeedback(aiFeedback);
    }

    /**
     * Round a band score to the nearest 0.5 according to IELTS rules.
     */
    private BigDecimal roundToNearestHalf(double score) {
        double rounded = Math.round(score * 2) / 2.0;
        return BigDecimal.valueOf(rounded).setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * Validate API key by making a simple test request.
     */
    public boolean validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }
        
        try {
            String url = GEMINI_API_URL + "?key=" + apiKey;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            List<Map<String, String>> parts = new ArrayList<>();
            Map<String, String> textPart = new HashMap<>();
            textPart.put("text", "Hi");
            parts.add(textPart);
            content.put("parts", parts);
            List<Map<String, Object>> contents = new ArrayList<>();
            contents.add(content);
            requestBody.put("contents", contents);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class
            );
            
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            logger.warn("API key validation failed: {}", e.getMessage());
            return false;
        }
    }
}
