package com.cramer.billing.web;

import com.cramer.billing.service.QuotaService;
import com.cramer.billing.web.dto.CanAttemptView;
import com.cramer.billing.web.dto.QuotaStatusView;
import com.cramer.platform.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Monthly quota endpoints (SPEC-15 §5, §9). Tier-aware status + an attempt pre-check.
 */
@RestController
@RequestMapping("/api/quotas")
public class QuotaController {

    private final QuotaService quotas;
    private final CurrentUser currentUser;

    public QuotaController(QuotaService quotas, CurrentUser currentUser) {
        this.quotas = quotas;
        this.currentUser = currentUser;
    }

    @GetMapping
    public QuotaStatusView quotas() {
        return quotas.status(currentUser.requireUserId());
    }

    @GetMapping("/check")
    public QuotaStatusView check() {
        return quotas.status(currentUser.requireUserId());
    }

    @GetMapping("/can-attempt")
    public CanAttemptView canAttempt(@RequestParam String skill,
                                     @RequestParam(defaultValue = "false") boolean ai) {
        return quotas.canAttempt(currentUser.requireUserId(), skill, ai);
    }
}
