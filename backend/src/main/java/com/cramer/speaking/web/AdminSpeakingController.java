package com.cramer.speaking.web;

import com.cramer.platform.security.CurrentUser;
import com.cramer.speaking.service.AdminSpeakingService;
import com.cramer.speaking.service.SpeakingGradingTrigger;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Admin Speaking regrade endpoint (SPEC-14 §7, §8). Admin-gated by the security chain. Resets the
 * session to {@code completed} + audits, then dispatches grading (post-commit, async).
 */
@RestController
@RequestMapping("/api/admin/speaking")
public class AdminSpeakingController {

    private final AdminSpeakingService adminSpeaking;
    private final SpeakingGradingTrigger gradingTrigger;
    private final CurrentUser currentUser;

    public AdminSpeakingController(AdminSpeakingService adminSpeaking, SpeakingGradingTrigger gradingTrigger,
                                   CurrentUser currentUser) {
        this.adminSpeaking = adminSpeaking;
        this.gradingTrigger = gradingTrigger;
        this.currentUser = currentUser;
    }

    @PostMapping("/sessions/{id}/regrade")
    public Map<String, Object> regrade(@PathVariable long id,
                                       @RequestParam(required = false) String mode,
                                       @RequestParam(defaultValue = "false") boolean force,
                                       @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        long sessionId = adminSpeaking.regrade(currentUser.requireUserId(), id, mode, force, reason);
        gradingTrigger.enqueue(sessionId); // post-commit, async claim of the now-completed session
        return Map.of("success", true, "sessionId", sessionId, "status", "completed");
    }
}
