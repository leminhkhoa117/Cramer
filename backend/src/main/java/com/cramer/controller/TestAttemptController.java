package com.cramer.controller;

import com.cramer.dto.AnswerSubmissionDTO;
import com.cramer.dto.TestResultDTO;
import com.cramer.entity.TestAttempt;
import com.cramer.service.TestAttemptService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/test-attempts")
@Tag(name = "Test Attempts API", description = "API for starting and submitting test attempts")
public class TestAttemptController {

    private final TestAttemptService testAttemptService;

    @Autowired
    public TestAttemptController(TestAttemptService testAttemptService) {
        this.testAttemptService = testAttemptService;
    }

    @PostMapping("/start")
    public ResponseEntity<TestAttempt> startOrGetAttempt(
            @RequestParam String source,
            @RequestParam String test,
            @RequestParam String skill,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());
        TestAttempt attempt = testAttemptService.startOrGetAttempt(source, test, skill, userId);
        return ResponseEntity.ok(attempt);
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<TestResultDTO> submitAttempt(@PathVariable Long id, @RequestBody AnswerSubmissionDTO submissionDTO, Authentication authentication) {
        String userIdStr = authentication.getName();
        UUID userId = UUID.fromString(userIdStr);
        TestResultDTO result = testAttemptService.submitAttempt(id, submissionDTO.getAnswers(), userId);
        return ResponseEntity.ok(result);
    }
}
