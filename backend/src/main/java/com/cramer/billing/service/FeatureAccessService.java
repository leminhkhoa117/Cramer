package com.cramer.billing.service;

import com.cramer.billing.domain.SubscriptionTier;
import com.cramer.billing.domain.UserSubscription;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * Feature gating (SPEC-15 §7) implementing {@link FeatureAccessPort}. Resolves the active tier and
 * parses {@code subscription_tiers.features}.
 *
 * <p><strong>Fix:</strong> a single parser handles <em>both</em> JSON shapes the data uses —
 * an array {@code ["a","b"]} and an object {@code {"a":true,"b":false}} — instead of the two
 * inconsistent parsers the old code had. Free-tier (Cramerie) defaults apply when no features
 * are configured.
 */
@Service
public class FeatureAccessService implements FeatureAccessPort {

    private static final Set<String> FREE_DEFAULTS =
            Set.of("limited_tests", "normal_grading", "vocabulary", "basic_progress");

    private final SubscriptionService subscriptions;

    public FeatureAccessService(SubscriptionService subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasFeature(UUID userId, String feature) {
        UserSubscription sub = subscriptions.getOrCreateActive(userId);
        SubscriptionTier tier = subscriptions.tierOf(sub);
        return featureEnabled(tier.getFeatures(), feature, tier.isPremium());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPremium(UUID userId) {
        return subscriptions.isPremium(subscriptions.getOrCreateActive(userId));
    }

    /** Visible for testing: parse either JSON shape (array or object), with free defaults. */
    static boolean featureEnabled(JsonNode features, String feature, boolean premium) {
        if (features != null) {
            if (features.isArray()) {
                for (JsonNode f : features) {
                    if (f.asText("").equals(feature)) {
                        return true;
                    }
                }
                if (!features.isEmpty()) {
                    return false; // explicit array is authoritative
                }
            } else if (features.isObject()) {
                JsonNode v = features.get(feature);
                if (v != null) {
                    return v.asBoolean(false);
                }
            }
        }
        // No explicit config: premium tiers grant everything; free tiers get the base set.
        return premium || FREE_DEFAULTS.contains(feature);
    }
}
