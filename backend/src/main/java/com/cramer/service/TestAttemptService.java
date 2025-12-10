package com.cramer.service;

import com.cramer.dto.AnswerSubmissionDTO;
import com.cramer.dto.SaveProgressDTO;
import com.cramer.dto.TestResultDTO;
import com.cramer.dto.TestReviewDTO;
import com.cramer.dto.UserAnswerDTO;
import com.cramer.dto.QuestionReviewDTO;
import com.cramer.dto.SectionReviewDTO;
import com.cramer.entity.Question;
import com.cramer.entity.Section;
import com.cramer.entity.TestAttempt;
import com.cramer.entity.UserAnswer;
import com.cramer.repository.QuestionRepository;
import com.cramer.repository.SectionRepository;
import com.cramer.util.IeltsScoreConverter;
import com.cramer.util.EntityMapper;
import com.cramer.repository.TestAttemptRepository;
import com.cramer.repository.UserAnswerRepository;
import com.cramer.repository.WritingSubmissionRepository;
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
    private final WritingSubmissionRepository writingSubmissionRepository;
    private final QuestionRepository questionRepository;
    private final SectionRepository sectionRepository;
    private final ObjectMapper objectMapper;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public TestAttemptService(TestAttemptRepository testAttemptRepository,
                              UserAnswerRepository userAnswerRepository,
                              WritingSubmissionRepository writingSubmissionRepository,
                              QuestionRepository questionRepository,
                              SectionRepository sectionRepository,
                              ObjectMapper objectMapper) {
        this.testAttemptRepository = testAttemptRepository;
        this.userAnswerRepository = userAnswerRepository;
        this.writingSubmissionRepository = writingSubmissionRepository;
        this.questionRepository = questionRepository;
        this.sectionRepository = sectionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TestAttempt startOrGetAttempt(String source, String testNum, String skill, UUID userId) {
        return startOrGetAttempt(source, testNum, skill, userId, false);
    }
    
    @Transactional
    public TestAttempt startOrGetAttempt(String source, String testNum, String skill, UUID userId, boolean forceNew) {
        final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestAttemptService.class);
        logger.info("--- NEW REQUEST ---");
        logger.info("🎯 [1] Starting startOrGetAttempt: userId={}, source={}, testNum={}, skill={}, forceNew={}", 
                        userId, source, testNum, skill, forceNew);
        
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
            
            logger.info("🎯 [2] Acquiring pessimistic lock to prevent race conditions");
            // Acquire pessimistic lock to prevent race conditions in concurrent requests
            // This ensures only one thread can create/modify attempts for this user+test combination at a time
            Optional<TestAttempt> lockedAttempt = testAttemptRepository
                    .findAndLockByUserIdAndExamSourceAndTestNumberAndSkill(
                            userId, trimmedSource, trimmedTestNum, trimmedSkill);
            logger.info("🎯 [2a] Lock acquired. Existing locked attempt: {}", lockedAttempt.isPresent() ? lockedAttempt.get().getId() : "none");
            
            // Now safely fetch all attempts (we hold the lock)
            List<TestAttempt> allAttempts = testAttemptRepository
                    .findByUserIdAndExamSourceAndTestNumberAndSkillOrderByStartedAtDesc(userId, trimmedSource, trimmedTestNum, trimmedSkill);
            
            // Find the latest attempt and all IN_PROGRESS attempts
            TestAttempt latestAttempt = allAttempts.isEmpty() ? null : allAttempts.get(0);
            List<TestAttempt> inProgressAttempts = allAttempts.stream()
                    .filter(a -> "IN_PROGRESS".equals(a.getStatus()))
                    .toList();
            
            logger.info("🎯 [3] Found {} total attempts, {} IN_PROGRESS", allAttempts.size(), inProgressAttempts.size());
            
            // If forceNew is true, cancel ALL IN_PROGRESS attempts and create new
            if (forceNew && !inProgressAttempts.isEmpty()) {
                logger.info("   -> forceNew=true. Cancelling all {} IN_PROGRESS attempts.", inProgressAttempts.size());
                for (TestAttempt oldAttempt : inProgressAttempts) {
                    logger.info("   -> Cancelling IN_PROGRESS attempt ID: {}", oldAttempt.getId());
                    oldAttempt.setStatus("CANCELLED");
                    testAttemptRepository.save(oldAttempt);
                }
                return createNewAttempt(userId, trimmedSource, trimmedTestNum, trimmedSkill, logger);
            }
            
            // If there are multiple IN_PROGRESS attempts, cancel all but the most recent one
            if (inProgressAttempts.size() > 1) {
                logger.info("   -> Found multiple IN_PROGRESS attempts. Keeping only the most recent.");
                TestAttempt mostRecentInProgress = inProgressAttempts.get(0); // Already sorted by startedAt DESC
                for (int i = 1; i < inProgressAttempts.size(); i++) {
                    TestAttempt oldAttempt = inProgressAttempts.get(i);
                    logger.info("   -> Cancelling stale IN_PROGRESS attempt ID: {}", oldAttempt.getId());
                    oldAttempt.setStatus("CANCELLED");
                    testAttemptRepository.save(oldAttempt);
                }
            }
            
            // Also check: if there's a COMPLETED attempt that's MORE RECENT than an IN_PROGRESS,
            // the IN_PROGRESS is stale and should be cancelled
            if (!inProgressAttempts.isEmpty() && latestAttempt != null && "COMPLETED".equals(latestAttempt.getStatus())) {
                logger.info("   -> Latest attempt is COMPLETED but there are stale IN_PROGRESS attempts. Cancelling them.");
                for (TestAttempt staleAttempt : inProgressAttempts) {
                    logger.info("   -> Cancelling stale IN_PROGRESS attempt ID: {}", staleAttempt.getId());
                    staleAttempt.setStatus("CANCELLED");
                    testAttemptRepository.save(staleAttempt);
                }
                // Return the COMPLETED attempt - frontend will decide what to do (show modal or redirect)
                logger.info("   -> Returning existing COMPLETED attempt ID: {}", latestAttempt.getId());
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
                return detachedAttempt;
            }

            if (latestAttempt != null) {
                logger.info("🎯 [4] Latest attempt ID: {}, Status: {}", latestAttempt.getId(), latestAttempt.getStatus());

                if ("COMPLETED".equals(latestAttempt.getStatus())) {
                    if (forceNew) {
                        logger.info("   -> Status is 'COMPLETED' and forceNew=true. Creating new attempt.");
                        return createNewAttempt(userId, trimmedSource, trimmedTestNum, trimmedSkill, logger);
                    } else {
                        // Return the COMPLETED attempt - frontend will decide what to do
                        logger.info("   -> Status is 'COMPLETED' and forceNew=false. Returning existing COMPLETED attempt.");
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
                        return detachedAttempt;
                    }
                }
                
                if ("CANCELLED".equals(latestAttempt.getStatus())) {
                    logger.info("   -> Status is 'CANCELLED'. Proceeding to create a new attempt.");
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
        
        // Cancel any existing IN_PROGRESS attempts for this test before creating a new one
        List<TestAttempt> existingInProgressAttempts = testAttemptRepository
                .findByUserIdAndExamSourceAndTestNumberAndSkillAndStatus(userId, source, testNum, skill, "IN_PROGRESS");
        
        for (TestAttempt oldAttempt : existingInProgressAttempts) {
            logger.info("   -> Cancelling old IN_PROGRESS attempt ID: {}", oldAttempt.getId());
            oldAttempt.setStatus("CANCELLED");
            testAttemptRepository.save(oldAttempt);
        }
        
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

        // 4. Fetch all sections for this test
        List<Section> sections = sectionRepository.findSectionsForTest(
            testAttempt.getExamSource(),
            Integer.valueOf(testAttempt.getTestNumber()),
            testAttempt.getSkill()
        );

        // 5. Group questions by sectionId
        Map<Long, List<Question>> questionsBySectionId = allTestQuestions.stream()
                .collect(Collectors.groupingBy(Question::getSectionId));

        // 6. Build the DTO
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

        // 7. Build flat list of QuestionReviewDTOs (for backward compatibility)
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

        // 8. Build SectionReviewDTOs with grouped questions
        List<SectionReviewDTO> sectionReviews = sections.stream()
            .sorted(Comparator.comparing(Section::getPartNumber))
            .map(section -> {
                List<Question> sectionQuestions = questionsBySectionId.getOrDefault(section.getId(), List.of());
                
                List<QuestionReviewDTO> sectionQuestionReviews = sectionQuestions.stream()
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

                return new SectionReviewDTO(
                    section.getId(),
                    section.getPartNumber(),
                    section.getPassageText(),
                    section.getDisplayContentUrl(),
                    section.getAudioUrl(),
                    section.getSectionLayout(),
                    sectionQuestionReviews
                );
            })
            .collect(Collectors.toList());

        reviewDTO.setSections(sectionReviews);

        return reviewDTO;
    }

    @Transactional
    public void cancelAttempt(Long attemptId, UUID userId) {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestAttemptService.class);
        logger.info("🗑️ Cancelling and deleting test attempt: attemptId={}, userId={}", attemptId, userId);

        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAttempt not found with id: " + attemptId));

        if (!attempt.getUserId().equals(userId)) {
            throw new AccessDeniedException("User does not have permission to cancel this attempt.");
        }

        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            throw new IllegalStateException("Only in-progress attempts can be cancelled.");
        }

        // First delete all associated user answers to avoid FK constraint violations
        userAnswerRepository.deleteByAttemptId(attemptId);
        logger.info("   -> Deleted all user answers for attemptId={}", attemptId);

        // Delete all associated writing submissions (for writing skill tests)
        writingSubmissionRepository.deleteByAttemptId(attemptId);
        logger.info("   -> Deleted all writing submissions for attemptId={}", attemptId);

        // Then delete the attempt itself using explicit JPQL query
        testAttemptRepository.deleteAttemptById(attemptId);
        logger.info("✅ Successfully cancelled and deleted test attempt: attemptId={}", attemptId);
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

    /**
     * Re-grade a completed test attempt by re-scoring all existing user answers
     * against the current correct answers in the database.
     * Useful when answer keys have been updated.
     */
    @Transactional
    public TestResultDTO regradeAttempt(Long attemptId, UUID userId) {
        final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestAttemptService.class);
        logger.info("🔄 Re-grading test attempt: attemptId={}, userId={}", attemptId, userId);

        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAttempt not found with id: " + attemptId));

        if (!attempt.getUserId().equals(userId)) {
            throw new AccessDeniedException("User does not have permission to re-grade this attempt.");
        }

        if (!"COMPLETED".equals(attempt.getStatus())) {
            throw new IllegalStateException("Only completed attempts can be re-graded.");
        }

        // Fetch all user answers for this attempt
        List<UserAnswer> userAnswers = userAnswerRepository.findByAttemptId(attemptId);
        logger.info("   -> Found {} user answers to re-grade", userAnswers.size());

        int correctCount = 0;

        // Re-grade each answer against the current correct answer
        for (UserAnswer userAnswer : userAnswers) {
            Question question = userAnswer.getQuestion();
            boolean isCorrect = compareAnswers(userAnswer.getAnswerContent(), question.getCorrectAnswer());
            userAnswer.setCorrect(isCorrect);
            if (isCorrect) {
                correctCount++;
            }
        }

        // Save all updated answers
        userAnswerRepository.saveAll(userAnswers);
        logger.info("   -> Re-graded answers: {} correct out of {}", correctCount, userAnswers.size());

        // Update the attempt score
        attempt.setScore(correctCount);
        testAttemptRepository.save(attempt);

        // Get total question count for the response
        int totalQuestions = questionRepository.countBySection_ExamSourceAndSection_TestNumberAndSection_Skill(
            attempt.getExamSource(),
            Integer.valueOf(attempt.getTestNumber()),
            attempt.getSkill()
        );

        logger.info("✅ Re-grading completed: score={}/{}", correctCount, totalQuestions);
        return new TestResultDTO(attempt.getId(), correctCount, totalQuestions, attempt.getStatus());
    }
}
