package com.cramer.service;

import com.cramer.dto.AnswerSubmissionDTO;
import com.cramer.dto.TestResultDTO;
import com.cramer.entity.Question;
import com.cramer.entity.TestAttempt;
import com.cramer.entity.UserAnswer;
import com.cramer.repository.QuestionRepository;
import com.cramer.repository.TestAttemptRepository;
import com.cramer.repository.UserAnswerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cramer.exception.ResourceNotFoundException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class TestAttemptService {

    private final TestAttemptRepository testAttemptRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final QuestionRepository questionRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public TestAttemptService(TestAttemptRepository testAttemptRepository,
                              UserAnswerRepository userAnswerRepository,
                              QuestionRepository questionRepository) {
        this.testAttemptRepository = testAttemptRepository;
        this.userAnswerRepository = userAnswerRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional
    public TestAttempt startOrGetAttempt(String source, String testNum, String skill, UUID userId) {
        try {
            org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestAttemptService.class);
            logger.info("🎯 Starting/Getting test attempt: userId={}, source={}, testNum={}, skill={}", 
                        userId, source, testNum, skill);
            
            // Validate inputs
            if (userId == null) {
                throw new IllegalArgumentException("User ID cannot be null");
            }
            if (source == null || source.trim().isEmpty()) {
                throw new IllegalArgumentException("Source cannot be null or empty");
            }
            if (testNum == null || testNum.trim().isEmpty()) {
                throw new IllegalArgumentException("Test number cannot be null or empty");
            }
            if (skill == null || skill.trim().isEmpty()) {
                throw new IllegalArgumentException("Skill cannot be null or empty");
            }
            
            Optional<TestAttempt> existingAttempt = testAttemptRepository
                    .findByUserIdAndExamSourceAndTestNumberAndSkill(userId, source, testNum, skill);

            if (existingAttempt.isPresent()) {
                logger.info("✅ Found existing attempt with id={}", existingAttempt.get().getId());
                return existingAttempt.get();
            } else {
                logger.info("📝 Creating new test attempt");
                TestAttempt newAttempt = new TestAttempt();
                newAttempt.setUserId(userId);
                newAttempt.setExamSource(source);
                newAttempt.setTestNumber(testNum);
                newAttempt.setSkill(skill);
                TestAttempt saved = testAttemptRepository.save(newAttempt);
                logger.info("✅ Created new attempt with id={}", saved.getId());
                return saved;
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestAttemptService.class);
            logger.error("❌ Error in startOrGetAttempt: userId={}, source={}, testNum={}, skill={}", 
                        userId, source, testNum, skill, e);
            throw new RuntimeException("Failed to start/get test attempt: " + e.getMessage(), e);
        }
    }

    @Transactional
    public TestResultDTO submitAttempt(Long testAttemptId, Map<Long, JsonNode> answers, UUID userId) {
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
        for (Map.Entry<Long, JsonNode> entry : answers.entrySet()) {
            Long questionId = entry.getKey();
            JsonNode answerValue = entry.getValue();

            if (answerValue == null || answerValue.get("value") == null || answerValue.get("value").asText().isEmpty()) {
                continue; // Skip unanswered questions
            }
            
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));

            boolean isCorrect = compareAnswers(answerValue, question.getCorrectAnswer());

            UserAnswer userAnswer = new UserAnswer();
            userAnswer.setUserId(userId);
            userAnswer.setAttempt(testAttempt);
            userAnswer.setQuestion(question);
            userAnswer.setAnswerContent(answerValue);
            userAnswer.setUserAnswer(answerValue.get("value").asText()); // Set the text value
            userAnswer.setCorrect(isCorrect);
            userAnswers.add(userAnswer);

            if (isCorrect) {
                correctCount++;
            }
        }
        long gradingTime = System.currentTimeMillis() - startGrading;
        logger.info("✅ Graded {} answers in {}ms", answers.size(), gradingTime);

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
        if (userAnswer == null || userAnswer.get("value") == null || correctAnswer == null) {
            return false;
        }

        // The user's answer, e.g., "innovation" or "NOT_GIVEN"
        // Normalize: replace underscore with space, trim, lowercase
        String userText = userAnswer.get("value").asText()
                .replace("_", " ")
                .trim()
                .toLowerCase();

        // The correct answers are stored in your DB as a simple array of strings, e.g., ["innovation"] or ["NOT GIVEN"]
        // So, the `correctAnswer` JsonNode is the array itself.
        if (!correctAnswer.isArray()) {
            // If the DB stores it as {"answers": [...]}, then use:
            // JsonNode correctAnswersArray = correctAnswer.get("answers");
            // if (correctAnswersArray == null || !correctAnswersArray.isArray()) return false;
            return false;
        }

        for (JsonNode correctNode : correctAnswer) {
            String correctText = correctNode.asText()
                    .replace("_", " ")
                    .trim()
                    .toLowerCase();
            
            if (correctText.equals(userText)) {
                return true;
            }
        }

        return false;
    }
}
