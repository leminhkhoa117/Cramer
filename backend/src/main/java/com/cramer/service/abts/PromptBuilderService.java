package com.cramer.service.abts;

import com.cramer.dto.abts.GenerationRequestDTO;
import com.cramer.service.abts.prompt.ListeningPromptBuilder;
import com.cramer.service.abts.prompt.PromptSchemaBuilder;
import com.cramer.service.abts.prompt.QuestionTypeInstructionBuilder;
import com.cramer.service.abts.prompt.ReadingPromptBuilder;
import com.cramer.service.abts.prompt.WritingPromptBuilder;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service facade for building AI prompts and schemas for IELTS content generation.
 *
 * @since 2025-12-20 - ABTS v2.0
 * @updated Phase 3 - Enhanced Listening prompts
 */
@Service
public class PromptBuilderService {

    private final ReadingPromptBuilder readingPromptBuilder;
    private final ListeningPromptBuilder listeningPromptBuilder;
    private final WritingPromptBuilder writingPromptBuilder;
    private final PromptSchemaBuilder promptSchemaBuilder;
    private final QuestionTypeInstructionBuilder questionTypeInstructionBuilder;

    public PromptBuilderService() {
        this.readingPromptBuilder = new ReadingPromptBuilder();
        this.listeningPromptBuilder = new ListeningPromptBuilder();
        this.writingPromptBuilder = new WritingPromptBuilder();
        this.promptSchemaBuilder = new PromptSchemaBuilder();
        this.questionTypeInstructionBuilder = new QuestionTypeInstructionBuilder();
    }

    public String buildReadingPrompt(GenerationRequestDTO request) {
        return readingPromptBuilder.buildReadingPrompt(request);
    }

    public String buildReadingSystemPrompt() {
        return readingPromptBuilder.buildReadingSystemPrompt();
    }

    public String buildReadingPassagePrompt(GenerationRequestDTO request) {
        return readingPromptBuilder.buildReadingPassagePrompt(request);
    }

    public String buildReadingQuestionsPrompt(GenerationRequestDTO request, String passageText) {
        return readingPromptBuilder.buildReadingQuestionsPrompt(request, passageText);
    }

    public Map<String, Object> getReadingPassageSchema() {
        return promptSchemaBuilder.getReadingPassageSchema();
    }

    public Map<String, Object> getReadingQuestionsSchema() {
        return promptSchemaBuilder.getReadingQuestionsSchema();
    }

    public String buildListeningPrompt(GenerationRequestDTO request) {
        return listeningPromptBuilder.buildListeningPrompt(request);
    }

    public String buildListeningSystemPrompt() {
        return listeningPromptBuilder.buildListeningSystemPrompt();
    }

    public String buildListeningTranscriptPrompt(GenerationRequestDTO request) {
        return listeningPromptBuilder.buildListeningTranscriptPrompt(request);
    }

    public String buildListeningQuestionsPrompt(GenerationRequestDTO request, String transcript) {
        return listeningPromptBuilder.buildListeningQuestionsPrompt(request, transcript);
    }

    public String buildListeningAnswersPrompt(GenerationRequestDTO request, String transcript, String questionsJson) {
        return listeningPromptBuilder.buildListeningAnswersPrompt(request, transcript, questionsJson);
    }

    public Map<String, Object> getListeningTranscriptSchema() {
        return promptSchemaBuilder.getListeningTranscriptSchema();
    }

    public Map<String, Object> getListeningQuestionsSchema() {
        return promptSchemaBuilder.getListeningQuestionsSchema();
    }

    public Map<String, Object> getListeningAnswersSchema() {
        return promptSchemaBuilder.getListeningAnswersSchema();
    }

    public String buildWritingPrompt(GenerationRequestDTO request) {
        return writingPromptBuilder.buildWritingPrompt(request);
    }

    public String buildWritingTaskPrompt(GenerationRequestDTO request) {
        return writingPromptBuilder.buildWritingTaskPrompt(request);
    }

    public String buildWritingSamplePrompt(GenerationRequestDTO request, String taskJson) {
        return writingPromptBuilder.buildWritingSamplePrompt(request, taskJson);
    }

    public String buildWritingBandPrompt(GenerationRequestDTO request, String taskJson, String sampleJson) {
        return writingPromptBuilder.buildWritingBandPrompt(request, taskJson, sampleJson);
    }

    public String buildWritingSystemPrompt() {
        return writingPromptBuilder.buildWritingSystemPrompt();
    }

    public Map<String, Object> getReadingJsonSchema() {
        return promptSchemaBuilder.getReadingJsonSchema();
    }

    public Map<String, Object> getListeningJsonSchema() {
        return promptSchemaBuilder.getListeningJsonSchema();
    }

    public Map<String, Object> getWritingJsonSchema() {
        return promptSchemaBuilder.getWritingJsonSchema();
    }

    public Map<String, Object> getWritingTaskSchema() {
        return promptSchemaBuilder.getWritingTaskSchema();
    }

    public Map<String, Object> getWritingSampleSchema() {
        return promptSchemaBuilder.getWritingSampleSchema();
    }

    public Map<String, Object> getWritingBandSchema() {
        return promptSchemaBuilder.getWritingBandSchema();
    }

    public String getQuestionTypeInstructions(String questionType) {
        return questionTypeInstructionBuilder.getQuestionTypeInstructions(questionType);
    }

    public String getListeningQuestionTypeInstructions(String questionType, int partNumber) {
        return questionTypeInstructionBuilder.getListeningQuestionTypeInstructions(questionType, partNumber);
    }
}
