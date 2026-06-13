package com.cramer.engagement.repository;

import com.cramer.engagement.domain.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** Spring Data repository for {@link ChatMessage} (SPEC-16 §2). */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<ChatMessage> findByUserIdOrderByCreatedAtAsc(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ChatMessage m where m.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
