package com.nexacorp.ai.controller;

import com.nexacorp.ai.dto.ChatRequest;
import com.nexacorp.ai.dto.ChatResponse;
import com.nexacorp.ai.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {

        String result = chatService.handleMessage(request.getMessage());

        return new ChatResponse(result);
    }

}
