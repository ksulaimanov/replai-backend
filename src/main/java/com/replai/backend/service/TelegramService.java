package com.replai.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramService {

    private final RestTemplate restTemplate = new RestTemplate();

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

