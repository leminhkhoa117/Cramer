package com.cramer.catalog.service;

import com.cramer.catalog.domain.Question;
import com.cramer.platform.common.ielts.QuestionType;
import com.cramer.catalog.domain.Section;
import com.cramer.catalog.domain.SectionStatus;
import com.cramer.platform.common.ielts.Skill;
import com.cramer.catalog.repository.QuestionRepository;
import com.cramer.catalog.repository.SectionRepository;
import com.cramer.catalog.web.dto.TestSectionView;
import com.cramer.platform.error.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestDeliveryServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    SectionRepository sections;
    @Mock
    QuestionRepository questions;
    @InjectMocks
    TestDeliveryService service;

    private JsonNode json(String raw) {
        try {
            return mapper.readTree(raw);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Section readingSection(long id) {
        Section s = new Section();
        s.setId(id);
        s.setExamSource("Cambridge 17");
        s.setTestNumber(1);
        s.setSkill(Skill.READING);
        s.setPartNumber(1);
        s.setPassageText("a passage");
        s.setStatus(SectionStatus.PUBLISHED);
        return s;
    }

    private Question question(long id) {
        Question q = new Question();
        q.setId(id);
        q.setQuestionNumber(1);
        q.setQuestionUid("uid-" + id);
        q.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        q.setQuestionContent(json("{\"prompt\":\"PROMPT-VISIBLE\",\"options\":[\"A\",\"B\"]}"));
        q.setCorrectAnswer(json("[\"ANSWER-SECRET\"]"));
        q.setExplanation(json("{\"detail\":\"EXPLANATION-SECRET\"}"));
        return q;
    }

    @Test
    @DisplayName("delivery exposes prompt content but never the answer key or explanation (SPEC-04 §3)")
    void deliveryNeverLeaksAnswers() throws Exception {
        when(sections.findByExamSourceAndTestNumberAndSkillAndStatusOrderByPartNumberAsc(
                eq("Cambridge 17"), eq(1), eq(Skill.READING), any()))
                .thenReturn(List.of(readingSection(1L)));
        when(questions.findBySectionIdOrderByQuestionNumberAsc(1L))
                .thenReturn(List.of(question(10L)));

        List<TestSectionView> views = service.getTestData("Cambridge 17", 1, Skill.READING);
        String serialized = mapper.writeValueAsString(views);

        assertThat(serialized).contains("PROMPT-VISIBLE");
        assertThat(serialized).doesNotContain("ANSWER-SECRET");
        assertThat(serialized).doesNotContain("EXPLANATION-SECRET");
        assertThat(views.getFirst().skill()).isEqualTo("reading");
    }

    @Test
    @DisplayName("no published content maps to 404")
    void missingContentIs404() {
        when(sections.findByExamSourceAndTestNumberAndSkillAndStatusOrderByPartNumberAsc(
                any(), any(), any(), any()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.getTestData("Nope", 9, Skill.LISTENING))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
