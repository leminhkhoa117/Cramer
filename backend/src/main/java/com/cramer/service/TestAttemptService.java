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
        Optional<TestAttempt> existingAttempt = testAttemptRepository
                .findByUserIdAndExamSourceAndTestNumberAndSkill(userId, source, testNum, skill);

        if (existingAttempt.isPresent()) {
            return existingAttempt.get();
        } else {
            TestAttempt newAttempt = new TestAttempt();
            newAttempt.setUserId(userId);
            newAttempt.setExamSource(source);
            newAttempt.setTestNumber(testNum);
            newAttempt.setSkill(skill);
            return testAttemptRepository.save(newAttempt);
        }
    }

    @Transactional
    public TestResultDTO submitAttempt(Long testAttemptId, Map<Long, JsonNode> answers, UUID userId) {
        TestAttempt testAttempt = testAttemptRepository.findById(testAttemptId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAttempt not found with id: " + testAttemptId));

        // Verify ownership
        if (!testAttempt.getUserId().equals(userId)) {
            throw new AccessDeniedException("User does not have permission to submit this test attempt.");
        }

        // Allow re-submission by deleting old answers
        userAnswerRepository.deleteByAttemptId(testAttemptId);

        List<UserAnswer> userAnswers = new ArrayList<>();
        int correctCount = 0;

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
            userAnswer.setAttempt(testAttempt);
            userAnswer.setQuestion(question);
            userAnswer.setAnswerContent(answerValue);
            userAnswer.setCorrect(isCorrect);
            userAnswers.add(userAnswer);

            if (isCorrect) {
                correctCount++;
            }
        }

        userAnswerRepository.saveAll(userAnswers);

        testAttempt.setStatus("COMPLETED");
        testAttempt.setCompletedAt(OffsetDateTime.now());
        testAttempt.setScore(correctCount);
        testAttemptRepository.save(testAttempt);

        int totalQuestions = questionRepository.countBySection_ExamSourceAndSection_TestNumberAndSection_Skill(
            testAttempt.getExamSource(),
            Integer.valueOf(testAttempt.getTestNumber()),
            testAttempt.getSkill()
        );

        return new TestResultDTO(testAttempt.getId(), correctCount, totalQuestions, testAttempt.getStatus());
    }

    private boolean compareAnswers(JsonNode userAnswer, JsonNode correctAnswer) {
        if (userAnswer == null || userAnswer.get("value") == null || correctAnswer == null) {
            return false;
        }

        // The user's answer, e.g., "innovation"
        String userText = userAnswer.get("value").asText().trim().toLowerCase();

        // The correct answers are stored in your DB as a simple array of strings, e.g., ["innovation"]
        // So, the `correctAnswer` JsonNode is the array itself.
        if (!correctAnswer.isArray()) {
            // If the DB stores it as {"answers": [...]}, then use:
            // JsonNode correctAnswersArray = correctAnswer.get("answers");
            // if (correctAnswersArray == null || !correctAnswersArray.isArray()) return false;
            return false;
        }

        for (JsonNode correctNode : correctAnswer) {
            if (correctNode.asText().trim().toLowerCase().equals(userText)) {
                return true;
            }
        }

        return false;
    }
}
