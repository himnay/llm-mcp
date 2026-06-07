package com.org.ai.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    @Query("""
            SELECT m FROM ChatMessageEntity m
            WHERE m.conversationId = :conversationId
            ORDER BY m.createdAt ASC
            """)
    List<ChatMessageEntity> findByConversationId(@Param("conversationId") String conversationId,
                                                  Pageable pageable);

    void deleteByConversationId(String conversationId);
}
