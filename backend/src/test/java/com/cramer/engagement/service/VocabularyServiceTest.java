package com.cramer.engagement.service;

import com.cramer.billing.service.TranslationBillingPort;
import com.cramer.engagement.domain.Vocabulary;
import com.cramer.engagement.repository.VocabularyRepository;
import com.cramer.engagement.web.dto.TranslationView;
import com.cramer.engagement.web.dto.VocabularyRequest;
import com.cramer.engagement.web.dto.VocabularyStats;
import com.cramer.platform.error.QuotaExceededException;
import com.cramer.platform.error.ResourceAlreadyExistsException;
import com.cramer.platform.integration.llm.DeepSeekClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VocabularyServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock VocabularyRepository vocab;
    @Mock DeepSeekClient deepSeek;
    @Mock TranslationBillingPort billing;

    private VocabularyService service() {
        lenient().when(vocab.save(any(Vocabulary.class))).thenAnswer(inv -> inv.getArgument(0));
        return new VocabularyService(vocab, deepSeek, billing);
    }

    private VocabularyRequest req(String word) {
        return new VocabularyRequest(word, "nghĩa", null, null, null, null, null, null, null, null);
    }

    @Test
    @DisplayName("create rejects a duplicate word for the same user (409)")
    void createDuplicate() {
        UUID user = UUID.randomUUID();
        when(vocab.existsByUserIdAndWordIgnoreCase(user, "ubiquitous")).thenReturn(true);

        assertThatThrownBy(() -> service().create(user, req("ubiquitous")))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(vocab, never()).save(any());
    }

    @Test
    @DisplayName("stats computes mastered percentage")
    void stats() {
        UUID user = UUID.randomUUID();
        when(vocab.countByUserId(user)).thenReturn(10L);
        when(vocab.countByUserIdAndIsMastered(user, true)).thenReturn(3L);

        VocabularyStats s = service().stats(user);

        assertThat(s.total()).isEqualTo(10);
        assertThat(s.mastered()).isEqualTo(3);
        assertThat(s.learning()).isEqualTo(7);
        assertThat(s.masteredPercent()).isEqualTo(30);
    }

    @Test
    @DisplayName("translate blocked when allowance exhausted -> 402, no LLM call, no charge")
    void translateBlocked() {
        UUID user = UUID.randomUUID();
        when(billing.canTranslate(user)).thenReturn(false);

        assertThatThrownBy(() -> service().translate(user, "serendipity"))
                .isInstanceOf(QuotaExceededException.class);
        verify(deepSeek, never()).chatJson(any(), any(), any(), anyDouble(), anyInt());
        verify(billing, never()).chargeTranslation(any(), anyString());
    }

    @Test
    @DisplayName("translate returns fields and charges AFTER success")
    void translateChargesAfterSuccess() throws Exception {
        UUID user = UUID.randomUUID();
        when(billing.canTranslate(user)).thenReturn(true);
        when(deepSeek.chatJson(any(), any(), any(), anyDouble(), anyInt())).thenReturn(mapper.readTree("""
                {"translation":"sự tình cờ may mắn","phonetic":"/ˌserənˈdɪpɪti/","partOfSpeech":"noun",
                 "definition":"luck","exampleSentence":"By serendipity..."}
                """));

        TranslationView v = service().translate(user, "serendipity");

        assertThat(v.translation()).isEqualTo("sự tình cờ may mắn");
        assertThat(v.partOfSpeech()).isEqualTo("noun");
        verify(billing).chargeTranslation(eq(user), anyString());
    }
}
