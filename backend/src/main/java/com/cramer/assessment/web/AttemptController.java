package com.cramer.assessment.web;

import com.cramer.assessment.service.AttemptReviewService;
import com.cramer.assessment.service.AttemptService;
import com.cramer.assessment.web.dto.AnswerView;
import com.cramer.assessment.web.dto.AttemptResultResponse;
import com.cramer.assessment.web.dto.AttemptReviewView;
import com.cramer.assessment.web.dto.AttemptView;
import com.cramer.assessment.web.dto.SaveProgressRequest;
import com.cramer.assessment.web.dto.SubmitAnswersRequest;
import com.cramer.platform.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Test-attempt lifecycle endpoints (SPEC-12 §2). The user id comes from {@link CurrentUser}; all
 * mutating ops are owner-checked in the services.
 */
@RestController
@RequestMapping("/api/test-attempts")
public class AttemptController {

    private final AttemptService attempts;
    private final AttemptReviewService review;
    private final CurrentUser currentUser;

    public AttemptController(AttemptService attempts, AttemptReviewService review, CurrentUser currentUser) {
        this.attempts = attempts;
        this.review = review;
        this.currentUser = currentUser;
    }

    @PostMapping("/start")
    public AttemptView start(@RequestParam("source") String source,
                             @RequestParam("test") String test,
                             @RequestParam("skill") String skill,
                             @RequestParam(value = "forceNew", defaultValue = "false") boolean forceNew) {
        return attempts.start(source, test, skill, currentUser.requireUserId(), forceNew);
    }

    @PostMapping("/{id}/progress")
    public AttemptView saveProgress(@PathVariable Long id, @Valid @RequestBody SaveProgressRequest request) {
        return attempts.saveProgress(id, currentUser.requireUserId(), request);
    }

    @PostMapping("/{id}/submit")
    public AttemptResultResponse submit(@PathVariable Long id, @Valid @RequestBody SubmitAnswersRequest request) {
        return attempts.submit(id, currentUser.requireUserId(), request);
    }

    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id) {
        attempts.cancel(id, currentUser.requireUserId());
    }

    @PostMapping("/{id}/resume")
    public AttemptView resume(@PathVariable Long id) {
        return attempts.resume(id, currentUser.requireUserId());
    }

    @PostMapping("/{id}/regrade")
    public AttemptResultResponse regrade(@PathVariable Long id) {
        return attempts.regrade(id, currentUser.requireUserId());
    }

    @GetMapping("/{id}/answers")
    public List<AnswerView> answers(@PathVariable Long id) {
        return attempts.getAnswers(id, currentUser.requireUserId());
    }

    @GetMapping("/{id}/review")
    public AttemptReviewView review(@PathVariable Long id) {
        return review.review(id, currentUser.requireUserId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        attempts.delete(id, currentUser.requireUserId());
    }
}
