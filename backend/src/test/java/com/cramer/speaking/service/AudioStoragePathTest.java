package com.cramer.speaking.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AudioStoragePathTest {

    @Test
    @DisplayName("relative object keys are accepted")
    void validKeys() {
        assertThat(AudioStoragePath.isValid("sessions/42/turn-1.webm")).isTrue();
        assertThat(AudioStoragePath.isValid("user-abc/audio.mp3")).isTrue();
    }

    @Test
    @DisplayName("empty, absolute, traversal, backslash, scheme, and colon paths are rejected")
    void invalidKeys() {
        assertThat(AudioStoragePath.isValid(null)).isFalse();
        assertThat(AudioStoragePath.isValid("  ")).isFalse();
        assertThat(AudioStoragePath.isValid("/etc/passwd")).isFalse();
        assertThat(AudioStoragePath.isValid("../secret.mp3")).isFalse();
        assertThat(AudioStoragePath.isValid("a\\b.mp3")).isFalse();
        assertThat(AudioStoragePath.isValid("https://evil.com/a.mp3")).isFalse();
        assertThat(AudioStoragePath.isValid("C:audio.mp3")).isFalse();
    }

    @Test
    @DisplayName("require throws 400-style IllegalArgumentException on an invalid path")
    void requireThrows() {
        assertThatThrownBy(() -> AudioStoragePath.require("../x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(AudioStoragePath.require("ok/key.mp3")).isEqualTo("ok/key.mp3");
    }
}
