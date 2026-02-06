package com.cramer.service.unit;

import com.cramer.dto.FullSectionDTO;
import com.cramer.dto.TestSectionDTO;
import com.cramer.entity.Question;
import com.cramer.entity.Section;
import com.cramer.repository.QuestionRepository;
import com.cramer.repository.SectionRepository;
import com.cramer.service.TestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TestService.
 * Tests fetching test data (sections, questions) for test taking.
 * 
 * @author Cramer Test Team
 * @since 2026-01-15
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TestService Unit Tests")
class TestServiceTest {

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private TestService testService;

    private Section testSection1;
    private Section testSection2;
    private Question question1;
    private Question question2;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        // Setup Section 1 (Reading Passage 1)
        testSection1 = new Section();
        testSection1.setId(1L);
        testSection1.setExamSource("cambridge");
        testSection1.setTestNumber(17);
        testSection1.setSkill("reading");
        testSection1.setPartNumber(1);
        testSection1.setPassageText("Plastics are synthetic materials...");

        // Setup Section 2 (Reading Passage 2)
        testSection2 = new Section();
        testSection2.setId(2L);
        testSection2.setExamSource("cambridge");
        testSection2.setTestNumber(17);
        testSection2.setSkill("reading");
        testSection2.setPartNumber(2);
        testSection2.setPassageText("Renewable energy has become...");

        // Setup Questions
        ObjectNode questionContent1 = objectMapper.createObjectNode();
        questionContent1.put("text", "The statement is true");

        question1 = new Question();
        question1.setId(1L);
        question1.setSectionId(1L);
        question1.setQuestionNumber(1);
        question1.setQuestionType("TRUE_FALSE_NG");
        question1.setQuestionContent(questionContent1);
        question1.setCorrectAnswer(new TextNode("TRUE"));

        ObjectNode questionContent2 = objectMapper.createObjectNode();
        questionContent2.put("text", "Match the paragraph");

        question2 = new Question();
        question2.setId(2L);
        question2.setSectionId(1L);
        question2.setQuestionNumber(2);
        question2.setQuestionType("MATCHING");
        question2.setQuestionContent(questionContent2);
        question2.setCorrectAnswer(new TextNode("B"));
    }

    // =========================================================================
    // GET SAFE TEST (WITHOUT ANSWERS) TESTS
    // =========================================================================
    @Nested
    @DisplayName("getSafeTest() Tests")
    class GetSafeTestTests {

        @Test
        @DisplayName("Should return test sections without answers")
        void getSafeTest_validParams_returnsSectionsWithoutAnswers() {
            // Arrange
            when(sectionRepository.findSectionsForTest("cambridge", 17, "reading"))
                    .thenReturn(List.of(testSection1, testSection2));
            when(questionRepository.findBySectionId(1L))
                    .thenReturn(List.of(question1, question2));
            when(questionRepository.findBySectionId(2L))
                    .thenReturn(List.of());

            // Act
            List<TestSectionDTO> result = testService.getSafeTest("cambridge", 17, "reading");

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getPassageText()).isEqualTo("Plastics are synthetic materials...");
            // Questions should not contain answers (TestQuestionDTO doesn't have correctAnswer field)
        }

        @Test
        @DisplayName("Should return empty list when no sections found")
        void getSafeTest_noSections_returnsEmpty() {
            // Arrange
            when(sectionRepository.findSectionsForTest("cambridge", 99, "reading"))
                    .thenReturn(List.of());

            // Act
            List<TestSectionDTO> result = testService.getSafeTest("cambridge", 99, "reading");

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when source is null")
        void getSafeTest_nullSource_throwsException() {
            // Act & Assert
            assertThatThrownBy(() -> testService.getSafeTest(null, 17, "reading"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Source");
        }

        @Test
        @DisplayName("Should throw exception when source is empty")
        void getSafeTest_emptySource_throwsException() {
            // Act & Assert
            assertThatThrownBy(() -> testService.getSafeTest("", 17, "reading"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Source");
        }

        @Test
        @DisplayName("Should throw exception when testNum is invalid")
        void getSafeTest_invalidTestNum_throwsException() {
            // Act & Assert
            assertThatThrownBy(() -> testService.getSafeTest("cambridge", 0, "reading"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Test number");

            assertThatThrownBy(() -> testService.getSafeTest("cambridge", -1, "reading"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Test number");
        }

        @Test
        @DisplayName("Should throw exception when skill is null")
        void getSafeTest_nullSkill_throwsException() {
            // Act & Assert
            assertThatThrownBy(() -> testService.getSafeTest("cambridge", 17, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Skill");
        }
    }

    // =========================================================================
    // GET FULL TEST (WITH ANSWERS - ADMIN) TESTS
    // =========================================================================
    @Nested
    @DisplayName("getFullTest() Tests")
    class GetFullTestTests {

        @Test
        @DisplayName("Should return test sections WITH answers for admin")
        void getFullTest_validParams_returnsSectionsWithAnswers() {
            // Arrange
            when(sectionRepository.findSectionsForTest("cambridge", 17, "reading"))
                    .thenReturn(List.of(testSection1));
            when(questionRepository.findBySectionId(1L))
                    .thenReturn(List.of(question1, question2));

            // Act
            List<FullSectionDTO> result = testService.getFullTest("cambridge", 17, "reading");

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getQuestions()).hasSize(2);
            // Full test includes answers - verify questions are mapped correctly
            assertThat(result.get(0).getQuestions().get(0).getQuestionNumber()).isEqualTo(1);
            assertThat(result.get(0).getQuestions().get(0).getQuestionType()).isEqualTo("TRUE_FALSE_NG");
        }

        @Test
        @DisplayName("Should handle sections with no questions")
        void getFullTest_sectionWithNoQuestions_returnsEmptyQuestions() {
            // Arrange
            when(sectionRepository.findSectionsForTest("cambridge", 17, "writing"))
                    .thenReturn(List.of(testSection1));
            when(questionRepository.findBySectionId(1L))
                    .thenReturn(List.of()); // No questions for writing sections

            // Act
            List<FullSectionDTO> result = testService.getFullTest("cambridge", 17, "writing");

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getQuestions()).isEmpty();
        }
    }

    // =========================================================================
    // INPUT VALIDATION TESTS
    // =========================================================================
    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {

        @Test
        @DisplayName("Should handle different skill types")
        void getTest_differentSkills_callsRepositoryCorrectly() {
            // Arrange
            when(sectionRepository.findSectionsForTest(anyString(), anyInt(), anyString()))
                    .thenReturn(List.of());

            // Act & Assert - All valid skills should work
            assertThatNoException().isThrownBy(() -> 
                    testService.getSafeTest("cambridge", 17, "reading"));
            assertThatNoException().isThrownBy(() -> 
                    testService.getSafeTest("cambridge", 17, "listening"));
            assertThatNoException().isThrownBy(() -> 
                    testService.getSafeTest("cambridge", 17, "writing"));
        }
    }
}
