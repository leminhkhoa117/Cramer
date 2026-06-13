package com.cramer.catalog.service;

import com.cramer.catalog.domain.Question;
import com.cramer.catalog.domain.Section;
import com.cramer.catalog.domain.SectionStatus;
import com.cramer.catalog.domain.TestSet;
import com.cramer.catalog.repository.QuestionRepository;
import com.cramer.catalog.repository.SectionRepository;
import com.cramer.catalog.repository.TestRepository;
import com.cramer.catalog.repository.TestSetRepository;
import com.cramer.catalog.service.ContentDraftPort.DraftQuestion;
import com.cramer.catalog.service.ContentDraftPort.DraftSection;
import com.cramer.catalog.service.ContentDraftPort.SaveDraftCommand;
import com.cramer.catalog.service.ContentDraftPort.SaveDraftResult;
import com.cramer.platform.common.ielts.Skill;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentDraftServiceTest {

    @Mock TestSetRepository testSets;
    @Mock TestRepository tests;
    @Mock SectionRepository sections;
    @Mock QuestionRepository questions;
    @Captor ArgumentCaptor<Question> questionCaptor;

    private ContentDraftService service() {
        return new ContentDraftService(testSets, tests, sections, questions);
    }

    private SaveDraftCommand command() {
        DraftQuestion q = new DraftQuestion(14, "MULTIPLE_CHOICE", null, null, null, null, null);
        DraftSection s = new DraftSection(Skill.READING, 2, "passage", null, null, null, null, List.of(q));
        return new SaveDraftCommand("cam20", null, 3, null, null, List.of(s));
    }

    @Test
    @DisplayName("save creates an unpublished set + draft test and never publishes (draft discipline)")
    void draftDiscipline() {
        when(testSets.findByCode("cam20")).thenReturn(Optional.empty());
        when(testSets.save(any(TestSet.class))).thenAnswer(inv -> {
            TestSet s = inv.getArgument(0);
            s.setId(7L);
            return s;
        });
        when(tests.findBySetIdAndTestNumber(7L, 3)).thenReturn(Optional.empty());
        when(tests.save(any(com.cramer.catalog.domain.Test.class))).thenAnswer(inv -> {
            com.cramer.catalog.domain.Test t = inv.getArgument(0);
            t.setId(70L);
            return t;
        });
        when(sections.findByTestIdAndSkillAndPartNumber(70L, Skill.READING, 2)).thenReturn(Optional.empty());
        when(sections.save(any(Section.class))).thenAnswer(inv -> {
            Section s = inv.getArgument(0);
            s.setId(700L);
            return s;
        });

        SaveDraftResult result = service().saveDraft(command());

        assertThat(result.questionCount()).isEqualTo(1);
        assertThat(result.sectionIds()).containsExactly(700L);

        ArgumentCaptor<TestSet> setCap = ArgumentCaptor.forClass(TestSet.class);
        verify(testSets).save(setCap.capture());
        assertThat(setCap.getValue().getIsPublished()).isFalse();
        assertThat(setCap.getValue().getSourceType()).isEqualTo("ai_generated");

        ArgumentCaptor<com.cramer.catalog.domain.Test> testCap = ArgumentCaptor.forClass(com.cramer.catalog.domain.Test.class);
        verify(tests).save(testCap.capture());
        assertThat(testCap.getValue().getIsPublished()).isFalse();
        assertThat(testCap.getValue().getIsAiGenerated()).isTrue();

        ArgumentCaptor<Section> secCap = ArgumentCaptor.forClass(Section.class);
        verify(sections).save(secCap.capture());
        assertThat(secCap.getValue().getStatus()).isEqualTo(SectionStatus.DRAFT);
    }

    @Test
    @DisplayName("question_uid follows {setCode}_{testNumber}_{skillInitial}_q{number}")
    void questionUidFormat() {
        when(testSets.findByCode("cam20")).thenReturn(Optional.empty());
        when(testSets.save(any(TestSet.class))).thenAnswer(inv -> {
            TestSet s = inv.getArgument(0);
            s.setId(7L);
            return s;
        });
        when(tests.findBySetIdAndTestNumber(7L, 3)).thenReturn(Optional.empty());
        when(tests.save(any(com.cramer.catalog.domain.Test.class))).thenAnswer(inv -> {
            com.cramer.catalog.domain.Test t = inv.getArgument(0);
            t.setId(70L);
            return t;
        });
        when(sections.findByTestIdAndSkillAndPartNumber(70L, Skill.READING, 2)).thenReturn(Optional.empty());
        when(sections.save(any(Section.class))).thenAnswer(inv -> {
            Section s = inv.getArgument(0);
            s.setId(700L);
            return s;
        });

        service().saveDraft(command());

        verify(questions).save(questionCaptor.capture());
        assertThat(questionCaptor.getValue().getQuestionUid()).isEqualTo("cam20_3_r_q14");
    }

    @Test
    @DisplayName("an existing section is updated and its questions replaced (upsert)")
    void upsertReplacesQuestions() {
        Section existing = new Section();
        existing.setId(700L);
        when(testSets.findByCode("cam20")).thenReturn(Optional.of(existingSet()));
        when(tests.findBySetIdAndTestNumber(7L, 3)).thenReturn(Optional.of(existingTest()));
        when(sections.findByTestIdAndSkillAndPartNumber(70L, Skill.READING, 2)).thenReturn(Optional.of(existing));
        when(sections.save(any(Section.class))).thenAnswer(inv -> inv.getArgument(0));

        service().saveDraft(command());

        verify(questions).deleteBySectionId(700L);
        verify(questions).save(any(Question.class));
    }

    private TestSet existingSet() {
        TestSet s = new TestSet();
        s.setId(7L);
        s.setCode("cam20");
        return s;
    }

    private com.cramer.catalog.domain.Test existingTest() {
        com.cramer.catalog.domain.Test t = new com.cramer.catalog.domain.Test();
        t.setId(70L);
        t.setSetId(7L);
        t.setTestNumber(3);
        return t;
    }
}
