package com.org.ai.controller;

import com.org.ai.dto.ChatRequest;
import com.org.ai.dto.ChatResponse;
import com.org.ai.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {

        String result = chatService.handleMessage(request.getMessage());

        return new ChatResponse(result);
    }

}
