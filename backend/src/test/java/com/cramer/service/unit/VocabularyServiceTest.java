package com.cramer.service.unit;

import com.cramer.config.LLMConfig;
import com.cramer.dto.VocabularyCreateDTO;
import com.cramer.dto.VocabularyDTO;
import com.cramer.entity.Vocabulary;
import com.cramer.repository.VocabularyRepository;
import com.cramer.service.TranslationBillingService;
import com.cramer.service.implement.VocabularyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VocabularyServiceImpl.
 * Tests CRUD operations and vocabulary statistics.
 * 
 * @author Cramer Test Team
 * @since 2026-01-25
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VocabularyService Unit Tests")
class VocabularyServiceTest {

    @Mock
    private VocabularyRepository vocabularyRepository;

    @Mock
    private TranslationBillingService translationBillingService;

    @Mock
    private LLMConfig llmConfig;

    @InjectMocks
    private VocabularyServiceImpl vocabularyService;

    private UUID testUserId;
    private Vocabulary mockVocabulary;

    @BeforeEach
    void setUp() {
        testUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockVocabulary = Vocabulary.builder()
                .id(1L)
                .userId(testUserId)
                .word("ubiquitous")
                .translation("phổ biến khắp nơi")
                .phonetic("/juːˈbɪk.wɪ.təs/")
                .partOfSpeech("adjective")
                .definition("existing or being everywhere")
                .exampleSentence("Smartphones have become ubiquitous in modern life.")
                .isMastered(false)
                .reviewCount(0)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    // =========================================================================
    // GET ALL BY USER ID TESTS
    // =========================================================================
    @Nested
    @DisplayName("getAllByUserId() Tests")
    class GetAllByUserIdTests {

        @Test
        @DisplayName("Should return list of vocabulary for user")
        void getAllByUserId_hasVocab_returnsList() {
            when(vocabularyRepository.findByUserIdOrderByCreatedAtDesc(testUserId))
                    .thenReturn(List.of(mockVocabulary));

            List<VocabularyDTO> result = vocabularyService.getAllByUserId(testUserId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getWord()).isEqualTo("ubiquitous");
        }

        @Test
        @DisplayName("Should return empty list when no vocabulary")
        void getAllByUserId_noVocab_returnsEmpty() {
            when(vocabularyRepository.findByUserIdOrderByCreatedAtDesc(testUserId))
                    .thenReturn(List.of());

            List<VocabularyDTO> result = vocabularyService.getAllByUserId(testUserId);

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // GET BY USER ID (PAGINATED) TESTS
    // =========================================================================
    @Nested
    @DisplayName("getByUserId() Paginated Tests")
    class GetByUserIdPaginatedTests {

        @Test
        @DisplayName("Should return paginated vocabulary")
        void getByUserId_validRequest_returnsPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Vocabulary> page = new PageImpl<>(List.of(mockVocabulary), pageable, 1);

            when(vocabularyRepository.findByUserId(testUserId, pageable)).thenReturn(page);

            Page<VocabularyDTO> result = vocabularyService.getByUserId(testUserId, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    // =========================================================================
    // GET BY ID TESTS
    // =========================================================================
    @Nested
    @DisplayName("getById() Tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should return vocabulary when found")
        void getById_found_returnsVocab() {
            when(vocabularyRepository.findByIdAndUserId(1L, testUserId))
                    .thenReturn(Optional.of(mockVocabulary));

            VocabularyDTO result = vocabularyService.getById(1L, testUserId);

            assertThat(result.getWord()).isEqualTo("ubiquitous");
            assertThat(result.getTranslation()).isEqualTo("phổ biến khắp nơi");
        }

        @Test
        @DisplayName("Should throw when vocabulary not found")
        void getById_notFound_throws() {
            when(vocabularyRepository.findByIdAndUserId(999L, testUserId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> vocabularyService.getById(999L, testUserId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found");
        }
    }

    // =========================================================================
    // CREATE TESTS
    // =========================================================================
    @Nested
    @DisplayName("create() Tests")
    class CreateTests {

        @Test
        @DisplayName("Should create vocabulary without auto-translate")
        void create_noAutoTranslate_savesVocab() {
            VocabularyCreateDTO createDTO = VocabularyCreateDTO.builder()
                    .word("ephemeral")
                    .translation("tạm thời")
                    .autoTranslate(false)
                    .build();

            when(vocabularyRepository.existsByUserIdAndWordIgnoreCase(testUserId, "ephemeral"))
                    .thenReturn(false);
            when(vocabularyRepository.save(any(Vocabulary.class)))
                    .thenAnswer(invocation -> {
                        Vocabulary v = invocation.getArgument(0);
                        v.setId(2L);
                        return v;
                    });

            VocabularyDTO result = vocabularyService.create(testUserId, createDTO);

            assertThat(result.getWord()).isEqualTo("ephemeral");
            assertThat(result.getTranslation()).isEqualTo("tạm thời");
            verify(vocabularyRepository).save(any(Vocabulary.class));
        }

        @Test
        @DisplayName("Should throw when word already exists")
        void create_duplicateWord_throws() {
            VocabularyCreateDTO createDTO = VocabularyCreateDTO.builder()
                    .word("ubiquitous")
                    .autoTranslate(false)
                    .build();

            when(vocabularyRepository.existsByUserIdAndWordIgnoreCase(testUserId, "ubiquitous"))
                    .thenReturn(true);

            assertThatThrownBy(() -> vocabularyService.create(testUserId, createDTO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("already exists");

            verify(vocabularyRepository, never()).save(any());
        }
    }

    // =========================================================================
    // UPDATE TESTS
    // =========================================================================
    @Nested
    @DisplayName("update() Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should update vocabulary fields")
        void update_validUpdate_savesChanges() {
            VocabularyDTO updateDTO = new VocabularyDTO();
            updateDTO.setTranslation("có mặt ở khắp nơi");
            updateDTO.setNotes("IELTS band 8 vocabulary");

            when(vocabularyRepository.findByIdAndUserId(1L, testUserId))
                    .thenReturn(Optional.of(mockVocabulary));
            when(vocabularyRepository.save(any(Vocabulary.class)))
                    .thenReturn(mockVocabulary);

            VocabularyDTO result = vocabularyService.update(1L, testUserId, updateDTO);

            assertThat(result).isNotNull();
            verify(vocabularyRepository).save(any(Vocabulary.class));
        }

        @Test
        @DisplayName("Should throw when vocabulary not found")
        void update_notFound_throws() {
            VocabularyDTO updateDTO = new VocabularyDTO();

            when(vocabularyRepository.findByIdAndUserId(999L, testUserId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> vocabularyService.update(999L, testUserId, updateDTO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("Should throw when changing word to existing one")
        void update_duplicateWord_throws() {
            VocabularyDTO updateDTO = new VocabularyDTO();
            updateDTO.setWord("ephemeral");

            when(vocabularyRepository.findByIdAndUserId(1L, testUserId))
                    .thenReturn(Optional.of(mockVocabulary));
            when(vocabularyRepository.existsByUserIdAndWordIgnoreCase(testUserId, "ephemeral"))
                    .thenReturn(true);

            assertThatThrownBy(() -> vocabularyService.update(1L, testUserId, updateDTO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("already exists");
        }
    }

    // =========================================================================
    // DELETE TESTS
    // =========================================================================
    @Nested
    @DisplayName("delete() Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should delete vocabulary")
        void delete_found_deletesVocab() {
            when(vocabularyRepository.findByIdAndUserId(1L, testUserId))
                    .thenReturn(Optional.of(mockVocabulary));
            doNothing().when(vocabularyRepository).delete(mockVocabulary);

            vocabularyService.delete(1L, testUserId);

            verify(vocabularyRepository).delete(mockVocabulary);
        }

        @Test
        @DisplayName("Should throw when vocabulary not found")
        void delete_notFound_throws() {
            when(vocabularyRepository.findByIdAndUserId(999L, testUserId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> vocabularyService.delete(999L, testUserId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found");
        }
    }

    // =========================================================================
    // TOGGLE MASTERED TESTS
    // =========================================================================
    @Nested
    @DisplayName("toggleMastered() Tests")
    class ToggleMasteredTests {

        @Test
        @DisplayName("Should toggle mastered from false to true")
        void toggleMastered_false_becomesTrue() {
            mockVocabulary.setIsMastered(false);
            mockVocabulary.setReviewCount(0);

            when(vocabularyRepository.findByIdAndUserId(1L, testUserId))
                    .thenReturn(Optional.of(mockVocabulary));
            when(vocabularyRepository.save(any(Vocabulary.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            VocabularyDTO result = vocabularyService.toggleMastered(1L, testUserId);

            assertThat(result.getIsMastered()).isTrue();
        }

        @Test
        @DisplayName("Should toggle mastered from true to false")
        void toggleMastered_true_becomesFalse() {
            mockVocabulary.setIsMastered(true);
            mockVocabulary.setReviewCount(5);

            when(vocabularyRepository.findByIdAndUserId(1L, testUserId))
                    .thenReturn(Optional.of(mockVocabulary));
            when(vocabularyRepository.save(any(Vocabulary.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            VocabularyDTO result = vocabularyService.toggleMastered(1L, testUserId);

            assertThat(result.getIsMastered()).isFalse();
        }
    }

    // =========================================================================
    // GET STATS TESTS
    // =========================================================================
    @Nested
    @DisplayName("getStats() Tests")
    class GetStatsTests {

        @Test
        @DisplayName("Should return vocabulary statistics")
        void getStats_hasVocab_returnsStats() {
            when(vocabularyRepository.countByUserId(testUserId)).thenReturn(100L);
            when(vocabularyRepository.countByUserIdAndIsMastered(testUserId, true)).thenReturn(40L);

            Map<String, Object> stats = vocabularyService.getStats(testUserId);

            assertThat(stats.get("total")).isEqualTo(100L);
            assertThat(stats.get("mastered")).isEqualTo(40L);
            assertThat(stats.get("learning")).isEqualTo(60L);
            assertThat((Double) stats.get("masteredPercentage")).isEqualTo(40.0);
        }

        @Test
        @DisplayName("Should return zero stats when no vocabulary")
        void getStats_noVocab_returnsZeros() {
            when(vocabularyRepository.countByUserId(testUserId)).thenReturn(0L);
            when(vocabularyRepository.countByUserIdAndIsMastered(testUserId, true)).thenReturn(0L);

            Map<String, Object> stats = vocabularyService.getStats(testUserId);

            assertThat(stats.get("total")).isEqualTo(0L);
            assertThat(stats.get("mastered")).isEqualTo(0L);
            assertThat(stats.get("learning")).isEqualTo(0L);
            assertThat((Double) stats.get("masteredPercentage")).isEqualTo(0.0);
        }
    }

    // =========================================================================
    // SEARCH TESTS
    // =========================================================================
    @Nested
    @DisplayName("search() Tests")
    class SearchTests {

        @Test
        @DisplayName("Should return matching vocabulary")
        void search_hasMatch_returnsResults() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Vocabulary> page = new PageImpl<>(List.of(mockVocabulary), pageable, 1);

            when(vocabularyRepository.searchByWord(testUserId, "ubiq", pageable))
                    .thenReturn(page);

            Page<VocabularyDTO> result = vocabularyService.search(testUserId, "ubiq", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getWord()).isEqualTo("ubiquitous");
        }

        @Test
        @DisplayName("Should return empty when no match")
        void search_noMatch_returnsEmpty() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Vocabulary> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(vocabularyRepository.searchByWord(testUserId, "xyz", pageable))
                    .thenReturn(emptyPage);

            Page<VocabularyDTO> result = vocabularyService.search(testUserId, "xyz", pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    // =========================================================================
    // GET BY USER ID AND MASTERED TESTS
    // =========================================================================
    @Nested
    @DisplayName("getByUserIdAndMastered() Tests")
    class GetByUserIdAndMasteredTests {

        @Test
        @DisplayName("Should return only mastered vocabulary")
        void getByUserIdAndMastered_mastered_returnsFiltered() {
            mockVocabulary.setIsMastered(true);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Vocabulary> page = new PageImpl<>(List.of(mockVocabulary), pageable, 1);

            when(vocabularyRepository.findByUserIdAndIsMastered(testUserId, true, pageable))
                    .thenReturn(page);

            Page<VocabularyDTO> result = vocabularyService.getByUserIdAndMastered(testUserId, true, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getIsMastered()).isTrue();
        }

        @Test
        @DisplayName("Should return only learning vocabulary")
        void getByUserIdAndMastered_learning_returnsFiltered() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Vocabulary> page = new PageImpl<>(List.of(mockVocabulary), pageable, 1);

            when(vocabularyRepository.findByUserIdAndIsMastered(testUserId, false, pageable))
                    .thenReturn(page);

            Page<VocabularyDTO> result = vocabularyService.getByUserIdAndMastered(testUserId, false, pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }
}
