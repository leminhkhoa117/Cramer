package com.cramer.service;

import com.cramer.dto.AnswerSubmissionDTO;
import com.cramer.dto.SaveProgressDTO;
import com.cramer.dto.TestResultDTO;
import com.cramer.dto.TestReviewDTO;
import com.cramer.dto.UserAnswerDTO;
import com.cramer.dto.QuestionReviewDTO;
import com.cramer.entity.Question;
import com.cramer.entity.TestAttempt;
import com.cramer.entity.UserAnswer;
import com.cramer.repository.QuestionRepository;
import com.cramer.util.IeltsScoreConverter;
import com.cramer.util.EntityMapper;
import com.cramer.repository.TestAttemptRepository;
import com.cramer.repository.UserAnswerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cramer.exception.ResourceNotFoundException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TestAttemptService {

    private final TestAttemptRepository testAttemptRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public TestAttemptService(TestAttemptRepository testAttemptRepository,
                              UserAnswerRepository userAnswerRepository,
                              QuestionRepository questionRepository,
                              ObjectMapper objectMapper) {
        this.testAttemptRepository = testAttemptRepository;
        this.userAnswerRepository = userAnswerRepository;
        this.questionRepository = questionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TestAttempt startOrGetAttempt(String source, String testNum, String skill, UUID userId) {
        final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestAttemptService.class);
        logger.info("--- NEW REQUEST ---");
        logger.info("🎯 [1] Starting startOrGetAttempt: userId={}, source={}, testNum={}, skill={}", 
                        userId, source, testNum, skill);
        
        try {
            // Trim inputs for robustness
            String trimmedSource = source != null ? source.trim() : null;
            String trimmedTestNum = testNum != null ? testNum.trim() : null;
            String trimmedSkill = skill != null ? skill.trim() : null;

            // Validate inputs
            if (userId == null) throw new IllegalArgumentException("User ID cannot be null");
            if (trimmedSource == null || trimmedSource.isEmpty()) throw new IllegalArgumentException("Source cannot be null or empty");
            if (trimmedTestNum == null || trimmedTestNum.isEmpty()) throw new IllegalArgumentException("Test number cannot be null or empty");
            if (trimmedSkill == null || trimmedSkill.isEmpty()) throw new IllegalArgumentException("Skill cannot be null or empty");
            
            logger.info("🎯 [2] Finding latest attempt with: source={}, testNum={}, skill={}", trimmedSource, trimmedTestNum, trimmedSkill);
            Optional<TestAttempt> latestAttemptOpt = testAttemptRepository
                    .findTopByUserIdAndExamSourceAndTestNumberAndSkillOrderByStartedAtDesc(userId, trimmedSource, trimmedTestNum, trimmedSkill);

            if (latestAttemptOpt.isPresent()) {
                TestAttempt latestAttempt = latestAttemptOpt.get();
                logger.info("🎯 [3A] Found existing attempt. ID: {}, Status: {}", latestAttempt.getId(), latestAttempt.getStatus());

                if ("COMPLETED".equals(latestAttempt.getStatus()) || "CANCELLED".equals(latestAttempt.getStatus())) {
                    logger.info("   -> Status is '{}'. Proceeding to create a new attempt.", latestAttempt.getStatus());
                    return createNewAttempt(userId, trimmedSource, trimmedTestNum, trimmedSkill, logger);
                }
                
                logger.info("   -> Status is 'IN_PROGRESS'. Resuming this attempt.");
                // Detached copy to prevent serialization issues
                TestAttempt detachedAttempt = new TestAttempt();
                detachedAttempt.setId(latestAttempt.getId());
                detachedAttempt.setUserId(latestAttempt.getUserId());
                detachedAttempt.setExamSource(latestAttempt.getExamSource());
                detachedAttempt.setTestNumber(latestAttempt.getTestNumber());
                detachedAttempt.setSkill(latestAttempt.getSkill());
                detachedAttempt.setStatus(latestAttempt.getStatus());
                detachedAttempt.setScore(latestAttempt.getScore());
                detachedAttempt.setStartedAt(latestAttempt.getStartedAt());
                detachedAttempt.setCompletedAt(latestAttempt.getCompletedAt());
                detachedAttempt.setTimeLeft(latestAttempt.getTimeLeft());
                detachedAttempt.setCurrentPart(latestAttempt.getCurrentPart());
                logger.info("🎯 [4A] Returning detached copy of attempt ID: {}", detachedAttempt.getId());
                return detachedAttempt;
            } else {
                logger.info("🎯 [3B] No existing attempt found. Proceeding to create a new attempt.");
                return createNewAttempt(userId, trimmedSource, trimmedTestNum, trimmedSkill, logger);
            }
        } catch (Exception e) {
            logger.error("❌ [ERROR] Unhandled exception in startOrGetAttempt: userId={}, source={}, testNum={}, skill={}", 
                        userId, source, testNum, skill, e);
            throw new RuntimeException("Failed to start/get test attempt: " + e.getMessage(), e);
        }
    }

    private TestAttempt createNewAttempt(UUID userId, String source, String testNum, String skill, org.slf4j.Logger logger) {
        logger.info("   -> [Sub-Process] Inside createNewAttempt");
        TestAttempt newAttempt = new TestAttempt();
        newAttempt.setUserId(userId);
        newAttempt.setExamSource(source);
        newAttempt.setTestNumber(testNum);
        newAttempt.setSkill(skill);
        
        logger.info("   -> Attempt object to be saved: userId={}, source={}, testNum={}, skill={}, status={}", 
            newAttempt.getUserId(), newAttempt.getExamSource(), newAttempt.getTestNumber(), newAttempt.getSkill(), newAttempt.getStatus());

        try {
            TestAttempt savedAttempt = testAttemptRepository.save(newAttempt);
            logger.info("   -> [SUCCESS] Successfully saved new attempt with ID: {}", savedAttempt.getId());
            return savedAttempt;
        } catch (Exception e) {
            logger.error("   -> [FATAL] FAILED to save new TestAttempt in repository. Error: {}", e.getMessage(), e);
            throw e; // Re-throw the exception to be caught by the main handler
        }
    }

    private TestAttempt createNewAttempt(UUID userId, String source, String testNum, String skill) {
        final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestAttemptService.class);
        return createNewAttempt(userId, source, testNum, skill, logger);
    }

    @Transactional
    public void saveProgress(Long attemptId, SaveProgressDTO saveProgressDTO, UUID userId) {
        final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestAttemptService.class);
        logger.info("🔄 Saving progress for attempt: attemptId={}, userId={}", attemptId, userId);

        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAttempt not found"));
        
        if (!attempt.getUserId().equals(userId)) {
            throw new AccessDeniedException("User does not have permission to update this attempt.");
        }

        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            throw new IllegalStateException("Cannot save progress for completed or cancelled test.");
        }

        // Update time and part
        if (saveProgressDTO.getTimeLeft() != null) attempt.setTimeLeft(saveProgressDTO.getTimeLeft());
        if (saveProgressDTO.getCurrentPart() != null) attempt.setCurrentPart(saveProgressDTO.getCurrentPart());
        
        // Save answers
        if (saveProgressDTO.getAnswers() != null && !saveProgressDTO.getAnswers().isEmpty()) {
            logger.info("   -> Saving {} answers for attempt {}", saveProgressDTO.getAnswers().size(), attemptId);
            // Delete existing answers for this attempt to handle un-selected options
            userAnswerRepository.deleteByAttemptId(attemptId);
            entityManager.flush(); // Ensure delete happens before new inserts

            List<UserAnswer> userAnswers = new ArrayList<>();
            for (Map.Entry<Long, String> entry : saveProgressDTO.getAnswers().entrySet()) {
                Long questionId = entry.getKey();
                String answerText = entry.getValue();

                if (answerText == null || answerText.trim().isEmpty()) {
                    continue; // Skip empty answers
                }

                Question question = questionRepository.findById(questionId)
                        .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));
                
                // Adapt the String answer to a JsonNode to maintain compatibility with downstream logic
                ObjectNode answerNode = objectMapper.createObjectNode();
                answerNode.put("value", answerText);

                UserAnswer userAnswer = new UserAnswer();
                userAnswer.setUserId(userId);
                userAnswer.setAttempt(attempt);
                userAnswer.setQuestion(question);
                userAnswer.setAnswerContent(answerNode);
                userAnswer.setUserAnswer(answerText);
                // isCorrect is not set here, as it's an in-progress save, not a submission
                userAnswers.add(userAnswer);
            }
            userAnswerRepository.saveAll(userAnswers);
            logger.info("   -> Successfully saved {} user answers.", userAnswers.size());
        } else {
            logger.info("   -> No answers provided or answers map is empty. Skipping answer save.");
        }

        testAttemptRepository.save(attempt);
        logger.info("✅ Successfully saved progress for attempt: attemptId={}", attemptId);
    }

    @Transactional
    public TestResultDTO submitAttempt(Long testAttemptId, Map<Long, String> answers, UUID userId) {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestAttemptService.class);
        logger.info("📝 Submitting test attempt: attemptId={}, userId={}, answersCount={}", 
                    testAttemptId, userId, answers != null ? answers.size() : 0);
        
        TestAttempt testAttempt = testAttemptRepository.findById(testAttemptId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAttempt not found with id: " + testAttemptId));

        // Verify ownership
        if (!testAttempt.getUserId().equals(userId)) {
            throw new AccessDeniedException("User does not have permission to submit this test attempt.");
        }

        // Allow re-submission by deleting old answers
        long startDelete = System.currentTimeMillis();
        userAnswerRepository.deleteByAttemptId(testAttemptId);
        entityManager.flush(); // Force delete to execute immediately before insert
        long deleteTime = System.currentTimeMillis() - startDelete;
        logger.info("🗑️ Deleted old answers in {}ms", deleteTime);

        List<UserAnswer> userAnswers = new ArrayList<>();
        int correctCount = 0;

        long startGrading = System.currentTimeMillis();
        if (answers != null) {
            for (Map.Entry<Long, String> entry : answers.entrySet()) {
                Long questionId = entry.getKey();
                String answerText = entry.getValue();

                if (answerText == null || answerText.trim().isEmpty()) {
                    continue; // Skip unanswered questions
                }
                
                Question question = questionRepository.findById(questionId)
                        .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));

                // Adapt the String answer to a JsonNode to maintain compatibility with downstream logic
                ObjectNode answerNode = objectMapper.createObjectNode();
                answerNode.put("value", answerText);

                boolean isCorrect = compareAnswers(answerNode, question.getCorrectAnswer());

                UserAnswer userAnswer = new UserAnswer();
                userAnswer.setUserId(userId);
                userAnswer.setAttempt(testAttempt);
                userAnswer.setQuestion(question);
                userAnswer.setAnswerContent(answerNode);
                userAnswer.setUserAnswer(answerText); // Set the plain text value
                userAnswer.setCorrect(isCorrect);
                userAnswers.add(userAnswer);

                if (isCorrect) {
                    correctCount++;
                }
            }
        }
        long gradingTime = System.currentTimeMillis() - startGrading;
        logger.info("✅ Graded {} answers in {}ms", answers != null ? answers.size() : 0, gradingTime);

        long startSave = System.currentTimeMillis();
        userAnswerRepository.saveAll(userAnswers);
        long saveTime = System.currentTimeMillis() - startSave;
        logger.info("💾 Saved {} answers in {}ms", userAnswers.size(), saveTime);

        testAttempt.setStatus("COMPLETED");
        testAttempt.setCompletedAt(OffsetDateTime.now());
        testAttempt.setScore(correctCount);
        
        long startUpdateAttempt = System.currentTimeMillis();
        testAttemptRepository.save(testAttempt);
        long updateTime = System.currentTimeMillis() - startUpdateAttempt;
        logger.info("🔄 Updated test attempt in {}ms", updateTime);

        long startCountQuestions = System.currentTimeMillis();
        int totalQuestions = questionRepository.countBySection_ExamSourceAndSection_TestNumberAndSection_Skill(
            testAttempt.getExamSource(),
            Integer.valueOf(testAttempt.getTestNumber()),
            testAttempt.getSkill()
        );
        long countTime = System.currentTimeMillis() - startCountQuestions;
        logger.info("🔢 Counted total questions in {}ms", countTime);

        logger.info("🎉 Test submission completed: score={}/{}", correctCount, totalQuestions);
        return new TestResultDTO(testAttempt.getId(), correctCount, totalQuestions, testAttempt.getStatus());
    }

    private boolean compareAnswers(JsonNode userAnswer, JsonNode correctAnswer) {
        if (userAnswer == null || userAnswer.get("value") == null || userAnswer.get("value").isNull() || correctAnswer == null) {
            return false;
        }

        String userText = userAnswer.get("value").asText()
                .replace("_", " ")
                .trim()
                .toLowerCase();

        // Handle cases where the correct answer is a JSON array (e.g., ["answer1", "answer2"])
        if (correctAnswer.isArray()) {
            for (JsonNode correctNode : correctAnswer) {
                String correctText = correctNode.asText()
                        .replace("_", " ")
                        .trim()
                        .toLowerCase();
                if (correctText.equals(userText)) {
                    return true;
                }
            }
        } else { // Handle cases where the correct answer is a single JSON string (e.g., "answer")
            String correctText = correctAnswer.asText()
                    .replace("_", " ")
                    .trim()
                    .toLowerCase();
            if (correctText.equals(userText)) {
                return true;
            }
        }

        return false;
    }

    @Transactional(readOnly = true)
    public TestReviewDTO getTestReview(Long attemptId, UUID userId) {
        // 1. Fetch attempt and verify ownership
        TestAttempt testAttempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAttempt not found with id: " + attemptId));

        if (!testAttempt.getUserId().equals(userId)) {
            throw new AccessDeniedException("User does not have permission to review this test attempt.");
        }

        // 2. Fetch all user answers for this attempt
        List<UserAnswer> userAnswers = userAnswerRepository.findByAttemptId(attemptId);
        Map<Long, UserAnswer> answersByQuestionId = userAnswers.stream()
                .collect(Collectors.toMap(answer -> answer.getQuestion().getId(), answer -> answer));

        // 3. Fetch all questions for the entire test
        List<Question> allTestQuestions = questionRepository.findBySection_ExamSourceAndSection_TestNumberAndSection_Skill(
            testAttempt.getExamSource(),
            Integer.valueOf(testAttempt.getTestNumber()),
            testAttempt.getSkill()
        );

        // 4. Build the DTO
        TestReviewDTO reviewDTO = new TestReviewDTO();
        reviewDTO.setAttemptId(testAttempt.getId());
        reviewDTO.setExamSource(testAttempt.getExamSource());
        reviewDTO.setTestNumber(testAttempt.getTestNumber());
        reviewDTO.setSkill(testAttempt.getSkill());
        reviewDTO.setScore(testAttempt.getScore());
        reviewDTO.setTotalQuestions(allTestQuestions.size());
        reviewDTO.setStartedAt(testAttempt.getStartedAt());
        reviewDTO.setCompletedAt(testAttempt.getCompletedAt());

        // Calculate and set duration
        if (testAttempt.getStartedAt() != null && testAttempt.getCompletedAt() != null) {
            long durationInSeconds = java.time.Duration.between(testAttempt.getStartedAt(), testAttempt.getCompletedAt()).getSeconds();
            reviewDTO.setDuration(durationInSeconds);
        }

        // Calculate and set band score
        if ("COMPLETED".equals(testAttempt.getStatus()) && ("reading".equalsIgnoreCase(testAttempt.getSkill()) || "listening".equalsIgnoreCase(testAttempt.getSkill()))) {
            reviewDTO.setBandScore(IeltsScoreConverter.convertToBand(testAttempt.getScore()));
        }

        List<QuestionReviewDTO> questionReviews = allTestQuestions.stream()
            .sorted(Comparator.comparing(Question::getQuestionNumber))
            .map(question -> {
                UserAnswer userAnswer = answersByQuestionId.get(question.getId());
                return new QuestionReviewDTO(
                    question.getQuestionNumber(),
                    question.getQuestionUid(),
                    question.getQuestionType(),
                    question.getQuestionContent(),
                    userAnswer != null ? userAnswer.getAnswerContent() : null,
                    question.getCorrectAnswer(),
                    userAnswer != null ? userAnswer.getCorrect() : null,
                    question.getExplanation()
                );
            })
            .collect(Collectors.toList());

        reviewDTO.setQuestions(questionReviews);

        return reviewDTO;
    }

    @Transactional
    public void cancelAttempt(Long attemptId, UUID userId) {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestAttemptService.class);
        logger.info("🔄 Cancelling test attempt: attemptId={}, userId={}", attemptId, userId);

        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAttempt not found with id: " + attemptId));

        if (!attempt.getUserId().equals(userId)) {
            throw new AccessDeniedException("User does not have permission to cancel this attempt.");
        }

        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            throw new IllegalStateException("Only in-progress attempts can be cancelled.");
        }

        attempt.setStatus("CANCELLED");
        attempt.setCompletedAt(OffsetDateTime.now()); // Mark completion time as now
        testAttemptRepository.save(attempt);

        logger.info("✅ Successfully cancelled test attempt: attemptId={}", attemptId);
    }

    @Transactional
    public void resumeAttempt(Long attemptId, UUID userId) {
        final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestAttemptService.class);
        logger.info("🔄 Resuming test attempt: attemptId={}, userId={}", attemptId, userId);

        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAttempt not found with id: " + attemptId));

        if (!attempt.getUserId().equals(userId)) {
            throw new AccessDeniedException("User does not have permission to resume this attempt.");
        }

        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            throw new IllegalStateException("Only in-progress attempts can be resumed.");
        }

        // By updating the timestamp, this attempt becomes the "latest" one
        attempt.setStartedAt(OffsetDateTime.now());
        testAttemptRepository.save(attempt);

        logger.info("✅ Successfully marked test attempt {} as latest for resuming.", attemptId);
    }

    @Transactional(readOnly = true)
    public List<UserAnswerDTO> getAnswersForAttempt(Long attemptId, UUID userId) {
        final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestAttemptService.class);
        logger.info("🔍 Fetching answers for attempt: attemptId={}, userId={}", attemptId, userId);

        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAttempt not found with id: " + attemptId));

        if (!attempt.getUserId().equals(userId)) {
            throw new AccessDeniedException("User does not have permission to view answers for this attempt.");
        }

        List<UserAnswer> userAnswers = userAnswerRepository.findByAttemptId(attemptId);
        logger.info("   -> Found {} answers for attempt {}.", userAnswers.size(), attemptId);

        return userAnswers.stream()
                .map(EntityMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAttempt(Long attemptId, UUID userId) {
        final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestAttemptService.class);
        logger.info("🗑️ Deleting test attempt: attemptId={}, userId={}", attemptId, userId);

        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAttempt not found with id: " + attemptId));

        if (!attempt.getUserId().equals(userId)) {
            throw new AccessDeniedException("User does not have permission to delete this attempt.");
        }

        // First, delete all associated UserAnswers to avoid foreign key constraint violations
        userAnswerRepository.deleteByAttemptId(attemptId);
        logger.info("   -> Deleted all user answers for attemptId={}", attemptId);

        // Then, delete the TestAttempt itself
        testAttemptRepository.deleteById(attemptId);
        logger.info("✅ Successfully deleted test attempt: attemptId={}", attemptId);
    }
}
