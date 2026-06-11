package com.cramer.engagement.web.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Dashboard read-model projections (SPEC-16 §4). Assembled read-only from
 * {@code test_attempts}/{@code user_answers}/{@code writing_submissions}/{@code profiles}; the
 * dashboard never writes through other modules' tables.
 */
public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record SummaryView(
            ProfileBrief profile,
            TargetView target,
            UserStats stats,
            List<SkillAccuracy> perSkillAccuracy,
            List<CourseProgressItem> courseProgress,
            List<ActivityBrief> recentActivity) {
    }

    public record ProfileBrief(String username, String fullName, String avatarUrl) {
    }

    public record UserStats(long testsCompleted, long questionsAnswered, long correctAnswers, double accuracy) {
    }

    public record SkillAccuracy(String skill, long answered, long correct, double accuracy) {
    }

    public record CourseProgressItem(
            String examSource, String testNumber, String skill,
            long attempts, long latestAnswered, long latestCorrect,
            Double band, double completionPct, OffsetDateTime latestAt) {
    }

    public record ActivityBrief(String activityType, String title, String description, OffsetDateTime createdAt) {
    }

    public record CourseHistoryItem(
            Long attemptId, String skill, String status, Integer score,
            long answered, long correct, OffsetDateTime startedAt, OffsetDateTime completedAt) {
    }
}
