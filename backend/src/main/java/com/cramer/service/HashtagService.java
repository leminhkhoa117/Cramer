package com.cramer.service;

import com.cramer.dto.testhierarchy.CreateHashtagRequest;
import com.cramer.dto.testhierarchy.HashtagDTO;
import com.cramer.entity.Hashtag;
import com.cramer.exception.ResourceAlreadyExistsException;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.repository.HashtagRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing Hashtag entities.
 * Handles CRUD operations and business logic for hashtags.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class HashtagService {

    private static final Logger logger = LoggerFactory.getLogger(HashtagService.class);

    private final HashtagRepository hashtagRepository;

    /**
     * Get all active hashtags.
     * @return list of all active hashtags
     */
    @Transactional(readOnly = true)
    public List<HashtagDTO> getAllHashtags() {
        logger.info("Fetching all active hashtags");
        return hashtagRepository.findByIsActiveTrueOrderByUseCountDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get hashtags by category.
     * @param category the category name
     * @return list of hashtags in that category
     */
    @Transactional(readOnly = true)
    public List<HashtagDTO> getHashtagsByCategory(String category) {
        logger.info("Fetching hashtags by category: {}", category);
        return hashtagRepository.findByCategoryAndIsActiveTrueOrderByUseCountDesc(category)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Search hashtags by name or code.
     * @param query the search query
     * @return list of matching hashtags
     */
    @Transactional(readOnly = true)
    public List<HashtagDTO> searchHashtags(String query) {
        logger.info("Searching hashtags with query: {}", query);
        return hashtagRepository.searchByNameOrCode(query)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get popular hashtags (most used).
     * @param limit maximum number of hashtags to return
     * @return list of popular hashtags
     */
    @Transactional(readOnly = true)
    public List<HashtagDTO> getPopularHashtags(int limit) {
        logger.info("Fetching top {} popular hashtags", limit);
        return hashtagRepository.findPopularHashtags(limit)
                .stream()
                .limit(limit)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Create a new hashtag.
     * @param request creation request
     * @return created hashtag DTO
     */
    public HashtagDTO createHashtag(CreateHashtagRequest request) {
        logger.info("Creating new hashtag with code: {}", request.getCode());
        
        if (hashtagRepository.existsByCode(request.getCode())) {
            throw new ResourceAlreadyExistsException("Hashtag", "code", request.getCode());
        }
        
        Hashtag hashtag = Hashtag.builder()
                .code(request.getCode())
                .nameVi(request.getNameVi())
                .nameEn(request.getNameEn())
                .category(request.getCategory())
                .icon(request.getIcon())
                .color(request.getColor())
                .useCount(0)
                .isActive(true)
                .build();
        
        Hashtag saved = hashtagRepository.save(hashtag);
        logger.info("Created hashtag with ID: {}", saved.getId());
        
        return toDTO(saved);
    }

    /**
     * Update an existing hashtag.
     * @param id hashtag ID
     * @param request update request
     * @return updated hashtag DTO
     */
    public HashtagDTO updateHashtag(Long id, CreateHashtagRequest request) {
        logger.info("Updating hashtag ID: {}", id);
        
        Hashtag hashtag = hashtagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hashtag", "id", id));
        
        // Check if code changed and if new code already exists
        if (!hashtag.getCode().equals(request.getCode()) && 
            hashtagRepository.existsByCode(request.getCode())) {
            throw new ResourceAlreadyExistsException("Hashtag", "code", request.getCode());
        }
        
        hashtag.setCode(request.getCode());
        hashtag.setNameVi(request.getNameVi());
        hashtag.setNameEn(request.getNameEn());
        hashtag.setCategory(request.getCategory());
        hashtag.setIcon(request.getIcon());
        hashtag.setColor(request.getColor());
        
        Hashtag saved = hashtagRepository.save(hashtag);
        logger.info("Updated hashtag ID: {}", saved.getId());
        
        return toDTO(saved);
    }

    /**
     * Soft delete a hashtag (set inactive).
     * @param id hashtag ID
     */
    public void deleteHashtag(Long id) {
        logger.info("Soft deleting hashtag ID: {}", id);
        
        Hashtag hashtag = hashtagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hashtag", "id", id));
        
        hashtag.setIsActive(false);
        hashtagRepository.save(hashtag);
        
        logger.info("Hashtag ID: {} is now inactive", id);
    }

    /**
     * Find or create hashtags by codes.
     * Used by ABTS to auto-create topic hashtags.
     * @param codes list of hashtag codes
     * @return set of hashtags (existing or newly created)
     */
    public Set<Hashtag> findOrCreateByCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return new HashSet<>();
        }
        
        logger.info("Finding or creating hashtags for codes: {}", codes);
        
        Set<String> codeSet = new HashSet<>(codes);
        Set<Hashtag> existing = hashtagRepository.findByCodeIn(codeSet);
        
        Set<String> existingCodes = existing.stream()
                .map(Hashtag::getCode)
                .collect(Collectors.toSet());
        
        // Create new hashtags for codes that don't exist
        for (String code : codes) {
            if (!existingCodes.contains(code)) {
                Hashtag newHashtag = Hashtag.builder()
                        .code(code)
                        .nameVi(formatCodeToName(code))
                        .nameEn(formatCodeToName(code))
                        .category("topic")
                        .useCount(0)
                        .isActive(true)
                        .build();
                existing.add(hashtagRepository.save(newHashtag));
                logger.info("Auto-created hashtag: {}", code);
            }
        }
        
        return existing;
    }

    /**
     * Increment use count for a hashtag.
     * @param hashtagId hashtag ID
     */
    public void incrementUseCount(Long hashtagId) {
        hashtagRepository.incrementUseCount(hashtagId);
    }

    /**
     * Increment use counts for multiple hashtags.
     * @param hashtags set of hashtags
     */
    public void incrementUseCounts(Set<Hashtag> hashtags) {
        if (hashtags != null) {
            for (Hashtag hashtag : hashtags) {
                hashtagRepository.incrementUseCount(hashtag.getId());
            }
        }
    }

    /**
     * Decrement use counts for multiple hashtags.
     * @param hashtags set of hashtags
     */
    public void decrementUseCounts(Set<Hashtag> hashtags) {
        if (hashtags != null) {
            for (Hashtag hashtag : hashtags) {
                hashtagRepository.decrementUseCount(hashtag.getId());
            }
        }
    }

    /**
     * Get hashtag by code.
     * @param code hashtag code
     * @return hashtag entity
     */
    @Transactional(readOnly = true)
    public Optional<Hashtag> getByCode(String code) {
        return hashtagRepository.findByCode(code);
    }

    /**
     * Get distinct categories.
     * @return list of category names
     */
    @Transactional(readOnly = true)
    public List<String> getDistinctCategories() {
        return hashtagRepository.findDistinctCategories();
    }

    /**
     * Convert hashtag entity to DTO.
     */
    private HashtagDTO toDTO(Hashtag entity) {
        return HashtagDTO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .nameVi(entity.getNameVi())
                .nameEn(entity.getNameEn())
                .category(entity.getCategory())
                .icon(entity.getIcon())
                .color(entity.getColor())
                .useCount(entity.getUseCount())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .testCount(entity.getTestCount())
                .build();
    }

    /**
     * Format a code to a readable name.
     * e.g., "climate_change" -> "Climate Change"
     */
    private String formatCodeToName(String code) {
        if (code == null || code.isEmpty()) {
            return code;
        }
        return Arrays.stream(code.split("[-_]"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
