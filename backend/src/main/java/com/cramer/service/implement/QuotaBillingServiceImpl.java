package com.cramer.service.implement;

import com.cramer.dto.BillingResultDTO;
import com.cramer.dto.UserSubscriptionDTO;
import com.cramer.entity.CreditTransaction;
import com.cramer.service.CreditService;
import com.cramer.service.QuotaBillingService;
import com.cramer.service.QuotaService;
import com.cramer.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * Implementation of QuotaBillingService.
 * Handles billing flow for attempts that exceed free quotas.
 */
@Service
@Transactional
public class QuotaBillingServiceImpl implements QuotaBillingService {

    private static final Logger logger = LoggerFactory.getLogger(QuotaBillingServiceImpl.class);

    // Premium tier codes (unlimited access)
    private static final Set<String> PREMIUM_TIERS = Set.of("cramerich", "cramerous");

    private final QuotaService quotaService;
    private final SubscriptionService subscriptionService;
    private final CreditService creditService;

    @Autowired
    public QuotaBillingServiceImpl(
            QuotaService quotaService,
            @Lazy SubscriptionService subscriptionService,
            @Lazy CreditService creditService) {
        this.quotaService = quotaService;
        this.subscriptionService = subscriptionService;
        this.creditService = creditService;
    }

    @Override
    public BillingResultDTO processAttemptBilling(UUID userId, String skill, boolean isAI) {
        logger.info("💳 Processing attempt billing for user {}, skill {}, isAI={}", userId, skill, isAI);

        // Step 1: Check if premium user (Cramerich/Cramerous)
        if (isPremiumUser(userId)) {
            logger.info("⭐ User {} is premium, allowing attempt without charge", userId);
            quotaService.incrementAttempt(userId, skill, isAI);
            return BillingResultDTO.allowed();
        }

        // Step 2: Check global and local caps
        boolean globalCapHit = quotaService.isGlobalCapHit(userId, isAI);
        boolean localCapHit = quotaService.isLocalCapHit(userId, skill, isAI);

        // Step 3: Determine billing
        if (globalCapHit || localCapHit) {
            int cost = isAI ? ATTEMPT_AI_OVERAGE_COST : ATTEMPT_OVERAGE_COST;
            
            // Check if user has enough Lua
            if (!creditService.hasEnoughCredits(userId, cost)) {
                logger.warn("❌ User {} has insufficient Lua for overage ({} required)", userId, cost);
                if (globalCapHit) {
                    return BillingResultDTO.blockedGlobal(cost);
                } else {
                    return BillingResultDTO.blockedLocal(skill, cost);
                }
            }

            // Deduct Lua
            String description = String.format("Quota overage: %s (%s)", 
                    skill, isAI ? "AI grading" : "attempt");
            creditService.spendCredits(userId, cost, CreditTransaction.Category.AI_GRADING, description);
            
            logger.info("💰 Charged {} Lua to user {} for quota overage", cost, userId);
            
            // Increment quota after successful charge
            quotaService.incrementAttempt(userId, skill, isAI);
            
            return BillingResultDTO.charged(cost);
        }

        // Step 4: Within free quota
        logger.info("✅ User {} is within free quota, allowing attempt", userId);
        quotaService.incrementAttempt(userId, skill, isAI);
        return BillingResultDTO.allowed();
    }

    @Override
    @Transactional(readOnly = true)
    public BillingResultDTO preCheckAttempt(UUID userId, String skill, boolean isAI) {
        logger.info("🔍 Pre-checking attempt for user {}, skill {}, isAI={}", userId, skill, isAI);

        // Step 1: Premium users always allowed
        if (isPremiumUser(userId)) {
            return BillingResultDTO.allowed();
        }

        // Step 2: Check caps
        boolean globalCapHit = quotaService.isGlobalCapHit(userId, isAI);
        boolean localCapHit = quotaService.isLocalCapHit(userId, skill, isAI);

        if (globalCapHit || localCapHit) {
            int cost = isAI ? ATTEMPT_AI_OVERAGE_COST : ATTEMPT_OVERAGE_COST;
            
            if (!creditService.hasEnoughCredits(userId, cost)) {
                if (globalCapHit) {
                    return BillingResultDTO.blockedGlobal(cost);
                } else {
                    return BillingResultDTO.blockedLocal(skill, cost);
                }
            }
            
            // Would be charged but can afford it
            return BillingResultDTO.builder()
                    .allowed(true)
                    .luaCharged(cost)
                    .reason("Sẽ tính phí " + cost + " Lua")
                    .build();
        }

        // Within free quota
        return BillingResultDTO.allowed();
    }

    // ===== PRIVATE HELPERS =====

    /**
     * Check if user is on a premium tier (Cramerich or Cramerous).
     */
    private boolean isPremiumUser(UUID userId) {
        try {
            UserSubscriptionDTO subscription = subscriptionService.getUserSubscription(userId);
            if (subscription == null || subscription.getTier() == null) {
                return false;
            }
            String tierCode = subscription.getTier().getCode();
            return tierCode != null && PREMIUM_TIERS.contains(tierCode.toLowerCase());
        } catch (Exception e) {
            logger.warn("⚠️ Failed to check premium status for user {}: {}", userId, e.getMessage());
            return false;
        }
    }
}
