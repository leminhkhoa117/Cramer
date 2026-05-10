package com.cramer.service;

import com.cramer.dto.*;
import com.cramer.entity.Question;
import com.cramer.entity.Section;
import com.cramer.entity.Target;
import com.cramer.entity.TestAttempt;
import com.cramer.entity.UserAnswer;
import com.cramer.entity.WritingSubmission;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.repository.*;
import com.cramer.util.EntityMapper;
import com.cramer.util.IeltsScoreConverter;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.cramer.entity.TestSet;
import com.cramer.entity.IeltsTest;

@Service
public class DashboardService {

    private final ProfileRepository profileRepository;
    private final TargetRepository targetRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final QuestionRepository questionRepository;
    private final SectionRepository sectionRepository;
    private final WritingSubmissionRepository writingSubmissionRepository;
    private final TestSetRepository testSetRepository;
    private final IeltsTestRepository ieltsTestRepository;

    public DashboardService(ProfileRepository profileRepository,
            TargetRepository targetRepository,
            TestAttemptRepository testAttemptRepository,
            UserAnswerRepository userAnswerRepository,
            QuestionRepository questionRepository,
            SectionRepository sectionRepository,
            WritingSubmissionRepository writingSubmissionRepository,
            TestSetRepository testSetRepository,
            IeltsTestRepository ieltsTestRepository) {
        this.profileRepository = profileRepository;
        this.targetRepository = targetRepository;
        this.testAttemptRepository = testAttemptRepository;
        this.userAnswerRepository = userAnswerRepository;
        this.questionRepository = questionRepository;
        this.sectionRepository = sectionRepository;
        this.writingSubmissionRepository = writingSubmissionRepository;
        this.testSetRepository = testSetRepository;
        this.ieltsTestRepository = ieltsTestRepository;
    }

    public DashboardSummaryDTO buildDashboardSummary(UUID userId, int page, int size, String search) {
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
        PageDTO<CourseProgressDTO> courseProgress = aggregateCourseProgress(attempts, allAnswers, page, size, search);
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

    public TargetDTO saveTarget(UUID userId, TargetDTO targetDTO) {
        Objects.requireNonNull(userId, "User ID cannot be null");
        Objects.requireNonNull(targetDTO, "Target data cannot be null");

        // Find existing target by userId or create a new one
        Target target = targetRepository.findByUserId(userId)
                .orElse(new Target());

        // Update target fields from DTO
        target.setUserId(userId); // Set the user ID
        target.setExamName(targetDTO.examName());
        target.setExamDate(targetDTO.examDate());
        target.setListening(targetDTO.listening());
        target.setReading(targetDTO.reading());
        target.setWriting(targetDTO.writing());
        target.setSpeaking(targetDTO.speaking());

        // Save and return as DTO
        Target savedTarget = targetRepository.save(target);
        return EntityMapper.toDTO(savedTarget);
    }

    private record CourseGroup(TestKey key, List<TestAttempt> attempts, TestAttempt latestAttempt) {}

    private PageDTO<CourseProgressDTO> aggregateCourseProgress(List<TestAttempt> attempts, List<UserAnswer> allAnswers,
            int page, int size, String search) {
        if (attempts == null || attempts.isEmpty()) {
            return new PageDTO<>(List.of(), page, size, 0, 0);
        }

        // 1. Group attempts by TestKey (Source + TestNum + Skill) — fast in-memory
        Map<TestKey, List<TestAttempt>> attemptsByTest = attempts.stream()
                .collect(Collectors.groupingBy(
                        a -> new TestKey(a.getExamSource(), parseTestNumber(a.getTestNumber()), a.getSkill())));

        // 2. Build lightweight group metadata (find latest non-cancelled, sort internally, apply search)
        List<CourseGroup> groups = new ArrayList<>();
        for (Map.Entry<TestKey, List<TestAttempt>> entry : attemptsByTest.entrySet()) {
            List<TestAttempt> testAttempts = entry.getValue();
            testAttempts.sort(
                    Comparator.comparing(TestAttempt::getStartedAt, Comparator.nullsLast(Comparator.reverseOrder())));

            TestAttempt latestAttempt = testAttempts.stream()
                    .filter(a -> !"CANCELLED".equals(a.getStatus()))
                    .findFirst()
                    .orElse(null);

            if (latestAttempt == null) continue;

            if (search != null && !search.trim().isEmpty()) {
                String lowerSearch = search.toLowerCase().trim();
                boolean matches = (latestAttempt.getExamSource() != null
                        && latestAttempt.getExamSource().toLowerCase().contains(lowerSearch)) ||
                        (latestAttempt.getSkill() != null
                                && latestAttempt.getSkill().toLowerCase().contains(lowerSearch));
                if (!matches) continue;
            }

            groups.add(new CourseGroup(entry.getKey(), testAttempts, latestAttempt));
        }

        // 3. Sort groups by latest attempt time
        groups.sort(Comparator.comparing(g -> g.latestAttempt.getStartedAt(),
                Comparator.nullsLast(Comparator.reverseOrder())));

        // 4. Pagination metadata
        int totalElements = groups.size();
        int totalPages = totalElements > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        int start = page * size;
        int end = Math.min(start + size, totalElements);

        if (start >= totalElements) {
            return new PageDTO<>(List.of(), page, size, totalElements, totalPages);
        }

        // 5. Build caches only once
        Map<Long, List<UserAnswer>> answersByAttemptId = allAnswers == null ? Collections.emptyMap()
                : allAnswers.stream().collect(Collectors.groupingBy(a -> a.getAttempt().getId()));
        Map<TestKey, Integer> totalQuestionsCache = new HashMap<>();

        Set<String> examSources = attempts.stream().map(TestAttempt::getExamSource).collect(Collectors.toSet());
        Map<String, TestSet> setsByCode = examSources.stream()
                .map(code -> testSetRepository.findByCode(code).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(TestSet::getCode, Function.identity()));

        Map<String, String> testNameLookup = new HashMap<>();
        for (TestSet ts : setsByCode.values()) {
            List<IeltsTest> tests = ieltsTestRepository.findByTestSetIdOrderByTestNumberAsc(ts.getId());
            for (IeltsTest t : tests) {
                testNameLookup.put(ts.getCode() + "_" + t.getTestNumber(), t.getName());
            }
        }

        // 6. Build DTOs ONLY for the paginated slice
        List<CourseGroup> pageGroups = groups.subList(start, end);
        List<CourseProgressDTO> pageContent = new ArrayList<>(pageGroups.size());

        for (CourseGroup group : pageGroups) {
            TestAttempt latestAttempt = group.latestAttempt;
            List<TestAttempt> testAttempts = group.attempts;

            // Build History
            List<AttemptHistoryDTO> history = testAttempts.stream()
                    .filter(a -> !"CANCELLED".equals(a.getStatus()))
                    .map(a -> {
                        int correct = (int) answersByAttemptId.getOrDefault(a.getId(), Collections.emptyList()).stream()
                                .filter(ans -> Boolean.TRUE.equals(ans.getCorrect()))
                                .count();
                        Double band = null;
                        if ("COMPLETED".equals(a.getStatus())) {
                            if ("reading".equalsIgnoreCase(a.getSkill())
                                    || "listening".equalsIgnoreCase(a.getSkill())) {
                                band = IeltsScoreConverter.convertToBand(a.getScore() != null ? a.getScore() : correct);
                            } else if ("writing".equalsIgnoreCase(a.getSkill())) {
                                band = getWritingAttemptBand(a.getId());
                            }
                        }
                        return new AttemptHistoryDTO(a.getId(), a.getCompletedAt(), a.getScore(), a.getStatus(), band);
                    })
                    .collect(Collectors.toList());

            int totalQuestions = resolveTotalQuestions(latestAttempt, totalQuestionsCache);
            List<UserAnswer> attemptAnswers = answersByAttemptId.getOrDefault(latestAttempt.getId(),
                    Collections.emptyList());
            int answersAttempted = attemptAnswers.size();
            int correctCount = (int) attemptAnswers.stream().filter(a -> Boolean.TRUE.equals(a.getCorrect())).count();

            double score;
            if ("writing".equalsIgnoreCase(latestAttempt.getSkill())) {
                Double writingBand = getWritingAttemptBand(latestAttempt.getId());
                score = writingBand != null ? writingBand : 0.0;
            } else {
                score = IeltsScoreConverter.convertToBand(correctCount);
            }

            double completionRate = totalQuestions > 0 ? (double) answersAttempted / totalQuestions : 0.0;

            String setName = null;
            String testName = null;
            String coverImageUrl = null;
            TestSet ts = setsByCode.get(latestAttempt.getExamSource());
            if (ts != null) {
                setName = ts.getName();
                coverImageUrl = ts.getCoverImageUrl();
                String lookupKey = ts.getCode() + "_" + parseTestNumber(latestAttempt.getTestNumber());
                testName = testNameLookup.get(lookupKey);
            }
            if (setName == null) setName = latestAttempt.getExamSource();
            if (testName == null) testName = "Test " + latestAttempt.getTestNumber();

            pageContent.add(new CourseProgressDTO(
                    latestAttempt.getId(),
                    latestAttempt.getExamSource(),
                    parseTestNumber(latestAttempt.getTestNumber()),
                    latestAttempt.getSkill(),
                    setName,
                    testName,
                    totalQuestions,
                    answersAttempted,
                    correctCount,
                    latestAttempt.getCompletedAt(),
                    completionRate,
                    latestAttempt.getStatus(),
                    score,
                    coverImageUrl,
                    history));
        }

        return new PageDTO<>(pageContent, page, size, totalElements, totalPages);
    }

    private List<SkillSummaryDTO> aggregateSkillSummaries(List<UserAnswer> answers) {
        if (answers == null || answers.isEmpty()) {
            return List.of();
        }

        // Create a map of Question ID -> Skill
        Set<Long> questionIds = answers.stream().map(a -> a.getQuestion().getId()).collect(Collectors.toSet());
        List<Question> questions = questionRepository.findAllById(Objects.requireNonNull(questionIds));

        Set<Long> sectionIds = questions.stream().map(Question::getSectionId).collect(Collectors.toSet());
        List<Section> sections = sectionRepository.findAllById(Objects.requireNonNull(sectionIds));

        Map<Long, String> questionIdToSkillMap = new HashMap<>();
        Map<Long, Section> sectionMap = sections.stream()
                .collect(Collectors.toMap(Section::getId, Function.identity()));

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
                            return new long[] { total, correct };
                        })));

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
                        answer.getCorrect()))
                .collect(Collectors.toList());
    }

    private List<DashboardGoalDTO> buildGoalsFromTarget(TargetDTO targetDto) {
        if (targetDto == null) {
            return List.of();
        }
        List<DashboardGoalDTO> goals = new ArrayList<>();
        LocalDate examDate = targetDto.examDate();
        if (targetDto.listening() != null)
            goals.add(new DashboardGoalDTO("Listening", String.valueOf(targetDto.listening()), examDate));
        if (targetDto.reading() != null)
            goals.add(new DashboardGoalDTO("Reading", String.valueOf(targetDto.reading()), examDate));
        if (targetDto.writing() != null)
            goals.add(new DashboardGoalDTO("Writing", String.valueOf(targetDto.writing()), examDate));
        if (targetDto.speaking() != null)
            goals.add(new DashboardGoalDTO("Speaking", String.valueOf(targetDto.speaking()), examDate));
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
                attempt.getSkill());

        return cache.computeIfAbsent(key,
                k -> questionRepository.countBySection_ExamSourceAndSection_TestNumberAndSection_Skill(
                        k.examSource(),
                        k.testNumber(),
                        k.skill()));
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

    /**
     * Get the overall band score for a writing attempt using IELTS weighted
     * average.
     * Task 1 = 1/3 weight, Task 2 = 2/3 weight (consistent with
     * WritingSubmissionService)
     */
    private Double getWritingAttemptBand(Long attemptId) {
        List<WritingSubmission> submissions = writingSubmissionRepository.findByAttemptId(attemptId);
        if (submissions.isEmpty()) {
            return null;
        }

        // Find graded bands for Task 1 and Task 2
        Double task1Band = null;
        Double task2Band = null;

        for (WritingSubmission sub : submissions) {
            if ("COMPLETED".equals(sub.getGradingStatus()) && sub.getOverallBand() != null) {
                if (sub.getTaskNumber() == 1) {
                    task1Band = sub.getOverallBand().doubleValue();
                } else if (sub.getTaskNumber() == 2) {
                    task2Band = sub.getOverallBand().doubleValue();
                }
            }
        }

        // Calculate weighted average using IELTS formula
        Double result;
        if (task1Band != null && task2Band != null) {
            // Weighted average: (Task1 * 1 + Task2 * 2) / 3
            double weighted = (task1Band + task2Band * 2) / 3.0;
            // Round to nearest 0.5
            result = Math.round(weighted * 2) / 2.0;
        } else if (task1Band != null) {
            result = task1Band;
        } else if (task2Band != null) {
            result = task2Band;
        } else {
            result = null;
        }

        return result;
    }

    private record TestKey(String examSource, Integer testNumber, String skill) {
    }
}