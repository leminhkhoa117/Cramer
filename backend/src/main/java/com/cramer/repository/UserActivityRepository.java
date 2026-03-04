package com.cramer.repository;

import com.cramer.entity.UserActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    // Lấy activities của user, sắp xếp theo thời gian mới nhất
    Page<UserActivity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // Lấy activities theo type
    Page<UserActivity> findByUserIdAndActivityTypeOrderByCreatedAtDesc(
            UUID userId, String activityType, Pageable pageable);

    // Lấy activities trong khoảng thời gian
    @Query("SELECT a FROM UserActivity a WHERE a.userId = :userId " +
           "AND a.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY a.createdAt DESC")
    List<UserActivity> findByUserIdAndDateRange(
            @Param("userId") UUID userId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate);

    // Đếm số activities theo type
    long countByUserIdAndActivityType(UUID userId, String activityType);

    // Lấy activities gần đây (limit)
    List<UserActivity> findTop10ByUserIdOrderByCreatedAtDesc(UUID userId);
}