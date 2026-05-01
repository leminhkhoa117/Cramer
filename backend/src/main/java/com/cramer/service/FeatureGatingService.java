package com.cramer.service;

import com.cramer.dto.FeatureAccessDTO;
import com.cramer.entity.SubscriptionTier;
import com.cramer.entity.UserSubscription;
import com.cramer.repository.UserSubscriptionRepository;
import com.cramer.repository.SubscriptionTierRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for checking feature access based on user's subscription tier.
 * Parses the JSONB features array from the subscription tier to determine access.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureGatingService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionTierRepository subscriptionTierRepository;
    private final ObjectMapper objectMapper;

    /**
     * All feature codes that can be gated.
     * These match the values stored in subscription_tiers.features JSONB.
     */
    public static final Set<String> ALL_FEATURE_CODES = Set.of(
            // Content access
            "all_tests",
            "all_topics",
            "limited_tests",
            
            // AI grading features
            "ai_writing_grading",
            "ai_reading_grading",
            "ai_listening_grading",
            "ai_speaking_grading",
            "normal_grading",
            
            // AI features
            "vocab_ai",
            "chatbot",
            
            // Vocabulary
            "vocabulary",
            
            // Progress tracking
            "basic_progress",
            "full_progress",
            "analytics",
            
            // Support
            "email_support",
            "priority_support"
    );

    /**
     * Check if a user can access a specific feature.
     *
     * @param userId      the user's UUID
     * @param featureCode the feature code to check (e.g., "ai_writing_grading")
     * @return true if user's subscription tier includes this feature
     */
    public boolean canAccessFeature(UUID userId, String featureCode) {
        try {
            SubscriptionTier tier = getActiveTier(userId);
            Set<String> tierFeatures = parseFeaturesJson(tier.getFeatures());
            return tierFeatures.contains(featureCode);
        } catch (Exception e) {
            log.error("Error checking feature access for user {}: {}", userId, e.getMessage());
            // Default to false (deny) on error for security
            return false;
        }
    }

    /**
     * Get full feature access map for a user.
     *
     * @param userId the user's UUID
     * @return FeatureAccessDTO with all feature access information
     */
    public FeatureAccessDTO getFullAccessMap(UUID userId) {
        try {
            SubscriptionTier tier = getActiveTier(userId);
            Set<String> tierFeatures = parseFeaturesJson(tier.getFeatures());

            // Build feature map for all known features
            Map<String, Boolean> featureMap = new HashMap<>();
            for (String featureCode : ALL_FEATURE_CODES) {
                featureMap.put(featureCode, tierFeatures.contains(featureCode));
            }

            // Also add any tier-specific features not in our known list
            for (String feature : tierFeatures) {
                if (!featureMap.containsKey(feature)) {
                    featureMap.put(feature, true);
                }
            }

            return FeatureAccessDTO.builder()
                    .tierCode(tier.getCode())
                    .tierNameVi(tier.getName())
                    .tierNameEn(tier.getName())
                    .features(featureMap)
                    .isPremium(tier.getPriceVnd() != null && tier.getPriceVnd() > 0)
                    .build();

        } catch (Exception e) {
            log.error("Error getting feature access map for user {}: {}", userId, e.getMessage());
            // Return default (Cramerie/free tier) access on error
            return getDefaultAccessMap();
        }
    }

    /**
     * Get the active subscription tier for a user.
     * Falls back to default free tier (Cramerie) if no subscription found.
     */
    private SubscriptionTier getActiveTier(UUID userId) {
        Optional<UserSubscription> subscription = userSubscriptionRepository.findActiveByUserId(userId);
        
        if (subscription.isPresent() && subscription.get().getTier() != null) {
            return subscription.get().getTier();
        }

        // Fall back to Cramerie (free tier)
        return subscriptionTierRepository.findByCode("cramerie")
                .orElseGet(this::createDefaultTier);
    }

    /**
     * Parse the features JSONB string into a Set of feature codes.
     * The JSONB is stored as a JSON array: ["feature1", "feature2", ...]
     */
    private Set<String> parseFeaturesJson(String featuresJson) {
        if (featuresJson == null || featuresJson.isBlank()) {
            return Collections.emptySet();
        }

        try {
            List<String> featureList = objectMapper.readValue(
                    featuresJson,
                    new TypeReference<List<String>>() {}
            );
            return new HashSet<>(featureList);
        } catch (Exception e) {
            log.warn("Failed to parse features JSON: {}. Error: {}", featuresJson, e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * Get default (free tier) access map for error cases.
     */
    private FeatureAccessDTO getDefaultAccessMap() {
        Map<String, Boolean> defaultFeatures = new HashMap<>();
        for (String featureCode : ALL_FEATURE_CODES) {
            // Only basic features for free tier
            boolean hasAccess = Set.of("limited_tests", "normal_grading", "vocabulary", "basic_progress")
                    .contains(featureCode);
            defaultFeatures.put(featureCode, hasAccess);
        }

        return FeatureAccessDTO.builder()
                .tierCode("cramerie")
                .tierNameVi("Cramerie")
                .tierNameEn("Cramerie")
                .features(defaultFeatures)
                .isPremium(false)
                .build();
    }

    /**
     * Create a default tier object when none exists in database.
     */
    private SubscriptionTier createDefaultTier() {
        return SubscriptionTier.builder()
                .code("cramerie")
                .name("Cramerie")
                .priceVnd(0)
                .features("[\"limited_tests\", \"normal_grading\", \"vocabulary\", \"basic_progress\"]")
                .build();
    }
}
