package com.cramer.platform.common.ids;

import java.util.Optional;
import java.util.UUID;

/**
 * UUID parsing/validation helper (SPEC-18 §7). A hard parse failure surfaces as HTTP 400 via
 * the global handler (IllegalArgumentException → 400).
 */
public final class Ids {

    private Ids() {
    }

    /** Parse a UUID or throw {@link IllegalArgumentException} (→ 400). */
    public static UUID parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing UUID");
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID: " + value);
        }
    }

    /** Parse a UUID if possible, otherwise empty (never throws). */
    public static Optional<UUID> tryParse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
