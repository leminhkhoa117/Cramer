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
    private static final String DEFAULT_GEMINI_MODEL = "gemini-2.5-flash";
    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    
    // Available models for user selection
    public static final String[] AVAILABLE_MODELS = {
        "gemini-2.5-flash",      // Fast, high rate limits (10 RPM free)
        "gemini-2.5-flash-lite", // Fastest, highest rate limits
        "gemini-2.5-pro"         // Most capable, lower rate limits (5 RPM free)
    };
    
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
     * @param model User's selected Gemini model (optional, defaults to gemini-2.5-flash)
     * @return Updated submission with grading results
     */
    public WritingSubmission gradeSubmission(WritingSubmission submission, String taskPrompt, 
                                              String taskImageUrl, String apiKey, String model) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.error("No Gemini API key provided for grading");
            submission.setGradingStatus("FAILED");
            Map<String, Object> errorFeedback = new HashMap<>();
            errorFeedback.put("error", "No API key provided. Please add your Gemini API key in Profile settings.");
            submission.setAiFeedback(errorFeedback);
            return submission;
        }
        
        // Use default model if not specified
        String selectedModel = (model != null && !model.trim().isEmpty()) ? model : DEFAULT_GEMINI_MODEL;

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
                apiKey,
                selectedModel
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
     * Enhanced with calibration anchors and generous scoring philosophy.
     * CALIBRATED based on official IELTS sample answers and band scores.
     */
    private String buildSystemPrompt(Integer taskNumber, int wordCount) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("# HỆ THỐNG CHẤM ĐIỂM IELTS WRITING - PHIÊN BẢN ĐÃ HIỆU CHUẨN\n\n");
        prompt.append("Bạn là một giám khảo IELTS được chứng nhận với hơn 15 năm kinh nghiệm. ");
        prompt.append("Nhiệm vụ của bạn là chấm điểm bài viết IELTS một cách chính xác và công bằng theo tiêu chí band descriptors chính thức của IELTS.\n\n");
        
        // CRITICAL: Calibration-focused grading philosophy - SIGNIFICANTLY ENHANCED
        prompt.append("## TRIẾT LÝ CHẤM ĐIỂM - CỰC KỲ QUAN TRỌNG\n\n");
        
        prompt.append("### 🎯 NGUYÊN TẮC VÀNG - ĐỌC KỸ TRƯỚC KHI CHẤM:\n\n");
        
        prompt.append("**1. CHẤM DỰA TRÊN NĂNG LỰC NGÔN NGỮ THỂ HIỆN:**\n");
        prompt.append("   - Đánh giá KHẢ NĂNG VIẾT TIẾNG ANH của thí sinh\n");
        prompt.append("   - Cấu trúc câu có đa dạng không? Từ vựng có phong phú không?\n");
        prompt.append("   - Có thể diễn đạt ý tưởng mạch lạc không?\n");
        prompt.append("   - **QUAN TRỌNG**: Ngay cả bài lạc đề vẫn có thể có điểm ngôn ngữ tốt!\n\n");
        
        prompt.append("**2. XỬ LÝ BÀI LẠC ĐỀ (OFF-TOPIC) - RẤT QUAN TRỌNG:**\n");
        prompt.append("   - Nếu bài HOÀN TOÀN lạc đề: Task Response/Achievement bị ảnh hưởng (giảm 1-2 band)\n");
        prompt.append("   - NHƯNG: Coherence, Lexical Resource, Grammar vẫn chấm BÌNH THƯỜNG theo năng lực thể hiện\n");
        prompt.append("   - Ví dụ thực tế: Bài lạc đề nhưng viết tốt có thể đạt: TR=4.5, CC=6.5, LR=6.5, GRA=6.0 → Overall = 6.0\n");
        prompt.append("   - **KHÔNG** cho tất cả tiêu chí điểm thấp chỉ vì lạc đề!\n\n");
        
        prompt.append("**3. PHÂN LOẠI LỖI - ẢNH HƯỞNG ĐIỂM:**\n");
        prompt.append("   - **Lỗi MINOR** (không ảnh hưởng hiểu): article, số ít/số nhiều nhỏ, typo 1-2 chữ → hầu như KHÔNG trừ điểm\n");
        prompt.append("   - **Lỗi MODERATE**: awkward phrasing, collocation hơi sai → trừ nhẹ, vẫn có thể band 7+\n");
        prompt.append("   - **Lỗi MAJOR** (gây hiểu sai/không hiểu): sai meaning, câu không có nghĩa → ảnh hưởng điểm\n\n");
        
        prompt.append("**4. QUY TẮC \"BENEFIT OF DOUBT\":**\n");
        prompt.append("   - Khi phân vân giữa 2 band liền kề → **LUÔN CHỌN BAND CAO HƠN**\n");
        prompt.append("   - Khi có cả điểm mạnh và điểm yếu rõ ràng → **ƯU TIÊN GHI NHẬN ĐIỂM MẠNH**\n");
        prompt.append("   - Nhớ: Thí sinh viết trong điều kiện thi giới hạn thời gian (20 phút Task 1, 40 phút Task 2)\n\n");
        
        prompt.append("### 📊 BẢNG CALIBRATION THỰC TẾ (dựa trên bài mẫu IELTS chính thức):\n\n");
        prompt.append("| Đặc điểm bài viết | Band thường đạt |\n");
        prompt.append("|-------------------|----------------|\n");
        prompt.append("| Lạc đề hoàn toàn nhưng ngôn ngữ khá | 5.5 - 6.0 |\n");
        prompt.append("| Đúng đề, ý tưởng cơ bản, nhiều lỗi grammar/vocab | 5.0 - 5.5 |\n");
        prompt.append("| Đúng đề, ý tưởng OK, một số lỗi grammar không ảnh hưởng hiểu | 6.0 - 6.5 |\n");
        prompt.append("| Đúng đề, ý tưởng tốt, cấu trúc rõ ràng, ít lỗi | 6.5 - 7.0 |\n");
        prompt.append("| Đúng đề, ý tưởng sâu, từ vựng đa dạng, grammar chính xác | 7.0 - 7.5 |\n");
        prompt.append("| Xuất sắc toàn diện, chỉ lỗi rất nhỏ | 7.5 - 8.0 |\n");
        prompt.append("| Gần như hoàn hảo | 8.0 - 8.5 |\n");
        prompt.append("| Hoàn hảo như native speaker | 9.0 |\n\n");
        
        prompt.append("### 📈 CHI TIẾT VỀ TỪNG BAND (quan trọng để không chấm quá khắt khe):\n\n");
        
        prompt.append("**Band 6.0 - 6.5 (Competent User - PHỔ BIẾN NHẤT):**\n");
        prompt.append("- Đây là band của đa số sinh viên đại học Việt Nam viết tốt\n");
        prompt.append("- Có lỗi grammar nhưng KHÔNG ảnh hưởng communication\n");
        prompt.append("- Từ vựng adequate (đủ dùng) dù không fancy\n");
        prompt.append("- Có thể có một số ý chưa developed đầy đủ\n");
        prompt.append("- **QUAN TRỌNG**: Bài có lỗi rải rác nhưng đọc hiểu được = Band 6.0+\n\n");
        
        prompt.append("**Band 7.0 - 7.5 (Good User):**\n");
        prompt.append("- Ý tưởng được develop rõ ràng với examples/support\n");
        prompt.append("- Có variety trong sentence structures\n");
        prompt.append("- Có sử dụng một số từ vựng less common\n");
        prompt.append("- Lỗi ít và không systematic\n");
        prompt.append("- **QUAN TRỌNG**: Error-free sentences FREQUENT (không phải tất cả câu)\n\n");
        
        prompt.append("**Band 8.0+ (Very Good User):**\n");
        prompt.append("- Majority of sentences error-free (cho phép 2-4 lỗi nhỏ trong toàn bài)\n");
        prompt.append("- Wide range of vocabulary với skilful use\n");
        prompt.append("- Ideas well-extended và well-supported\n");
        prompt.append("- **QUAN TRỌNG**: 'Occasional errors' = VẪN CÓ THỂ ĐẠT BAND 8.0!\n\n");
        
        // Word count context
        int minWords = taskNumber == 1 ? TASK_1_MIN_WORDS : TASK_2_MIN_WORDS;
        prompt.append("## 📝 THÔNG TIN SỐ TỪ\n");
        prompt.append("- **Số từ đã nộp**: ").append(wordCount).append(" từ\n");
        prompt.append("- **Yêu cầu tối thiểu**: ").append(minWords).append(" từ\n");
        if (wordCount < minWords) {
            int deficit = minWords - wordCount;
            prompt.append("- **CHÚ Ý**: Bài viết THIẾU ").append(deficit).append(" từ. ");
            prompt.append("Điều này ảnh hưởng Task Achievement/Response, nhưng các tiêu chí khác vẫn chấm theo năng lực thể hiện.\n");
        } else {
            prompt.append("- Đã đạt yêu cầu số từ ✓\n");
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
     * Get official IELTS Task 1 band descriptors (bands 5-9) with full detail.
     * Based on official IELTS.org band descriptors (May 2023).
     * ENHANCED with calibration notes for fair scoring.
     */
    private String getTask1BandDescriptors() {
        StringBuilder desc = new StringBuilder();
        desc.append("## TIÊU CHÍ CHẤM ĐIỂM CHÍNH THỨC - IELTS WRITING TASK 1\n\n");
        
        // Band 9
        desc.append("### Band 9 (Expert User) - RẤT HIẾM\n");
        desc.append("- **Task Achievement**: Tất cả yêu cầu được đáp ứng đầy đủ. Có thể có sơ suất cực kỳ hiếm về nội dung.\n");
        desc.append("- **Coherence & Cohesion**: Thông điệp được theo dõi effortlessly. Cohesion rất hiếm khi thu hút sự chú ý. Paragraphing skilfully managed.\n");
        desc.append("- **Lexical Resource**: Full flexibility và precise use. Wide range với very natural và sophisticated control. Errors extremely rare.\n");
        desc.append("- **Grammar**: Wide range với full flexibility và control. Errors extremely rare.\n\n");
        
        // Band 8
        desc.append("### Band 8 (Very Good User)\n");
        desc.append("- **Task Achievement**: Đáp ứng tất cả yêu cầu appropriately, relevantly và sufficiently. Key features được chọn lọc khéo léo và trình bày rõ ràng. Occasional omissions OK.\n");
        desc.append("- **Coherence & Cohesion**: Message followed with ease. Information logically sequenced. Cohesion well managed. Occasional lapses OK.\n");
        desc.append("- **Lexical Resource**: Wide resource used fluently và flexibly. Skilful use of uncommon/idiomatic items. Occasional errors minimal impact.\n");
        desc.append("- **Grammar**: Wide range used flexibly và accurately. **MAJORITY of sentences error-free**. Occasional non-systematic errors.\n");
        desc.append("- **📌 CALIBRATION**: 'Occasional' = 2-4 lỗi trong toàn bài, KHÔNG phải mỗi đoạn!\n\n");
        
        // Band 7
        desc.append("### Band 7 (Good User)\n");
        desc.append("- **Task Achievement**: Đáp ứng các yêu cầu. Nội dung relevant và accurate với vài omissions. Có CLEAR OVERVIEW, data được categorised phù hợp.\n");
        desc.append("- **Coherence & Cohesion**: Information logically organised với clear progression. A few lapses OK. Cohesive devices used flexibly.\n");
        desc.append("- **Lexical Resource**: Sufficient flexibility và precision. Ability to use less common items. Few errors in spelling/word form.\n");
        desc.append("- **Grammar**: Variety of complex structures với some flexibility. Generally well controlled. **Error-free sentences FREQUENT**.\n");
        desc.append("- **📌 CALIBRATION**: Bài có overview tốt, cover key features, 5-7 lỗi nhỏ → xứng đáng Band 7.0-7.5\n\n");
        
        // Band 6
        desc.append("### Band 6 (Competent User) - PHỔ BIẾN NHẤT\n");
        desc.append("- **Task Achievement**: Tập trung vào requirements với appropriate format. Key features covered adequately. Có thể có vài chi tiết irrelevant/inaccurate.\n");
        desc.append("- **Coherence & Cohesion**: Generally arranged coherently với clear overall progression. Some cohesion may be faulty/mechanical.\n");
        desc.append("- **Lexical Resource**: Generally adequate cho task. Meaning generally clear dù restricted range.\n");
        desc.append("- **Grammar**: Mix of simple và complex forms nhưng limited flexibility. Errors **RARELY impede communication**.\n");
        desc.append("- **📌 CALIBRATION**: Đây là band phổ biến. Bài có lỗi nhưng vẫn đọc hiểu được = Band 6.0-6.5\n\n");
        
        // Band 5
        desc.append("### Band 5 (Modest User)\n");
        desc.append("- **Task Achievement**: Generally addresses requirements. Key features not adequately covered. May focus quá nhiều vào details.\n");
        desc.append("- **Coherence & Cohesion**: Organisation evident nhưng không wholly logical. Sentences không fluently linked.\n");
        desc.append("- **Lexical Resource**: Limited nhưng minimally adequate. Simple vocabulary, frequent lapses in appropriacy.\n");
        desc.append("- **Grammar**: Limited và repetitive structures. Complex sentences thường faulty.\n");
        desc.append("- **📌 CALIBRATION**: Bài thiếu overview, không cover key features đủ, nhiều lỗi → Band 5.0\n\n");
        
        return desc.toString();
    }

    /**
     * Get official IELTS Task 2 band descriptors (bands 5-9) with full detail.
     * Based on official IELTS.org band descriptors (May 2023).
     * ENHANCED with calibration notes for fair scoring.
     */
    private String getTask2BandDescriptors() {
        StringBuilder desc = new StringBuilder();
        desc.append("## TIÊU CHÍ CHẤM ĐIỂM CHÍNH THỨC - IELTS WRITING TASK 2\n\n");
        
        // Special note about off-topic essays
        desc.append("### ⚠️ XỬ LÝ BÀI LẠC ĐỀ (OFF-TOPIC):\n");
        desc.append("Nếu bài viết KHÔNG trả lời đúng câu hỏi đề bài:\n");
        desc.append("- **Task Response**: Bị ảnh hưởng nặng → giảm xuống Band 4.0-5.0\n");
        desc.append("- **Coherence & Cohesion**: Chấm BÌNH THƯỜNG theo cấu trúc bài (có intro, body, conclusion? Có linking words?)\n");
        desc.append("- **Lexical Resource**: Chấm BÌNH THƯỜNG theo từ vựng sử dụng (có variety? Có advanced vocab?)\n");
        desc.append("- **Grammar**: Chấm BÌNH THƯỜNG theo grammar (có complex sentences? Errors có impede meaning?)\n");
        desc.append("- **VÍ DỤ**: Bài lạc đề với ngôn ngữ tốt có thể đạt: TR=4.5, CC=6.5, LR=6.5, GRA=6.5 → Overall=6.0\n\n");
        
        // Band 9
        desc.append("### Band 9 (Expert User) - RẤT HIẾM\n");
        desc.append("- **Task Response**: Prompt được addressed và explored sâu sắc. Position rõ ràng, fully developed. Ideas relevant, fully extended và well supported.\n");
        desc.append("- **Coherence & Cohesion**: Message followed effortlessly. Cohesion barely attracts attention. Paragraphing skilfully managed.\n");
        desc.append("- **Lexical Resource**: Full flexibility và precise use. Very natural và sophisticated control. Errors extremely rare.\n");
        desc.append("- **Grammar**: Wide range với full flexibility và control. Errors extremely rare.\n\n");
        
        // Band 8
        desc.append("### Band 8 (Very Good User)\n");
        desc.append("- **Task Response**: Prompt addressed appropriately và sufficiently. Position clear và well-developed. Ideas relevant, well extended và supported.\n");
        desc.append("- **Coherence & Cohesion**: Message followed with ease. Information logically sequenced. Cohesion well managed. Occasional lapses OK.\n");
        desc.append("- **Lexical Resource**: Wide resource used fluently và flexibly. Skilful use of uncommon/idiomatic items. Occasional inaccuracies minimal impact.\n");
        desc.append("- **Grammar**: Wide range used flexibly và accurately. **MAJORITY of sentences error-free**. Occasional non-systematic errors OK.\n");
        desc.append("- **📌 CALIBRATION**: 'Occasional' = 2-4 lỗi rải rác trong TOÀN BÀI. Bài có vài lỗi nhỏ VẪN có thể đạt Band 8!\n\n");
        
        // Band 7
        desc.append("### Band 7 (Good User)\n");
        desc.append("- **Task Response**: Main parts addressed appropriately. Position clear và developed. May have tendency to over-generalise/lack focus.\n");
        desc.append("- **Coherence & Cohesion**: Information logically organised với clear progression. A few minor lapses OK. Paragraphing effective.\n");
        desc.append("- **Lexical Resource**: Sufficient flexibility và precision. Ability to use less common items. Few errors in spelling/word form.\n");
        desc.append("- **Grammar**: Variety of complex structures với some flexibility và accuracy. **Error-free sentences FREQUENT**. Few errors don't impede.\n");
        desc.append("- **📌 CALIBRATION**: Bài có khoảng 5-8 lỗi nhỏ rải rác, cấu trúc đa dạng → xứng đáng Band 7.0\n\n");
        
        // Band 6
        desc.append("### Band 6 (Competent User) - PHỔ BIẾN NHẤT\n");
        desc.append("- **Task Response**: Main parts addressed (có thể không đều). Position relevant nhưng conclusions may be unclear/repetitive.\n");
        desc.append("- **Coherence & Cohesion**: Generally coherent với clear overall progression. Some faulty/mechanical cohesion OK.\n");
        desc.append("- **Lexical Resource**: Generally adequate. Meaning generally clear dù restricted range. Some errors in spelling/word form.\n");
        desc.append("- **Grammar**: Mix of simple và complex nhưng limited flexibility. Errors **RARELY impede communication**.\n");
        desc.append("- **📌 CALIBRATION**: Đây là band của sinh viên đại học Việt Nam viết khá. Có lỗi nhưng vẫn đọc hiểu được = Band 6.0-6.5\n\n");
        
        // Band 5
        desc.append("### Band 5 (Modest User)\n");
        desc.append("- **Task Response**: Main parts incompletely addressed. Position expressed nhưng development không always clear.\n");
        desc.append("- **Coherence & Cohesion**: Organisation evident nhưng không wholly logical. Paragraphing may be inadequate.\n");
        desc.append("- **Lexical Resource**: Limited nhưng minimally adequate. Frequent lapses in appropriacy.\n");
        desc.append("- **Grammar**: Limited, repetitive structures. Complex sentences tend to be faulty.\n");
        desc.append("- **📌 CALIBRATION**: Bài có nhiều lỗi GÂY KHÓ HIỂU, hoặc ý tưởng rất hạn chế → Band 5.0\n\n");
        
        return desc.toString();
    }

    /**
     * Build the user prompt with task details and essay.
     * Enhanced with Vietnamese code-switching and detailed feedback schema.
     */
    private String buildUserPrompt(Integer taskNumber, String taskPrompt, String essay) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("## Đề bài (Task Prompt):\n");
        prompt.append(taskPrompt).append("\n\n");
        
        prompt.append("## Bài viết của thí sinh:\n");
        prompt.append("```\n").append(essay).append("\n```\n\n");
        
        prompt.append("## YÊU CẦU ĐỊNH DẠNG RESPONSE\n");
        prompt.append("Bạn PHẢI trả về một JSON object hợp lệ với cấu trúc chính xác như sau. ");
        prompt.append("KHÔNG thêm bất kỳ text nào ngoài JSON. KHÔNG dùng markdown code fences.\n\n");
        
        prompt.append("### NGÔN NGỮ OUTPUT:\n");
        prompt.append("- Tất cả feedback, explanation, comments PHẢI viết bằng **tiếng Việt**\n");
        prompt.append("- Có thể code-switch với thuật ngữ tiếng Anh khi cần (ví dụ: 'coherence', 'collocation', 'topic sentence')\n");
        prompt.append("- Sample essays vẫn viết bằng tiếng Anh (vì đây là bài IELTS)\n\n");
        
        prompt.append("{\n");
        prompt.append("  \"band_scores\": {\n");
        if (taskNumber == 1) {
            prompt.append("    \"task_achievement\": <số từ 3.0-9.0, dùng bước 0.5>,\n");
        } else {
            prompt.append("    \"task_response\": <số từ 3.0-9.0, dùng bước 0.5>,\n");
        }
        prompt.append("    \"coherence_cohesion\": <số từ 3.0-9.0, dùng bước 0.5>,\n");
        prompt.append("    \"lexical_resource\": <số từ 3.0-9.0, dùng bước 0.5>,\n");
        prompt.append("    \"grammatical_range_accuracy\": <số từ 3.0-9.0, dùng bước 0.5>\n");
        prompt.append("  },\n");
        prompt.append("  \"overall_band\": <trung bình 4 tiêu chí, làm tròn đến 0.5 gần nhất>,\n\n");
        
        // Enhanced sentence corrections with severity
        prompt.append("  \"sentence_corrections\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"original\": \"<câu gốc chính xác từ bài viết - copy nguyên văn>\",\n");
        prompt.append("      \"corrected\": \"<câu đã sửa>\",\n");
        prompt.append("      \"error_type\": \"<grammar|spelling|vocabulary|punctuation|coherence|style>\",\n");
        prompt.append("      \"severity\": \"<major|minor>\",\n");
        prompt.append("      \"explanation\": \"<giải thích bằng tiếng Việt, ví dụ: 'Thiếu article 'the' trước danh từ xác định'>\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n\n");
        
        // Enhanced paragraph rewrites
        prompt.append("  \"paragraph_rewrites\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"paragraph_index\": <index 0-based>,\n");
        prompt.append("      \"original\": \"<đoạn văn gốc - copy nguyên văn cả đoạn>\",\n");
        prompt.append("      \"improved\": \"<đoạn văn cải thiện ở mức band+1>\",\n");
        prompt.append("      \"improvements_made\": [\"<cải thiện 1 bằng tiếng Việt>\", \"<cải thiện 2>\"]\n");
        prompt.append("    }\n");
        prompt.append("  ],\n\n");
        
        // NEW: Highlighted vocabulary with position info
        prompt.append("  \"vocabulary_highlights\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"word\": \"<từ/cụm từ đáng chú ý trong bài>\",\n");
        prompt.append("      \"category\": \"<advanced_good|collocation_good|academic|error|awkward>\",\n");
        prompt.append("      \"note\": \"<nhận xét ngắn bằng tiếng Việt, ví dụ: 'Dùng collocation tốt' hoặc 'Sai word form'>\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n\n");
        
        // NEW: Error severity summary
        prompt.append("  \"error_analysis\": {\n");
        prompt.append("    \"major_errors\": <số lỗi lớn ảnh hưởng nghĩa>,\n");
        prompt.append("    \"minor_errors\": <số lỗi nhỏ không ảnh hưởng nghĩa>,\n");
        prompt.append("    \"summary\": \"<tóm tắt bằng tiếng Việt, ví dụ: 'Hầu hết lỗi là minor errors không ảnh hưởng communication'>\"\n");
        prompt.append("  },\n\n");
        
        prompt.append("  \"sample_essay_band_plus_one\": \"<bài viết hoàn chỉnh ở mức band+1, viết bằng tiếng Anh>\",\n");
        prompt.append("  \"sample_essay_band_9\": \"<bài mẫu band 9 cho đề này, viết bằng tiếng Anh>\",\n\n");
        
        prompt.append("  \"feedback_summary\": {\n");
        prompt.append("    \"strengths\": [\"<điểm mạnh 1 - tiếng Việt>\", \"<điểm mạnh 2>\", \"<điểm mạnh 3>\"],\n");
        prompt.append("    \"weaknesses\": [\"<điểm yếu 1 - tiếng Việt>\", \"<điểm yếu 2>\"],\n");
        prompt.append("    \"writing_approach\": \"<gợi ý cách tiếp cận bài viết - tiếng Việt, 2-3 câu>\",\n");
        prompt.append("    \"improvement_tips\": \"<tips cải thiện cụ thể - tiếng Việt, 2-3 câu>\"\n");
        prompt.append("  },\n\n");
        
        prompt.append("  \"word_analysis\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"word\": \"<từ/cụm từ nổi bật>\",\n");
        prompt.append("      \"definition\": \"<định nghĩa tiếng Việt>\",\n");
        prompt.append("      \"context\": \"<câu chứa từ đó trong bài>\",\n");
        prompt.append("      \"usage_quality\": \"<good|acceptable|incorrect>\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n\n");
        
        prompt.append("  \"criteria_comments\": {\n");
        if (taskNumber == 1) {
            prompt.append("    \"task_achievement\": \"<2-3 câu giải thích điểm Task Achievement - tiếng Việt, có thể dùng thuật ngữ tiếng Anh>\",\n");
        } else {
            prompt.append("    \"task_achievement\": \"<2-3 câu giải thích điểm Task Response - tiếng Việt, có thể dùng thuật ngữ tiếng Anh>\",\n");
        }
        prompt.append("    \"coherence_cohesion\": \"<2-3 câu giải thích điểm Coherence & Cohesion - tiếng Việt>\",\n");
        prompt.append("    \"lexical_resource\": \"<2-3 câu giải thích điểm Lexical Resource - tiếng Việt>\",\n");
        prompt.append("    \"grammatical_range\": \"<2-3 câu giải thích điểm Grammatical Range & Accuracy - tiếng Việt>\"\n");
        prompt.append("  }\n");
        prompt.append("}\n\n");
        
        prompt.append("## HƯỚNG DẪN CHẤM ĐIỂM - ĐÃ HIỆU CHUẨN\n\n");
        
        prompt.append("### ⚠️ CẢNH BÁO: TRÁNH CHẤM QUÁ KHẮT KHE\n");
        prompt.append("Hệ thống AI thường có xu hướng chấm khắt khe hơn giám khảo thực. ");
        prompt.append("Hãy nhớ các nguyên tắc sau:\n\n");
        
        prompt.append("### Về điểm số:\n");
        prompt.append("1. **ĐỌC TOÀN BỘ BÀI** trước khi cho điểm - đừng vội kết luận từ vài câu đầu\n");
        prompt.append("2. **TÁCH BIỆT 4 TIÊU CHÍ**: Mỗi tiêu chí được chấm độc lập\n");
        prompt.append("   - Bài lạc đề → Task Response thấp, nhưng CC/LR/GRA có thể vẫn cao\n");
        prompt.append("   - Bài nhiều lỗi grammar → GRA thấp, nhưng TR/CC/LR có thể vẫn cao\n");
        prompt.append("3. **NHỚ**: Band 6.0-6.5 là band PHỔ BIẾN nhất - đừng ngại cho điểm này\n");
        prompt.append("4. **NHỚ**: Band 7.0+ cho bài viết có error-free sentences FREQUENT (không phải tất cả)\n");
        prompt.append("5. **NHỚ**: Band 8.0 vẫn cho phép 'occasional errors' - 2-4 lỗi nhỏ rải rác\n");
        prompt.append("6. **QUAN TRỌNG**: Khi phân vân giữa band X và band X+0.5 → CHỌN BAND CAO HƠN\n\n");
        
        prompt.append("### Ví dụ calibration thực tế:\n");
        prompt.append("| Tình huống | Điểm đúng | Điểm sai (quá khắt khe) |\n");
        prompt.append("|------------|-----------|------------------------|\n");
        prompt.append("| Bài lạc đề nhưng viết mạch lạc, grammar OK | 5.5-6.0 | 4.0-5.0 |\n");
        prompt.append("| Bài đúng đề, có 5-7 lỗi grammar nhỏ | 6.5-7.0 | 5.5-6.0 |\n");
        prompt.append("| Bài tốt, từ vựng đa dạng, 2-3 lỗi nhỏ | 7.5-8.0 | 6.5-7.0 |\n");
        prompt.append("| Data coverage tốt, có overview, vài lỗi nhỏ | 7.0-7.5 | 6.0-6.5 |\n\n");
        
        prompt.append("### Về nội dung feedback:\n");
        prompt.append("7. Cung cấp ít nhất 3-5 sentence corrections với giải thích rõ ràng\n");
        prompt.append("8. Viết lại ít nhất introduction và 1 body paragraph\n");
        prompt.append("9. Sample essays phải realistic và relevant với đề bài cụ thể\n");
        prompt.append("10. **TẤT CẢ feedback phải bằng tiếng Việt**, có thể code-switch thuật ngữ tiếng Anh\n");
        prompt.append("11. Highlight ít nhất 5-8 từ/cụm từ đáng chú ý (cả tốt và cần sửa)\n");
        prompt.append("12. **Khuyến khích thí sinh** - nêu điểm mạnh trước điểm yếu\n\n");
        
        prompt.append("### Lưu ý cuối:\n");
        prompt.append("- Chỉ trả về JSON object, không có markdown fences hay text thừa\n");
        prompt.append("- Đảm bảo JSON hợp lệ, escape đúng các ký tự đặc biệt\n");
        
        return prompt.toString();
    }

    /**
     * Call Gemini API with multimodal support (text + image for Task 1).
     */
    private String callGeminiApiWithImage(Integer taskNumber, String taskPrompt, String essay, 
                                           int wordCount, String imageUrl, String apiKey, String model) {
        String url = GEMINI_API_BASE_URL + model + ":generateContent?key=" + apiKey;
        
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
        // Using slightly higher temperature for more generous scoring
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.4); // Slightly higher for more generous, less rigid scoring
        generationConfig.put("topP", 0.92);
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
     * Includes calibration adjustment to counteract AI's tendency to score too harshly.
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
        
        // Extract band scores and apply calibration adjustment
        JsonNode bandScoresNode = gradingResult.path("band_scores");
        Map<String, Object> rawBandScores = objectMapper.convertValue(bandScoresNode, Map.class);
        
        // Apply calibration adjustment to each criterion
        // AI tends to score 0.5-1.0 band lower than human examiners
        Map<String, Object> calibratedBandScores = new HashMap<>();
        for (Map.Entry<String, Object> entry : rawBandScores.entrySet()) {
            double rawScore = ((Number) entry.getValue()).doubleValue();
            double calibratedScore = applyCriteriaCalibratedAdjustment(rawScore);
            calibratedBandScores.put(entry.getKey(), calibratedScore);
        }
        submission.setBandScores(calibratedBandScores);
        
        // Calculate and set overall band (rounded to nearest 0.5)
        // Use calibrated scores for overall calculation
        double calibratedOverall = calibratedBandScores.values().stream()
            .mapToDouble(v -> ((Number) v).doubleValue())
            .average()
            .orElse(0.0);
        BigDecimal overallBand = roundToNearestHalf(calibratedOverall);
        submission.setOverallBand(overallBand);
        
        logger.info("Score calibration applied: raw overall={}, calibrated overall={}", 
            gradingResult.path("overall_band").asDouble(), overallBand);
        
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
        
        // NEW: Vocabulary highlights for essay annotation
        if (gradingResult.has("vocabulary_highlights")) {
            aiFeedback.put("vocabulary_highlights", 
                objectMapper.convertValue(gradingResult.path("vocabulary_highlights"), List.class));
        }
        
        // NEW: Error analysis summary
        if (gradingResult.has("error_analysis")) {
            aiFeedback.put("error_analysis", 
                objectMapper.convertValue(gradingResult.path("error_analysis"), Map.class));
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
     * Apply calibration adjustment to individual criterion scores.
     * Based on empirical observation that AI grades approximately 0.5 band lower than human examiners.
     * 
     * Adjustment curve:
     * - Scores 4.0-5.5: Add 0.5 (these are often underscored significantly)
     * - Scores 6.0-6.5: Add 0.5 (most common band, often underscored)
     * - Scores 7.0-7.5: Add 0.5 (still commonly underscored)
     * - Scores 8.0+: Add 0.0 (high scores are usually accurate)
     * 
     * Maximum score after adjustment is 9.0
     */
    private double applyCriteriaCalibratedAdjustment(double rawScore) {
        double adjustment;
        
        if (rawScore < 4.0) {
            // Very low scores - minimal adjustment
            adjustment = 0.0;
        } else if (rawScore < 8.0) {
            // Scores 4.0-7.5: Apply 0.5 band uplift
            adjustment = 0.5;
        } else {
            // Scores 8.0+: No adjustment needed (high scores are usually accurate)
            adjustment = 0.0;
        }
        
        double adjusted = rawScore + adjustment;
        
        // Cap at 9.0 and ensure rounded to 0.5
        adjusted = Math.min(9.0, adjusted);
        adjusted = Math.round(adjusted * 2) / 2.0;
        
        return adjusted;
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
        return validateApiKey(apiKey, DEFAULT_GEMINI_MODEL);
    }
    
    /**
     * Validate API key by making a simple test request with specified model.
     */
    public boolean validateApiKey(String apiKey, String model) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }
        
        String selectedModel = (model != null && !model.trim().isEmpty()) ? model : DEFAULT_GEMINI_MODEL;
        
        try {
            String url = GEMINI_API_BASE_URL + selectedModel + ":generateContent?key=" + apiKey;
            
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
