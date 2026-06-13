package com.cramer.engagement.repository;

import com.cramer.engagement.domain.UserActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data repository for {@link UserActivity} (SPEC-16 §6). */
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    List<UserActivity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
