package com.cramer.catalog.service;

import com.cramer.catalog.domain.Hashtag;
import com.cramer.catalog.repository.HashtagRepository;
import com.cramer.catalog.web.dto.HashtagRequest;
import com.cramer.catalog.web.dto.HashtagView;
import com.cramer.platform.config.CacheConfig;
import com.cramer.platform.error.ResourceAlreadyExistsException;
import com.cramer.platform.error.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hashtag management (SPEC-11 §4.1). Soft delete via {@code is_active=false}; list/search return
 * active only; {@link #findOrCreateByCodes(List)} creates missing as category {@code topic}.
 */
@Service
@Transactional
public class HashtagService {

    private static final int MAX_PER_TEST = 20;

    private final HashtagRepository hashtags;

    public HashtagService(HashtagRepository hashtags) {
        this.hashtags = hashtags;
    }

    @Transactional(readOnly = true)
    @Cacheable(CacheConfig.CACHE_HASHTAGS)
    public List<HashtagView> listActive() {
        return hashtags.findByIsActiveTrueOrderByUseCountDesc().stream().map(HashtagView::of).toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(CacheConfig.CACHE_HASHTAGS)
    public List<HashtagView> byCategory(String category) {
        return hashtags.findByCategoryAndIsActiveTrueOrderByUseCountDesc(category).stream()
                .map(HashtagView::of).toList();
    }

    @Transactional(readOnly = true)
    public List<HashtagView> search(String query) {
        return hashtags.findByIsActiveTrueAndNameContainingIgnoreCaseOrderByUseCountDesc(query).stream()
                .map(HashtagView::of).toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(CacheConfig.CACHE_HASHTAGS)
    public List<HashtagView> popular(int limit) {
        return hashtags.findByIsActiveTrueOrderByUseCountDesc().stream()
                .limit(Math.max(0, limit)).map(HashtagView::of).toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(CacheConfig.CACHE_HASHTAGS)
    public List<String> categories() {
        return hashtags.findByIsActiveTrueOrderByUseCountDesc().stream()
                .map(Hashtag::getCategory).filter(c -> c != null).distinct().toList();
    }

    /** Resolve hashtag ids to their codes (batch; used to project a test's hashtags). */
    @Transactional(readOnly = true)
    public List<String> codesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return hashtags.findAllById(ids).stream().map(Hashtag::getCode).toList();
    }

    @CacheEvict(value = CacheConfig.CACHE_HASHTAGS, allEntries = true)
    public HashtagView create(HashtagRequest req) {
        if (hashtags.findByCode(req.code()).isPresent()) {
            throw new ResourceAlreadyExistsException("Hashtag code already exists: " + req.code());
        }
        Hashtag h = new Hashtag();
        h.setCode(req.code());
        h.setName(req.name() != null ? req.name() : formatCodeToName(req.code()));
        h.setCategory(req.category());
        h.setIcon(req.icon());
        h.setColor(req.color());
        h.setUseCount(0);
        h.setIsActive(true);
        return HashtagView.of(hashtags.save(h));
    }

    @CacheEvict(value = CacheConfig.CACHE_HASHTAGS, allEntries = true)
    public HashtagView update(Long id, HashtagRequest req) {
        Hashtag h = load(id);
        if (!h.getCode().equals(req.code()) && hashtags.findByCode(req.code()).isPresent()) {
            throw new ResourceAlreadyExistsException("Hashtag code already exists: " + req.code());
        }
        h.setCode(req.code());
        h.setName(req.name());
        h.setCategory(req.category());
        h.setIcon(req.icon());
        h.setColor(req.color());
        return HashtagView.of(hashtags.save(h));
    }

    /** Soft delete (SPEC-11 §4.1): mark inactive, never hard-delete. */
    @CacheEvict(value = CacheConfig.CACHE_HASHTAGS, allEntries = true)
    public void softDelete(Long id) {
        Hashtag h = load(id);
        h.setIsActive(false);
        hashtags.save(h);
    }

    /**
     * Resolve hashtag codes to entities, creating any missing as category {@code topic}
     * (SPEC-11 §4.1). Preserves the requested order and dedupes. Enforces the max-per-test cap.
     */
    @CacheEvict(value = CacheConfig.CACHE_HASHTAGS, allEntries = true)
    public List<Hashtag> findOrCreateByCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        List<String> distinct = codes.stream().distinct().toList();
        if (distinct.size() > MAX_PER_TEST) {
            throw new IllegalArgumentException("A test may have at most " + MAX_PER_TEST + " hashtags");
        }
        Map<String, Hashtag> byCode = new LinkedHashMap<>();
        for (Hashtag h : hashtags.findByCodeIn(distinct)) {
            byCode.put(h.getCode(), h);
        }
        List<Hashtag> result = new ArrayList<>();
        for (String code : distinct) {
            Hashtag h = byCode.get(code);
            if (h == null) {
                Hashtag created = new Hashtag();
                created.setCode(code);
                created.setName(formatCodeToName(code));
                created.setCategory("topic");
                created.setUseCount(0);
                created.setIsActive(true);
                h = hashtags.save(created);
            }
            result.add(h);
        }
        return result;
    }

    private Hashtag load(Long id) {
        return hashtags.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Hashtag", id));
    }

    private static String formatCodeToName(String code) {
        String spaced = code.replace('_', ' ').replace('-', ' ').trim();
        if (spaced.isEmpty()) {
            return code;
        }
        String[] parts = spaced.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }
}
