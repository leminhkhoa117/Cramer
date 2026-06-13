package com.cramer.engagement.web;

import com.cramer.engagement.service.VocabularyService;
import com.cramer.engagement.web.dto.TranslationView;
import com.cramer.engagement.web.dto.VocabularyRequest;
import com.cramer.engagement.web.dto.VocabularyStats;
import com.cramer.engagement.web.dto.VocabularyView;
import com.cramer.platform.security.CurrentUser;
import com.cramer.platform.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Vocabulary notebook endpoints (SPEC-16 §3). */
@RestController
@RequestMapping("/api/vocabulary")
public class VocabularyController {

    private final VocabularyService vocab;
    private final CurrentUser currentUser;

    public VocabularyController(VocabularyService vocab, CurrentUser currentUser) {
        this.vocab = vocab;
        this.currentUser = currentUser;
    }

    @GetMapping
    public PageResponse<VocabularyView> list(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(required = false) String search,
                                             @RequestParam(required = false) String filter) {
        Page<VocabularyView> p = vocab.list(currentUser.requireUserId(), page, size, search, filter);
        return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @GetMapping("/stats")
    public VocabularyStats stats() {
        return vocab.stats(currentUser.requireUserId());
    }

    @PostMapping("/translate")
    public TranslationView translate(@RequestBody Map<String, String> body) {
        return vocab.translate(currentUser.requireUserId(), body.getOrDefault("word", ""));
    }

    @GetMapping("/{id}")
    public VocabularyView get(@PathVariable Long id) {
        return vocab.get(currentUser.requireUserId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VocabularyView create(@Valid @RequestBody VocabularyRequest request) {
        return vocab.create(currentUser.requireUserId(), request);
    }

    @PutMapping("/{id}")
    public VocabularyView update(@PathVariable Long id, @Valid @RequestBody VocabularyRequest request) {
        return vocab.update(currentUser.requireUserId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        vocab.delete(currentUser.requireUserId(), id);
    }

    @PutMapping("/{id}/toggle-mastered")
    public VocabularyView toggleMastered(@PathVariable Long id) {
        return vocab.toggleMastered(currentUser.requireUserId(), id);
    }
}
