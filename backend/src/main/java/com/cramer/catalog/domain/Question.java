package com.cramer.catalog.domain;

import com.cramer.platform.common.ielts.QuestionType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * An authored IELTS question or Speaking prompt, table {@code questions} (SPEC-11 §1).
 *
 * <p>{@code correctAnswer} and {@code explanation} are answer-key material and must never be
 * exposed by test-delivery or generic reads — only via review (owner) and admin (SPEC-04 §3).
 */
@Entity
@Table(name = "questions", schema = "public")
@Getter
@Setter
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "section_id")
    private Long sectionId;

    @Column(name = "question_number")
    private Integer questionNumber;

    @Column(name = "question_uid", unique = true)
    private String questionUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type")
    private QuestionType questionType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_content", columnDefinition = "jsonb")
    private JsonNode questionContent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "correct_answer", columnDefinition = "jsonb")
    private JsonNode correctAnswer;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "explanation", columnDefinition = "jsonb")
    private JsonNode explanation;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "word_limit")
    private String wordLimit;
}
