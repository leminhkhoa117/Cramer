package com.cramer.service.unit;

import com.cramer.dto.FullSectionDTO;
import com.cramer.entity.Question;
import com.cramer.entity.Section;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.repository.SectionRepository;
import com.cramer.service.QuestionService;
import com.cramer.service.SectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SectionService.
 * Tests section CRUD operations and query methods.
 * 
 * @author Cramer Test Team
 * @since 2026-01-25
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SectionService Unit Tests")
class SectionServiceTest {

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private QuestionService questionService;

    @InjectMocks
    private SectionService sectionService;

    private Section mockSection;
    private Question mockQuestion;

    @BeforeEach
    void setUp() {
        mockSection = new Section();
        mockSection.setId(1L);
        mockSection.setExamSource("cam17");
        mockSection.setTestNumber(1);
        mockSection.setSkill("reading");
        mockSection.setPartNumber(1);
        mockSection.setPassageText("Sample passage text for reading test.");
        mockSection.setDisplayContentUrl("https://example.com/content");

        mockQuestion = new Question();
        mockQuestion.setId(1L);
        mockQuestion.setSectionId(1L);
        mockQuestion.setQuestionNumber(1);
        mockQuestion.setQuestionType("multiple_choice");
        mockQuestion.setQuestionUid("cam17-t1-reading-p1-q1");
    }

    // =========================================================================
    // GET FULL SECTION BY ID TESTS
    // =========================================================================
    @Nested
    @DisplayName("getFullSectionById() Tests")
    class GetFullSectionByIdTests {

        @Test
        @DisplayName("Should return section with questions")
        void getFullSectionById_found_returnsSectionWithQuestions() {
            when(sectionRepository.findById(1L)).thenReturn(Optional.of(mockSection));
            when(questionService.getQuestionsBySectionId(1L)).thenReturn(List.of(mockQuestion));

            FullSectionDTO result = sectionService.getFullSectionById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getSkill()).isEqualTo("reading");
            assertThat(result.getQuestions()).hasSize(1);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when not found")
        void getFullSectionById_notFound_throws() {
            when(sectionRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sectionService.getFullSectionById(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should return section with empty questions list")
        void getFullSectionById_noQuestions_returnsEmptyList() {
            when(sectionRepository.findById(1L)).thenReturn(Optional.of(mockSection));
            when(questionService.getQuestionsBySectionId(1L)).thenReturn(List.of());

            FullSectionDTO result = sectionService.getFullSectionById(1L);

            assertThat(result.getQuestions()).isEmpty();
        }
    }

    // =========================================================================
    // GET ALL SECTIONS TESTS
    // =========================================================================
    @Nested
    @DisplayName("getAllSections() Tests")
    class GetAllSectionsTests {

        @Test
        @DisplayName("Should return all sections")
        void getAllSections_hasSections_returnsList() {
            Section section2 = new Section();
            section2.setId(2L);
            section2.setExamSource("cam17");
            section2.setTestNumber(1);
            section2.setSkill("listening");
            section2.setPartNumber(1);

            when(sectionRepository.findAll()).thenReturn(List.of(mockSection, section2));

            List<Section> result = sectionService.getAllSections();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty list when no sections")
        void getAllSections_noSections_returnsEmpty() {
            when(sectionRepository.findAll()).thenReturn(List.of());

            List<Section> result = sectionService.getAllSections();

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // GET SECTION BY ID TESTS
    // =========================================================================
    @Nested
    @DisplayName("getSectionById() Tests")
    class GetSectionByIdTests {

        @Test
        @DisplayName("Should return section when found")
        void getSectionById_found_returnsSection() {
            when(sectionRepository.findById(1L)).thenReturn(Optional.of(mockSection));

            Optional<Section> result = sectionService.getSectionById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getExamSource()).isEqualTo("cam17");
        }

        @Test
        @DisplayName("Should return empty when not found")
        void getSectionById_notFound_returnsEmpty() {
            when(sectionRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<Section> result = sectionService.getSectionById(999L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should throw NullPointerException for null id")
        void getSectionById_nullId_throws() {
            assertThatThrownBy(() -> sectionService.getSectionById(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // =========================================================================
    // GET SECTIONS BY EXAM SOURCE TESTS
    // =========================================================================
    @Nested
    @DisplayName("getSectionsByExamSource() Tests")
    class GetSectionsByExamSourceTests {

        @Test
        @DisplayName("Should filter by exam source")
        void getSectionsByExamSource_found_returnsList() {
            when(sectionRepository.findByExamSource("cam17")).thenReturn(List.of(mockSection));

            List<Section> result = sectionService.getSectionsByExamSource("cam17");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getExamSource()).isEqualTo("cam17");
        }

        @Test
        @DisplayName("Should return empty when no match")
        void getSectionsByExamSource_noMatch_returnsEmpty() {
            when(sectionRepository.findByExamSource("cam99")).thenReturn(List.of());

            List<Section> result = sectionService.getSectionsByExamSource("cam99");

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // GET SECTIONS BY TEST TESTS
    // =========================================================================
    @Nested
    @DisplayName("getSectionsByTest() Tests")
    class GetSectionsByTestTests {

        @Test
        @DisplayName("Should filter by source and testNumber")
        void getSectionsByTest_found_returnsList() {
            when(sectionRepository.findByExamSourceAndTestNumber("cam17", 1))
                    .thenReturn(List.of(mockSection));

            List<Section> result = sectionService.getSectionsByTest("cam17", 1);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should return empty when no match")
        void getSectionsByTest_noMatch_returnsEmpty() {
            when(sectionRepository.findByExamSourceAndTestNumber("cam17", 99))
                    .thenReturn(List.of());

            List<Section> result = sectionService.getSectionsByTest("cam17", 99);

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // GET SECTIONS BY SKILL TESTS
    // =========================================================================
    @Nested
    @DisplayName("getSectionsBySkill() Tests")
    class GetSectionsBySkillTests {

        @Test
        @DisplayName("Should filter by skill")
        void getSectionsBySkill_found_returnsList() {
            when(sectionRepository.findBySkill("reading")).thenReturn(List.of(mockSection));

            List<Section> result = sectionService.getSectionsBySkill("reading");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSkill()).isEqualTo("reading");
        }
    }

    // =========================================================================
    // GET SPECIFIC SECTION TESTS
    // =========================================================================
    @Nested
    @DisplayName("getSpecificSection() Tests")
    class GetSpecificSectionTests {

        @Test
        @DisplayName("Should find specific section by all parameters")
        void getSpecificSection_found_returnsSection() {
            when(sectionRepository.findByExamSourceAndTestNumberAndSkillAndPartNumber(
                    "cam17", 1, "reading", 1))
                    .thenReturn(Optional.of(mockSection));

            Optional<Section> result = sectionService.getSpecificSection("cam17", 1, "reading", 1);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("Should return empty when not found")
        void getSpecificSection_notFound_returnsEmpty() {
            when(sectionRepository.findByExamSourceAndTestNumberAndSkillAndPartNumber(
                    "cam17", 1, "reading", 99))
                    .thenReturn(Optional.empty());

            Optional<Section> result = sectionService.getSpecificSection("cam17", 1, "reading", 99);

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // GET SECTIONS FOR TEST TESTS
    // =========================================================================
    @Nested
    @DisplayName("getSectionsForTest() Tests")
    class GetSectionsForTestTests {

        @Test
        @DisplayName("Should return ordered sections")
        void getSectionsForTest_found_returnsOrderedList() {
            Section part2 = new Section();
            part2.setId(2L);
            part2.setExamSource("cam17");
            part2.setTestNumber(1);
            part2.setSkill("reading");
            part2.setPartNumber(2);

            when(sectionRepository.findSectionsForTest("cam17", 1, "reading"))
                    .thenReturn(List.of(mockSection, part2));

            List<Section> result = sectionService.getSectionsForTest("cam17", 1, "reading");

            assertThat(result).hasSize(2);
        }
    }

    // =========================================================================
    // CREATE SECTION TESTS
    // =========================================================================
    @Nested
    @DisplayName("createSection() Tests")
    class CreateSectionTests {

        @Test
        @DisplayName("Should create section successfully")
        void createSection_valid_savesSection() {
            Section newSection = new Section();
            newSection.setExamSource("cam18");
            newSection.setTestNumber(1);
            newSection.setSkill("writing");
            newSection.setPartNumber(1);

            when(sectionRepository.existsByExamSourceAndTestNumberAndSkillAndPartNumber(
                    "cam18", 1, "writing", 1)).thenReturn(false);
            when(sectionRepository.save(any(Section.class)))
                    .thenAnswer(invocation -> {
                        Section s = invocation.getArgument(0);
                        s.setId(10L);
                        return s;
                    });

            Section result = sectionService.createSection(newSection);

            assertThat(result.getId()).isEqualTo(10L);
            verify(sectionRepository).save(newSection);
        }

        @Test
        @DisplayName("Should throw on duplicate section")
        void createSection_duplicate_throws() {
            when(sectionRepository.existsByExamSourceAndTestNumberAndSkillAndPartNumber(
                    "cam17", 1, "reading", 1)).thenReturn(true);

            assertThatThrownBy(() -> sectionService.createSection(mockSection))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");

            verify(sectionRepository, never()).save(any());
        }
    }

    // =========================================================================
    // UPDATE SECTION TESTS
    // =========================================================================
    @Nested
    @DisplayName("updateSection() Tests")
    class UpdateSectionTests {

        @Test
        @DisplayName("Should update section successfully")
        void updateSection_valid_updatesSection() {
            Section updatedData = new Section();
            updatedData.setExamSource("cam17");
            updatedData.setTestNumber(1);
            updatedData.setSkill("reading");
            updatedData.setPartNumber(1);
            updatedData.setPassageText("Updated passage text");

            when(sectionRepository.findById(1L)).thenReturn(Optional.of(mockSection));
            when(sectionRepository.save(any(Section.class))).thenReturn(mockSection);

            Section result = sectionService.updateSection(1L, updatedData);

            assertThat(result).isNotNull();
            verify(sectionRepository).save(any(Section.class));
        }

        @Test
        @DisplayName("Should throw when section not found")
        void updateSection_notFound_throws() {
            when(sectionRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sectionService.updateSection(999L, mockSection))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("Should throw NullPointerException for null id")
        void updateSection_nullId_throws() {
            assertThatThrownBy(() -> sectionService.updateSection(null, mockSection))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // =========================================================================
    // DELETE SECTION TESTS
    // =========================================================================
    @Nested
    @DisplayName("deleteSection() Tests")
    class DeleteSectionTests {

        @Test
        @DisplayName("Should delete section successfully")
        void deleteSection_exists_deletes() {
            when(sectionRepository.existsById(1L)).thenReturn(true);
            doNothing().when(sectionRepository).deleteById(1L);

            sectionService.deleteSection(1L);

            verify(sectionRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw when section not found")
        void deleteSection_notFound_throws() {
            when(sectionRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> sectionService.deleteSection(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");

            verify(sectionRepository, never()).deleteById(any());
        }
    }

    // =========================================================================
    // COUNT METHODS TESTS
    // =========================================================================
    @Nested
    @DisplayName("Count Methods Tests")
    class CountMethodsTests {

        @Test
        @DisplayName("Should count sections by exam source")
        void countByExamSource_returnsCount() {
            when(sectionRepository.countByExamSource("cam17")).thenReturn(12L);

            long result = sectionService.countByExamSource("cam17");

            assertThat(result).isEqualTo(12L);
        }

        @Test
        @DisplayName("Should get total section count")
        void getTotalSectionCount_returnsCount() {
            when(sectionRepository.count()).thenReturn(100L);

            long result = sectionService.getTotalSectionCount();

            assertThat(result).isEqualTo(100L);
        }
    }

    // =========================================================================
    // GET SECTIONS BY TEST ID TESTS
    // =========================================================================
    @Nested
    @DisplayName("getSectionsByTestId() Tests")
    class GetSectionsByTestIdTests {

        @Test
        @DisplayName("Should return sections for test ID")
        void getSectionsByTestId_found_returnsList() {
            when(sectionRepository.findByIeltsTestId(100L)).thenReturn(List.of(mockSection));

            List<Section> result = sectionService.getSectionsByTestId(100L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should return empty when no sections for test ID")
        void getSectionsByTestId_noMatch_returnsEmpty() {
            when(sectionRepository.findByIeltsTestId(999L)).thenReturn(List.of());

            List<Section> result = sectionService.getSectionsByTestId(999L);

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // GET SECTIONS BY TEST ID AND SKILL TESTS
    // =========================================================================
    @Nested
    @DisplayName("getSectionsByTestIdAndSkill() Tests")
    class GetSectionsByTestIdAndSkillTests {

        @Test
        @DisplayName("Should return sections for test ID and skill")
        void getSectionsByTestIdAndSkill_found_returnsList() {
            when(sectionRepository.findSectionsForTestId(100L, "reading"))
                    .thenReturn(List.of(mockSection));

            List<Section> result = sectionService.getSectionsByTestIdAndSkill(100L, "reading");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSkill()).isEqualTo("reading");
        }
    }
}
