package com.cramer.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entity representing individual questions within a section.
 * Uses JSONB columns for flexible question content and answers.
 */
@Entity
@Table(name = "questions", schema = "public")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "section_id", nullable = false)
    private Long sectionId; // Foreign key to sections table

    @Column(name = "question_number", nullable = false)
    private Integer questionNumber; // Sequential number within section

    @Column(name = "question_uid", nullable = false)
    private String questionUid; // Unique identifier (e.g., "cam17-t1-r-q1")

    @Column(name = "question_type", nullable = false)
    private String questionType; // e.g., "FILL_IN_BLANK", "TRUE_FALSE_NOT_GIVEN"

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_content", columnDefinition = "jsonb", nullable = false)
    private JsonNode questionContent; // Stores question text and options as JSON

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "correct_answer", columnDefinition = "jsonb", nullable = false)
    private JsonNode correctAnswer; // Stores correct answer(s) as JSON array

    // Constructors
    public Question() {
    }

    public Question(Long sectionId, Integer questionNumber, String questionUid, 
                   String questionType, JsonNode questionContent, JsonNode correctAnswer) {
        this.sectionId = sectionId;
        this.questionNumber = questionNumber;
        this.questionUid = questionUid;
        this.questionType = questionType;
        this.questionContent = questionContent;
        this.correctAnswer = correctAnswer;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public Integer getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(Integer questionNumber) {
        this.questionNumber = questionNumber;
    }

    public String getQuestionUid() {
        return questionUid;
    }

    public void setQuestionUid(String questionUid) {
        this.questionUid = questionUid;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public JsonNode getQuestionContent() {
        return questionContent;
    }

    public void setQuestionContent(JsonNode questionContent) {
        this.questionContent = questionContent;
    }

    public JsonNode getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(JsonNode correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    @Override
    public String toString() {
        return "Question{" +
                "id=" + id +
                ", sectionId=" + sectionId +
                ", questionNumber=" + questionNumber +
                ", questionUid='" + questionUid + '\'' +
                ", questionType='" + questionType + '\'' +
                '}';
    }
}
