package com.cramer.service;

import com.cramer.dto.UserActivityDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface UserActivityService {
    
    // Lấy activities của user (phân trang)
    Page<UserActivityDTO> getUserActivities(UUID userId, Pageable pageable);
    
    // Lấy activities theo type
    Page<UserActivityDTO> getUserActivitiesByType(UUID userId, String type, Pageable pageable);
    
    // Lấy activities gần đây (cho user detail page)
    List<UserActivityDTO> getRecentActivities(UUID userId, int limit);
    
    // Log activity mới
    void logActivity(UUID userId, String activityType, String title, 
                     String description, Map<String, Object> metadata);
    
    // Các helper methods để log các loại activity cụ thể
    void logTestCompleted(UUID userId, String testName, String skill, Integer score, Integer total);
    void logVocabSaved(UUID userId, int count);
    void logSubscriptionChanged(UUID userId, String oldTier, String newTier);
    void logLogin(UUID userId);
    void logAchievementEarned(UUID userId, String achievementName);
    void logCreditsChanged(UUID userId, int amount, String reason);
}