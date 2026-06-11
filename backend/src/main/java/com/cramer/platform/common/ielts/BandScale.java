package com.cramer.platform.common.ielts;

/**
 * Converts a raw correct-answer count (out of 40) to an IELTS band for Reading/Listening.
 * Single source of truth for band conversion (SPEC-12 §4.1). Mirrors the official 40-question
 * academic conversion table used across the platform.
 */
public final class BandScale {

    private BandScale() {
    }

    /**
     * @param correct number of correct answers (0..40)
     * @return IELTS band (0.0 .. 9.0) in 0.5 steps
     */
    public static double bandFor(int correct) {
        if (correct >= 39) return 9.0;
        if (correct >= 37) return 8.5;
        if (correct >= 35) return 8.0;
        if (correct >= 33) return 7.5;
        if (correct >= 30) return 7.0;
        if (correct >= 27) return 6.5;
        if (correct >= 23) return 6.0;
        if (correct >= 19) return 5.5;
        if (correct >= 15) return 5.0;
        if (correct >= 13) return 4.5;
        if (correct >= 10) return 4.0;
        if (correct >= 8) return 3.5;
        if (correct >= 6) return 3.0;
        if (correct >= 4) return 2.5;
        return 0.0;
    }

    /** Round an arbitrary band value to the nearest 0.5 (used for weighted Writing bands). */
    public static double roundToHalf(double band) {
        return Math.round(band * 2.0) / 2.0;
    }
}
