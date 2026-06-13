package com.cramer.writing.web;

import com.cramer.platform.security.CurrentUser;
import com.cramer.writing.service.WritingSubmissionService;
import com.cramer.writing.web.dto.SaveDraftRequest;
import com.cramer.writing.web.dto.SubmitEssayRequest;
import com.cramer.writing.web.dto.WritingReviewView;
import com.cramer.writing.web.dto.WritingStatusResponse;
import com.cramer.writing.web.dto.WritingTaskReview;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
 * Writing endpoints (SPEC-13 §2). Owner-checked via the assessment attempt; the user id comes
 * from {@link CurrentUser}. Submit/regrade are rate-limited (429 on exceed).
 */
@RestController
@RequestMapping("/api/writing")
public class WritingController {

    private final WritingSubmissionService writing;
    private final CurrentUser currentUser;

    public WritingController(WritingSubmissionService writing, CurrentUser currentUser) {
        this.writing = writing;
        this.currentUser = currentUser;
    }

    @PostMapping("/draft/{attemptId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveDraft(@PathVariable Long attemptId,
                          @RequestParam(defaultValue = "1") int taskNumber,
                          @Valid @RequestBody SaveDraftRequest request) {
        writing.saveDraft(attemptId, taskNumber, request.essayText(), currentUser.requireUserId());
    }

    @PostMapping("/submit/{attemptId}")
    public WritingStatusResponse submit(@PathVariable Long attemptId,
                                        @Valid @RequestBody SubmitEssayRequest request) {
        return writing.submit(attemptId, request.essays(), currentUser.requireUserId());
    }

    @GetMapping("/status/{attemptId}")
    public WritingStatusResponse status(@PathVariable Long attemptId) {
        return writing.status(attemptId, currentUser.requireUserId());
    }

    @GetMapping("/review/{attemptId}")
    public WritingReviewView review(@PathVariable Long attemptId) {
        return writing.review(attemptId, currentUser.requireUserId());
    }

    @GetMapping("/submissions/{attemptId}")
    public List<WritingTaskReview> submissions(@PathVariable Long attemptId) {
        return writing.rawSubmissions(attemptId, currentUser.requireUserId());
    }

    @PostMapping("/regrade/{attemptId}")
    public WritingStatusResponse regrade(@PathVariable Long attemptId) {
        return writing.regrade(attemptId, currentUser.requireUserId());
    }
}
