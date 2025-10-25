package com.cramer.controller;

import com.cramer.dto.QuestionDTO;
import com.cramer.entity.Question;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.service.QuestionService;
import com.cramer.util.EntityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for Question management.
 */
@RestController
@RequestMapping("/api/questions")
@CrossOrigin(origins = "*")
public class QuestionController {

    private static final Logger logger = LoggerFactory.getLogger(QuestionController.class);

    private final QuestionService questionService;

    @Autowired
    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    public ResponseEntity<List<QuestionDTO>> getAllQuestions() {
        List<QuestionDTO> questions = questionService.getAllQuestions()
                .stream().map(EntityMapper::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(questions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionDTO> getQuestionById(@PathVariable Long id) {
        Question question = questionService.getQuestionById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", id));
        return ResponseEntity.ok(EntityMapper.toDTO(question));
    }

    @GetMapping("/section/{sectionId}")
    public ResponseEntity<List<QuestionDTO>> getQuestionsBySectionId(@PathVariable Long sectionId) {
        List<QuestionDTO> questions = questionService.getQuestionsBySectionId(sectionId)
                .stream().map(EntityMapper::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(questions);
    }

    @GetMapping("/uid/{questionUid}")
    public ResponseEntity<QuestionDTO> getQuestionByUid(@PathVariable String questionUid) {
        Question question = questionService.getQuestionByUid(questionUid)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "questionUid", questionUid));
        return ResponseEntity.ok(EntityMapper.toDTO(question));
    }

    @GetMapping("/type/{questionType}")
    public ResponseEntity<List<QuestionDTO>> getQuestionsByType(@PathVariable String questionType) {
        List<QuestionDTO> questions = questionService.getQuestionsByType(questionType)
                .stream().map(EntityMapper::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(questions);
    }

    @GetMapping("/section/{sectionId}/type/{questionType}")
    public ResponseEntity<List<QuestionDTO>> getQuestionsBySectionAndType(
            @PathVariable Long sectionId, @PathVariable String questionType) {
        List<QuestionDTO> questions = questionService.getQuestionsBySectionAndType(sectionId, questionType)
                .stream().map(EntityMapper::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(questions);
    }

    @GetMapping("/types")
    public ResponseEntity<List<String>> getAllQuestionTypes() {
        return ResponseEntity.ok(questionService.getAllQuestionTypes());
    }

    @PostMapping
    public ResponseEntity<QuestionDTO> createQuestion(@RequestBody QuestionDTO questionDTO) {
        Question question = EntityMapper.toEntity(questionDTO);
        Question created = questionService.createQuestion(question);
        return ResponseEntity.status(HttpStatus.CREATED).body(EntityMapper.toDTO(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuestionDTO> updateQuestion(@PathVariable Long id, @RequestBody QuestionDTO questionDTO) {
        Question question = EntityMapper.toEntity(questionDTO);
        Question updated = questionService.updateQuestion(id, question);
        return ResponseEntity.ok(EntityMapper.toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count/section/{sectionId}")
    public ResponseEntity<Long> countBySectionId(@PathVariable Long sectionId) {
        return ResponseEntity.ok(questionService.countBySectionId(sectionId));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getQuestionCount() {
        return ResponseEntity.ok(questionService.getTotalQuestionCount());
    }
}
