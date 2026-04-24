package com.cramer.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for Jackson serialization of `boolean is*` fields.
 *
 * <p>Background (BUG_AUDIT_2026-04-23.md S1/S2/S4): Lombok generates `isFree()` getter
 * for `boolean isFree` (no `get` prefix). Jackson's default introspector strips the `is`
 * prefix → JSON key becomes "free" instead of "isFree". The frontend reads `isFree`/`isPremium`
 * → undefined → broken UX (free-tier shown as paid, premium gated as free).
 *
 * <p>Fix: `@JsonProperty("isFree")` / `@JsonProperty("isPremium")` annotations preserve
 * the expected JSON key. These tests guard against regression.
 */
@DisplayName("DTO Boolean isX Jackson Key Regression Tests")
class BooleanIsFieldJsonKeyTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("SubscriptionStatusDTO.TierInfo.isFree → JSON key 'isFree'")
    void tierInfoIsFreeKeyShouldBeCamelCase() throws Exception {
        SubscriptionStatusDTO.TierInfo tier = SubscriptionStatusDTO.TierInfo.builder()
                .code("cramerie")
                .name("Free")
                .isFree(true)
                .build();

        String json = mapper.writeValueAsString(tier);

        assertThat(json).contains("\"isFree\":true");
        assertThat(json).doesNotContain("\"free\":");
    }

    @Test
    @DisplayName("QuotaStatusDTO.isPremium → JSON key 'isPremium'")
    void quotaStatusIsPremiumKeyShouldBeCamelCase() throws Exception {
        QuotaStatusDTO dto = QuotaStatusDTO.forPremiumUser("2026-04");

        String json = mapper.writeValueAsString(dto);

        assertThat(json).contains("\"isPremium\":true");
        assertThat(json).doesNotContain("\"premium\":");
    }

    @Test
    @DisplayName("FeatureAccessDTO.isPremium → JSON key 'isPremium'")
    void featureAccessIsPremiumKeyShouldBeCamelCase() throws Exception {
        FeatureAccessDTO dto = FeatureAccessDTO.builder()
                .tierCode("cramerich")
                .isPremium(true)
                .build();

        String json = mapper.writeValueAsString(dto);

        assertThat(json).contains("\"isPremium\":true");
        assertThat(json).doesNotContain("\"premium\":");
    }
}
