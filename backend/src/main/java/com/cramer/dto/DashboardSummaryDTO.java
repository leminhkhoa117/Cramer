package com.cramer.dto;

import java.util.ArrayList;
import java.util.List;

public class DashboardSummaryDTO {

    private ProfileDTO profile;
    private UserStatsDTO stats;
    private List<RecentActivityDTO> recentAttempts;
    private List<SectionDTO> enrolledSections;
    private List<SkillSummaryDTO> skillSummary;
    private PageDTO<CourseProgressDTO> courseProgress;
    private List<DashboardGoalDTO> goals;

    public ProfileDTO getProfile() {
        return profile;
    }

    public void setProfile(ProfileDTO profile) {
        this.profile = profile;
    }

    public UserStatsDTO getStats() {
        return stats;
    }

    public void setStats(UserStatsDTO stats) {
        this.stats = stats;
    }

    public List<RecentActivityDTO> getRecentAttempts() {
        return recentAttempts;
    }

    public void setRecentAttempts(List<RecentActivityDTO> recentAttempts) {
        this.recentAttempts = recentAttempts;
    }

    public List<SectionDTO> getEnrolledSections() {
        if (enrolledSections == null) {
            enrolledSections = new ArrayList<>();
        }
        return enrolledSections;
    }

    public void setEnrolledSections(List<SectionDTO> enrolledSections) {
        this.enrolledSections = enrolledSections;
    }

    public List<SkillSummaryDTO> getSkillSummary() {
        if (skillSummary == null) {
            skillSummary = new ArrayList<>();
        }
        return skillSummary;
    }

    public void setSkillSummary(List<SkillSummaryDTO> skillSummary) {
        this.skillSummary = skillSummary;
    }

    public PageDTO<CourseProgressDTO> getCourseProgress() {
        return courseProgress;
    }

    public void setCourseProgress(PageDTO<CourseProgressDTO> courseProgress) {
        this.courseProgress = courseProgress;
    }

    public List<DashboardGoalDTO> getGoals() {
        if (goals == null) {
            goals = new ArrayList<>();
        }
        return goals;
    }

    public void setGoals(List<DashboardGoalDTO> goals) {
        this.goals = goals;
    }

    private TargetDTO target;

    public TargetDTO getTarget() {
        return target;
    }

    public void setTarget(TargetDTO target) {
        this.target = target;
    }
}
