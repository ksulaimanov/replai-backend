package com.replai.backend.controller;

import com.replai.backend.dto.telegram.TelegramWebhookUpdate;
import com.replai.backend.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/webhook/telegram")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/{token}")
    public ResponseEntity<Void> handleTelegramWebhook(
            @PathVariable String token,
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretToken,
            @RequestBody TelegramWebhookUpdate update) {
        log.info("Received Telegram webhook for token suffix {}",
                token.length() > 6 ? token.substring(token.length() - 6) : token);
        webhookService.processTelegramUpdate(token, secretToken, update);
        return ResponseEntity.ok().build();
    }
}
