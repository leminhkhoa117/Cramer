package com.cramer.service;

import com.cramer.dto.GradingStatusDTO;
import com.cramer.dto.SubscriptionStatusDTO;
import com.cramer.dto.SubscriptionTierDTO;
import com.cramer.dto.UserSubscriptionDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for subscription operations.
 * Manages user subscriptions, tier information, and AI grading limits.
 */
public interface SubscriptionService {

    /**
     * Get all available subscription tiers.
     *
     * @return list of tier DTOs ordered by display order
     */
    List<SubscriptionTierDTO> getAllTiers();

    /**
     * Get a specific tier by code.
     *
     * @param code the tier code (cramerie, cramerich, cramerous)
     * @return the tier DTO
     */
    SubscriptionTierDTO getTierByCode(String code);

    /**
     * Get user's current active subscription.
     * Creates a free tier subscription if none exists.
     *
     * @param userId the user's UUID
     * @return the subscription DTO
     */
    UserSubscriptionDTO getUserSubscription(UUID userId);

    /**
     * Check if AI grading is allowed for a user.
     * Considers subscription limits and Lúa balance.
     *
     * @param userId the user's UUID
     * @return grading status with details
     */
    GradingStatusDTO checkAIGradingAllowed(UUID userId);

    /**
     * Increment AI grading usage count for a user.
     * Call after successful AI grading.
     *
     * @param userId the user's UUID
     * @return updated subscription DTO
     */
    UserSubscriptionDTO incrementAIGradingUsage(UUID userId);

    /**
     * Get remaining AI gradings for this billing period.
     *
     * @param userId the user's UUID
     * @return remaining count
     */
    int getMonthlyGradingsRemaining(UUID userId);

    /**
     * Initialize subscription for new user.
     * Creates free tier subscription and initial credits.
     *
     * @param userId the user's UUID
     * @return the created subscription DTO
     */
    UserSubscriptionDTO initializeNewUser(UUID userId);

    /**
     * Get daily chat limit for user based on subscription.
     * @deprecated Use {@link #getMonthlyChatLimit(UUID)} instead
     *
     * @param userId the user's UUID
     * @return daily chat limit (-1 for unlimited)
     */
    @Deprecated
    int getDailyChatLimit(UUID userId);

    /**
     * Get monthly chat limit for user based on subscription.
     *
     * @param userId the user's UUID
     * @return monthly chat limit (-1 for unlimited)
     */
    int getMonthlyChatLimit(UUID userId);

    /**
     * Get remaining chatbot messages for this month.
     *
     * @param userId the user's UUID
     * @return remaining count (-1 for unlimited)
     */
    int getRemainingChatMessages(UUID userId);

    /**
     * Increment chatbot usage for a user.
     * Call after successful chat message.
     *
     * @param userId the user's UUID
     */
    void incrementChatUsage(UUID userId);

    /**
     * Get comprehensive subscription status for the user.
     * Includes tier info, usage stats, credits, and payment history.
     *
     * @param userId the user's UUID
     * @return complete subscription status DTO
     */
    SubscriptionStatusDTO getSubscriptionStatus(UUID userId);

    /**
     * Set AI grading enabled/disabled for a user.
     * Only Cramerich+ users can enable AI grading.
     *
     * @param userId the user's UUID
     * @param enabled whether to enable AI grading
     * @return the new state of AI grading
     * @throws IllegalStateException if Cramerie user tries to enable
     */
    boolean setAiGradingEnabled(UUID userId, boolean enabled);

    /**
     * Check if AI grading is enabled for a user.
     *
     * @param userId the user's UUID
     * @return true if AI grading is enabled
     */
    boolean isAiGradingEnabled(UUID userId);
}
