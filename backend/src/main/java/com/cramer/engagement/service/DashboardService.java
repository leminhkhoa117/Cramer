package com.cramer.engagement.service;

import com.cramer.engagement.web.dto.DashboardDtos;
import com.cramer.engagement.web.dto.TargetView;
import com.cramer.platform.common.ielts.BandScale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Dashboard read-model (SPEC-16 §4). Assembles a user's aggregate view read-only from
 * {@code test_attempts}/{@code user_answers}/{@code profiles}/{@code user_activities}. Course
 * progress groups non-cancelled attempts by {@code exam_source+test_number+skill}, takes the
 * latest, and derives the Reading/Listening band via {@link BandScale}. No cross-module writes.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final JdbcTemplate jdbc;
    private final TargetService targets;

    public DashboardService(JdbcTemplate jdbc, TargetService targets) {
        this.jdbc = jdbc;
        this.targets = targets;
    }

    public DashboardDtos.SummaryView summary(UUID userId, int page, int size, String search) {
        return new DashboardDtos.SummaryView(
                profile(userId),
                targets.current(userId).orElse(null),
                userStats(userId),
                perSkillAccuracy(userId),
                courseProgress(userId, page, size, search),
                recentActivity(userId));
    }

    public List<DashboardDtos.CourseHistoryItem> courseHistory(UUID userId, String examSource,
                                                               String testNumber, String skill) {
        StringBuilder sql = new StringBuilder("""
                SELECT a.id, a.skill, a.status, a.score, a.started_at, a.completed_at,
                       (SELECT COUNT(*) FROM user_answers ua WHERE ua.attempt_id = a.id) answered,
                       (SELECT COUNT(*) FROM user_answers ua WHERE ua.attempt_id = a.id AND ua.is_correct) correct
                FROM test_attempts a
                WHERE a.user_id = ? AND a.status <> 'CANCELLED'""");
        List<Object> args = new ArrayList<>();
        args.add(userId);
        if (examSource != null && !examSource.isBlank()) {
            sql.append(" AND a.exam_source = ?");
            args.add(examSource.trim());
        }
        if (testNumber != null && !testNumber.isBlank()) {
            sql.append(" AND a.test_number = ?");
            args.add(testNumber.trim());
        }
        if (skill != null && !skill.isBlank()) {
            sql.append(" AND a.skill = ?");
            args.add(skill.trim().toLowerCase(Locale.ROOT));
        }
        sql.append(" ORDER BY a.started_at DESC");
        return jdbc.query(sql.toString(), (rs, i) -> new DashboardDtos.CourseHistoryItem(
                rs.getLong("id"), rs.getString("skill"), rs.getString("status"),
                (Integer) rs.getObject("score"), rs.getLong("answered"), rs.getLong("correct"),
                rs.getObject("started_at", java.time.OffsetDateTime.class),
                rs.getObject("completed_at", java.time.OffsetDateTime.class)), args.toArray());
    }

    // ---------------------------------------------------------------- pieces

    private DashboardDtos.ProfileBrief profile(UUID userId) {
        return jdbc.query("SELECT username, full_name, avatar_url FROM profiles WHERE id = ?",
                rs -> rs.next()
                        ? new DashboardDtos.ProfileBrief(rs.getString("username"), rs.getString("full_name"),
                        rs.getString("avatar_url"))
                        : new DashboardDtos.ProfileBrief(null, null, null), userId);
    }

    private DashboardDtos.UserStats userStats(UUID userId) {
        long completed = scalar("SELECT COUNT(*) FROM test_attempts WHERE user_id = ? AND status = 'COMPLETED'", userId);
        long answered = scalar("SELECT COUNT(*) FROM user_answers WHERE user_id = ?", userId);
        long correct = scalar("SELECT COUNT(*) FROM user_answers WHERE user_id = ? AND is_correct", userId);
        double accuracy = answered == 0 ? 0.0 : round1((correct * 100.0) / answered);
        return new DashboardDtos.UserStats(completed, answered, correct, accuracy);
    }

    private List<DashboardDtos.SkillAccuracy> perSkillAccuracy(UUID userId) {
        return jdbc.query("""
                SELECT a.skill, COUNT(*) answered, COUNT(*) FILTER (WHERE ua.is_correct) correct
                FROM user_answers ua JOIN test_attempts a ON a.id = ua.attempt_id
                WHERE ua.user_id = ? GROUP BY a.skill ORDER BY a.skill
                """, (rs, i) -> {
            long answered = rs.getLong("answered");
            long correct = rs.getLong("correct");
            return new DashboardDtos.SkillAccuracy(rs.getString("skill"), answered, correct,
                    answered == 0 ? 0.0 : round1((correct * 100.0) / answered));
        }, userId);
    }

    private List<DashboardDtos.CourseProgressItem> courseProgress(UUID userId, int page, int size, String search) {
        int capped = Math.min(Math.max(size, 1), 100);
        StringBuilder sql = new StringBuilder("""
                SELECT a.exam_source, a.test_number, a.skill,
                       COUNT(*) attempts, MAX(a.started_at) latest_at
                FROM test_attempts a
                WHERE a.user_id = ? AND a.status <> 'CANCELLED'""");
        List<Object> args = new ArrayList<>();
        args.add(userId);
        if (search != null && !search.isBlank()) {
            sql.append(" AND a.exam_source ILIKE ?");
            args.add("%" + search.trim() + "%");
        }
        sql.append(" GROUP BY a.exam_source, a.test_number, a.skill ORDER BY latest_at DESC LIMIT ? OFFSET ?");
        args.add(capped);
        args.add(Math.max(page, 0) * capped);

        List<DashboardDtos.CourseProgressItem> items = new ArrayList<>();
        jdbc.query(sql.toString(), rs -> {
            String examSource = rs.getString("exam_source");
            String testNumber = rs.getString("test_number");
            String skill = rs.getString("skill");
            long attempts = rs.getLong("attempts");
            java.time.OffsetDateTime latestAt = rs.getObject("latest_at", java.time.OffsetDateTime.class);
            long[] ac = latestAnsweredCorrect(userId, examSource, testNumber, skill);
            Double band = isReadingOrListening(skill) ? BandScale.bandFor((int) ac[1]) : null;
            double completion = "COMPLETED".equalsIgnoreCase(latestStatus(userId, examSource, testNumber, skill))
                    ? 100.0 : 0.0;
            items.add(new DashboardDtos.CourseProgressItem(examSource, testNumber, skill, attempts,
                    ac[0], ac[1], band, completion, latestAt));
        }, args.toArray());
        return items;
    }

    private long[] latestAnsweredCorrect(UUID userId, String examSource, String testNumber, String skill) {
        return jdbc.query("""
                SELECT (SELECT COUNT(*) FROM user_answers ua WHERE ua.attempt_id = a.id) answered,
                       (SELECT COUNT(*) FROM user_answers ua WHERE ua.attempt_id = a.id AND ua.is_correct) correct
                FROM test_attempts a
                WHERE a.user_id = ? AND a.exam_source = ? AND a.test_number = ? AND a.skill = ? AND a.status <> 'CANCELLED'
                ORDER BY a.started_at DESC LIMIT 1
                """, rs -> rs.next() ? new long[]{rs.getLong("answered"), rs.getLong("correct")} : new long[]{0, 0},
                userId, examSource, testNumber, skill);
    }

    private String latestStatus(UUID userId, String examSource, String testNumber, String skill) {
        return jdbc.query("""
                SELECT status FROM test_attempts
                WHERE user_id = ? AND exam_source = ? AND test_number = ? AND skill = ? AND status <> 'CANCELLED'
                ORDER BY started_at DESC LIMIT 1
                """, rs -> rs.next() ? rs.getString("status") : null, userId, examSource, testNumber, skill);
    }

    private List<DashboardDtos.ActivityBrief> recentActivity(UUID userId) {
        return jdbc.query("""
                SELECT activity_type, title, description, created_at
                FROM user_activities WHERE user_id = ? ORDER BY created_at DESC LIMIT 10
                """, (rs, i) -> new DashboardDtos.ActivityBrief(
                rs.getString("activity_type"), rs.getString("title"), rs.getString("description"),
                rs.getObject("created_at", java.time.OffsetDateTime.class)), userId);
    }

    private boolean isReadingOrListening(String skill) {
        return "reading".equalsIgnoreCase(skill) || "listening".equalsIgnoreCase(skill);
    }

    private long scalar(String sql, Object... args) {
        Long n = jdbc.queryForObject(sql, Long.class, args);
        return n == null ? 0 : n;
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
