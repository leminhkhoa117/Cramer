package com.cramer.service.unit;

import com.cramer.service.implement.SpeakingGradingPromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpeakingGradingPromptBuilder Unit Tests")
class SpeakingGradingPromptBuilderTest {

    private SpeakingGradingPromptBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new SpeakingGradingPromptBuilder();
    }

    @Test
    @DisplayName("Should include IELTS band descriptors in system prompt")
    void shouldContainBandDescriptors() {
        String prompt = builder.buildSystemPrompt("FULL", List.of(1, 2, 3));

        assertThat(prompt).contains("Band 9");
        assertThat(prompt).contains("Fluency");
    }

    @Test
    @DisplayName("Should include operational definitions in system prompt")
    void shouldContainOperationalDefinitions() {
        String prompt = builder.buildSystemPrompt("FULL", List.of(1, 2, 3));

        assertThat(prompt).contains("≥2.0s");
        assertThat(prompt).contains("filler word");
    }

    @Test
    @DisplayName("Should specify feedback language rules in system prompt")
    void shouldContainLanguageRules() {
        String prompt = builder.buildSystemPrompt("FULL", List.of(1, 2, 3));

        assertThat(prompt).contains("Vietnamese");
    }

    @Test
    @DisplayName("Should include output contract with schema name")
    void shouldContainOutputContract() {
        String prompt = builder.buildSystemPrompt("PARTIAL", List.of(1));

        assertThat(prompt).contains("speaking_grading_v2");
    }

    @Test
    @DisplayName("Should reflect session mode and parts in prompt")
    void shouldIncludeSessionMetadata() {
        String prompt = builder.buildSystemPrompt("PARTIAL", List.of(1, 3));

        assertThat(prompt).contains("PARTIAL");
        assertThat(prompt).contains("[1, 3]");
    }
}
