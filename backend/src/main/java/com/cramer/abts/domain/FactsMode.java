package com.cramer.abts.domain;

/**
 * Content-construction mode (SPEC-20 §4.3, SPEC-22 §3). STRICT (≥3 facts/part) constrains the
 * model to the supplied facts; AUTO lets it invent plausible academic detail.
 */
public enum FactsMode {
    AUTO,
    STRICT;

    public static FactsMode parse(String raw) {
        if (raw == null) {
            return AUTO;
        }
        return "STRICT".equalsIgnoreCase(raw.trim()) ? STRICT : AUTO;
    }
}
