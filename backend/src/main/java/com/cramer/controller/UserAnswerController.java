package com.cramer.controller;

import com.cramer.dto.UserAnswerDTO;
import com.cramer.dto.UserStatsDTO;
import com.cramer.entity.UserAnswer;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.service.UserAnswerService;
import com.cramer.util.EntityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for UserAnswer management.
 */
@RestController
@RequestMapping("/api/user-answers")
@CrossOrigin(origins = "*")
public class UserAnswerController {

    private static final Logger logger = LoggerFactory.getLogger(UserAnswerController.class);

    private final UserAnswerService userAnswerService;

    @Autowired
    public UserAnswerController(UserAnswerService userAnswerService) {
        this.userAnswerService = userAnswerService;
    }

    @GetMapping
    public ResponseEntity<List<UserAnswerDTO>> getAllUserAnswers() {
        List<UserAnswerDTO> answers = userAnswerService.getAllUserAnswers()
                .stream().map(EntityMapper::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(answers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserAnswerDTO> getUserAnswerById(@PathVariable Long id) {
        UserAnswer answer = userAnswerService.getUserAnswerById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserAnswer", "id", id));
        return ResponseEntity.ok(EntityMapper.toDTO(answer));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserAnswerDTO>> getUserAnswersByUserId(@PathVariable UUID userId) {
        List<UserAnswerDTO> answers = userAnswerService.getUserAnswersByUserId(userId)
                .stream().map(EntityMapper::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(answers);
    }

    @GetMapping("/question/{questionId}")
    public ResponseEntity<List<UserAnswerDTO>> getAnswersByQuestionId(@PathVariable Long questionId) {
        List<UserAnswerDTO> answers = userAnswerService.getAnswersByQuestionId(questionId)
                .stream().map(EntityMapper::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(answers);
    }

    @GetMapping("/user/{userId}/question/{questionId}")
    public ResponseEntity<UserAnswerDTO> getUserAnswerForQuestion(
            @PathVariable UUID userId, @PathVariable Long questionId) {
        UserAnswer answer = userAnswerService.getUserAnswerForQuestion(userId, questionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Answer not found for user %s and question %d", userId, questionId)));
        return ResponseEntity.ok(EntityMapper.toDTO(answer));
    }

    @GetMapping("/user/{userId}/correct")
    public ResponseEntity<List<UserAnswerDTO>> getCorrectAnswers(@PathVariable UUID userId) {
        List<UserAnswerDTO> answers = userAnswerService.getCorrectAnswers(userId)
                .stream().map(EntityMapper::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(answers);
    }

    @GetMapping("/user/{userId}/incorrect")
    public ResponseEntity<List<UserAnswerDTO>> getIncorrectAnswers(@PathVariable UUID userId) {
        List<UserAnswerDTO> answers = userAnswerService.getIncorrectAnswers(userId)
                .stream().map(EntityMapper::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(answers);
    }

    @GetMapping("/user/{userId}/recent")
    public ResponseEntity<List<UserAnswerDTO>> getRecentAnswers(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "10") int limit) {
        List<UserAnswerDTO> answers = userAnswerService.getRecentAnswers(userId, limit)
                .stream().map(EntityMapper::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(answers);
    }

    @GetMapping("/user/{userId}/stats")
    public ResponseEntity<UserStatsDTO> getUserStatistics(@PathVariable UUID userId) {
        UserAnswerService.UserStats stats = userAnswerService.getUserStatistics(userId);
        return ResponseEntity.ok(EntityMapper.toDTO(stats));
    }

    @GetMapping("/user/{userId}/accuracy")
    public ResponseEntity<Double> getUserAccuracy(@PathVariable UUID userId) {
        return ResponseEntity.ok(userAnswerService.calculateUserAccuracy(userId));
    }

    @PostMapping
    public ResponseEntity<UserAnswerDTO> submitAnswer(@RequestBody UserAnswerDTO answerDTO) {
        UserAnswer answer = EntityMapper.toEntity(answerDTO);
        UserAnswer created = userAnswerService.submitAnswer(answer);
        return ResponseEntity.status(HttpStatus.CREATED).body(EntityMapper.toDTO(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserAnswerDTO> updateAnswer(
            @PathVariable Long id, @RequestBody UserAnswerDTO answerDTO) {
        UserAnswer answer = EntityMapper.toEntity(answerDTO);
        UserAnswer updated = userAnswerService.updateAnswer(id, answer);
        return ResponseEntity.ok(EntityMapper.toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnswer(@PathVariable Long id) {
        userAnswerService.deleteAnswer(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteUserAnswers(@PathVariable UUID userId) {
        userAnswerService.deleteUserAnswers(userId);
        return ResponseEntity.noContent().build();
    }
}
