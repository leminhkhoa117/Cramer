package com.cramer.repository;

import com.cramer.entity.SpeakingTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SpeakingTopic entity.
 */
@Repository
public interface SpeakingTopicRepository extends JpaRepository<SpeakingTopic, Long> {

    /**
     * Find all active topics ordered by name.
     */
    List<SpeakingTopic> findByIsActiveTrueOrderByNameEnAsc();

    /**
     * Find a topic by its unique code.
     */
    Optional<SpeakingTopic> findByCode(String code);

    /**
     * Check if a topic exists by code.
     */
    boolean existsByCode(String code);

    /**
     * Find all active topics.
     */
    @Query("SELECT t FROM SpeakingTopic t WHERE t.isActive = true ORDER BY t.nameEn ASC")
    List<SpeakingTopic> findAllActiveTopics();
}
