package com.cramer.repository;

import com.cramer.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for ChatMessage entity.
 * Provides methods for managing chat history and retrieving conversation context.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Find all messages for a user with pagination (newest first).
     *
     * @param userId   the user's UUID
     * @param pageable pagination parameters
     * @return page of chat messages
     */
    Page<ChatMessage> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find recent messages for conversation context.
     * Returns messages in ascending order (oldest first) for building conversation history.
     *
     * @param userId the user's UUID
     * @param pageable pagination with limit
     * @return list of recent messages
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.userId = :userId ORDER BY m.createdAt DESC")
    List<ChatMessage> findRecentByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Count total messages for a user.
     *
     * @param userId the user's UUID
     * @return count of messages
     */
    long countByUserId(UUID userId);

    /**
     * Delete old messages (cleanup - older than specified date).
     *
     * @param beforeDate delete messages created before this date
     * @return number of deleted messages
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ChatMessage m WHERE m.createdAt < :beforeDate")
    int deleteOldMessages(@Param("beforeDate") OffsetDateTime beforeDate);

    /**
     * Delete all messages for a user (for account cleanup or reset).
     *
     * @param userId the user's UUID
     * @return number of deleted messages
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ChatMessage m WHERE m.userId = :userId")
    int deleteByUserId(@Param("userId") UUID userId);
}
