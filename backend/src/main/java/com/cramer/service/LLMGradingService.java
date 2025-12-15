package com.cramer.service;

import com.cramer.config.LLMConfig;
import com.cramer.entity.WritingSubmission;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Service for grading IELTS Writing essays using DeepSeek AI API
 * (OpenAI-compatible).
 * Provides detailed feedback including band scores, corrections, and sample
 * essays.
 * 
 * Uses DeepSeek V3.2 models:
 * - deepseek-chat: Non-thinking mode (fast, cost-effective)
 * - deepseek-reasoner: Thinking mode (more accurate, longer outputs)
 * 
 * API Key Resolution:
 * 1. If user has llmApiKey set in profile → use user's key
 * 2. Otherwise → use server-side llmConfig.getApiKey()
 * 3. If BOTH are empty → throw clear error message
 * 
 * Migrated from GeminiGradingService on 2025-12-12.
 * Server-side API key support added on 2025-12-13.
 */
@Service
public class LLMGradingService {

        private static final Logger logger = LoggerFactory.getLogger(LLMGradingService.class);

        // DeepSeek API configuration (OpenAI-compatible)
        private static final String DEFAULT_LLM_MODEL = "deepseek-chat";

        // Available models for user selection
        public static final String[] AVAILABLE_MODELS = {
                        "deepseek-chat", // DeepSeek V3.2 non-thinking mode (fast, cheap)
                        "deepseek-reasoner" // DeepSeek V3.2 thinking mode (accurate, up to 64K output)
        };

        // Minimum word thresholds for IELTS Writing
        private static final int TASK_1_MIN_WORDS = 150;
        private static final int TASK_2_MIN_WORDS = 250;
        private static final int MINIMUM_ESSAY_WORDS = 20; // Below this = band 0-1

        // Timeout for DeepSeek API calls (10 minutes - reasoner model can take long
        // under high traffic)
        private static final int API_TIMEOUT_MS = 10 * 60 * 1000; // 10 minutes

        private final RestTemplate restTemplate;
        private final ObjectMapper objectMapper;
        private final LLMConfig llmConfig;

        public LLMGradingService(LLMConfig llmConfig) {
                // Configure RestTemplate with timeout
                org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(30000); // 30 seconds to connect
                factory.setReadTimeout(API_TIMEOUT_MS); // 10 minutes for response

                this.restTemplate = new RestTemplate(factory);
                this.objectMapper = new ObjectMapper();
                this.llmConfig = llmConfig;
        }

        /**
         * Resolve the API key to use for grading.
         * Priority: User's key > Server-side key > Error
         * 
         * @param userApiKey User's API key from profile (may be null or empty)
         * @return The resolved API key to use
         * @throws IllegalStateException if no API key is available
         */
        public String resolveApiKey(String userApiKey) {
                // Priority 1: User's own API key
                if (userApiKey != null && !userApiKey.trim().isEmpty()) {
                        logger.debug("Using user's personal API key for grading");
                        return userApiKey.trim();
                }

                // Priority 2: Server-side API key from configuration
                if (llmConfig.hasApiKey()) {
                        logger.debug("Using server-side API key for grading");
                        return llmConfig.getApiKey().trim();
                }

                // No API key available
                throw new IllegalStateException(
                                "No DeepSeek API key available. " +
                                                "Either set DEEPSEEK_API_KEY environment variable on server, " +
                                                "or add your personal API key in Profile settings.");
        }

        /**
         * Grade a writing submission using DeepSeek AI.
         * 
         * @param submission       The writing submission to grade
         * @param taskPrompt       The original task prompt/question
         * @param taskImageUrl     Optional image URL for Task 1 (NOTE: DeepSeek doesn't
         *                         support images yet)
         * @param imageDescription Text description of charts/maps for Task 1 (used
         *                         instead of images)
         * @param userApiKey       User's DeepSeek API key (optional - falls back to
         *                         server key)
         * @param model            User's selected model (optional, defaults to
         *                         deepseek-chat)
         * @return Updated submission with grading results
         */
        public WritingSubmission gradeSubmission(WritingSubmission submission, String taskPrompt,
                        String taskImageUrl, String imageDescription, String userApiKey, String model) {

                // Resolve API key with fallback logic
                String apiKey;
                try {
                        apiKey = resolveApiKey(userApiKey);
                } catch (IllegalStateException e) {
                        logger.error("No DeepSeek API key available for grading: {}", e.getMessage());
                        submission.setGradingStatus("FAILED");
                        Map<String, Object> errorFeedback = new HashMap<>();
                        errorFeedback.put("error", e.getMessage());
                        submission.setAiFeedback(errorFeedback);
                        return submission;
                }

                // Use grading model by default (deepseek-reasoner for accuracy)
                // Priority: user-specified model > llmConfig.gradingModel > DEFAULT_LLM_MODEL
                String selectedModel = (model != null && !model.trim().isEmpty())
                                ? model
                                : (llmConfig.getGradingModel() != null ? llmConfig.getGradingModel()
                                                : DEFAULT_LLM_MODEL);

                logger.info("Using model '{}' for writing grading", selectedModel);

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

                        // Call DeepSeek API (text-only, no image support)
                        // Note: taskImageUrl is ignored for now as DeepSeek doesn't support images
                        if (taskImageUrl != null && !taskImageUrl.trim().isEmpty()) {
                                logger.info("Note: Image URL provided but DeepSeek doesn't support image input. Proceeding with text-only grading.");
                        }

                        String response = callLLMApi(
                                        submission.getTaskNumber(),
                                        taskPrompt,
                                        essayText,
                                        wordCount,
                                        imageDescription,
                                        apiKey,
                                        selectedModel);

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
                                "Không đủ nội dung để đánh giá"));
                int minWords = submission.getTaskNumber() == 1 ? TASK_1_MIN_WORDS : TASK_2_MIN_WORDS;
                feedbackSummary.put("improvement_tips",
                                "Task " + submission.getTaskNumber() + " yêu cầu tối thiểu " + minWords + " từ. " +
                                                "Bài viết của bạn chỉ có " + wordCount + " từ.");
                feedback.put("feedback_summary", feedbackSummary);

                Map<String, String> criteriaComments = new HashMap<>();
                criteriaComments.put("task_achievement",
                                "Bài viết quá ngắn, không thể đánh giá Task Achievement/Response.");
                criteriaComments.put("coherence_cohesion", "Không đủ nội dung để đánh giá tính mạch lạc và liên kết.");
                criteriaComments.put("lexical_resource", "Không đủ nội dung để đánh giá vốn từ vựng.");
                criteriaComments.put("grammatical_range", "Không đủ nội dung để đánh giá ngữ pháp.");
                feedback.put("criteria_comments", criteriaComments);

                submission.setAiFeedback(feedback);
                submission.setGradedAt(OffsetDateTime.now());

                return submission;
        }

        /**
         * Build the comprehensive IELTS grading system prompt with official band
         * descriptors.
         * Enhanced with calibration anchors and generous scoring philosophy.
         * CALIBRATED based on official IELTS sample answers and band scores.
         */
        private String buildSystemPrompt(Integer taskNumber, int wordCount) {
                StringBuilder prompt = new StringBuilder();

                prompt.append("# HỆ THỐNG CHẤM ĐIỂM IELTS WRITING - PHIÊN BẢN ĐÃ HIỆU CHUẨN\n\n");
                prompt.append("Bạn là một giám khảo IELTS được chứng nhận với hơn 15 năm kinh nghiệm. ");
                prompt.append(
                                "Nhiệm vụ của bạn là chấm điểm bài viết IELTS một cách chính xác và công bằng theo tiêu chí band descriptors chính thức của IELTS.\n\n");

                // CRITICAL: Calibration-focused grading philosophy
                prompt.append("## TRIẾT LÝ CHẤM ĐIỂM - CỰC KỲ QUAN TRỌNG\\n\\n");

                prompt.append("### 🎯 NGUYÊN TẮC VÀNG - ĐỌC KỸ TRƯỚC KHI CHẤM:\\n\\n");

                prompt.append("**1. CHẤM DỰA TRÊN NĂNG LỰC NGÔN NGỮ THỂ HIỆN:**\\n");
                prompt.append("   - Đánh giá KHẢ NĂNG VIẾT TIẾNG ANH của thí sinh\\n");
                prompt.append("   - Cấu trúc câu có đa dạng không? Từ vựng có phong phú không?\\n");
                prompt.append("   - Có thể diễn đạt ý tưởng mạch lạc không?\\n");
                prompt.append("   - **QUAN TRỌNG**: Ngay cả bài lạc đề vẫn có thể có điểm ngôn ngữ tốt!\\n\\n");

                prompt.append("**2. XỬ LÝ BÀI LẠC ĐỀ (OFF-TOPIC) - RẤT QUAN TRỌNG:**\\n");
                prompt.append("   - Nếu bài HOÀN TOÀN lạc đề: Task Response/Achievement bị ảnh hưởng (giảm 1-2 band)\\n");
                prompt.append("   - NHƯNG: Coherence, Lexical Resource, Grammar vẫn chấm BÌNH THƯỜNG theo năng lực thể hiện\\n");
                prompt.append(
                                "   - Ví dụ thực tế: Bài lạc đề nhưng viết tốt có thể đạt: TR=4.5, CC=6.5, LR=6.5, GRA=6.0 → Overall = 6.0\\n");
                prompt.append("   - **KHÔNG** cho tất cả tiêu chí điểm thấp chỉ vì lạc đề!\\n\\n");

                prompt.append("**3. PHÂN LOẠI LỖI - ẢNH HƯỞNG ĐIỂM:**\\n");
                prompt.append(
                                "   - **Lỗi MINOR** (không ảnh hưởng hiểu): article, số ít/số nhiều nhỏ, typo 1-2 chữ → hầu như KHÔNG trừ điểm\\n");
                prompt.append("   - **Lỗi MODERATE**: awkward phrasing, collocation hơi sai → trừ nhẹ, vẫn có thể band 7+\\n");
                prompt.append(
                                "   - **Lỗi MAJOR** (gây hiểu sai/không hiểu): sai meaning, câu không có nghĩa → ảnh hưởng điểm\\n\\n");

                prompt.append("**4. QUY TẮC \\\"BENEFIT OF DOUBT\\\" - LUÔN ƯU TIÊN:**\\n");
                prompt.append("   - Khi phân vân giữa 2 band liền kề → **LUÔN CHỌN BAND CAO HƠN**\\n");
                prompt.append("   - Khi có cả điểm mạnh và điểm yếu rõ ràng → **ƯU TIÊN GHI NHẬN ĐIỂM MẠNH**\\n");
                prompt.append(
                                "   - Nhớ: Thí sinh viết trong điều kiện thi giới hạn thời gian (20 phút Task 1, 40 phút Task 2)\\n");
                prompt.append("   - **CỰC KỲ QUAN TRỌNG**: AI có xu hướng chấm khắt khe hơn con người. Hãy bù đắp bằng cách hào phóng hơn!\\n\\n");

                prompt.append("### 🚨 CẢNH BÁO: TRÁNH CHẤM QUÁ THẤP\\n\\n");
                prompt.append("**AI models thường có xu hướng undergrade (cho điểm thấp hơn thực tế). Vì vậy:**\\n");
                prompt.append("- Nếu bài viết có ý tưởng rõ ràng + coherent structure + từ vựng đủ dùng → PHẢI TỐI THIỂU Band 6.0\\n");
                prompt.append("- Nếu có variety of sentence structures + less common vocabulary sử dụng đúng → PHẢI TỐI THIỂU Band 7.0\\n");
                prompt.append("- Nếu chỉ có 3-5 lỗi minor trong toàn bài → PHẢI CÓ THỂ ĐẠT Band 8.0\\n");
                prompt.append("- **ĐỌC LẠI LẦN NỮA**: Khi nghi ngờ, CHỌN ĐIỂM CAO HƠN!\\n\\n");

                // Task-specific calibration
                if (taskNumber == 1) {
                        prompt.append("### 📝 HIỆU CHUẨN ĐẶC BIỆT CHO TASK 1:\\n\\n");
                        prompt.append("- Task 1 thường dễ đạt điểm cao hơn Task 2 (do yêu cầu đơn giản hơn)\\n");
                        prompt.append("- Nếu bài cover đủ key features + có overview → TỐI THIỂU Band 6.5\\n");
                        prompt.append("- Nếu có data selection tốt + comparison hợp lý → NÊN Band 7.0-7.5\\n");
                        prompt.append("- Chỉ cần thông tin accurate và đầy đủ, KHÔNG CẦN ngôn ngữ fancy → vẫn có thể Band 7+\\n");
                        prompt.append("- **TRÁNH**: Cho điểm dưới 6.0 trừ khi bài THỰC SỰ thiếu nội dung hoặc sai lệch nghiêm trọng\\n\\n");
                } else {
                        prompt.append("### 📝 HIỆU CHUẨN ĐẶC BIỆT CHO TASK 2:\\n\\n");
                        prompt.append("- Task 2 khó hơn Task 1, NHƯNG KHÔNG ĐƯỢC chấm quá khắt khe\\n");
                        prompt.append("- Nếu có position rõ ràng + 2 body paragraphs với examples → TỐI THIỂU Band 6.0\\n");
                        prompt.append("- Nếu ideas được develop với specific examples/reasons → NÊN Band 6.5-7.0\\n");
                        prompt.append("- Nếu có critical thinking + well-structured argument → NÊN Band 7.5+\\n");
                        prompt.append("- **QUAN TRỌNG**: Ý tưởng sâu sắc quan trọng HƠN ngôn ngữ hoàn hảo\\n");
                        prompt.append("- **TRÁNH**: Cho điểm dưới 5.5 trừ khi bài THỰC SỰ không trả lời câu hỏi hoặc quá ngắn\\n\\n");
                }

                prompt.append("### 📊 BẢNG CALIBRATION THỰC TẾ (dựa trên bài mẫu IELTS chính thức):\\n\\n");
                prompt.append("| Đặc điểm bài viết | Band thường đạt |\\n");
                prompt.append("|-------------------|----------------|\\n");
                prompt.append("| Lạc đề hoàn toàn nhưng ngôn ngữ khá | 5.5 - 6.0 |\\n");
                prompt.append("| Đúng đề, ý tưởng cơ bản, nhiều lỗi grammar/vocab | 5.5 - 6.0 |\\n");
                prompt.append("| Đúng đề, ý tưởng OK, một số lỗi grammar không ảnh hưởng hiểu | 6.5 - 7.0 |\\n");
                prompt.append("| Đúng đề, ý tưởng tốt, cấu trúc rõ ràng, ít lỗi | 7.0 - 7.5 |\\n");
                prompt.append("| Đúng đề, ý tưởng sâu, từ vựng đa dạng, grammar chính xác | 7.5 - 8.0 |\\n");
                prompt.append("| Xuất sắc toàn diện, chỉ lỗi rất nhỏ | 8.0 - 8.5 |\\n");
                prompt.append("| Gần như hoàn hảo | 8.5 - 9.0 |\\n");
                prompt.append("| Hoàn hảo như native speaker | 9.0 |\\n\\n");

                prompt.append("### 📈 CHI TIẾT VỀ TỪNG BAND (quan trọng để không chấm quá khắt khe):\\n\\n");

                prompt.append("**Band 6.0 - 6.5 (Competent User - PHỔ BIẾN NHẤT):**\\n");
                prompt.append("- Đây là band của đa số sinh viên đại học Việt Nam viết tốt\\n");
                prompt.append("- Có lỗi grammar nhưng KHÔNG ảnh hưởng communication\\n");
                prompt.append("- Từ vựng adequate (đủ dùng) dù không fancy\\n");
                prompt.append("- Có thể có một số ý chưa developed đầy đủ\\n");
                prompt.append("- **QUAN TRỌNG**: Bài có lỗi rải rác nhưng đọc hiểu được = Band 6.0+\\n");
                prompt.append("- **VÍ DỤ**: 8-12 lỗi grammar/vocabulary nhưng vẫn clear communication → Band 6.5\\n\\n");

                prompt.append("**Band 7.0 - 7.5 (Good User):**\\n");
                prompt.append("- Ý tưởng được develop rõ ràng với examples/support\\n");
                prompt.append("- Có variety trong sentence structures\\n");
                prompt.append("- Có sử dụng một số từ vựng less common\\n");
                prompt.append("- Lỗi ít và không systematic\\n");
                prompt.append("- **QUAN TRỌNG**: Error-free sentences FREQUENT (không phải tất cả câu)\\n");
                prompt.append("- **VÍ DỤ**: 4-7 lỗi nhỏ rải rác + good vocabulary range → Band 7.0-7.5\\n\\n");

                prompt.append("**Band 8.0+ (Very Good User):**\\n");
                prompt.append("- Majority of sentences error-free (cho phép 2-4 lỗi nhỏ trong toàn bài)\\n");
                prompt.append("- Wide range of vocabulary với skilful use\\n");
                prompt.append("- Ideas well-extended và well-supported\\n");
                prompt.append("- **QUAN TRỌNG**: 'Occasional errors' = VẪN CÓ THỂ ĐẠT BAND 8.0!\\n");
                prompt.append("- **VÍ DỤ**: 2-3 lỗi typo hoặc article + excellent content → Band 8.0\\n\\n");

                prompt.append("**Band 9.0 (Expert User) - HIẾM KHI ĐẠT:**\\n");
                prompt.append("- Chỉ cho Band 9.0 khi bài viết THỰC SỰ hoàn hảo, native-like\\n");
                prompt.append("- Hầu hết bài viết xuất sắc nên được 8.5, KHÔNG PHẢI 9.0\\n\\n");

                // Word count context
                int minWords = taskNumber == 1 ? TASK_1_MIN_WORDS : TASK_2_MIN_WORDS;
                prompt.append("## 📝 THÔNG TIN SỐ TỪ\n");
                prompt.append("- **Số từ đã nộp**: ").append(wordCount).append(" từ\n");
                prompt.append("- **Yêu cầu tối thiểu**: ").append(minWords).append(" từ\n");
                if (wordCount < minWords) {
                        int deficit = minWords - wordCount;
                        prompt.append("- **CHÚ Ý**: Bài viết THIẾU ").append(deficit).append(" từ. ");
                        prompt.append(
                                        "Điều này ảnh hưởng Task Achievement/Response, nhưng các tiêu chí khác vẫn chấm theo năng lực thể hiện.\n");
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
         */
        private String getTask1BandDescriptors() {
                StringBuilder desc = new StringBuilder();
                desc.append("## TIÊU CHÍ CHẤM ĐIỂM CHÍNH THỨC - IELTS WRITING TASK 1\n\n");

                // Band 9
                desc.append("### Band 9 (Expert User) - RẤT HIẾM\n");
                desc.append(
                                "- **Task Achievement**: Tất cả yêu cầu được đáp ứng đầy đủ. Có thể có sơ suất cực kỳ hiếm về nội dung.\n");
                desc.append(
                                "- **Coherence & Cohesion**: Thông điệp được theo dõi effortlessly. Cohesion rất hiếm khi thu hút sự chú ý. Paragraphing skilfully managed.\n");
                desc.append(
                                "- **Lexical Resource**: Full flexibility và precise use. Wide range với very natural và sophisticated control. Errors extremely rare.\n");
                desc.append("- **Grammar**: Wide range với full flexibility và control. Errors extremely rare.\n\n");

                // Band 8
                desc.append("### Band 8 (Very Good User)\n");
                desc.append(
                                "- **Task Achievement**: Đáp ứng tất cả yêu cầu appropriately, relevantly và sufficiently. Key features được chọn lọc khéo léo và trình bày rõ ràng. Occasional omissions OK.\n");
                desc.append(
                                "- **Coherence & Cohesion**: Message followed with ease. Information logically sequenced. Cohesion well managed. Occasional lapses OK.\n");
                desc.append(
                                "- **Lexical Resource**: Wide resource used fluently và flexibly. Skilful use of uncommon/idiomatic items. Occasional errors minimal impact.\n");
                desc.append(
                                "- **Grammar**: Wide range used flexibly và accurately. **MAJORITY of sentences error-free**. Occasional non-systematic errors.\n");
                desc.append("- **📌 CALIBRATION**: 'Occasional' = 2-4 lỗi trong toàn bài, KHÔNG phải mỗi đoạn!\n\n");

                // Band 7
                desc.append("### Band 7 (Good User)\n");
                desc.append(
                                "- **Task Achievement**: Đáp ứng các yêu cầu. Nội dung relevant và accurate với vài omissions. Có CLEAR OVERVIEW, data được categorised phù hợp.\n");
                desc.append(
                                "- **Coherence & Cohesion**: Information logically organised với clear progression. A few lapses OK. Cohesive devices used flexibly.\n");
                desc.append(
                                "- **Lexical Resource**: Sufficient flexibility và precision. Ability to use less common items. Few errors in spelling/word form.\n");
                desc.append(
                                "- **Grammar**: Variety of complex structures với some flexibility. Generally well controlled. **Error-free sentences FREQUENT**.\n");
                desc.append(
                                "- **📌 CALIBRATION**: Bài có overview tốt, cover key features, 5-7 lỗi nhỏ → xứng đáng Band 7.0-7.5\n\n");

                // Band 6
                desc.append("### Band 6 (Competent User) - PHỔ BIẾN NHẤT\n");
                desc.append(
                                "- **Task Achievement**: Tập trung vào requirements với appropriate format. Key features covered adequately. Có thể có vài chi tiết irrelevant/inaccurate.\n");
                desc.append(
                                "- **Coherence & Cohesion**: Generally arranged coherently với clear overall progression. Some cohesion may be faulty/mechanical.\n");
                desc.append(
                                "- **Lexical Resource**: Generally adequate cho task. Meaning generally clear dù restricted range.\n");
                desc.append(
                                "- **Grammar**: Mix of simple và complex forms nhưng limited flexibility. Errors **RARELY impede communication**.\n");
                desc.append(
                                "- **📌 CALIBRATION**: Đây là band phổ biến. Bài có lỗi nhưng vẫn đọc hiểu được = Band 6.0-6.5\n\n");

                // Band 5
                desc.append("### Band 5 (Modest User)\n");
                desc.append(
                                "- **Task Achievement**: Generally addresses requirements. Key features not adequately covered. May focus quá nhiều vào details.\n");
                desc.append(
                                "- **Coherence & Cohesion**: Organisation evident nhưng không wholly logical. Sentences không fluently linked.\n");
                desc.append(
                                "- **Lexical Resource**: Limited nhưng minimally adequate. Simple vocabulary, frequent lapses in appropriacy.\n");
                desc.append("- **Grammar**: Limited và repetitive structures. Complex sentences thường faulty.\n");
                desc.append("- **📌 CALIBRATION**: Bài thiếu overview, không cover key features đủ, nhiều lỗi → Band 5.0\n\n");

                return desc.toString();
        }

        /**
         * Get official IELTS Task 2 band descriptors (bands 5-9) with full detail.
         */
        private String getTask2BandDescriptors() {
                StringBuilder desc = new StringBuilder();
                desc.append("## TIÊU CHÍ CHẤM ĐIỂM CHÍNH THỨC - IELTS WRITING TASK 2\n\n");

                // Special note about off-topic essays
                desc.append("### ⚠️ XỬ LÝ BÀI LẠC ĐỀ (OFF-TOPIC):\n");
                desc.append("Nếu bài viết KHÔNG trả lời đúng câu hỏi đề bài:\n");
                desc.append("- **Task Response**: Bị ảnh hưởng nặng → giảm xuống Band 4.0-5.0\n");
                desc.append(
                                "- **Coherence & Cohesion**: Chấm BÌNH THƯỜNG theo cấu trúc bài (có intro, body, conclusion? Có linking words?)\n");
                desc.append("- **Lexical Resource**: Chấm BÌNH THƯỜNG theo từ vựng sử dụng (có variety? Có advanced vocab?)\n");
                desc.append("- **Grammar**: Chấm BÌNH THƯỜNG theo grammar (có complex sentences? Errors có impede meaning?)\n");
                desc.append(
                                "- **VÍ DỤ**: Bài lạc đề với ngôn ngữ tốt có thể đạt: TR=4.5, CC=6.5, LR=6.5, GRA=6.5 → Overall=6.0\n\n");

                // Band 9
                desc.append("### Band 9 (Expert User) - RẤT HIẾM\n");
                desc.append(
                                "- **Task Response**: Prompt được addressed và explored sâu sắc. Position rõ ràng, fully developed. Ideas relevant, fully extended và well supported.\n");
                desc.append(
                                "- **Coherence & Cohesion**: Message followed effortlessly. Cohesion barely attracts attention. Paragraphing skilfully managed.\n");
                desc.append(
                                "- **Lexical Resource**: Full flexibility và precise use. Very natural và sophisticated control. Errors extremely rare.\n");
                desc.append("- **Grammar**: Wide range với full flexibility và control. Errors extremely rare.\n\n");

                // Band 8
                desc.append("### Band 8 (Very Good User)\n");
                desc.append(
                                "- **Task Response**: Prompt addressed appropriately và sufficiently. Position clear và well-developed. Ideas relevant, well extended và supported.\n");
                desc.append(
                                "- **Coherence & Cohesion**: Message followed with ease. Information logically sequenced. Cohesion well managed. Occasional lapses OK.\n");
                desc.append(
                                "- **Lexical Resource**: Wide resource used fluently và flexibly. Skilful use of uncommon/idiomatic items. Occasional inaccuracies minimal impact.\n");
                desc.append(
                                "- **Grammar**: Wide range used flexibly và accurately. **MAJORITY of sentences error-free**. Occasional non-systematic errors OK.\n");
                desc.append(
                                "- **📌 CALIBRATION**: 'Occasional' = 2-4 lỗi rải rác trong TOÀN BÀI. Bài có vài lỗi nhỏ VẪN có thể đạt Band 8!\n\n");

                // Band 7
                desc.append("### Band 7 (Good User)\n");
                desc.append(
                                "- **Task Response**: Main parts addressed appropriately. Position clear và developed. May have tendency to over-generalise/lack focus.\n");
                desc.append(
                                "- **Coherence & Cohesion**: Information logically organised với clear progression. A few minor lapses OK. Paragraphing effective.\n");
                desc.append(
                                "- **Lexical Resource**: Sufficient flexibility và precision. Ability to use less common items. Few errors in spelling/word form.\n");
                desc.append(
                                "- **Grammar**: Variety of complex structures với some flexibility và accuracy. **Error-free sentences FREQUENT**. Few errors don't impede.\n");
                desc.append(
                                "- **📌 CALIBRATION**: Bài có khoảng 5-8 lỗi nhỏ rải rác, cấu trúc đa dạng → xứng đáng Band 7.0\n\n");

                // Band 6
                desc.append("### Band 6 (Competent User) - PHỔ BIẾN NHẤT\n");
                desc.append(
                                "- **Task Response**: Main parts addressed (có thể không đều). Position relevant nhưng conclusions may be unclear/repetitive.\n");
                desc.append(
                                "- **Coherence & Cohesion**: Generally coherent với clear overall progression. Some faulty/mechanical cohesion OK.\n");
                desc.append(
                                "- **Lexical Resource**: Generally adequate. Meaning generally clear dù restricted range. Some errors in spelling/word form.\n");
                desc.append(
                                "- **Grammar**: Mix of simple và complex nhưng limited flexibility. Errors **RARELY impede communication**.\n");
                desc.append(
                                "- **📌 CALIBRATION**: Đây là band của sinh viên đại học Việt Nam viết khá. Có lỗi nhưng vẫn đọc hiểu được = Band 6.0-6.5\n\n");

                // Band 5
                desc.append("### Band 5 (Modest User)\n");
                desc.append(
                                "- **Task Response**: Main parts incompletely addressed. Position expressed nhưng development không always clear.\n");
                desc.append(
                                "- **Coherence & Cohesion**: Organisation evident nhưng không wholly logical. Paragraphing may be inadequate.\n");
                desc.append("- **Lexical Resource**: Limited nhưng minimally adequate. Frequent lapses in appropriacy.\n");
                desc.append("- **Grammar**: Limited, repetitive structures. Complex sentences tend to be faulty.\n");
                desc.append("- **📌 CALIBRATION**: Bài có nhiều lỗi GÂY KHÓ HIỂU, hoặc ý tưởng rất hạn chế → Band 5.0\n\n");

                return desc.toString();
        }

        /**
         * Build the user prompt with task details and essay.
         */
        private String buildUserPrompt(Integer taskNumber, String taskPrompt, String essay, String imageDescription) {
                StringBuilder prompt = new StringBuilder();

                prompt.append("## Đề bài (Task Prompt):\n");
                prompt.append(taskPrompt).append("\n\n");

                // Add image description for Task 1 if available
                if (taskNumber == 1 && imageDescription != null && !imageDescription.trim().isEmpty()) {
                        prompt.append("## Mô tả chi tiết hình ảnh/biểu đồ (Image/Chart Description):\n");
                        prompt.append(imageDescription).append("\n\n");
                        prompt.append("**Lưu ý:** Đánh giá bài viết dựa trên việc thí sinh có mô tả chính xác ");
                        prompt.append("các thông tin trong biểu đồ/bản đồ/sơ đồ này hay không.\n\n");
                }

                prompt.append("## Bài viết của thí sinh:\n");
                prompt.append("```\n").append(essay).append("\n```\n\n");

                prompt.append("## YÊU CẦU ĐỊNH DẠNG RESPONSE\n");
                prompt.append("Bạn PHẢI trả về một JSON object hợp lệ với cấu trúc chính xác như sau. ");
                prompt.append("KHÔNG thêm bất kỳ text nào ngoài JSON. KHÔNG dùng markdown code fences.\n\n");

                prompt.append("### NGÔN NGỮ OUTPUT:\n");
                prompt.append("- Tất cả feedback, explanation, comments PHẢI viết bằng **tiếng Việt**\n");
                prompt.append(
                                "- Có thể code-switch với thuật ngữ tiếng Anh khi cần (ví dụ: 'coherence', 'collocation', 'topic sentence')\n");
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
                prompt.append(
                                "      \"explanation\": \"<giải thích bằng tiếng Việt, ví dụ: 'Thiếu article 'the' trước danh từ xác định'>\"\n");
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

                // Vocabulary highlights
                prompt.append("  \"vocabulary_highlights\": [\n");
                prompt.append("    {\n");
                prompt.append("      \"word\": \"<từ/cụm từ đáng chú ý trong bài>\",\n");
                prompt.append("      \"category\": \"<advanced_good|collocation_good|academic|error|awkward>\",\n");
                prompt.append(
                                "      \"note\": \"<nhận xét ngắn bằng tiếng Việt, ví dụ: 'Dùng collocation tốt' hoặc 'Sai word form'>\"\n");
                prompt.append("    }\n");
                prompt.append("  ],\n\n");

                // Error severity summary
                prompt.append("  \"error_analysis\": {\n");
                prompt.append("    \"major_errors\": <số lỗi lớn ảnh hưởng nghĩa>,\n");
                prompt.append("    \"minor_errors\": <số lỗi nhỏ không ảnh hưởng nghĩa>,\n");
                prompt.append(
                                "    \"summary\": \"<tóm tắt bằng tiếng Việt, ví dụ: 'Hầu hết lỗi là minor errors không ảnh hưởng communication'>\"\n");
                prompt.append("  },\n\n");

                prompt.append(
                                "  \"sample_essay_band_plus_one\": \"<bài viết hoàn chỉnh ở mức band+1, viết bằng tiếng Anh>\",\n");
                prompt.append("  \"sample_essay_band_9\": \"<bài mẫu band 9 cho đề này, viết bằng tiếng Anh>\",\n\n");

                prompt.append("  \\\"feedback_summary\\\": {\\n");
                prompt.append("    \\\"strengths\\\": [\\\"<điểm mạnh 1 CỤ THỂ với ví dụ - tiếng Việt>\\\", \\\"<điểm mạnh 2>\\\", \\\"<điểm mạnh 3>\\\"],\\n");
                prompt.append("    \\\"weaknesses\\\": [\\\"<điểm yếu 1 CỤ THỂ với ví dụ - tiếng Việt>\\\", \\\"<điểm yếu 2>\\\"],\\n");
                prompt.append("    \\\"grammar_patterns\\\": {\\n");
                prompt.append("      \\\"strong_patterns\\\": [\\\"<các cấu trúc ngữ pháp thí sinh sử dụng TỐT, ví dụ: 'Relative clauses', 'Conditional sentences'>\\\"],\\n");
                prompt.append("      \\\"weak_patterns\\\": [\\\"<các cấu trúc ngữ pháp cần cải thiện, ví dụ: 'Subject-verb agreement', 'Articles'>\\\"],\\n");
                prompt.append("      \\\"missing_patterns\\\": [\\\"<các cấu trúc nâng cao CHƯA dùng nhưng NÊN dùng để tăng band, ví dụ: 'Passive voice', 'Inversion'>\\\"]\\n");
                prompt.append("    },\\n");
                prompt.append("    \\\"writing_approach\\\": \\\"<gợi ý cách tiếp cận bài viết - tiếng Việt, 3-4 câu CỤ THỂ>\\\",\\n");
                prompt.append("    \\\"improvement_tips\\\": \\\"<tips cải thiện CỤ THỂ với bước làm rõ ràng - tiếng Việt, 3-4 câu>\\\"\\n");
                prompt.append("  },\\n\\n");

                prompt.append("  \"word_analysis\": [\n");
                prompt.append("    {\n");
                prompt.append("      \"word\": \"<từ/cụm từ trong bài>\",\n");
                prompt.append("      \"word_type\": \"<noun|verb|adjective|adverb|preposition|conjunction|phrase>\",\n");
                prompt.append("      \"definition\": \"<định nghĩa tiếng Việt>\",\n");
                prompt.append("      \"context\": \"<câu chứa từ đó trong bài>\",\n");
                prompt.append("      \"usage_quality\": \"<good|acceptable|incorrect>\",\n");
                prompt.append("      \"vocab_level\": \"<A1|A2|B1|B2|C1|C2 - CEFR level của từ này>\",\n");
                prompt.append("      \"correction\": \"<nếu sai chính tả hoặc sai từ, ghi từ đúng ở đây; nếu đúng thì để null hoặc bỏ trống>\"\n");
                prompt.append("    }\n");
                prompt.append("  ],\n\n");

                prompt.append("  \"criteria_comments\": {\n");
                if (taskNumber == 1) {
                        prompt.append(
                                        "    \"task_achievement\": \"<2-3 câu giải thích điểm Task Achievement - tiếng Việt, có thể dùng thuật ngữ tiếng Anh>\",\n");
                } else {
                        prompt.append(
                                        "    \"task_achievement\": \"<2-3 câu giải thích điểm Task Response - tiếng Việt, có thể dùng thuật ngữ tiếng Anh>\",\n");
                }
                prompt.append("    \"coherence_cohesion\": \"<2-3 câu giải thích điểm Coherence & Cohesion - tiếng Việt>\",\n");
                prompt.append("    \"lexical_resource\": \"<2-3 câu giải thích điểm Lexical Resource - tiếng Việt>\",\n");
                prompt.append(
                                "    \"grammatical_range\": \"<2-3 câu giải thích điểm Grammatical Range & Accuracy - tiếng Việt>\"\n");
                prompt.append("  }\n");
                prompt.append("}\n\n");

                prompt.append("## HƯỚNG DẪN CHẤM ĐIỂM - ĐÃ HIỆU CHUẨN\n\n");

                prompt.append("### ⚠️ CẢNH BÁO: TRÁNH CHẤM QUÁ KHẮT KHE\n");
                prompt.append("Hệ thống AI thường có xu hướng chấm khắt khe hơn giám khảo thực. ");
                prompt.append("Hãy nhớ các nguyên tắc sau:\n\n");

                prompt.append("### Về điểm số:\n");
                prompt.append("1. **ĐỌC TOÀN BỘ BÀI** trước khi cho điểm - đừng vội kết luận từ vài câu đầu\n");
                prompt.append("2. **TÁCH BIỆT 4 TIÊU CHÍ**: Mỗi tiêu chí được chấm độc lập\n");
                prompt.append("3. **NHỚ**: Band 6.0-6.5 là band PHỔ BIẾN nhất - đừng ngại cho điểm này\n");
                prompt.append("4. **NHỚ**: Band 7.0+ cho bài viết có error-free sentences FREQUENT (không phải tất cả)\n");
                prompt.append("5. **NHỚ**: Band 8.0 vẫn cho phép 'occasional errors' - 2-4 lỗi nhỏ rải rác\n");
                prompt.append("6. **QUAN TRỌNG**: Khi phân vân giữa band X và band X+0.5 → CHỌN BAND CAO HƠN\n\n");

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
         * Call DeepSeek API using OpenAI-compatible format.
         */
        private String callLLMApi(Integer taskNumber, String taskPrompt, String essay,
                        int wordCount, String imageDescription, String apiKey, String model) {
                String baseUrl = llmConfig.getBaseUrl() != null ? llmConfig.getBaseUrl() : "https://api.deepseek.com";
                String url = baseUrl + "/chat/completions";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + apiKey);

                // Build request in OpenAI-compatible format
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", model);

                // Messages array with system and user messages
                List<Map<String, Object>> messages = new ArrayList<>();

                // System message
                Map<String, Object> systemMessage = new HashMap<>();
                systemMessage.put("role", "system");
                systemMessage.put("content", buildSystemPrompt(taskNumber, wordCount));
                messages.add(systemMessage);

                // User message
                Map<String, Object> userMessage = new HashMap<>();
                userMessage.put("role", "user");
                userMessage.put("content", buildUserPrompt(taskNumber, taskPrompt, essay, imageDescription));
                messages.add(userMessage);

                requestBody.put("messages", messages);

                // Generation parameters
                requestBody.put("temperature", 0.4); // Slightly higher for more generous, less rigid scoring
                requestBody.put("max_tokens", 8192); // DeepSeek max limit (deepseek-chat supports 8K)
                requestBody.put("response_format", Map.of("type", "json_object")); // Force JSON output
                requestBody.put("stream", false);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                try {
                        ResponseEntity<String> response = restTemplate.exchange(
                                        url, HttpMethod.POST, entity, String.class);

                        if (response.getStatusCode() != HttpStatus.OK) {
                                throw new RuntimeException("DeepSeek API returned status: " + response.getStatusCode());
                        }

                        return response.getBody();
                } catch (Exception e) {
                        logger.error("DeepSeek API call failed: {}", e.getMessage());
                        throw new RuntimeException("Failed to call DeepSeek API: " + e.getMessage(), e);
                }
        }

        /**
         * Parse DeepSeek API response (OpenAI format) and apply grading results to
         * submission.
         */
        private void parseAndApplyGradingResults(WritingSubmission submission, String apiResponse)
                        throws JsonProcessingException {

                JsonNode root = objectMapper.readTree(apiResponse);

                // Validate OpenAI-compatible response structure
                JsonNode choices = root.path("choices");
                if (choices.isMissingNode() || !choices.isArray() || choices.size() == 0) {
                        logger.error("Invalid DeepSeek API response: missing choices array. Response: {}",
                                        apiResponse.length() > 500 ? apiResponse.substring(0, 500) + "..."
                                                        : apiResponse);
                        throw new RuntimeException("Invalid DeepSeek API response: missing choices array");
                }

                JsonNode firstChoice = choices.get(0);
                if (firstChoice == null) {
                        logger.error("Invalid DeepSeek API response: empty choices array");
                        throw new RuntimeException("Invalid DeepSeek API response: empty choices");
                }

                JsonNode messageNode = firstChoice.path("message");
                if (messageNode.isMissingNode()) {
                        logger.error("Invalid DeepSeek API response: missing message. First choice: {}", firstChoice);
                        throw new RuntimeException("Invalid DeepSeek API response: missing message");
                }

                JsonNode contentNode = messageNode.path("content");
                if (contentNode.isMissingNode()) {
                        logger.error("Invalid DeepSeek API response: missing content in message. Message: {}",
                                        messageNode);
                        throw new RuntimeException("Invalid DeepSeek API response: missing content in message");
                }

                // Extract the generated text from response
                String generatedText = contentNode.asText();

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

                // Extract band scores - NO MORE hardcoded calibration, let the enhanced prompt
                // handle it
                JsonNode bandScoresNode = gradingResult.path("band_scores");
                Map<String, Object> bandScores = objectMapper.convertValue(bandScoresNode, Map.class);
                submission.setBandScores(bandScores);

                // Calculate and set overall band (rounded to nearest 0.5)
                double overallBandValue = bandScores.values().stream()
                                .mapToDouble(v -> ((Number) v).doubleValue())
                                .average()
                                .orElse(0.0);
                BigDecimal overallBand = roundToNearestHalf(overallBandValue);
                submission.setOverallBand(overallBand);

                logger.info("AI scoring complete - overall band: {}", overallBand);

                // Build AI feedback object
                Map<String, Object> aiFeedback = new HashMap<>();

                if (gradingResult.has("sentence_corrections")) {
                        aiFeedback.put("sentence_corrections",
                                        objectMapper.convertValue(gradingResult.path("sentence_corrections"),
                                                        List.class));
                }

                if (gradingResult.has("paragraph_rewrites")) {
                        aiFeedback.put("paragraph_rewrites",
                                        objectMapper.convertValue(gradingResult.path("paragraph_rewrites"),
                                                        List.class));
                }

                if (gradingResult.has("vocabulary_highlights")) {
                        aiFeedback.put("vocabulary_highlights",
                                        objectMapper.convertValue(gradingResult.path("vocabulary_highlights"),
                                                        List.class));
                }

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
                return validateApiKey(apiKey, DEFAULT_LLM_MODEL);
        }

        /**
         * Validate API key by making a simple test request with specified model.
         */
        public boolean validateApiKey(String apiKey, String model) {
                if (apiKey == null || apiKey.trim().isEmpty()) {
                        return false;
                }

                String selectedModel = (model != null && !model.trim().isEmpty()) ? model : DEFAULT_LLM_MODEL;

                try {
                        String baseUrl = llmConfig.getBaseUrl() != null ? llmConfig.getBaseUrl()
                                        : "https://api.deepseek.com";
                        String url = baseUrl + "/chat/completions";

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.set("Authorization", "Bearer " + apiKey);

                        Map<String, Object> requestBody = new HashMap<>();
                        requestBody.put("model", selectedModel);

                        List<Map<String, String>> messages = new ArrayList<>();
                        Map<String, String> userMessage = new HashMap<>();
                        userMessage.put("role", "user");
                        userMessage.put("content", "Hi");
                        messages.add(userMessage);
                        requestBody.put("messages", messages);
                        requestBody.put("max_tokens", 10);
                        requestBody.put("stream", false);

                        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                        ResponseEntity<String> response = restTemplate.exchange(
                                        url, HttpMethod.POST, entity, String.class);

                        return response.getStatusCode() == HttpStatus.OK;
                } catch (Exception e) {
                        logger.warn("API key validation failed: {}", e.getMessage());
                        return false;
                }
        }
}
