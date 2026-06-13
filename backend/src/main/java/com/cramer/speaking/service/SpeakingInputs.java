package com.cramer.speaking.service;

import com.cramer.platform.error.OperationNotAllowedException;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Normalizes and validates speaking session inputs (SPEC-14 §2): session mode, accent, and speed.
 * Pure and side-effect-free.
 */
public final class SpeakingInputs {

    private static final Set<String> MODES = Set.of("FULL", "PART_1", "PART_2", "PART_3", "PART_2_AND_3");
    private static final Set<String> ACCENTS = Set.of("british", "american", "australian", "neutral");
    private static final Set<BigDecimal> SPEEDS = Set.of(
            new BigDecimal("0.85"), new BigDecimal("1.00"), new BigDecimal("1.15"));

    private SpeakingInputs() {
    }

    public static String normalizeMode(String raw) {
        if (raw == null) {
            throw new OperationNotAllowedException("session mode is required");
        }
        String m = raw.trim().toUpperCase();
        if (!MODES.contains(m)) {
            throw new OperationNotAllowedException("Unsupported session mode: " + raw);
        }
        return m;
    }

    public static String normalizeAccent(String raw) {
        String a = raw == null || raw.isBlank() ? "neutral" : raw.trim().toLowerCase();
        if (!ACCENTS.contains(a)) {
            throw new OperationNotAllowedException("Unsupported accent: " + raw);
        }
        return a;
    }

    public static BigDecimal normalizeSpeed(Double raw) {
        BigDecimal s = raw == null ? new BigDecimal("1.00") : new BigDecimal(String.format("%.2f", raw));
        if (!SPEEDS.contains(s)) {
            throw new OperationNotAllowedException("Unsupported speed: " + raw);
        }
        return s;
    }
}
