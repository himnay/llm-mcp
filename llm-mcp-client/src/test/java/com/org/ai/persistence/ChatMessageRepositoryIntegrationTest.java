package com.org.ai.persistence;

import com.org.ai.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ChatMessageRepositoryIntegrationTest {

    @Autowired
    private ChatMessageRepository repository;

    @BeforeEach
    void cleanup() {
        repository.deleteAll();
    }

    @DisplayName("Saves a chat message and generates id, timestamp, and stores all fields")
    @Test
    void save_persistsMessageWithGeneratedId() {
        ChatMessageEntity entity = new ChatMessageEntity("conv-1", "USER", "Hello!");

        ChatMessageEntity saved = repository.save(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getConversationId()).isEqualTo("conv-1");
        assertThat(saved.getMessageType()).isEqualTo("USER");
        assertThat(saved.getContent()).isEqualTo("Hello!");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @DisplayName("Returns messages for a conversation in insertion order")
    @Test
    void findByConversationId_returnsInOrder() {
        repository.save(new ChatMessageEntity("conv-2", "USER", "Question 1"));
        repository.save(new ChatMessageEntity("conv-2", "ASSISTANT", "Answer 1"));
        repository.save(new ChatMessageEntity("conv-2", "USER", "Question 2"));

        List<ChatMessageEntity> messages = repository
                .findByConversationId("conv-2", PageRequest.of(0, 10));

        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).getMessageType()).isEqualTo("USER");
        assertThat(messages.get(1).getMessageType()).isEqualTo("ASSISTANT");
    }

    @DisplayName("Excludes messages belonging to other conversation ids")
    @Test
    void findByConversationId_excludesOtherConversations() {
        repository.save(new ChatMessageEntity("conv-A", "USER", "msg A"));
        repository.save(new ChatMessageEntity("conv-B", "USER", "msg B"));

        List<ChatMessageEntity> messages = repository
                .findByConversationId("conv-A", PageRequest.of(0, 10));

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getContent()).isEqualTo("msg A");
    }

    @DisplayName("Deletes only the messages of the targeted conversation id")
    @Test
    void deleteByConversationId_removesOnlyThatConversation() {
        repository.save(new ChatMessageEntity("conv-X", "USER", "keep X"));
        repository.save(new ChatMessageEntity("conv-Y", "USER", "remove Y1"));
        repository.save(new ChatMessageEntity("conv-Y", "ASSISTANT", "remove Y2"));

        repository.deleteByConversationId("conv-Y");

        assertThat(repository.findByConversationId("conv-Y", PageRequest.of(0, 10))).isEmpty();
        assertThat(repository.findByConversationId("conv-X", PageRequest.of(0, 10))).hasSize(1);
    }

    @DisplayName("Limits the number of returned messages to the requested page size")
    @Test
    void findByConversationId_respectsPageSizeLimit() {
        for (int i = 0; i < 5; i++) {
            repository.save(new ChatMessageEntity("conv-page", "USER", "msg " + i));
        }

        List<ChatMessageEntity> messages = repository
                .findByConversationId("conv-page", PageRequest.of(0, 3));

        assertThat(messages).hasSize(3);
    }
}
