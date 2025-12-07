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
     * Enhanced with calibration anchors and generous scoring philosophy.
     */
    private String buildSystemPrompt(Integer taskNumber, int wordCount) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("# HỆ THỐNG CHẤM ĐIỂM IELTS WRITING\n\n");
        prompt.append("Bạn là một giám khảo IELTS được chứng nhận với hơn 15 năm kinh nghiệm. ");
        prompt.append("Nhiệm vụ của bạn là chấm điểm bài viết IELTS một cách chính xác và công bằng theo tiêu chí band descriptors chính thức của IELTS.\n\n");
        
        // CRITICAL: Calibration-focused grading philosophy
        prompt.append("## TRIẾT LÝ CHẤM ĐIỂM (RẤT QUAN TRỌNG)\n\n");
        prompt.append("### Nguyên tắc cốt lõi:\n");
        prompt.append("1. **Chấm điểm DỰA TRÊN NHỮNG GÌ THÍ SINH THỂ HIỆN ĐƯỢC**, không phải những gì thiếu sót\n");
        prompt.append("2. **Lỗi nhỏ (minor errors)** = lỗi KHÔNG ảnh hưởng đến việc hiểu ý → KHÔNG trừ điểm nặng\n");
        prompt.append("3. **Lỗi lớn (major errors)** = lỗi GÂY HIỂU SAI hoặc không hiểu được → mới ảnh hưởng band score\n");
        prompt.append("4. **Khi phân vân giữa 2 band, CHỌN BAND CAO HƠN** cho thí sinh\n");
        prompt.append("5. Nhớ rằng thí sinh viết trong điều kiện thi có giới hạn thời gian\n\n");
        
        prompt.append("### Mốc calibration quan trọng:\n");
        prompt.append("| Đối tượng | Band thường đạt |\n");
        prompt.append("|-----------|----------------|\n");
        prompt.append("| Sinh viên đại học Việt Nam viết tốt | 6.0 - 6.5 |\n");
        prompt.append("| Người đi làm có tiếng Anh khá | 6.5 - 7.0 |\n");
        prompt.append("| Giáo viên tiếng Anh / du học sinh | 7.0 - 7.5 |\n");
        prompt.append("| Người gần như native speaker | 8.0 - 8.5 |\n");
        prompt.append("| Bài mẫu có thể in trong sách IELTS chính thức | 9.0 |\n\n");
        
        prompt.append("### Về Band 8.0 - 9.0:\n");
        prompt.append("- **Band 8.0**: Bài viết xuất sắc với \"occasional errors\" - có thể có 2-4 lỗi nhỏ rải rác\n");
        prompt.append("- **Band 8.5**: Gần như hoàn hảo, chỉ có 1-2 lỗi rất nhỏ\n");
        prompt.append("- **Band 9.0**: Hiếm gặp, bài viết như native speaker viết, hầu như không có lỗi\n");
        prompt.append("- **QUAN TRỌNG**: Nếu bài viết có ý tưởng hay, cấu trúc tốt, từ vựng đa dạng với chỉ vài lỗi nhỏ → XỨng đáng Band 8.0+\n\n");
        
        prompt.append("### Phân loại lỗi:\n");
        prompt.append("**Lỗi NHỎ (minor) - KHÔNG trừ nặng:**\n");
        prompt.append("- Thiếu/thừa article (a, an, the) nhưng vẫn hiểu được\n");
        prompt.append("- Lỗi số ít/số nhiều không gây hiểu lầm\n");
        prompt.append("- Lỗi chính tả nhỏ (1-2 chữ cái)\n");
        prompt.append("- Dấu phẩy không hoàn hảo\n");
        prompt.append("- Collocation hơi awkward nhưng vẫn tự nhiên\n\n");
        
        prompt.append("**Lỗi LỚN (major) - CÓ ẢNH HƯỞNG band score:**\n");
        prompt.append("- Câu không có nghĩa, không hiểu được\n");
        prompt.append("- Sai thì động từ gây hiểu sai timeline\n");
        prompt.append("- Dùng từ sai hoàn toàn meaning\n");
        prompt.append("- Cấu trúc câu khiến người đọc phải đọc lại nhiều lần\n");
        prompt.append("- Thiếu coherence khiến không theo được logic\n\n");
        
        // Word count context
        int minWords = taskNumber == 1 ? TASK_1_MIN_WORDS : TASK_2_MIN_WORDS;
        prompt.append("## Thông tin số từ\n");
        prompt.append("- **Số từ đã nộp**: ").append(wordCount).append(" từ\n");
        prompt.append("- **Yêu cầu tối thiểu**: ").append(minWords).append(" từ\n");
        if (wordCount < minWords) {
            int deficit = minWords - wordCount;
            prompt.append("- **CHÚ Ý**: Bài viết THIẾU ").append(deficit).append(" từ so với yêu cầu. ");
            prompt.append("Điều này ảnh hưởng Task Achievement/Response, nhưng các tiêu chí khác vẫn chấm công bằng.\n");
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
     */
    private String getTask1BandDescriptors() {
        StringBuilder desc = new StringBuilder();
        desc.append("## TIÊU CHÍ CHẤM ĐIỂM CHÍNH THỨC - IELTS WRITING TASK 1\n\n");
        
        // Band 9
        desc.append("### Band 9 (Expert User)\n");
        desc.append("- **Task Achievement**: Tất cả yêu cầu được đáp ứng đầy đủ và phù hợp. Có thể có những sơ suất cực kỳ hiếm về nội dung.\n");
        desc.append("- **Coherence & Cohesion**: Thông điệp được theo dõi một cách dễ dàng tuyệt đối. Cohesion được sử dụng rất hiếm khi thu hút sự chú ý. Paragraphing được quản lý khéo léo.\n");
        desc.append("- **Lexical Resource**: Linh hoạt hoàn toàn và chính xác. Phạm vi từ vựng rộng được sử dụng chính xác với sự kiểm soát rất tự nhiên và tinh tế. Lỗi chính tả cực kỳ hiếm.\n");
        desc.append("- **Grammar**: Phạm vi cấu trúc rộng với sự linh hoạt và kiểm soát hoàn toàn. Lỗi cực kỳ hiếm và ảnh hưởng tối thiểu.\n\n");
        
        // Band 8
        desc.append("### Band 8 (Very Good User)\n");
        desc.append("- **Task Achievement**: Đáp ứng tất cả yêu cầu một cách phù hợp, liên quan và đầy đủ. Key features được chọn lọc khéo léo và trình bày rõ ràng. Có thể có sơ suất occasional.\n");
        desc.append("- **Coherence & Cohesion**: Thông điệp được theo dõi dễ dàng. Thông tin được sắp xếp logic. Cohesion được quản lý tốt. Occasional lapses có thể xảy ra.\n");
        desc.append("- **Lexical Resource**: Nguồn từ vựng rộng được sử dụng linh hoạt và trôi chảy. Sử dụng khéo léo từ uncommon/idiomatic. Occasional errors có ảnh hưởng tối thiểu.\n");
        desc.append("- **Grammar**: Phạm vi cấu trúc rộng được sử dụng linh hoạt và chính xác. **Đa số câu không có lỗi**. Occasional non-systematic errors.\n");
        desc.append("- **LƯU Ý**: 'Occasional' = 2-4 lỗi trong toàn bài, không phải mỗi đoạn!\n\n");
        
        // Band 7
        desc.append("### Band 7 (Good User)\n");
        desc.append("- **Task Achievement**: Đáp ứng các yêu cầu. Nội dung liên quan và chính xác với vài thiếu sót. Có overview rõ ràng, data được phân loại phù hợp.\n");
        desc.append("- **Coherence & Cohesion**: Thông tin được tổ chức logic với progression rõ ràng. Một vài lapses có thể xảy ra. Cohesive devices được sử dụng linh hoạt.\n");
        desc.append("- **Lexical Resource**: Đủ linh hoạt và chính xác. Có khả năng sử dụng less common items. Chỉ có vài lỗi spelling/word form.\n");
        desc.append("- **Grammar**: Đa dạng cấu trúc phức tạp với sự linh hoạt. Generally well controlled. **Error-free sentences thường xuyên xuất hiện**.\n\n");
        
        // Band 6
        desc.append("### Band 6 (Competent User)\n");
        desc.append("- **Task Achievement**: Tập trung vào yêu cầu với format phù hợp. Key features được cover adequately. Có thể có vài chi tiết irrelevant hoặc inaccurate.\n");
        desc.append("- **Coherence & Cohesion**: Generally arranged coherently với overall progression rõ ràng. Một số cohesion có thể faulty hoặc mechanical.\n");
        desc.append("- **Lexical Resource**: Generally adequate cho task. Meaning generally clear dù có restricted range hoặc thiếu precision.\n");
        desc.append("- **Grammar**: Mix of simple and complex forms nhưng flexibility hạn chế. Errors **rarely impede communication**.\n\n");
        
        // Band 5
        desc.append("### Band 5 (Modest User)\n");
        desc.append("- **Task Achievement**: Generally addresses yêu cầu. Key features not adequately covered. Có thể focus quá nhiều vào details.\n");
        desc.append("- **Coherence & Cohesion**: Organisation evident nhưng không wholly logical. Sentences không fluently linked.\n");
        desc.append("- **Lexical Resource**: Limited nhưng minimally adequate. Simple vocabulary, frequent lapses in appropriacy.\n");
        desc.append("- **Grammar**: Limited và repetitive structures. Complex sentences thường faulty.\n\n");
        
        return desc.toString();
    }

    /**
     * Get official IELTS Task 2 band descriptors (bands 5-9) with full detail.
     * Based on official IELTS.org band descriptors (May 2023).
     */
    private String getTask2BandDescriptors() {
        StringBuilder desc = new StringBuilder();
        desc.append("## TIÊU CHÍ CHẤM ĐIỂM CHÍNH THỨC - IELTS WRITING TASK 2\n\n");
        
        // Band 9
        desc.append("### Band 9 (Expert User)\n");
        desc.append("- **Task Response**: Prompt được addressed và explored một cách sâu sắc. Vị trí (position) rõ ràng và phát triển đầy đủ. Ideas được extended và supported tốt.\n");
        desc.append("- **Coherence & Cohesion**: Thông điệp được theo dõi dễ dàng tuyệt đối. Cohesion rất hiếm khi thu hút sự chú ý. Paragraphing khéo léo.\n");
        desc.append("- **Lexical Resource**: Linh hoạt hoàn toàn và chính xác. Kiểm soát rất tự nhiên và tinh tế. Lỗi cực kỳ hiếm.\n");
        desc.append("- **Grammar**: Phạm vi rộng với sự linh hoạt và kiểm soát hoàn toàn. Lỗi cực kỳ hiếm.\n\n");
        
        // Band 8
        desc.append("### Band 8 (Very Good User)\n");
        desc.append("- **Task Response**: Prompt được addressed đầy đủ và phù hợp. Vị trí rõ ràng, well-developed. Ideas relevant, well extended và supported. Occasional omissions có thể có.\n");
        desc.append("- **Coherence & Cohesion**: Thông điệp dễ theo dõi. Thông tin sắp xếp logic. Cohesion managed tốt. Occasional lapses.\n");
        desc.append("- **Lexical Resource**: Wide resource sử dụng fluently và flexibly. Skilful use of uncommon/idiomatic items. Occasional inaccuracies có minimal impact.\n");
        desc.append("- **Grammar**: Wide range sử dụng flexibly và accurately. **Majority of sentences error-free**. Occasional non-systematic errors.\n");
        desc.append("- **LƯU Ý QUAN TRỌNG**: Band 8 cho phép 'occasional errors' - tức là 2-4 lỗi rải rác trong bài, KHÔNG phải bài hoàn hảo!\n\n");
        
        // Band 7
        desc.append("### Band 7 (Good User)\n");
        desc.append("- **Task Response**: Main parts được addressed phù hợp. Vị trí rõ ràng và developed. Có thể có tendency to over-generalise hoặc thiếu focus.\n");
        desc.append("- **Coherence & Cohesion**: Thông tin tổ chức logic với clear progression. A few lapses (minor). Paragraphing hiệu quả.\n");
        desc.append("- **Lexical Resource**: Đủ flexibility và precision. Có khả năng dùng less common items. Few spelling/word form errors.\n");
        desc.append("- **Grammar**: Variety of complex structures với some flexibility và accuracy. **Error-free sentences frequent**. Few errors persist but don't impede.\n\n");
        
        // Band 6
        desc.append("### Band 6 (Competent User)\n");
        desc.append("- **Task Response**: Main parts addressed (có thể không đều). Vị trí relevant nhưng conclusions có thể unclear hoặc repetitive. Some ideas insufficiently developed.\n");
        desc.append("- **Coherence & Cohesion**: Generally coherent với clear progression. Some faulty/mechanical cohesion. Paragraphing có thể không always logical.\n");
        desc.append("- **Lexical Resource**: Generally adequate. Meaning generally clear dù restricted range. Some errors in spelling/word form.\n");
        desc.append("- **Grammar**: Mix of simple và complex nhưng limited flexibility. Errors **rarely impede communication**.\n\n");
        
        // Band 5
        desc.append("### Band 5 (Modest User)\n");
        desc.append("- **Task Response**: Main parts incompletely addressed. Position expressed nhưng development không always clear. Limited ideas, có thể irrelevant detail.\n");
        desc.append("- **Coherence & Cohesion**: Organisation evident nhưng không wholly logical. Paragraphing có thể inadequate.\n");
        desc.append("- **Lexical Resource**: Limited nhưng minimally adequate. Frequent lapses in appropriacy.\n");
        desc.append("- **Grammar**: Limited, repetitive structures. Complex sentences tend to be faulty.\n\n");
        
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
        
        prompt.append("## HƯỚNG DẪN CHẤM ĐIỂM\n\n");
        
        prompt.append("### Về điểm số:\n");
        prompt.append("1. Đọc kỹ bài viết và đánh giá theo từng tiêu chí\n");
        prompt.append("2. **NHỚ**: Band 8 cho phép 'occasional errors' (2-4 lỗi nhỏ rải rác)\n");
        prompt.append("3. **NHỚ**: Nếu bài có ý tưởng hay, cấu trúc tốt, chỉ vài lỗi nhỏ → xứng đáng Band 7.5-8.0\n");
        prompt.append("4. **KHÔNG** chấm quá khắt khe - focus vào những gì thí sinh làm được\n");
        prompt.append("5. Phân biệt major errors (ảnh hưởng nghĩa) vs minor errors (không ảnh hưởng)\n\n");
        
        prompt.append("### Về nội dung feedback:\n");
        prompt.append("6. Cung cấp ít nhất 3-5 sentence corrections với giải thích rõ ràng\n");
        prompt.append("7. Viết lại ít nhất introduction và 1 body paragraph\n");
        prompt.append("8. Sample essays phải realistic và relevant với đề bài cụ thể\n");
        prompt.append("9. **TẤT CẢ feedback phải bằng tiếng Việt**, có thể code-switch thuật ngữ tiếng Anh\n");
        prompt.append("10. Highlight ít nhất 5-8 từ/cụm từ đáng chú ý (cả tốt và cần sửa)\n");
        prompt.append("11. Khuyến khích thí sinh - nêu điểm mạnh trước điểm yếu\n\n");
        
        prompt.append("### Lưu ý cuối:\n");
        prompt.append("- Chỉ trả về JSON object, không có markdown fences hay text thừa\n");
        prompt.append("- Đảm bảo JSON hợp lệ, escape đúng các ký tự đặc biệt\n");
        
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
