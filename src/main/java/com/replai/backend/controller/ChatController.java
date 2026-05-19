package com.replai.backend.controller;

import com.replai.backend.dto.chat.ChatSummaryResponse;
import com.replai.backend.dto.chat.MessageResponse;
import com.replai.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    public ResponseEntity<List<ChatSummaryResponse>> getChats() {
        return ResponseEntity.ok(chatService.getChats());
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(@PathVariable Long id) {
        return ResponseEntity.ok(chatService.getMessages(id));
    }
}

