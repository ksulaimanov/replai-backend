package com.replai.backend.controller;

import com.replai.backend.dto.telegram.TelegramWebhookUpdate;
import com.replai.backend.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/webhook/telegram")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/{token}")
    public ResponseEntity<Void> handleTelegramWebhook(@PathVariable String token,
                                                      @RequestBody TelegramWebhookUpdate update) {
        log.info("Received Telegram webhook for token suffix {}", token.length() > 6 ? token.substring(token.length() - 6) : token);
        webhookService.processTelegramUpdate(token, update);
        return ResponseEntity.ok().build();
    }
}

