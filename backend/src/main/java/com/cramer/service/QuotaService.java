package com.cramer.service;

import com.cramer.dto.QuotaStatusDTO;

import java.util.UUID;

/**
 * Service interface for quota operations.
 * Manages monthly quota tracking for Cramerie (free tier) users.
 */
public interface QuotaService {

    /**
     * Get current quota status for a user.
     * Returns global and per-skill quota usage.
     * 
     * @param userId the user's UUID
     * @return quota status DTO
     */
    QuotaStatusDTO getQuotaStatus(UUID userId);

    /**
     * Increment attempt count for a user and skill.
     * Auto-creates quota rows for current month if missing.
     * 
     * @param userId the user's UUID
     * @param skill the skill (READING, LISTENING, WRITING, SPEAKING)
     * @param isAI whether this is an AI-graded attempt
     */
    void incrementAttempt(UUID userId, String skill, boolean isAI);

    /**
     * Check if user can make an attempt without exceeding quota.
     * Does NOT check Lua balance or premium status.
     * 
     * @param userId the user's UUID
     * @param skill the skill
     * @param isAI whether this is an AI-graded attempt
     * @return true if within free quota limits
     */
    boolean canAttempt(UUID userId, String skill, boolean isAI);

    /**
     * Check if global quota is exceeded.
     * 
     * @param userId the user's UUID
     * @param isAI whether checking AI quota
     * @return true if global cap is hit
     */
    boolean isGlobalCapHit(UUID userId, boolean isAI);

    /**
     * Check if local (per-skill) quota is exceeded.
     * 
     * @param userId the user's UUID
     * @param skill the skill
     * @param isAI whether checking AI quota
     * @return true if local cap is hit
     */
    boolean isLocalCapHit(UUID userId, String skill, boolean isAI);
}
