package com.org.ai.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    @Query("""
            SELECT m FROM ChatMessageEntity m
            WHERE m.conversationId = :conversationId
            ORDER BY m.createdAt ASC
            """)
    List<ChatMessageEntity> findByConversationId(@Param("conversationId") String conversationId,
                                                 Pageable pageable);

    // Derived delete queries don't inherit @Transactional from SimpleJpaRepository the way
    // deleteAll()/deleteById() do — without this, EntityManager.remove() runs with no active
    // transaction and Hibernate rejects it.
    @Transactional
    void deleteByConversationId(String conversationId);
}
