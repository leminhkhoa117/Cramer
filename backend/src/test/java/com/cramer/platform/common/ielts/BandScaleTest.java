package com.cramer.platform.common.ielts;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BandScaleTest {

    @Test
    @DisplayName("maps raw correct counts to the official IELTS band table")
    void bandFor_boundaries() {
        assertThat(BandScale.bandFor(40)).isEqualTo(9.0);
        assertThat(BandScale.bandFor(39)).isEqualTo(9.0);
        assertThat(BandScale.bandFor(38)).isEqualTo(8.5);
        assertThat(BandScale.bandFor(35)).isEqualTo(8.0);
        assertThat(BandScale.bandFor(30)).isEqualTo(7.0);
        assertThat(BandScale.bandFor(23)).isEqualTo(6.0);
        assertThat(BandScale.bandFor(15)).isEqualTo(5.0);
        assertThat(BandScale.bandFor(10)).isEqualTo(4.0);
        assertThat(BandScale.bandFor(4)).isEqualTo(2.5);
        assertThat(BandScale.bandFor(3)).isEqualTo(0.0);
        assertThat(BandScale.bandFor(0)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("rounds weighted bands to the nearest 0.5")
    void roundToHalf() {
        assertThat(BandScale.roundToHalf(6.33)).isEqualTo(6.5);
        assertThat(BandScale.roundToHalf(6.24)).isEqualTo(6.0);
        assertThat(BandScale.roundToHalf(7.0)).isEqualTo(7.0);
    }
}
