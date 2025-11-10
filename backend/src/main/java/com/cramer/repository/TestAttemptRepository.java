package com.cramer.repository;

import com.cramer.entity.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {
    Optional<TestAttempt> findByUserIdAndExamSourceAndTestNumberAndSkill(
            UUID userId, String examSource, String testNumber, String skill
    );

    List<TestAttempt> findByUserId(UUID userId);
}
