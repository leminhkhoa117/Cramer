package com.cramer.engagement.service;

import com.cramer.engagement.domain.UserActivity;
import com.cramer.engagement.repository.UserActivityRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implements {@link ActivityPort} (SPEC-16 §6): writes {@code user_activities}. The owning module
 * of the timeline, so other modules log through this port rather than touching the table.
 */
@Service
public class ActivityService implements ActivityPort {

    private final UserActivityRepository activities;

    public ActivityService(UserActivityRepository activities) {
        this.activities = activities;
    }

    @Override
    @Transactional
    public void log(UUID userId, String activityType, String title, String description, JsonNode metadata) {
        UserActivity a = new UserActivity();
        a.setUserId(userId);
        a.setActivityType(activityType);
        a.setTitle(title);
        a.setDescription(description);
        a.setMetadata(metadata);
        activities.save(a);
    }
}
