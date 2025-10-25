package com.cramer.service;

import com.cramer.entity.Question;
import com.cramer.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for Question entity.
 * Handles business logic for question management.
 */
@Service
@Transactional
public class QuestionService {

    private static final Logger logger = LoggerFactory.getLogger(QuestionService.class);

    private final QuestionRepository questionRepository;

    @Autowired
    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /**
     * Get all questions.
     * 
     * @return list of all questions
     */
    @Transactional(readOnly = true)
    public List<Question> getAllQuestions() {
        logger.info("Fetching all questions");
        return questionRepository.findAll();
    }

    /**
     * Get question by ID.
     * 
     * @param id the question ID
     * @return Optional containing the question if found
     */
    @Transactional(readOnly = true)
    public Optional<Question> getQuestionById(Long id) {
        logger.info("Fetching question by ID: {}", id);
        return questionRepository.findById(id);
    }

    /**
     * Get questions by section ID.
     * 
     * @param sectionId the section ID
     * @return list of questions in that section
     */
    @Transactional(readOnly = true)
    public List<Question> getQuestionsBySectionId(Long sectionId) {
        logger.info("Fetching questions for section ID: {}", sectionId);
        return questionRepository.findBySectionId(sectionId);
    }

    /**
     * Get question by unique identifier.
     * 
     * @param questionUid the unique question identifier
     * @return Optional containing the question if found
     */
    @Transactional(readOnly = true)
    public Optional<Question> getQuestionByUid(String questionUid) {
        logger.info("Fetching question by UID: {}", questionUid);
        return questionRepository.findByQuestionUid(questionUid);
    }

    /**
     * Get questions by type.
     * 
     * @param questionType the question type
     * @return list of questions of that type
     */
    @Transactional(readOnly = true)
    public List<Question> getQuestionsByType(String questionType) {
        logger.info("Fetching questions by type: {}", questionType);
        return questionRepository.findByQuestionType(questionType);
    }

    /**
     * Get questions by section and type.
     * 
     * @param sectionId the section ID
     * @param questionType the question type
     * @return list of matching questions
     */
    @Transactional(readOnly = true)
    public List<Question> getQuestionsBySectionAndType(Long sectionId, String questionType) {
        logger.info("Fetching questions for section {} with type {}", sectionId, questionType);
        return questionRepository.findBySectionIdAndQuestionType(sectionId, questionType);
    }

    /**
     * Get a specific question by section and number.
     * 
     * @param sectionId the section ID
     * @param questionNumber the question number
     * @return Optional containing the question if found
     */
    @Transactional(readOnly = true)
    public Optional<Question> getQuestionBySectionAndNumber(Long sectionId, Integer questionNumber) {
        logger.info("Fetching question {} from section {}", questionNumber, sectionId);
        return questionRepository.findBySectionIdAndQuestionNumber(sectionId, questionNumber);
    }

    /**
     * Get all distinct question types.
     * 
     * @return list of question types
     */
    @Transactional(readOnly = true)
    public List<String> getAllQuestionTypes() {
        logger.info("Fetching all distinct question types");
        return questionRepository.findAllDistinctQuestionTypes();
    }

    /**
     * Create a new question.
     * 
     * @param question the question to create
     * @return the created question
     * @throws IllegalArgumentException if question UID already exists
     */
    public Question createQuestion(Question question) {
        logger.info("Creating new question with UID: {}", question.getQuestionUid());
        
        if (questionRepository.existsByQuestionUid(question.getQuestionUid())) {
            logger.error("Question UID already exists: {}", question.getQuestionUid());
            throw new IllegalArgumentException("Question UID already exists: " + question.getQuestionUid());
        }
        
        Question savedQuestion = questionRepository.save(question);
        logger.info("Question created successfully with ID: {}", savedQuestion.getId());
        return savedQuestion;
    }

    /**
     * Update an existing question.
     * 
     * @param id the question ID
     * @param updatedQuestion the updated question data
     * @return the updated question
     * @throws IllegalArgumentException if question not found
     */
    public Question updateQuestion(Long id, Question updatedQuestion) {
        logger.info("Updating question with ID: {}", id);
        
        Question existingQuestion = questionRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Question not found with ID: {}", id);
                    return new IllegalArgumentException("Question not found with ID: " + id);
                });
        
        // Check UID conflict if changed
        if (!existingQuestion.getQuestionUid().equals(updatedQuestion.getQuestionUid()) 
                && questionRepository.existsByQuestionUid(updatedQuestion.getQuestionUid())) {
            logger.error("Question UID already taken: {}", updatedQuestion.getQuestionUid());
            throw new IllegalArgumentException("Question UID already taken: " + updatedQuestion.getQuestionUid());
        }
        
        existingQuestion.setSectionId(updatedQuestion.getSectionId());
        existingQuestion.setQuestionNumber(updatedQuestion.getQuestionNumber());
        existingQuestion.setQuestionUid(updatedQuestion.getQuestionUid());
        existingQuestion.setQuestionType(updatedQuestion.getQuestionType());
        existingQuestion.setQuestionContent(updatedQuestion.getQuestionContent());
        existingQuestion.setCorrectAnswer(updatedQuestion.getCorrectAnswer());
        
        Question savedQuestion = questionRepository.save(existingQuestion);
        logger.info("Question updated successfully: {}", id);
        return savedQuestion;
    }

    /**
     * Delete a question by ID.
     * 
     * @param id the question ID
     * @throws IllegalArgumentException if question not found
     */
    public void deleteQuestion(Long id) {
        logger.info("Deleting question with ID: {}", id);
        
        if (!questionRepository.existsById(id)) {
            logger.error("Question not found with ID: {}", id);
            throw new IllegalArgumentException("Question not found with ID: " + id);
        }
        
        questionRepository.deleteById(id);
        logger.info("Question deleted successfully: {}", id);
    }

    /**
     * Delete all questions in a section.
     * 
     * @param sectionId the section ID
     */
    public void deleteQuestionsBySection(Long sectionId) {
        logger.info("Deleting all questions in section: {}", sectionId);
        long count = questionRepository.countBySectionId(sectionId);
        questionRepository.deleteBySectionId(sectionId);
        logger.info("Deleted {} questions from section {}", count, sectionId);
    }

    /**
     * Count questions in a section.
     * 
     * @param sectionId the section ID
     * @return number of questions
     */
    @Transactional(readOnly = true)
    public long countBySectionId(Long sectionId) {
        logger.info("Counting questions in section: {}", sectionId);
        return questionRepository.countBySectionId(sectionId);
    }

    /**
     * Get total question count.
     * 
     * @return total number of questions
     */
    @Transactional(readOnly = true)
    public long getTotalQuestionCount() {
        logger.info("Getting total question count");
        return questionRepository.count();
    }
}
