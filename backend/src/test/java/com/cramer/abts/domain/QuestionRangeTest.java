package com.cramer.abts.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cramer.platform.common.ielts.Skill;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QuestionRangeTest {

    @Test
    @DisplayName("Reading parts map to canonical ranges")
    void reading() {
        assertThat(QuestionRange.of(Skill.READING, 1)).isEqualTo(new QuestionRange(1, 13));
        assertThat(QuestionRange.of(Skill.READING, 2)).isEqualTo(new QuestionRange(14, 26));
        assertThat(QuestionRange.of(Skill.READING, 3)).isEqualTo(new QuestionRange(27, 40));
        assertThat(QuestionRange.of(Skill.READING, 2).count()).isEqualTo(13);
    }

    @Test
    @DisplayName("Listening parts map to canonical ranges of 10")
    void listening() {
        assertThat(QuestionRange.of(Skill.LISTENING, 1)).isEqualTo(new QuestionRange(1, 10));
        assertThat(QuestionRange.of(Skill.LISTENING, 4)).isEqualTo(new QuestionRange(31, 40));
        assertThat(QuestionRange.of(Skill.LISTENING, 4).count()).isEqualTo(10);
        assertThat(QuestionRange.of(Skill.LISTENING, 3).contains(25)).isTrue();
        assertThat(QuestionRange.of(Skill.LISTENING, 3).contains(31)).isFalse();
    }

    @Test
    @DisplayName("Writing/Speaking are not number-ranged; invalid parts rejected")
    void invalid() {
        assertThatThrownBy(() -> QuestionRange.of(Skill.WRITING, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QuestionRange.of(Skill.READING, 4))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
