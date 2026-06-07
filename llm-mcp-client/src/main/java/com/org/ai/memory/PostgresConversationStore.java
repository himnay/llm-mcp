package com.org.ai.memory;

import com.org.ai.persistence.ChatMessageEntity;
import com.org.ai.persistence.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class PostgresConversationStore {

    private final ChatMessageRepository repository;

    @Transactional(readOnly = true)
    public List<Message> loadMessages(String conversationId, int limit) {
        return repository.findByConversationId(conversationId, PageRequest.of(0, limit))
                .stream()
                .map(e -> switch (e.getMessageType()) {
                    case "USER"      -> (Message) new UserMessage(e.getContent());
                    case "ASSISTANT" -> new AssistantMessage(e.getContent());
                    default          -> null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional
    public void saveExchange(String conversationId, String userText, String assistantText) {
        repository.save(new ChatMessageEntity(conversationId, "USER", userText));
        repository.save(new ChatMessageEntity(conversationId, "ASSISTANT", assistantText));
    }

    @Transactional
    public void clearConversation(String conversationId) {
        repository.deleteByConversationId(conversationId);
    }
}
