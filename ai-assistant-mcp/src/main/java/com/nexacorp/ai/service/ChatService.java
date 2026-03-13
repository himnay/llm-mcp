package com.nexacorp.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;

    public String handleMessage(String message) {
        String response = chatClient
                .prompt()
                .user(message)
                .call()
                .content();

        return response;
    }

}
