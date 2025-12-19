package com.cramer.controller;

import com.cramer.dto.VocabularyCreateDTO;
import com.cramer.dto.VocabularyDTO;
import com.cramer.service.VocabularyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Vocabulary management.
 * Provides CRUD operations and AI-powered translation for vocabulary entries.
 * 
 * All endpoints are protected and require JWT authentication.
 * Users can only access their own vocabulary entries (IDOR protection).
 */
@RestController
@RequestMapping("/api/vocabulary")
@Tag(name = "Vocabulary", description = "APIs for managing user vocabulary notebook")
public class VocabularyController {

    private static final Logger logger = LoggerFactory.getLogger(VocabularyController.class);

    private final VocabularyService vocabularyService;

    public VocabularyController(VocabularyService vocabularyService) {
        this.vocabularyService = vocabularyService;
    }

    /**
     * Get authenticated user's ID from security context.
     */
    private UUID getCurrentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    @Operation(summary = "List user vocabulary", description = "Get all vocabulary entries for the authenticated user with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved vocabulary list"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    @GetMapping
    public ResponseEntity<Page<VocabularyDTO>> listVocabulary(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "desc") String sortDir,
            @Parameter(description = "Search term (optional)") @RequestParam(required = false) String search,
            @Parameter(description = "Filter by mastered status: 'all', 'mastered', 'unmastered'") @RequestParam(defaultValue = "all") String filter,
            Authentication authentication) {

        UUID userId = getCurrentUserId(authentication);
        logger.info("📥 GET /api/vocabulary - User: {}, page: {}, size: {}, search: '{}', filter: {}",
                userId, page, size, search, filter);

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<VocabularyDTO> result;
        String searchTerm = (search != null) ? search.trim() : "";
        boolean hasSearch = !searchTerm.isEmpty();
        boolean isMasteredFilter = "mastered".equalsIgnoreCase(filter);
        boolean isUnmasteredFilter = "unmastered".equalsIgnoreCase(filter);

        if (hasSearch && isMasteredFilter) {
            // Search + Mastered filter
            result = vocabularyService.searchWithFilter(userId, searchTerm, true, pageable);
        } else if (hasSearch && isUnmasteredFilter) {
            // Search + Unmastered filter
            result = vocabularyService.searchWithFilter(userId, searchTerm, false, pageable);
        } else if (hasSearch) {
            // Search only (all)
            result = vocabularyService.search(userId, searchTerm, pageable);
        } else if (isMasteredFilter) {
            // Mastered filter only
            result = vocabularyService.getByUserIdAndMastered(userId, true, pageable);
        } else if (isUnmasteredFilter) {
            // Unmastered filter only
            result = vocabularyService.getByUserIdAndMastered(userId, false, pageable);
        } else {
            // No filter, no search - return all
            result = vocabularyService.getByUserId(userId, pageable);
        }

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get vocabulary by ID", description = "Get a specific vocabulary entry by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vocabulary found"),
            @ApiResponse(responseCode = "404", description = "Vocabulary not found or access denied")
    })
    @GetMapping("/{id}")
    public ResponseEntity<VocabularyDTO> getVocabularyById(
            @Parameter(description = "Vocabulary entry ID") @PathVariable Long id,
            Authentication authentication) {

        UUID userId = getCurrentUserId(authentication);
        logger.info("📥 GET /api/vocabulary/{} - User: {}", id, userId);

        VocabularyDTO vocabulary = vocabularyService.getById(id, userId);
        return ResponseEntity.ok(vocabulary);
    }

    @Operation(summary = "Create vocabulary entry", description = "Add a new word to the vocabulary notebook. Optionally auto-translate using AI.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Vocabulary created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or word already exists"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<VocabularyDTO> createVocabulary(
            @Valid @RequestBody VocabularyCreateDTO createDTO,
            Authentication authentication) {

        UUID userId = getCurrentUserId(authentication);
        logger.info("📥 POST /api/vocabulary - User: {}, word: {}", userId, createDTO.getWord());

        try {
            VocabularyDTO created = vocabularyService.create(userId, createDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            logger.warn("Failed to create vocabulary: {}", e.getMessage());
            throw e;
        }
    }

    @Operation(summary = "Update vocabulary entry", description = "Update an existing vocabulary entry")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vocabulary updated successfully"),
            @ApiResponse(responseCode = "404", description = "Vocabulary not found or access denied"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PutMapping("/{id}")
    public ResponseEntity<VocabularyDTO> updateVocabulary(
            @Parameter(description = "Vocabulary entry ID") @PathVariable Long id,
            @RequestBody VocabularyDTO updateDTO,
            Authentication authentication) {

        UUID userId = getCurrentUserId(authentication);
        logger.info("📥 PUT /api/vocabulary/{} - User: {}", id, userId);

        VocabularyDTO updated = vocabularyService.update(id, userId, updateDTO);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Delete vocabulary entry", description = "Remove a word from the vocabulary notebook")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Vocabulary deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Vocabulary not found or access denied")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVocabulary(
            @Parameter(description = "Vocabulary entry ID") @PathVariable Long id,
            Authentication authentication) {

        UUID userId = getCurrentUserId(authentication);
        logger.info("📥 DELETE /api/vocabulary/{} - User: {}", id, userId);

        vocabularyService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Translate word using AI", description = "Translate an English word to Vietnamese using DeepSeek AI. Returns translation, phonetic, part of speech, definition, and example sentence.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Translation successful", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Translation failed")
    })
    @PostMapping("/translate")
    public ResponseEntity<Map<String, String>> translateWord(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        UUID userId = getCurrentUserId(authentication);
        String word = request.get("word");
        String context = request.get("context");

        if (word == null || word.trim().isEmpty()) {
            throw new IllegalArgumentException("Word is required");
        }

        logger.info("📥 POST /api/vocabulary/translate - User: {}, word: {}", userId, word);

        try {
            Map<String, String> translation = vocabularyService.translateWord(word.trim(), context, userId);
            return ResponseEntity.ok(translation);
        } catch (RuntimeException e) {
            logger.error("Translation failed: {}", e.getMessage());
            throw e;
        }
    }

    @Operation(summary = "Toggle mastered status", description = "Toggle the mastered status of a vocabulary entry and increment review count")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mastered status toggled"),
            @ApiResponse(responseCode = "404", description = "Vocabulary not found or access denied")
    })
    @PutMapping("/{id}/toggle-mastered")
    public ResponseEntity<VocabularyDTO> toggleMastered(
            @Parameter(description = "Vocabulary entry ID") @PathVariable Long id,
            Authentication authentication) {

        UUID userId = getCurrentUserId(authentication);
        logger.info("📥 PUT /api/vocabulary/{}/toggle-mastered - User: {}", id, userId);

        VocabularyDTO updated = vocabularyService.toggleMastered(id, userId);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Get vocabulary statistics", description = "Get statistics about user's vocabulary including total count, mastered count, and achievement progress")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        logger.info("📥 GET /api/vocabulary/stats - User: {}", userId);

        Map<String, Object> stats = vocabularyService.getStats(userId);
        return ResponseEntity.ok(stats);
    }
}
