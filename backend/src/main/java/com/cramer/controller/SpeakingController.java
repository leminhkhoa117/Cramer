package com.cramer.controller;

import com.cramer.dto.CreateSpeakingSessionDTO;
import com.cramer.dto.PageDTO;
import com.cramer.dto.SaveSpeakingTranscriptDTO;
import com.cramer.dto.SpeakingGradingStatusDTO;
import com.cramer.dto.SpeakingHistoryItemDTO;
import com.cramer.dto.SpeakingResultDTO;
import com.cramer.dto.SpeakingSessionActionDTO;
import com.cramer.dto.SpeakingSessionDTO;
import com.cramer.dto.SpeakingTranscriptDTO;
import com.cramer.service.SpeakingSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/speaking")
@Tag(name = "Speaking API", description = "APIs for IELTS Speaking session runtime")
public class SpeakingController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(SpeakingController.class);

    private final SpeakingSessionService speakingSessionService;

    public SpeakingController(SpeakingSessionService speakingSessionService) {
        this.speakingSessionService = speakingSessionService;
    }

    @PostMapping("/sessions")
    @Operation(summary = "Create speaking session", description = "Create a new authenticated Speaking session runtime")
    public ResponseEntity<SpeakingSessionDTO> createSession(
            @Valid @RequestBody CreateSpeakingSessionDTO request,
            Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        logger.info("POST /api/speaking/sessions - user={}, testId={}, sessionMode={}", userId, request.getTestId(),
                request.getSessionMode());
        SpeakingSessionDTO response = speakingSessionService.createSession(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/sessions/{id}")
    @Operation(summary = "Get speaking session", description = "Get an authenticated user's Speaking session metadata")
    public ResponseEntity<SpeakingSessionDTO> getSession(
            @PathVariable @Min(1) Long id,
            Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        logger.info("GET /api/speaking/sessions/{} - user={}", id, userId);
        return ResponseEntity.ok(speakingSessionService.getSession(id, userId));
    }

    @PostMapping("/sessions/{id}/transcripts")
    @Operation(summary = "Save transcript", description = "Retry-safe upsert for a Speaking turn transcript")
    public ResponseEntity<SpeakingTranscriptDTO> saveTranscript(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody SaveSpeakingTranscriptDTO request,
            Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        logger.info("POST /api/speaking/sessions/{}/transcripts - user={}, turnIndex={}", id, userId,
                request.getTurnIndex());
        return ResponseEntity.ok(speakingSessionService.saveTranscript(id, request, userId));
    }

    @PostMapping("/sessions/{id}/complete")
    @Operation(summary = "Complete speaking session", description = "Finalize a Speaking session and queue evaluation")
    public ResponseEntity<SpeakingSessionActionDTO> completeSession(
            @PathVariable @Min(1) Long id,
            Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        logger.info("POST /api/speaking/sessions/{}/complete - user={}", id, userId);
        return ResponseEntity.ok(speakingSessionService.completeSession(id, userId));
    }

    @PostMapping("/sessions/{id}/abandon")
    @Operation(summary = "Abandon speaking session", description = "Finalize a Speaking session without deducting Lúa")
    public ResponseEntity<SpeakingSessionActionDTO> abandonSession(
            @PathVariable @Min(1) Long id,
            Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        logger.info("POST /api/speaking/sessions/{}/abandon - user={}", id, userId);
        return ResponseEntity.ok(speakingSessionService.abandonSession(id, userId));
    }

    @GetMapping("/sessions/{id}/grading-status")
    @Operation(summary = "Get grading status", description = "Poll the grading status for a Speaking session")
    public ResponseEntity<SpeakingGradingStatusDTO> getGradingStatus(
            @PathVariable @Min(1) Long id,
            Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        logger.info("GET /api/speaking/sessions/{}/grading-status - user={}", id, userId);
        return ResponseEntity.ok(speakingSessionService.getGradingStatus(id, userId));
    }

    @GetMapping("/sessions/{id}/results")
    @Operation(summary = "Get speaking results", description = "Get detailed Speaking grading results after grading completes")
    public ResponseEntity<SpeakingResultDTO> getResults(
            @PathVariable @Min(1) Long id,
            Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        logger.info("GET /api/speaking/sessions/{}/results - user={}", id, userId);
        return ResponseEntity.ok(speakingSessionService.getResults(id, userId));
    }

    @GetMapping("/history")
    @Operation(summary = "Get speaking history", description = "Get paginated Speaking session history for the authenticated user")
    public ResponseEntity<PageDTO<SpeakingHistoryItemDTO>> getHistory(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String status,
            Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        logger.info("GET /api/speaking/history - user={}, page={}, size={}, status={}", userId, page, size, status);
        return ResponseEntity.ok(speakingSessionService.getHistory(userId, pageable, status));
    }
}
