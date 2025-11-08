package com.cramer.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cramer.dto.CourseProgressDTO;
import com.cramer.dto.DashboardSummaryDTO;
import com.cramer.dto.ProfileDTO;
import com.cramer.dto.RecentActivityDTO;
import com.cramer.dto.SectionDTO;
import com.cramer.dto.SkillSummaryDTO;
import com.cramer.dto.TargetDTO;
import com.cramer.dto.UserStatsDTO;
import com.cramer.entity.Question;
import com.cramer.entity.Section;
import com.cramer.entity.UserAnswer;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.repository.ProfileRepository;
import com.cramer.repository.QuestionRepository;
import com.cramer.repository.SectionRepository;
import com.cramer.repository.TargetRepository;
import com.cramer.repository.UserAnswerRepository;
import com.cramer.util.EntityMapper;

@Service
public class DashboardService {

    private final ProfileRepository profileRepository;
    private final SectionRepository sectionRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final UserAnswerService userAnswerService;
    private final QuestionRepository questionRepository;
    private final TargetRepository targetRepository;

    public DashboardService(ProfileRepository profileRepository,
                            SectionRepository sectionRepository,
                            UserAnswerRepository userAnswerRepository,
                            UserAnswerService userAnswerService,
                            QuestionRepository questionRepository,
                            TargetRepository targetRepository) {
        this.profileRepository= profileRepository;
        this.sectionRepository= sectionRepository;
        this.userAnswerRepository=userAnswerRepository;
        this.userAnswerService=userAnswerService;
        this.questionRepository = questionRepository;
        this.targetRepository = targetRepository;
    }

    public DashboardSummaryDTO buildDashboardSummary(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        ProfileDTO profile = profileRepository.findById(userId)
            .map(EntityMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Profile", "id", userId));

        UserStatsDTO stats = EntityMapper.toDTO(userAnswerService.getUserStatistics(userId));
        
       List<RecentActivityDTO> recent = userAnswerRepository.findRecentAnswersByUserId(userId, 5)
            .stream()
            .map(answer -> new RecentActivityDTO(
                    answer.getQuestionId(),
                    answer.getSubmittedAt(),
                    answer.getIsCorrect()))
            .toList();

        List<UserAnswer> userAnswers = userAnswerRepository.findByUserId(userId);

        AggregatedDashboardData aggregates = aggregateUserProgress(userAnswers);

        // Fetch target and convert to DTO
        TargetDTO targetDto = targetRepository.findByUserId(userId)
                .map(EntityMapper::toDTO)
                .orElse(null);

        DashboardSummaryDTO dto = new DashboardSummaryDTO();
        dto.setProfile(profile);
        dto.setStats(stats);
        dto.setRecentAttempts(recent);
        dto.setEnrolledSections(aggregates.sections());
        dto.setSkillSummary(aggregates.skillSummaries());
        dto.setCourseProgress(aggregates.courseProgress());
        dto.setTarget(targetDto); // Set the target DTO

        // Dynamically build goals list from target DTO
        List<com.cramer.dto.DashboardGoalDTO> goals = new ArrayList<>();
        if (targetDto != null) {
            LocalDate examDate = targetDto.getExamDate();
            if (targetDto.getListening() != null) goals.add(new com.cramer.dto.DashboardGoalDTO("Listening", String.valueOf(targetDto.getListening()), examDate));
            if (targetDto.getReading() != null) goals.add(new com.cramer.dto.DashboardGoalDTO("Reading", String.valueOf(targetDto.getReading()), examDate));
            if (targetDto.getWriting() != null) goals.add(new com.cramer.dto.DashboardGoalDTO("Writing", String.valueOf(targetDto.getWriting()), examDate));
            if (targetDto.getSpeaking() != null) goals.add(new com.cramer.dto.DashboardGoalDTO("Speaking", String.valueOf(targetDto.getSpeaking()), examDate));
        }
        dto.setGoals(goals);

        return dto;
    }

    private AggregatedDashboardData aggregateUserProgress(List<UserAnswer> answers) {
        if (answers == null || answers.isEmpty()) {
            return new AggregatedDashboardData(List.of(), List.of(), List.of());
        }

    List<Long> questionIds = answers.stream()
            .map(UserAnswer::getQuestionId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

        Iterable<Long> questionIdIterable = Objects.requireNonNull(questionIds);
        Map<Long, Question> questions = questionRepository.findAllById(questionIdIterable).stream()
            .collect(Collectors.toMap(Question::getId, Function.identity()));

        List<Long> sectionIds = questions.values().stream()
            .map(Question::getSectionId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

        Iterable<Long> sectionIdIterable = Objects.requireNonNull(sectionIds);
        Map<Long, Section> sections = sectionRepository.findAllById(sectionIdIterable).stream()
            .collect(Collectors.toMap(Section::getId, Function.identity()));

        // Efficiently count questions per section from in-memory data
        Map<Long, Long> totalQuestionsPerSection = questions.values().stream()
                .collect(Collectors.groupingBy(Question::getSectionId, Collectors.counting()));

        Map<CourseKey, CourseAggregate> courseAggregates = new LinkedHashMap<>();
        Map<String, SkillAggregate> skillAggregates = new LinkedHashMap<>();

        answers.forEach(answer -> {
            Question question = questions.get(answer.getQuestionId());
            if (question == null) {
                return;
            }
            Section section = sections.get(question.getSectionId());
            if (section == null) {
                return;
            }

            // More robust check to ensure all key parts for aggregation are present
            if (section.getSkill() == null || section.getExamSource() == null || section.getTestNumber() == null) {
                // Optionally log this data inconsistency
                return;
            }

            OffsetDateTime submittedAt = answer.getSubmittedAt();
            boolean correct = Boolean.TRUE.equals(answer.getIsCorrect());

            CourseKey key = new CourseKey(section.getExamSource(), section.getTestNumber(), section.getSkill());
            CourseAggregate aggregate = courseAggregates.computeIfAbsent(key, CourseAggregate::new);
            aggregate.registerAnswer(section.getId(), correct, submittedAt);

            SkillAggregate skillAggregate = skillAggregates.computeIfAbsent(section.getSkill(), SkillAggregate::new);
            skillAggregate.registerAttempt(correct);
        });

        List<CourseProgressDTO> courseProgress = courseAggregates.entrySet().stream()
            .map(entry -> {
                CourseKey key = entry.getKey();
                CourseAggregate aggregate = entry.getValue();
                int totalQuestions = resolveTotalQuestionsForCourse(key, aggregate, totalQuestionsPerSection);
                return aggregate.toDto(totalQuestions);
            })
            .sorted(Comparator.comparing(CourseProgressDTO::getLastAttempt, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();

        List<SkillSummaryDTO> skillSummaries = skillAggregates.values().stream()
            .map(SkillAggregate::toDto)
            .toList();

        List<SectionDTO> enrolledSections = sections.values().stream()
            .map(EntityMapper::toDTO)
            .toList();

        return new AggregatedDashboardData(enrolledSections, skillSummaries, courseProgress);
    }

    private record AggregatedDashboardData(List<SectionDTO> sections,
                                           List<SkillSummaryDTO> skillSummaries,
                                           List<CourseProgressDTO> courseProgress) {}

    private static class CourseKey {
        private final String examSource;
        private final Integer testNumber;
        private final String skill;

        private CourseKey(String examSource, Integer testNumber, String skill) {
            this.examSource = examSource;
            this.testNumber = testNumber;
            this.skill = skill;
        }

        @Override
        public int hashCode() {
            return Objects.hash(examSource, testNumber, skill);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            CourseKey other = (CourseKey) obj;
            return Objects.equals(examSource, other.examSource)
                && Objects.equals(testNumber, other.testNumber)
                && Objects.equals(skill, other.skill);
        }
    }

    private int resolveTotalQuestionsForCourse(CourseKey key, CourseAggregate aggregate,
                                              Map<Long, Long> totalQuestionsPerSection) {
        int total = aggregate.sectionIds.stream()
            .mapToInt(sectionId -> {
                Long cached = totalQuestionsPerSection.get(sectionId);
                if (cached != null) {
                    return Math.toIntExact(cached);
                }
                return Math.toIntExact(questionRepository.countBySectionId(sectionId));
            })
            .sum();

        if (total == 0) {
            List<Section> courseSections = sectionRepository.findSectionsForTest(key.examSource, key.testNumber, key.skill);
            total = courseSections.stream()
                .mapToInt(section -> {
                    Long cached = totalQuestionsPerSection.get(section.getId());
                    if (cached != null) {
                        return Math.toIntExact(cached);
                    }
                    return Math.toIntExact(questionRepository.countBySectionId(section.getId()));
                })
                .sum();
        }

        return total;
    }

    private static class CourseAggregate {
        private final CourseKey key;
        private int answersAttempted;
        private int correctAnswers;
        private OffsetDateTime lastAttempt;
    private final Set<Long> sectionIds = new java.util.HashSet<>();

        private CourseAggregate(CourseKey key) {
            this.key = key;
        }

        private void registerAnswer(Long sectionId, boolean correct, OffsetDateTime submittedAt) {
            answersAttempted++;
            if (correct) {
                correctAnswers++;
            }
            if (sectionId != null) {
                sectionIds.add(sectionId);
            }
            if (submittedAt != null && (lastAttempt == null || submittedAt.isAfter(lastAttempt))) {
                lastAttempt = submittedAt;
            }
        }

        private CourseProgressDTO toDto(int totalQuestions) {
            double completion = totalQuestions > 0 ? (double) answersAttempted / (double) totalQuestions : 0.0;
            String status;
            if (answersAttempted == 0) {
                status = "Chưa bắt đầu";
            } else if (correctAnswers >= totalQuestions && totalQuestions > 0) {
                status = "Hoàn thành";
            } else {
                status = "Đang học";
            }

            return new CourseProgressDTO(
                key.examSource,
                key.testNumber,
                key.skill,
                totalQuestions,
                answersAttempted,
                correctAnswers,
                lastAttempt,
                completion,
                status
            );
        }
    }

    private static class SkillAggregate {
        private final String skill;
        private long attempts;
        private long correct;

        private SkillAggregate(String skill) {
            this.skill = skill;
        }

        private void registerAttempt(boolean isCorrect) {
            attempts++;
            if (isCorrect) {
                correct++;
            }
        }

        private SkillSummaryDTO toDto() {
            long incorrect = attempts - correct;
            double accuracy = attempts > 0 ? (double) correct * 100.0 / (double) attempts : 0.0;
            return new SkillSummaryDTO(skill, attempts, correct, incorrect, accuracy);
        }
    }
}
