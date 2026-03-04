package com.cramer.service;

import com.cramer.dto.SpeakingQuestionDTO;
import com.cramer.dto.SpeakingTopicDTO;
import com.cramer.entity.SpeakingQuestion;
import com.cramer.entity.SpeakingTopic;
import com.cramer.repository.SpeakingQuestionRepository;
import com.cramer.repository.SpeakingTopicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing speaking topics and questions.
 */
@Service
public class SpeakingQuestionService {

    private static final Logger logger = LoggerFactory.getLogger(SpeakingQuestionService.class);

    // Question counts per part for different session modes
    private static final int PART_1_QUESTIONS = 4;
    private static final int PART_2_QUESTIONS = 1;
    private static final int PART_3_QUESTIONS = 3;

    // Time allocations
    private static final int PART_2_PREP_TIME = 60;
    private static final int PART_2_TALK_TIME = 120;

    private final SpeakingTopicRepository topicRepository;
    private final SpeakingQuestionRepository questionRepository;

    @Autowired
    public SpeakingQuestionService(SpeakingTopicRepository topicRepository,
                                   SpeakingQuestionRepository questionRepository) {
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
    }

    /**
     * Get all active topics.
     */
    @Transactional(readOnly = true)
    public List<SpeakingTopicDTO> getAllActiveTopics() {
        logger.info("Fetching all active speaking topics");
        return topicRepository.findAllActiveTopics().stream()
                .map(this::toTopicDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get topic by ID.
     */
    @Transactional(readOnly = true)
    public SpeakingTopicDTO getTopicById(Long topicId) {
        return topicRepository.findById(topicId)
                .filter(SpeakingTopic::getIsActive)
                .map(this::toTopicDTO)
                .orElseThrow(() -> new IllegalArgumentException("Topic not found: " + topicId));
    }

    /**
     * Get questions for a topic and session mode.
     * Mode determines which parts are included:
     * - FULL: Part 1, 2, 3
     * - PART_1: Part 1 only
     * - PART_2: Part 2 only
     * - PART_3: Part 3 only
     * - PART_2_3: Part 2 and 3
     *
     * If topicId is null, uses generic/fallback questions.
     */
    @Transactional(readOnly = true)
    public List<SpeakingQuestionDTO> getQuestionsForSession(Long topicId, String mode) {
        logger.info("Getting questions for topic {} with mode {}", topicId, mode);

        List<SpeakingQuestionDTO> questions = new ArrayList<>();

        switch (mode.toUpperCase()) {
            case "FULL":
                questions.addAll(getPart1Questions(topicId));
                questions.addAll(getPart2Questions(topicId));
                questions.addAll(getPart3Questions(topicId));
                break;
            case "PART_1":
                questions.addAll(getPart1Questions(topicId));
                break;
            case "PART_2":
                questions.addAll(getPart2Questions(topicId));
                break;
            case "PART_3":
                questions.addAll(getPart3Questions(topicId));
                break;
            case "PART_2_3":
                questions.addAll(getPart2Questions(topicId));
                questions.addAll(getPart3Questions(topicId));
                break;
            default:
                throw new IllegalArgumentException("Invalid session mode: " + mode);
        }

        // If no questions found, generate generic placeholders
        if (questions.isEmpty()) {
            logger.warn("No questions found for topic {}, generating generic placeholders", topicId);
            questions = generateGenericQuestions(mode);
        }

        logger.info("Returning {} questions for session", questions.size());
        return questions;
    }

    /**
     * Generate generic placeholder questions when no topic is selected.
     */
    private List<SpeakingQuestionDTO> generateGenericQuestions(String mode) {
        List<SpeakingQuestionDTO> questions = new ArrayList<>();
        long idCounter = -1; // Negative IDs for generated questions

        switch (mode.toUpperCase()) {
            case "FULL":
            case "PART_1":
                // Part 1: Generic intro questions
                questions.add(createGenericQuestion(idCounter--, 1,
                    "Can you tell me your full name?", null));
                questions.add(createGenericQuestion(idCounter--, 1,
                    "Where are you from?", null));
                questions.add(createGenericQuestion(idCounter--, 1,
                    "Do you work or study?", null));
                questions.add(createGenericQuestion(idCounter--, 1,
                    "What do you enjoy doing in your free time?", null));
                if (!"PART_1".equals(mode.toUpperCase())) {
                    // Fall through to add Part 2 & 3
                }
                if ("PART_1".equals(mode.toUpperCase())) break;
                // Fall through for FULL mode
            case "PART_2":
            case "PART_2_3":
                // Part 2: Generic cue card
                questions.add(createGenericQuestion(idCounter--, 2,
                    "Describe something you learned recently",
                    List.of(
                        "What it was",
                        "How you learned it",
                        "Why you wanted to learn it",
                        "And explain how useful this skill/knowledge is"
                    )));
                if ("PART_2".equals(mode.toUpperCase())) break;
                // Fall through for FULL and PART_2_3
            case "PART_3":
                // Part 3: Discussion questions
                questions.add(createGenericQuestion(idCounter--, 3,
                    "What are the best ways for people to learn new things?", null));
                questions.add(createGenericQuestion(idCounter--, 3,
                    "Do you think online learning is as effective as traditional classroom learning?", null));
                questions.add(createGenericQuestion(idCounter--, 3,
                    "How has technology changed the way people learn?", null));
                break;
        }

        return questions;
    }

    /**
     * Create a generic question DTO without database backing.
     */
    private SpeakingQuestionDTO createGenericQuestion(long id, int part, String text, List<String> cueCardBullets) {
        SpeakingQuestionDTO dto = new SpeakingQuestionDTO();
        dto.setId(id);
        dto.setPart(part);
        dto.setText(text);
        dto.setTopicId(null);
        dto.setCueCardBullets(cueCardBullets);
        dto.setDifficulty("medium");

        // Set time allocations for Part 2
        if (part == 2) {
            dto.setPrepTimeSeconds(PART_2_PREP_TIME);
            dto.setTalkTimeSeconds(PART_2_TALK_TIME);
        }

        return dto;
    }

    /**
     * Get Part 1 questions (generic + topic-specific).
     */
    private List<SpeakingQuestionDTO> getPart1Questions(Long topicId) {
        List<SpeakingQuestionDTO> result = new ArrayList<>();

        // Part 1 has generic intro questions (no topic) + topic-related questions
        List<SpeakingQuestion> genericQuestions = questionRepository.findRandomGenericQuestions(1, 2);
        result.addAll(genericQuestions.stream().map(this::toQuestionDTO).collect(Collectors.toList()));

        // Only fetch topic-specific questions if topicId is provided
        if (topicId != null) {
            List<SpeakingQuestion> topicQuestions = questionRepository.findRandomQuestions(
                topicId, 1, PART_1_QUESTIONS - result.size());
            result.addAll(topicQuestions.stream().map(this::toQuestionDTO).collect(Collectors.toList()));
        }

        // Ensure we have enough questions (fallback to any Part 1 questions)
        if (result.size() < PART_1_QUESTIONS) {
            List<SpeakingQuestion> fallback = questionRepository.findByPartAndIsActiveTrueOrderById(1);
            for (SpeakingQuestion q : fallback) {
                if (result.size() >= PART_1_QUESTIONS) break;
                SpeakingQuestionDTO dto = toQuestionDTO(q);
                if (result.stream().noneMatch(existing -> existing.getId().equals(dto.getId()))) {
                    result.add(dto);
                }
            }
        }

        return result;
    }

    /**
     * Get Part 2 question (cue card).
     */
    private List<SpeakingQuestionDTO> getPart2Questions(Long topicId) {
        List<SpeakingQuestion> questions = new ArrayList<>();

        // Only fetch topic-specific questions if topicId is provided
        if (topicId != null) {
            questions = questionRepository.findRandomQuestions(topicId, 2, PART_2_QUESTIONS);
        }

        if (questions.isEmpty()) {
            // Fallback: get any Part 2 question
            questions = questionRepository.findByPartAndIsActiveTrueOrderById(2);
            if (!questions.isEmpty()) {
                questions = List.of(questions.get(0));
            }
        }

        return questions.stream()
                .map(q -> {
                    SpeakingQuestionDTO dto = toQuestionDTO(q);
                    dto.setPrepTimeSeconds(PART_2_PREP_TIME);
                    dto.setTalkTimeSeconds(PART_2_TALK_TIME);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get Part 3 questions (discussion).
     */
    private List<SpeakingQuestionDTO> getPart3Questions(Long topicId) {
        List<SpeakingQuestion> questions = new ArrayList<>();

        // Only fetch topic-specific questions if topicId is provided
        if (topicId != null) {
            questions = new ArrayList<>(questionRepository.findRandomQuestions(topicId, 3, PART_3_QUESTIONS));
        }

        if (questions.size() < PART_3_QUESTIONS) {
            // Fallback: get any Part 3 questions
            List<SpeakingQuestion> fallback = questionRepository.findByPartAndIsActiveTrueOrderById(3);
            for (SpeakingQuestion q : fallback) {
                if (questions.size() >= PART_3_QUESTIONS) break;
                if (questions.stream().noneMatch(existing -> existing.getId().equals(q.getId()))) {
                    questions.add(q);
                }
            }
        }

        return questions.stream()
                .map(this::toQuestionDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific question by ID.
     */
    @Transactional(readOnly = true)
    public SpeakingQuestionDTO getQuestionById(Long questionId) {
        return questionRepository.findById(questionId)
                .filter(SpeakingQuestion::getIsActive)
                .map(this::toQuestionDTO)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));
    }

    /**
     * Get questions by IDs.
     */
    @Transactional(readOnly = true)
    public List<SpeakingQuestionDTO> getQuestionsByIds(List<Long> questionIds) {
        return questionRepository.findByIdInAndIsActiveTrue(questionIds).stream()
                .map(this::toQuestionDTO)
                .collect(Collectors.toList());
    }

    // DTO Conversion methods
    private SpeakingTopicDTO toTopicDTO(SpeakingTopic topic) {
        return new SpeakingTopicDTO(
                topic.getId(),
                topic.getCode(),
                topic.getNameVi(),
                topic.getNameEn(),
                topic.getIcon(),
                topic.getColor()
        );
    }

    private SpeakingQuestionDTO toQuestionDTO(SpeakingQuestion question) {
        SpeakingQuestionDTO dto = new SpeakingQuestionDTO();
        dto.setId(question.getId());
        dto.setPart(question.getPart());
        dto.setText(question.getQuestionText());
        dto.setTopicId(question.getTopicId());
        dto.setCueCardBullets(question.getCueCardBullets());
        dto.setDifficulty(question.getDifficulty());

        // TTS audio fields
        dto.setExaminerAudioUrl(question.getExaminerAudioUrl());
        dto.setExaminerAudioDurationMs(question.getExaminerAudioDurationMs());

        // Set time allocations for Part 2
        if (question.getPart() == 2) {
            dto.setPrepTimeSeconds(PART_2_PREP_TIME);
            dto.setTalkTimeSeconds(PART_2_TALK_TIME);
        }

        return dto;
    }
}
