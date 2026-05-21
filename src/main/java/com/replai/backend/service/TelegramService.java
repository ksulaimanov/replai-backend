package com.replai.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramService {

    @Value("${app.domain}")
    private String domain;

    private final RestTemplate restTemplate = new RestTemplate();

    public void setWebhook(String botToken, String secretToken) {
        String webhookUrl = String.format("%s/api/webhook/telegram/%s", domain, botToken);
        String apiUrl = String.format("https://api.telegram.org/bot%s/setWebhook", botToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("url", webhookUrl);
        payload.put("secret_token", secretToken);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);
            Map body = response.getBody();
            boolean ok = Boolean.TRUE.equals(body != null ? body.get("ok") : null);
            if (!ok) {
                String description = body != null ? String.valueOf(body.get("description")) : "null response";
                log.error("Telegram rejected webhook registration: {}", description);
                throw new IllegalStateException("Telegram setWebhook failed: " + description);
            }
            log.info("Telegram webhook registered successfully for domain {}", domain);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to reach Telegram API during webhook registration: {}", e.getMessage());
            throw new IllegalStateException("Could not register Telegram webhook", e);
        }
    }

    public void sendMessage(String botToken, Long chatId, String text) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(Map.of(
                "chat_id", chatId,
                "text", text
        ), headers);

        restTemplate.postForObject(url, request, String.class);
        log.info("Telegram message sent to chat {}", chatId);
    }
}
