package com.cramer.controller;

import com.cramer.dto.BillingResultDTO;
import com.cramer.dto.QuotaStatusDTO;
import com.cramer.service.QuotaBillingService;
import com.cramer.service.QuotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for quota operations.
 * Provides endpoints for quota status and pre-checks.
 */
@RestController
@RequestMapping("/api/quotas")
public class QuotaController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(QuotaController.class);

    private final QuotaService quotaService;
    private final QuotaBillingService quotaBillingService;

    @Autowired
    public QuotaController(QuotaService quotaService, QuotaBillingService quotaBillingService) {
        this.quotaService = quotaService;
        this.quotaBillingService = quotaBillingService;
    }

    /**
     * Get current quota status for authenticated user.
     * Returns global and per-skill quota usage.
     *
     * GET /api/quotas
     */
    @GetMapping
    public ResponseEntity<QuotaStatusDTO> getQuotaStatus(Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        logger.info("📊 GET /api/quotas - userId: {}", userId);

        QuotaStatusDTO status = quotaService.getQuotaStatus(userId);
        return ResponseEntity.ok(status);
    }

    /**
     * Pre-check if an attempt would be allowed.
     * Does NOT charge or increment quotas.
     *
     * GET /api/quotas/can-attempt?skill=WRITING&ai=true
     *
     * @param skill the skill (READING, LISTENING, WRITING, SPEAKING)
     * @param ai whether this is an AI-graded attempt
     */
    @GetMapping("/can-attempt")
    public ResponseEntity<BillingResultDTO> canAttempt(
            Authentication authentication,
            @RequestParam @jakarta.validation.constraints.Pattern(regexp = "^(READING|LISTENING|WRITING|SPEAKING)$", flags = jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE) String skill,
            @RequestParam(defaultValue = "false") boolean ai) {
        UUID userId = getCurrentUserId(authentication);
        logger.info("🔍 GET /api/quotas/can-attempt - userId: {}, skill: {}, ai: {}", userId, skill, ai);

        BillingResultDTO result = quotaBillingService.preCheckAttempt(userId, skill, ai);
        return ResponseEntity.ok(result);
    }

    /**
     * Alternative endpoint name for compatibility.
     * GET /api/quotas/check?skill=WRITING&isAI=true
     */
    @GetMapping("/check")
    public ResponseEntity<BillingResultDTO> checkAttempt(
            Authentication authentication,
            @RequestParam @jakarta.validation.constraints.Pattern(regexp = "^(READING|LISTENING|WRITING|SPEAKING)$", flags = jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE) String skill,
            @RequestParam(defaultValue = "false") boolean isAI) {
        return canAttempt(authentication, skill, isAI);
    }
}
