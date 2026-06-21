package com.org.ai.controller;

import com.org.ai.dto.ChatRequest;
import com.org.ai.dto.ChatResponse;
import com.org.ai.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "AI Assistant")
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "Send chat message")
    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {

        String result = chatService.handleMessage(request.getMessage());

        return new ChatResponse(result);
    }

    @Operation(summary = "Stream chat response")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestParam String conversationId,
                                 @RequestParam String message) {
        SseEmitter emitter = new SseEmitter(60_000L);
        chatService.streamChat(conversationId, message, emitter);
        return emitter;
    }
}
