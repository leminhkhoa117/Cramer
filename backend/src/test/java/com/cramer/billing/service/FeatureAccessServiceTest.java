package com.cramer.billing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureAccessServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode json(String raw) {
        try {
            return mapper.readTree(raw);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("array shape: listed features are enabled, others are not")
    void arrayShape() {
        JsonNode features = json("[\"advanced_analytics\",\"unlimited_tests\"]");
        assertThat(FeatureAccessService.featureEnabled(features, "advanced_analytics", true)).isTrue();
        assertThat(FeatureAccessService.featureEnabled(features, "missing", true)).isFalse();
    }

    @Test
    @DisplayName("object shape: boolean values decide; unknown keys fall back to tier defaults")
    void objectShape() {
        JsonNode features = json("{\"advanced_analytics\":true,\"beta\":false}");
        assertThat(FeatureAccessService.featureEnabled(features, "advanced_analytics", false)).isTrue();
        assertThat(FeatureAccessService.featureEnabled(features, "beta", true)).isFalse();
        // unknown key on a premium tier -> granted
        assertThat(FeatureAccessService.featureEnabled(features, "unknown", true)).isTrue();
    }

    @Test
    @DisplayName("no config: free tier gets base defaults, premium gets everything")
    void defaults() {
        assertThat(FeatureAccessService.featureEnabled(null, "vocabulary", false)).isTrue(); // free default
        assertThat(FeatureAccessService.featureEnabled(null, "advanced_analytics", false)).isFalse(); // free, not default
        assertThat(FeatureAccessService.featureEnabled(null, "advanced_analytics", true)).isTrue(); // premium
    }
}
