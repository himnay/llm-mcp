package com.org.ai.controller;

import com.org.ai.dto.ChatRequest;
import com.org.ai.dto.ChatResponse;
import com.org.ai.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return chatService.handleMessage(request.getMessage());
    }
}
