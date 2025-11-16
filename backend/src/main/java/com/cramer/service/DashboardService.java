package com.cramer.service;

import com.cramer.dto.*;
import com.cramer.entity.Question;
import com.cramer.entity.Section;
import com.cramer.entity.TestAttempt;
import com.cramer.entity.UserAnswer;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.repository.*;
import com.cramer.util.EntityMapper;
import com.cramer.util.IeltsScoreConverter;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final ProfileRepository profileRepository;
    private final TargetRepository targetRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final QuestionRepository questionRepository;
    private final SectionRepository sectionRepository;

    public DashboardService(ProfileRepository profileRepository,
                            TargetRepository targetRepository,
                            TestAttemptRepository testAttemptRepository,
                            UserAnswerRepository userAnswerRepository,
                            QuestionRepository questionRepository,
                            SectionRepository sectionRepository) {
        this.profileRepository = profileRepository;
        this.targetRepository = targetRepository;
        this.testAttemptRepository = testAttemptRepository;
        this.userAnswerRepository = userAnswerRepository;
        this.questionRepository = questionRepository;
        this.sectionRepository = sectionRepository;
    }

    public DashboardSummaryDTO buildDashboardSummary(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        // 1. Fetch primary entities
        ProfileDTO profile = profileRepository.findById(userId)
                .map(EntityMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Profile", "id", userId));

        TargetDTO target = targetRepository.findByUserId(userId)
                .map(EntityMapper::toDTO)
                .orElse(null);

        List<TestAttempt> attempts = testAttemptRepository.findByUserId(userId);
        List<UserAnswer> allAnswers = userAnswerRepository.findByAttempt_UserId(userId);

        // 2. Aggregate data
        List<CourseProgressDTO> courseProgress = aggregateCourseProgress(attempts, allAnswers);
        List<SkillSummaryDTO> skillSummaries = aggregateSkillSummaries(allAnswers);
        UserStatsDTO stats = calculateUserStats(allAnswers);
        List<RecentActivityDTO> recentActivities = getRecentActivities(allAnswers);

        // 3. Build final DTO
        DashboardSummaryDTO dto = new DashboardSummaryDTO();
        dto.setProfile(profile);
        dto.setTarget(target);
        dto.setCourseProgress(courseProgress);
        dto.setSkillSummary(skillSummaries);
        dto.setStats(stats);
        dto.setRecentAttempts(recentActivities);
        dto.setGoals(buildGoalsFromTarget(target));

        return dto;
    }

    private List<CourseProgressDTO> aggregateCourseProgress(List<TestAttempt> attempts, List<UserAnswer> allAnswers) {
        if (attempts == null || attempts.isEmpty()) {
            return List.of();
        }

        Map<Long, List<UserAnswer>> answersByAttemptId = allAnswers == null ?
                Collections.emptyMap() :
                allAnswers.stream().collect(Collectors.groupingBy(a -> a.getAttempt().getId()));
        Map<TestKey, Integer> totalQuestionsCache = new HashMap<>();

        return attempts.stream()
                .map(attempt -> {
                    int totalQuestions = resolveTotalQuestions(attempt, totalQuestionsCache);

                    List<UserAnswer> attemptAnswers = answersByAttemptId.getOrDefault(
                            attempt.getId(),
                            Collections.emptyList()
                    );

                    int answersAttempted = attemptAnswers.size();
                    int correctAnswers = (int) attemptAnswers.stream()
                            .filter(a -> a.getCorrect() != null && a.getCorrect())
                            .count();

                    double completionRate = totalQuestions > 0 ? (double) answersAttempted / totalQuestions : 0.0;

                    Double bandScore = null;
                    if ("COMPLETED".equals(attempt.getStatus()) && ("reading".equalsIgnoreCase(attempt.getSkill()) || "listening".equalsIgnoreCase(attempt.getSkill()))) {
                        bandScore = IeltsScoreConverter.convertToBand(correctAnswers);
                    }

                    return new CourseProgressDTO(
                            attempt.getId(),
                            attempt.getExamSource(),
                            Integer.valueOf(attempt.getTestNumber()),
                            attempt.getSkill(),
                            totalQuestions,
                            answersAttempted,
                            correctAnswers,
                            attempt.getCompletedAt(),
                            completionRate,
                            attempt.getStatus(),
                            bandScore
                    );
                })
                .sorted(Comparator.comparing(CourseProgressDTO::getLastAttempt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private List<SkillSummaryDTO> aggregateSkillSummaries(List<UserAnswer> answers) {
        if (answers == null || answers.isEmpty()) {
            return List.of();
        }

        // Create a map of Question ID -> Skill
        Set<Long> questionIds = answers.stream().map(a -> a.getQuestion().getId()).collect(Collectors.toSet());
        List<Question> questions = questionRepository.findAllById(questionIds);
        
        Set<Long> sectionIds = questions.stream().map(Question::getSectionId).collect(Collectors.toSet());
        List<Section> sections = sectionRepository.findAllById(sectionIds);

        Map<Long, String> questionIdToSkillMap = new HashMap<>();
        Map<Long, Section> sectionMap = sections.stream().collect(Collectors.toMap(Section::getId, Function.identity()));

        for (Question q : questions) {
            Section s = sectionMap.get(q.getSectionId());
            if (s != null) {
                questionIdToSkillMap.put(q.getId(), s.getSkill());
            }
        }

        // Aggregate stats per skill
        Map<String, long[]> skillStats = answers.stream()
                .collect(Collectors.groupingBy(
                        answer -> questionIdToSkillMap.getOrDefault(answer.getQuestion().getId(), "unknown"),
                        Collectors.collectingAndThen(Collectors.toList(), list -> {
                            long total = list.size();
                            long correct = list.stream().filter(a -> a.getCorrect() != null && a.getCorrect()).count();
                            return new long[]{total, correct};
                        })
                ));

        return skillStats.entrySet().stream()
                .map(entry -> {
                    String skill = entry.getKey();
                    long total = entry.getValue()[0];
                    long correct = entry.getValue()[1];
                    long incorrect = total - correct;
                    double accuracy = total > 0 ? (double) correct * 100.0 / total : 0.0;
                    return new SkillSummaryDTO(skill, total, correct, incorrect, accuracy);
                })
                .collect(Collectors.toList());
    }

    private UserStatsDTO calculateUserStats(List<UserAnswer> answers) {
        List<UserAnswer> safeAnswers = answers == null ? List.of() : answers;
        long testsCompleted = safeAnswers.stream().map(a -> a.getAttempt().getId()).distinct().count();
        long questionsAnswered = safeAnswers.size();
        long correctAnswers = safeAnswers.stream().filter(a -> a.getCorrect() != null && a.getCorrect()).count();
        double accuracy = questionsAnswered > 0 ? (double) correctAnswers * 100.0 / questionsAnswered : 0.0;
        return new UserStatsDTO(testsCompleted, questionsAnswered, correctAnswers, accuracy);
    }

    public List<RecentActivityDTO> getRecentActivities(List<UserAnswer> answers) {
        if (answers == null || answers.isEmpty()) {
            return List.of();
        }
        return answers.stream()
                .filter(answer -> answer.getSubmittedAt() != null)
                .sorted(Comparator.comparing(UserAnswer::getSubmittedAt, Comparator.reverseOrder()))
                .limit(10)
                .map(answer -> new RecentActivityDTO(
                        answer.getQuestion().getId(),
                        answer.getSubmittedAt(),
                        answer.getCorrect()
                ))
                .collect(Collectors.toList());
    }

    private List<DashboardGoalDTO> buildGoalsFromTarget(TargetDTO targetDto) {
        if (targetDto == null) {
            return List.of();
        }
        List<DashboardGoalDTO> goals = new ArrayList<>();
        LocalDate examDate = targetDto.getExamDate();
        if (targetDto.getListening() != null) goals.add(new DashboardGoalDTO("Listening", String.valueOf(targetDto.getListening()), examDate));
        if (targetDto.getReading() != null) goals.add(new DashboardGoalDTO("Reading", String.valueOf(targetDto.getReading()), examDate));
        if (targetDto.getWriting() != null) goals.add(new DashboardGoalDTO("Writing", String.valueOf(targetDto.getWriting()), examDate));
        if (targetDto.getSpeaking() != null) goals.add(new DashboardGoalDTO("Speaking", String.valueOf(targetDto.getSpeaking()), examDate));
        return goals;
    }

    private int resolveTotalQuestions(TestAttempt attempt, Map<TestKey, Integer> cache) {
        Integer parsedTestNumber = parseTestNumber(attempt.getTestNumber());
        if (parsedTestNumber == null) {
            return 0;
        }

        TestKey key = new TestKey(
                attempt.getExamSource(),
                parsedTestNumber,
                attempt.getSkill()
        );

        return cache.computeIfAbsent(key, k ->
                questionRepository.countBySection_ExamSourceAndSection_TestNumberAndSection_Skill(
                        k.examSource(),
                        k.testNumber(),
                        k.skill()
                )
        );
    }

    private Integer parseTestNumber(String rawTestNumber) {
        if (rawTestNumber == null) {
            return null;
        }
        try {
            return Integer.parseInt(rawTestNumber);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record TestKey(String examSource, Integer testNumber, String skill) {
    }
}
