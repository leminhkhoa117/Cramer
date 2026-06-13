package com.cramer.engagement.service;

import com.cramer.billing.service.TranslationBillingPort;
import com.cramer.engagement.domain.Vocabulary;
import com.cramer.engagement.repository.VocabularyRepository;
import com.cramer.engagement.web.dto.TranslationView;
import com.cramer.engagement.web.dto.VocabularyRequest;
import com.cramer.engagement.web.dto.VocabularyStats;
import com.cramer.engagement.web.dto.VocabularyView;
import com.cramer.platform.common.json.Json;
import com.cramer.platform.error.QuotaExceededException;
import com.cramer.platform.error.ResourceAlreadyExistsException;
import com.cramer.platform.error.ResourceNotFoundException;
import com.cramer.platform.integration.llm.DeepSeekClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Vocabulary notebook (SPEC-16 §3): CRUD with per-user duplicate prevention, mastery toggle,
 * stats, and AI translation. Translation uses the server DeepSeek key and is billed via
 * {@link TranslationBillingPort} <strong>after success</strong> (no charge on failure).
 */
@Service
public class VocabularyService {

    private final VocabularyRepository vocab;
    private final DeepSeekClient deepSeek;
    private final TranslationBillingPort billing;

    public VocabularyService(VocabularyRepository vocab, DeepSeekClient deepSeek, TranslationBillingPort billing) {
        this.vocab = vocab;
        this.deepSeek = deepSeek;
        this.billing = billing;
    }

    @Transactional(readOnly = true)
    public Page<VocabularyView> list(UUID userId, int page, int size, String search, String filter) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Vocabulary> result;
        if (search != null && !search.isBlank()) {
            result = vocab.findByUserIdAndWordContainingIgnoreCase(userId, search.trim(), pageable);
        } else if ("mastered".equalsIgnoreCase(filter)) {
            result = vocab.findByUserIdAndIsMastered(userId, true, pageable);
        } else if ("learning".equalsIgnoreCase(filter)) {
            result = vocab.findByUserIdAndIsMastered(userId, false, pageable);
        } else {
            result = vocab.findByUserId(userId, pageable);
        }
        return result.map(VocabularyView::of);
    }

    @Transactional(readOnly = true)
    public VocabularyView get(UUID userId, Long id) {
        return VocabularyView.of(owned(userId, id));
    }

    @Transactional
    public VocabularyView create(UUID userId, VocabularyRequest req) {
        if (vocab.existsByUserIdAndWordIgnoreCase(userId, req.word())) {
            throw new ResourceAlreadyExistsException("Word already in notebook: " + req.word());
        }
        Vocabulary v = new Vocabulary();
        v.setUserId(userId);
        apply(v, req);
        v.setIsMastered(false);
        v.setReviewCount(0);
        return VocabularyView.of(vocab.save(v));
    }

    @Transactional
    public VocabularyView update(UUID userId, Long id, VocabularyRequest req) {
        Vocabulary v = owned(userId, id);
        apply(v, req);
        return VocabularyView.of(vocab.save(v));
    }

    @Transactional
    public void delete(UUID userId, Long id) {
        vocab.delete(owned(userId, id));
    }

    @Transactional
    public VocabularyView toggleMastered(UUID userId, Long id) {
        Vocabulary v = owned(userId, id);
        v.setIsMastered(!Boolean.TRUE.equals(v.getIsMastered()));
        v.setReviewCount((v.getReviewCount() == null ? 0 : v.getReviewCount()) + 1);
        v.setLastReviewedAt(OffsetDateTime.now());
        return VocabularyView.of(vocab.save(v));
    }

    @Transactional(readOnly = true)
    public VocabularyStats stats(UUID userId) {
        long total = vocab.countByUserId(userId);
        long mastered = vocab.countByUserIdAndIsMastered(userId, true);
        long learning = total - mastered;
        int pct = total == 0 ? 0 : (int) Math.round(mastered * 100.0 / total);
        return new VocabularyStats(total, mastered, learning, pct);
    }

    /** AI translate a word; billed after success (SPEC-16 §3). */
    @Transactional
    public TranslationView translate(UUID userId, String word) {
        if (!billing.canTranslate(userId)) {
            throw new QuotaExceededException("TRANSLATION_LIMIT",
                    "Monthly translation allowance exhausted and insufficient Lúa");
        }
        JsonNode r = deepSeek.chatJson(null,
                "You are a bilingual English–Vietnamese IELTS lexicographer.",
                "Translate the English word \"" + word + "\" for a Vietnamese IELTS learner. "
                        + "Return JSON {translation, phonetic, partOfSpeech, definition, exampleSentence}.",
                0.3, 400);
        TranslationView view = new TranslationView(
                r.path("translation").asText(""),
                r.path("phonetic").asText(""),
                r.path("partOfSpeech").asText(""),
                r.path("definition").asText(""),
                r.path("exampleSentence").asText(""));
        billing.chargeTranslation(userId, "translate_" + System.currentTimeMillis() + "_" + userId);
        return view;
    }

    private void apply(Vocabulary v, VocabularyRequest req) {
        v.setWord(req.word());
        v.setTranslation(req.translation());
        v.setPhonetic(req.phonetic());
        v.setPartOfSpeech(req.partOfSpeech());
        v.setDefinition(req.definition());
        v.setExampleSentence(req.exampleSentence());
        v.setSourceContext(req.sourceContext());
        v.setSourceTestId(req.sourceTestId());
        v.setSourceSectionId(req.sourceSectionId());
        v.setNotes(req.notes());
    }

    private Vocabulary owned(UUID userId, Long id) {
        return vocab.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Vocabulary", id));
    }
}
