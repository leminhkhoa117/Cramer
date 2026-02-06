package com.cramer.service.unit;

import com.cramer.entity.Question;
import com.cramer.repository.QuestionRepository;
import com.cramer.service.QuestionService;
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
 * Unit tests for QuestionService.
 * Tests question CRUD operations and query methods.
 * 
 * @author Cramer Test Team
 * @since 2026-01-25
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionService Unit Tests")
class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private QuestionService questionService;

    private Question mockQuestion;

    @BeforeEach
    void setUp() {
        mockQuestion = new Question();
        mockQuestion.setId(1L);
        mockQuestion.setSectionId(100L);
        mockQuestion.setQuestionNumber(1);
        mockQuestion.setQuestionUid("cam17-t1-reading-p1-q1");
        mockQuestion.setQuestionType("multiple_choice");
    }

    // =========================================================================
    // GET ALL QUESTIONS TESTS
    // =========================================================================
    @Nested
    @DisplayName("getAllQuestions() Tests")
    class GetAllQuestionsTests {

        @Test
        @DisplayName("Should return all questions")
        void getAllQuestions_hasQuestions_returnsList() {
            Question question2 = new Question();
            question2.setId(2L);
            question2.setSectionId(100L);
            question2.setQuestionNumber(2);
            question2.setQuestionUid("cam17-t1-reading-p1-q2");

            when(questionRepository.findAll()).thenReturn(List.of(mockQuestion, question2));

            List<Question> result = questionService.getAllQuestions();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty list when no questions")
        void getAllQuestions_noQuestions_returnsEmpty() {
            when(questionRepository.findAll()).thenReturn(List.of());

            List<Question> result = questionService.getAllQuestions();

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // GET QUESTION BY ID TESTS
    // =========================================================================
    @Nested
    @DisplayName("getQuestionById() Tests")
    class GetQuestionByIdTests {

        @Test
        @DisplayName("Should return question when found")
        void getQuestionById_found_returnsQuestion() {
            when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));

            Optional<Question> result = questionService.getQuestionById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getQuestionUid()).isEqualTo("cam17-t1-reading-p1-q1");
        }

        @Test
        @DisplayName("Should return empty when not found")
        void getQuestionById_notFound_returnsEmpty() {
            when(questionRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<Question> result = questionService.getQuestionById(999L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should throw NullPointerException for null id")
        void getQuestionById_nullId_throws() {
            assertThatThrownBy(() -> questionService.getQuestionById(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // =========================================================================
    // GET QUESTIONS BY SECTION ID TESTS
    // =========================================================================
    @Nested
    @DisplayName("getQuestionsBySectionId() Tests")
    class GetQuestionsBySectionIdTests {

        @Test
        @DisplayName("Should return list of questions for section")
        void getQuestionsBySectionId_found_returnsList() {
            when(questionRepository.findBySectionId(100L)).thenReturn(List.of(mockQuestion));

            List<Question> result = questionService.getQuestionsBySectionId(100L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSectionId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Should return empty list when no questions in section")
        void getQuestionsBySectionId_noMatch_returnsEmpty() {
            when(questionRepository.findBySectionId(999L)).thenReturn(List.of());

            List<Question> result = questionService.getQuestionsBySectionId(999L);

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // GET QUESTION BY UID TESTS
    // =========================================================================
    @Nested
    @DisplayName("getQuestionByUid() Tests")
    class GetQuestionByUidTests {

        @Test
        @DisplayName("Should find question by unique identifier")
        void getQuestionByUid_found_returnsQuestion() {
            when(questionRepository.findByQuestionUid("cam17-t1-reading-p1-q1"))
                    .thenReturn(Optional.of(mockQuestion));

            Optional<Question> result = questionService.getQuestionByUid("cam17-t1-reading-p1-q1");

            assertThat(result).isPresent();
            assertThat(result.get().getQuestionNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return empty when UID not found")
        void getQuestionByUid_notFound_returnsEmpty() {
            when(questionRepository.findByQuestionUid("unknown-uid"))
                    .thenReturn(Optional.empty());

            Optional<Question> result = questionService.getQuestionByUid("unknown-uid");

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // GET QUESTIONS BY TYPE TESTS
    // =========================================================================
    @Nested
    @DisplayName("getQuestionsByType() Tests")
    class GetQuestionsByTypeTests {

        @Test
        @DisplayName("Should filter by question type")
        void getQuestionsByType_found_returnsList() {
            when(questionRepository.findByQuestionType("multiple_choice"))
                    .thenReturn(List.of(mockQuestion));

            List<Question> result = questionService.getQuestionsByType("multiple_choice");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getQuestionType()).isEqualTo("multiple_choice");
        }

        @Test
        @DisplayName("Should return empty when no match")
        void getQuestionsByType_noMatch_returnsEmpty() {
            when(questionRepository.findByQuestionType("unknown_type"))
                    .thenReturn(List.of());

            List<Question> result = questionService.getQuestionsByType("unknown_type");

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // GET QUESTIONS BY SECTION AND TYPE TESTS
    // =========================================================================
    @Nested
    @DisplayName("getQuestionsBySectionAndType() Tests")
    class GetQuestionsBySectionAndTypeTests {

        @Test
        @DisplayName("Should filter by section and type")
        void getQuestionsBySectionAndType_found_returnsList() {
            when(questionRepository.findBySectionIdAndQuestionType(100L, "multiple_choice"))
                    .thenReturn(List.of(mockQuestion));

            List<Question> result = questionService.getQuestionsBySectionAndType(100L, "multiple_choice");

            assertThat(result).hasSize(1);
        }
    }

    // =========================================================================
    // GET QUESTION BY SECTION AND NUMBER TESTS
    // =========================================================================
    @Nested
    @DisplayName("getQuestionBySectionAndNumber() Tests")
    class GetQuestionBySectionAndNumberTests {

        @Test
        @DisplayName("Should find question by section and number")
        void getQuestionBySectionAndNumber_found_returnsQuestion() {
            when(questionRepository.findBySectionIdAndQuestionNumber(100L, 1))
                    .thenReturn(Optional.of(mockQuestion));

            Optional<Question> result = questionService.getQuestionBySectionAndNumber(100L, 1);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("Should return empty when not found")
        void getQuestionBySectionAndNumber_notFound_returnsEmpty() {
            when(questionRepository.findBySectionIdAndQuestionNumber(100L, 99))
                    .thenReturn(Optional.empty());

            Optional<Question> result = questionService.getQuestionBySectionAndNumber(100L, 99);

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // GET ALL QUESTION TYPES TESTS
    // =========================================================================
    @Nested
    @DisplayName("getAllQuestionTypes() Tests")
    class GetAllQuestionTypesTests {

        @Test
        @DisplayName("Should return all distinct question types")
        void getAllQuestionTypes_hasTypes_returnsList() {
            when(questionRepository.findAllDistinctQuestionTypes())
                    .thenReturn(List.of("multiple_choice", "true_false_ng", "matching", "sentence_completion"));

            List<String> result = questionService.getAllQuestionTypes();

            assertThat(result).hasSize(4);
            assertThat(result).contains("multiple_choice", "true_false_ng");
        }
    }

    // =========================================================================
    // CREATE QUESTION TESTS
    // =========================================================================
    @Nested
    @DisplayName("createQuestion() Tests")
    class CreateQuestionTests {

        @Test
        @DisplayName("Should create question successfully")
        void createQuestion_valid_savesQuestion() {
            Question newQuestion = new Question();
            newQuestion.setSectionId(100L);
            newQuestion.setQuestionNumber(2);
            newQuestion.setQuestionUid("cam17-t1-reading-p1-q2");
            newQuestion.setQuestionType("true_false_ng");

            when(questionRepository.existsByQuestionUid("cam17-t1-reading-p1-q2")).thenReturn(false);
            when(questionRepository.save(any(Question.class)))
                    .thenAnswer(invocation -> {
                        Question q = invocation.getArgument(0);
                        q.setId(2L);
                        return q;
                    });

            Question result = questionService.createQuestion(newQuestion);

            assertThat(result.getId()).isEqualTo(2L);
            verify(questionRepository).save(newQuestion);
        }

        @Test
        @DisplayName("Should throw on duplicate UID")
        void createQuestion_duplicateUid_throws() {
            when(questionRepository.existsByQuestionUid("cam17-t1-reading-p1-q1")).thenReturn(true);

            assertThatThrownBy(() -> questionService.createQuestion(mockQuestion))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");

            verify(questionRepository, never()).save(any());
        }
    }

    // =========================================================================
    // UPDATE QUESTION TESTS
    // =========================================================================
    @Nested
    @DisplayName("updateQuestion() Tests")
    class UpdateQuestionTests {

        @Test
        @DisplayName("Should update question successfully")
        void updateQuestion_valid_updatesQuestion() {
            Question updatedData = new Question();
            updatedData.setSectionId(100L);
            updatedData.setQuestionNumber(1);
            updatedData.setQuestionUid("cam17-t1-reading-p1-q1");
            updatedData.setQuestionType("multiple_choice");

            when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));
            when(questionRepository.save(any(Question.class))).thenReturn(mockQuestion);

            Question result = questionService.updateQuestion(1L, updatedData);

            assertThat(result).isNotNull();
            verify(questionRepository).save(any(Question.class));
        }

        @Test
        @DisplayName("Should throw when question not found")
        void updateQuestion_notFound_throws() {
            when(questionRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> questionService.updateQuestion(999L, mockQuestion))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("Should throw when changing to existing UID")
        void updateQuestion_duplicateUid_throws() {
            Question updatedData = new Question();
            updatedData.setSectionId(100L);
            updatedData.setQuestionNumber(1);
            updatedData.setQuestionUid("existing-uid");

            when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));
            when(questionRepository.existsByQuestionUid("existing-uid")).thenReturn(true);

            assertThatThrownBy(() -> questionService.updateQuestion(1L, updatedData))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already taken");
        }

        @Test
        @DisplayName("Should allow update with same UID")
        void updateQuestion_sameUid_succeeds() {
            Question updatedData = new Question();
            updatedData.setSectionId(100L);
            updatedData.setQuestionNumber(1);
            updatedData.setQuestionUid("cam17-t1-reading-p1-q1");

            when(questionRepository.findById(1L)).thenReturn(Optional.of(mockQuestion));
            when(questionRepository.save(any(Question.class))).thenReturn(mockQuestion);

            Question result = questionService.updateQuestion(1L, updatedData);

            assertThat(result).isNotNull();
            verify(questionRepository, never()).existsByQuestionUid(any());
        }
    }

    // =========================================================================
    // DELETE QUESTION TESTS
    // =========================================================================
    @Nested
    @DisplayName("deleteQuestion() Tests")
    class DeleteQuestionTests {

        @Test
        @DisplayName("Should delete question successfully")
        void deleteQuestion_exists_deletes() {
            when(questionRepository.existsById(1L)).thenReturn(true);
            doNothing().when(questionRepository).deleteById(1L);

            questionService.deleteQuestion(1L);

            verify(questionRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw when question not found")
        void deleteQuestion_notFound_throws() {
            when(questionRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> questionService.deleteQuestion(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");

            verify(questionRepository, never()).deleteById(any());
        }
    }

    // =========================================================================
    // DELETE QUESTIONS BY SECTION TESTS
    // =========================================================================
    @Nested
    @DisplayName("deleteQuestionsBySection() Tests")
    class DeleteQuestionsBySectionTests {

        @Test
        @DisplayName("Should delete all questions in section")
        void deleteQuestionsBySection_hasQuestions_deletesAll() {
            when(questionRepository.countBySectionId(100L)).thenReturn(5L);
            doNothing().when(questionRepository).deleteBySectionId(100L);

            questionService.deleteQuestionsBySection(100L);

            verify(questionRepository).deleteBySectionId(100L);
        }
    }

    // =========================================================================
    // COUNT METHODS TESTS
    // =========================================================================
    @Nested
    @DisplayName("Count Methods Tests")
    class CountMethodsTests {

        @Test
        @DisplayName("Should count questions by section ID")
        void countBySectionId_returnsCount() {
            when(questionRepository.countBySectionId(100L)).thenReturn(13L);

            long result = questionService.countBySectionId(100L);

            assertThat(result).isEqualTo(13L);
        }

        @Test
        @DisplayName("Should get total question count")
        void getTotalQuestionCount_returnsCount() {
            when(questionRepository.count()).thenReturn(500L);

            long result = questionService.getTotalQuestionCount();

            assertThat(result).isEqualTo(500L);
        }
    }
}
