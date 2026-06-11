package com.cramer.abts.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * ABTS tuning (SPEC-24 §3). Bound from {@code abts.*}. Streaming uses a bounded executor and
 * emitter/per-part timeouts; refinement is capped by {@code maxRefinementRounds}.
 *
 * @param streaming          streaming executor + timeout settings
 * @param maxRefinementRounds refine cap (SPEC-23 §5)
 */
@ConfigurationProperties(prefix = "abts")
public record AbtsProperties(
        @DefaultValue Streaming streaming,
        @DefaultValue("5") int maxRefinementRounds) {

    public record Streaming(
            @DefaultValue("1800000") int emitterTimeoutMs,
            @DefaultValue("600000") int partTimeoutMs,
            @DefaultValue("8") int poolSize,
            @DefaultValue("4") int queueCapacity) {
    }
}
