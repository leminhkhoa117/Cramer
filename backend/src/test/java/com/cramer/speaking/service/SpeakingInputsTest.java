package com.cramer.speaking.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cramer.platform.error.OperationNotAllowedException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpeakingInputsTest {

    @Test
    @DisplayName("mode is upper-cased and validated against the allowed set")
    void mode() {
        assertThat(SpeakingInputs.normalizeMode("full")).isEqualTo("FULL");
        assertThat(SpeakingInputs.normalizeMode("part_2_and_3")).isEqualTo("PART_2_AND_3");
        assertThatThrownBy(() -> SpeakingInputs.normalizeMode("part_4"))
                .isInstanceOf(OperationNotAllowedException.class);
    }

    @Test
    @DisplayName("accent defaults to neutral and is validated")
    void accent() {
        assertThat(SpeakingInputs.normalizeAccent(null)).isEqualTo("neutral");
        assertThat(SpeakingInputs.normalizeAccent("British")).isEqualTo("british");
        assertThatThrownBy(() -> SpeakingInputs.normalizeAccent("scottish"))
                .isInstanceOf(OperationNotAllowedException.class);
    }

    @Test
    @DisplayName("speed defaults to 1.00 and only the three allowed values pass")
    void speed() {
        assertThat(SpeakingInputs.normalizeSpeed(null)).isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(SpeakingInputs.normalizeSpeed(0.85)).isEqualByComparingTo(new BigDecimal("0.85"));
        assertThat(SpeakingInputs.normalizeSpeed(1.15)).isEqualByComparingTo(new BigDecimal("1.15"));
        assertThatThrownBy(() -> SpeakingInputs.normalizeSpeed(2.0))
                .isInstanceOf(OperationNotAllowedException.class);
    }
}
