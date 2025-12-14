package com.cramer.service;

import com.cramer.dto.BillingResultDTO;

import java.util.UUID;

/**
 * Service interface for quota billing operations.
 * Handles the billing flow for attempts that exceed free quotas.
 */
public interface QuotaBillingService {

    /**
     * Process billing for an attempt.
     * 
     * Flow:
     * 1. Premium users → always allowed, no charge
     * 2. Check global/local caps
     * 3. If cap hit → charge Lua or block if insufficient
     * 4. If within cap → allowed, no charge
     * 
     * This method also increments the quota counts.
     * 
     * @param userId the user's UUID
     * @param skill the skill (READING, LISTENING, WRITING, SPEAKING)
     * @param isAI whether this is an AI-graded attempt
     * @return billing result with allowed status, charge amount, and reason
     */
    BillingResultDTO processAttemptBilling(UUID userId, String skill, boolean isAI);

    /**
     * Pre-check if an attempt would be allowed.
     * Does NOT charge or increment quotas.
     * 
     * @param userId the user's UUID
     * @param skill the skill
     * @param isAI whether this is an AI-graded attempt
     * @return billing result preview (allowed status, would-be charge, reason)
     */
    BillingResultDTO preCheckAttempt(UUID userId, String skill, boolean isAI);

    // ===== BILLING RATES (constants) =====
    
    /**
     * Lua cost for exceeding ATTEMPT quota.
     */
    int ATTEMPT_OVERAGE_COST = 10;

    /**
     * Lua cost for exceeding ATTEMPT_AI quota.
     */
    int ATTEMPT_AI_OVERAGE_COST = 20;
}
