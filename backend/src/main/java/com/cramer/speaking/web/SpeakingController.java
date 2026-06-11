package com.cramer.speaking.web;

import com.cramer.platform.security.CurrentUser;
import com.cramer.platform.web.PageResponse;
import com.cramer.speaking.service.SpeakingSessionService;
import com.cramer.speaking.web.dto.CreateSessionRequest;
import com.cramer.speaking.web.dto.SaveTranscriptRequest;
import com.cramer.speaking.web.dto.SpeakingSessionView;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Speaking session endpoints (SPEC-14 §8). Realtime examiner audio uses the WS endpoint. */
@RestController
@RequestMapping("/api/speaking")
public class SpeakingController {

    private final SpeakingSessionService speaking;
    private final CurrentUser currentUser;

    public SpeakingController(SpeakingSessionService speaking, CurrentUser currentUser) {
        this.speaking = speaking;
        this.currentUser = currentUser;
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public SpeakingSessionView create(@Valid @RequestBody CreateSessionRequest request) {
        return speaking.create(currentUser.requireUserId(), request);
    }

    @GetMapping("/sessions/{id}")
    public SpeakingSessionView get(@PathVariable Long id) {
        return speaking.get(currentUser.requireUserId(), id);
    }

    @PostMapping("/sessions/{id}/transcripts")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveTranscript(@PathVariable Long id, @Valid @RequestBody SaveTranscriptRequest request) {
        speaking.saveTranscript(currentUser.requireUserId(), id, request);
    }

    @PostMapping("/sessions/{id}/complete")
    public SpeakingSessionView complete(@PathVariable Long id,
                                        @RequestParam(required = false) Integer durationSeconds) {
        return speaking.complete(currentUser.requireUserId(), id, durationSeconds);
    }

    @PostMapping("/sessions/{id}/abandon")
    public SpeakingSessionView abandon(@PathVariable Long id) {
        return speaking.abandon(currentUser.requireUserId(), id);
    }

    @GetMapping("/sessions/{id}/grading-status")
    public Map<String, String> gradingStatus(@PathVariable Long id) {
        return Map.of("status", speaking.gradingStatus(currentUser.requireUserId(), id));
    }

    @GetMapping("/sessions/{id}/results")
    public JsonNode results(@PathVariable Long id) {
        return speaking.results(currentUser.requireUserId(), id);
    }

    @GetMapping("/history")
    public PageResponse<SpeakingSessionView> history(@RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size,
                                                     @RequestParam(required = false) String status) {
        Page<SpeakingSessionView> p = speaking.history(currentUser.requireUserId(), page, size, status);
        return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}
