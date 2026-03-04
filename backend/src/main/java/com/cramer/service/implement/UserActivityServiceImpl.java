package com.cramer.service.implement;

import com.cramer.dto.UserActivityDTO;
import com.cramer.entity.UserActivity;
import com.cramer.repository.UserActivityRepository;
import com.cramer.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserActivityServiceImpl implements UserActivityService {

    private final UserActivityRepository activityRepository;

    @Override
    public Page<UserActivityDTO> getUserActivities(UUID userId, Pageable pageable) {
        return activityRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDTO);
    }

    @Override
    public Page<UserActivityDTO> getUserActivitiesByType(UUID userId, String type, Pageable pageable) {
        return activityRepository.findByUserIdAndActivityTypeOrderByCreatedAtDesc(userId, type, pageable)
                .map(this::toDTO);
    }

    @Override
    public List<UserActivityDTO> getRecentActivities(UUID userId, int limit) {
        return activityRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit))
                .getContent()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void logActivity(UUID userId, String activityType, String title,
                            String description, Map<String, Object> metadata) {
        UserActivity activity = new UserActivity();
        activity.setUserId(userId);
        activity.setActivityType(activityType);
        activity.setTitle(title);
        activity.setDescription(description);
        activity.setMetadata(metadata);
        activity.setCreatedAt(OffsetDateTime.now());
        
        activityRepository.save(activity);
        log.info("Logged activity: {} for user: {}", activityType, userId);
    }

    @Override
    public void logTestCompleted(UUID userId, String testName, String skill, Integer score, Integer total) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("testName", testName);
        metadata.put("skill", skill);
        metadata.put("score", score);
        metadata.put("total", total);
        
        String title = String.format("Hoàn thành bài %s", testName);
        String description = String.format("Điểm: %d/%d", score, total);
        
        logActivity(userId, UserActivity.TYPE_TEST_COMPLETED, title, description, metadata);
    }

    @Override
    public void logVocabSaved(UUID userId, int count) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("count", count);
        
        String title = String.format("Lưu %d từ vựng mới", count);
        
        logActivity(userId, UserActivity.TYPE_VOCAB_SAVED, title, null, metadata);
    }

    @Override
    public void logSubscriptionChanged(UUID userId, String oldTier, String newTier) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("oldTier", oldTier);
        metadata.put("newTier", newTier);
        
        String title = String.format("Đổi gói %s → %s", oldTier, newTier);
        
        logActivity(userId, UserActivity.TYPE_SUBSCRIPTION_CHANGED, title, null, metadata);
    }

    @Override
    public void logLogin(UUID userId) {
        logActivity(userId, UserActivity.TYPE_LOGIN, "Đăng nhập", null, null);
    }

    @Override
    public void logAchievementEarned(UUID userId, String achievementName) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("achievement", achievementName);
        
        String title = String.format("Đạt huy hiệu \"%s\"", achievementName);
        
        logActivity(userId, UserActivity.TYPE_ACHIEVEMENT_EARNED, title, null, metadata);
    }

    @Override
    public void logCreditsChanged(UUID userId, int amount, String reason) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("amount", amount);
        metadata.put("reason", reason);
        
        String title = amount > 0 
            ? String.format("Nhận +%d Lúa", amount)
            : String.format("Sử dụng %d Lúa", Math.abs(amount));
        
        logActivity(userId, UserActivity.TYPE_CREDITS_CHANGED, title, reason, metadata);
    }

    private UserActivityDTO toDTO(UserActivity activity) {
        UserActivityDTO dto = new UserActivityDTO();
        dto.setId(activity.getId());
        dto.setUserId(activity.getUserId());
        dto.setActivityType(activity.getActivityType());
        dto.setTitle(activity.getTitle());
        dto.setDescription(activity.getDescription());
        dto.setMetadata(activity.getMetadata());
        dto.setCreatedAt(activity.getCreatedAt());
        return dto;
    }
}