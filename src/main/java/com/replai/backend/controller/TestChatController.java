package com.replai.backend.controller;

import com.replai.backend.service.AiService;
import com.replai.backend.service.BotService;
import com.replai.backend.dto.bot.BotResponse;
import com.replai.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test-chat")
@RequiredArgsConstructor
public class TestChatController {

    private final AiService aiService;
    private final BotService botService;
    private final SecurityUtils securityUtils;

    @PostMapping
    public ResponseEntity<Map<String, String>> testChat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "").trim();
        if (message.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Сообщение не может быть пустым"));
        }

        BotResponse bot = botService.getCurrentBot();
        String sessionId = "web_test_" + securityUtils.getCurrentUserEmail().hashCode();
        String reply = aiService.generateReply(bot.getId(), sessionId, message, bot.getSystemPrompt()).getReply();

        return ResponseEntity.ok(Map.of("reply", reply));
    }
}
