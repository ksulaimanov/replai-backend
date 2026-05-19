package com.replai.backend.controller;

import com.replai.backend.dto.bot.BotResponse;
import com.replai.backend.dto.bot.UpdateBotSettingsRequest;
import com.replai.backend.service.BotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bot")
@RequiredArgsConstructor
public class BotController {

    private final BotService botService;

    @GetMapping
    public ResponseEntity<BotResponse> getBot() {
        return ResponseEntity.ok(botService.getCurrentBot());
    }

    @PutMapping("/settings")
    public ResponseEntity<BotResponse> updateSettings(@Valid @RequestBody UpdateBotSettingsRequest request) {
        return ResponseEntity.ok(botService.updateSettings(request));
    }
}

