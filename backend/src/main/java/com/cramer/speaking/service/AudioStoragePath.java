package com.cramer.speaking.service;

/**
 * Validates a transcript {@code audio_storage_path} (SPEC-14 §5): it must be a <strong>relative
 * object key</strong> inside the speaking-audio bucket. Rejects empty, absolute, parent-traversal
 * ({@code ..}), backslash, scheme ({@code ://}), or colon-bearing values — a client cannot point
 * grading at an arbitrary object.
 */
public final class AudioStoragePath {

    private AudioStoragePath() {
    }

    public static boolean isValid(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String p = path.trim();
        if (p.startsWith("/") || p.startsWith("\\")) {
            return false; // absolute
        }
        if (p.contains("\\") || p.contains("..") || p.contains("://") || p.contains(":")) {
            return false; // backslash / traversal / scheme / drive-or-scheme colon
        }
        return true;
    }

    /** @throws IllegalArgumentException (→400) when invalid. */
    public static String require(String path) {
        if (!isValid(path)) {
            throw new IllegalArgumentException("Invalid audio storage path: must be a relative object key");
        }
        return path.trim();
    }
}
