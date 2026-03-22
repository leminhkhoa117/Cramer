package com.cramer.service.unit;

import com.cramer.config.SpeakingSessionProperties;
import com.cramer.entity.IeltsTest;
import com.cramer.entity.Question;
import com.cramer.entity.Section;
import com.cramer.repository.IeltsTestRepository;
import com.cramer.repository.QuestionRepository;
import com.cramer.repository.SectionRepository;
import com.cramer.service.SpeakingContentService;
import com.cramer.service.implement.HeuristicSpeakingSelectionPlannerService;
import com.cramer.service.implement.SpeakingContentServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpeakingContentServiceImpl Unit Tests")
class SpeakingContentServiceImplTest {

    @Mock
    private IeltsTestRepository ieltsTestRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private QuestionRepository questionRepository;

    private SpeakingContentServiceImpl speakingContentService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        SpeakingSessionProperties properties = new SpeakingSessionProperties();
        properties.setPart1(new SpeakingSessionProperties.PartPlan(30, 8, 12, false));
        properties.setPart2(new SpeakingSessionProperties.PartPlan(1, 1, 1, false));
        properties.setPart3(new SpeakingSessionProperties.PartPlan(15, 3, 6, true));

        speakingContentService = new SpeakingContentServiceImpl(
                ieltsTestRepository,
                sectionRepository,
                questionRepository,
                new HeuristicSpeakingSelectionPlannerService(),
                properties,
                objectMapper);
    }

    @Test
    @DisplayName("buildSessionPlan should select 8 to 12 Part 1 turns from a 30-question bank")
    void buildSessionPlan_part1Mode_selectsConfiguredRange() {
        Long testId = 32L;
        stubPublishedSpeakingTest(testId);

        Section part1 = Section.builder().id(101L).skill("speaking").partNumber(1).status("PUBLISHED").build();
        when(sectionRepository.findByIeltsTestId(testId)).thenReturn(List.of(part1));
        when(questionRepository.findBySectionId(101L)).thenReturn(part1Bank(30));

        SpeakingContentService.SpeakingContentPlan plan = speakingContentService.buildSessionPlan(
                testId,
                "PART_1",
                "british",
                new BigDecimal("1.00"));

        assertThat(plan.turns()).hasSizeBetween(8, 12);
        assertThat(plan.turns()).allMatch(turn -> turn.getPartNumber() == 1);
        JsonNode partNode = plan.sessionBlueprint().get("parts").get(0);
        assertThat(partNode.get("bankSize").asInt()).isEqualTo(30);
        assertThat(partNode.get("selectionStatus").asText()).isEqualTo("selected");
        assertThat(partNode.get("targetTurnCount").asInt()).isBetween(8, 12);
    }

    @Test
    @DisplayName("buildSessionPlan should defer Part 3 in FULL mode until Part 2 context exists")
    void buildSessionPlan_fullMode_defersPart3Selection() {
        Long testId = 33L;
        stubPublishedSpeakingTest(testId);

        Section part1 = Section.builder().id(201L).skill("speaking").partNumber(1).status("PUBLISHED").build();
        Section part2 = Section.builder().id(202L).skill("speaking").partNumber(2).status("PUBLISHED").build();
        Section part3 = Section.builder().id(203L).skill("speaking").partNumber(3).status("PUBLISHED").build();

        when(sectionRepository.findByIeltsTestId(testId)).thenReturn(List.of(part1, part2, part3));
        when(questionRepository.findBySectionId(201L)).thenReturn(part1Bank(30));
        when(questionRepository.findBySectionId(202L)).thenReturn(List.of(part2CueCard(2001L, "City life")));
        when(questionRepository.findBySectionId(203L)).thenReturn(part3Bank(15, "City life"));

        SpeakingContentService.SpeakingContentPlan plan = speakingContentService.buildSessionPlan(
                testId,
                "FULL",
                "british",
                new BigDecimal("1.00"));

        assertThat(plan.turns().stream().filter(turn -> turn.getPartNumber() == 3)).isEmpty();
        assertThat(plan.turns().stream().filter(turn -> turn.getPartNumber() == 1)).hasSizeBetween(8, 12);
        assertThat(plan.turns().stream().filter(turn -> turn.getPartNumber() == 2)).hasSize(1);

        JsonNode part3Node = findPartNode(plan.sessionBlueprint(), 3);
        assertThat(part3Node).isNotNull();
        assertThat(part3Node.get("selectionStatus").asText()).isEqualTo("pending_after_part_2");
        assertThat(part3Node.get("minTurnCount").asInt()).isEqualTo(3);
        assertThat(part3Node.get("maxTurnCount").asInt()).isEqualTo(6);
        assertThat(part3Node.get("turns")).isEmpty();
    }

    @Test
    @DisplayName("materializeDeferredPart3 should select 3 to 6 follow-up turns from the frozen Part 3 bank")
    void materializeDeferredPart3_selectsConfiguredRange() {
        Long testId = 34L;
        stubPublishedSpeakingTest(testId);

        Section part1 = Section.builder().id(301L).skill("speaking").partNumber(1).status("PUBLISHED").build();
        Section part2 = Section.builder().id(302L).skill("speaking").partNumber(2).status("PUBLISHED").build();
        Section part3 = Section.builder().id(303L).skill("speaking").partNumber(3).status("PUBLISHED").build();

        when(sectionRepository.findByIeltsTestId(testId)).thenReturn(List.of(part1, part2, part3));
        when(questionRepository.findBySectionId(301L)).thenReturn(part1Bank(30));
        when(questionRepository.findBySectionId(302L)).thenReturn(List.of(part2CueCard(3001L, "Travel")));
        when(questionRepository.findBySectionId(303L)).thenReturn(part3Bank(15, "Travel"));

        SpeakingContentService.SpeakingContentPlan initialPlan = speakingContentService.buildSessionPlan(
                testId,
                "FULL",
                "british",
                new BigDecimal("1.00"));

        SpeakingContentService.SpeakingContentPlan updatedPlan = speakingContentService.materializeDeferredPart3(
                initialPlan.sessionBlueprint(),
                "I enjoy travelling because it broadens my view and helps me understand different cities.");

        JsonNode part3Node = findPartNode(updatedPlan.sessionBlueprint(), 3);
        assertThat(part3Node).isNotNull();
        assertThat(part3Node.get("selectionStatus").asText()).isEqualTo("selected");
        assertThat(part3Node.get("selectedTurnCount").asInt()).isBetween(3, 6);
        assertThat(part3Node.get("turns")).hasSizeBetween(3, 6);
        assertThat(updatedPlan.turns().stream().filter(turn -> turn.getPartNumber() == 3)).hasSizeBetween(3, 6);
    }

    @Test
    @DisplayName("buildSessionPlan should reject when the official Speaking banks are undersized")
    void buildSessionPlan_insufficientBank_throws() {
        Long testId = 35L;
        stubPublishedSpeakingTest(testId);

        Section part1 = Section.builder().id(401L).skill("speaking").partNumber(1).status("PUBLISHED").build();
        Section part2 = Section.builder().id(402L).skill("speaking").partNumber(2).status("PUBLISHED").build();
        Section part3 = Section.builder().id(403L).skill("speaking").partNumber(3).status("PUBLISHED").build();

        when(sectionRepository.findByIeltsTestId(testId)).thenReturn(List.of(part1, part2, part3));
        when(questionRepository.findBySectionId(401L)).thenReturn(part1Bank(12));
        when(questionRepository.findBySectionId(402L)).thenReturn(List.of(part2CueCard(4001L, "Travel")));
        when(questionRepository.findBySectionId(403L)).thenReturn(part3Bank(10, "Travel"));

        assertThatThrownBy(() -> speakingContentService.buildSessionPlan(
                testId,
                "FULL",
                "british",
                new BigDecimal("1.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires at least 30 published PART_1 prompts");
    }

    private void stubPublishedSpeakingTest(Long testId) {
        IeltsTest test = IeltsTest.builder().id(testId).isPublished(true).build();
        when(ieltsTestRepository.findById(testId)).thenReturn(Optional.of(test));
    }

    private JsonNode findPartNode(JsonNode blueprint, int partNumber) {
        for (JsonNode partNode : blueprint.get("parts")) {
            if (partNode.get("partNumber").asInt() == partNumber) {
                return partNode;
            }
        }
        return null;
    }

    private List<Question> part1Bank(int count) {
        List<Question> questions = new ArrayList<>();
        String[] topics = {"Work", "Study", "Hobbies"};
        for (int index = 0; index < count; index++) {
            String topic = topics[index % topics.length];
            questions.add(speakingQuestion(
                    1000L + index,
                    index + 1,
                    "PART_1",
                    topic,
                    "Part 1 prompt " + (index + 1)));
        }
        return questions;
    }

    private List<Question> part3Bank(int count, String dominantTopic) {
        List<Question> questions = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            questions.add(speakingQuestion(
                    3000L + index,
                    index + 1,
                    "PART_3",
                    dominantTopic,
                    "Part 3 prompt " + (index + 1) + " about " + dominantTopic));
        }
        return questions;
    }

    private Question part2CueCard(Long id, String topicLabel) {
        ObjectNode content = objectMapper.createObjectNode();
        content.put("schemaVersion", 1);
        content.put("partType", "PART_2");
        content.put("promptText", "Describe a topic about " + topicLabel + ".");
        content.put("topicLabel", topicLabel);
        content.putArray("cueCardBullets").add("when").add("where").add("why");
        content.put("prepTimeSeconds", 60);
        content.put("talkTimeSeconds", 120);

        Question question = new Question();
        question.setId(id);
        question.setQuestionNumber(1);
        question.setQuestionType("PART_2");
        question.setQuestionContent(content);
        return question;
    }

    private Question speakingQuestion(Long id, int questionNumber, String questionType, String topicLabel, String promptText) {
        ObjectNode content = objectMapper.createObjectNode();
        content.put("schemaVersion", 1);
        content.put("partType", questionType);
        content.put("promptText", promptText);
        content.put("topicLabel", topicLabel);

        Question question = new Question();
        question.setId(id);
        question.setQuestionNumber(questionNumber);
        question.setQuestionType(questionType);
        question.setQuestionContent(content);
        return question;
    }
}
