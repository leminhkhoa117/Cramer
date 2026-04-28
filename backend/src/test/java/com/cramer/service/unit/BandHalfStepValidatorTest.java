package com.cramer.service.unit;

import com.cramer.service.implement.SpeakingGradingWorker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BandHalfStepValidator Unit Tests")
class BandHalfStepValidatorTest {

    @Test
    @DisplayName("Should accept valid half-step band scores")
    void shouldAcceptValidHalfSteps() {
        assertTrue(SpeakingGradingWorker.isValidBand(new BigDecimal("6.0")));
        assertTrue(SpeakingGradingWorker.isValidBand(new BigDecimal("6.5")));
        assertTrue(SpeakingGradingWorker.isValidBand(new BigDecimal("7.0")));
        assertTrue(SpeakingGradingWorker.isValidBand(new BigDecimal("0.0")));
        assertTrue(SpeakingGradingWorker.isValidBand(new BigDecimal("9.0")));
        assertTrue(SpeakingGradingWorker.isValidBand(new BigDecimal("4.5")));
        assertTrue(SpeakingGradingWorker.isValidBand(new BigDecimal("8.5")));
    }

    @Test
    @DisplayName("Should reject invalid band scores")
    void shouldRejectInvalidValues() {
        assertFalse(SpeakingGradingWorker.isValidBand(new BigDecimal("6.3")));
        assertFalse(SpeakingGradingWorker.isValidBand(new BigDecimal("6.25")));
        assertFalse(SpeakingGradingWorker.isValidBand(new BigDecimal("9.5")));
        assertFalse(SpeakingGradingWorker.isValidBand(new BigDecimal("-0.1")));
        assertFalse(SpeakingGradingWorker.isValidBand(new BigDecimal("-0.5")));
        assertFalse(SpeakingGradingWorker.isValidBand(new BigDecimal("10.0")));
        assertFalse(SpeakingGradingWorker.isValidBand(new BigDecimal("3.7")));
    }

    @Test
    @DisplayName("Should accept null as valid (absent band)")
    void shouldAcceptNullBand() {
        assertTrue(SpeakingGradingWorker.isValidBand(null));
    }
}
