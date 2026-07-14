package com.org.ai.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Table(name = "chat_message")
@Entity
@Getter
@Setter
@NoArgsConstructor
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private String conversationId;

    @Column(name = "message_type", nullable = false, length = 20)
    private String messageType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public ChatMessageEntity(String conversationId, String messageType, String content) {
        this.conversationId = conversationId;
        this.messageType = messageType;
        this.content = content;
        this.createdAt = Instant.now();
    }
}
