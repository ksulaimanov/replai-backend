package com.replai.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${AI_SERVICE_URL:http://localhost:8000}")
    private String aiServiceUrl;

    public String generateReply(String userMessage) {
        try {
            Map<String, String> response = restTemplate.postForObject(
                    aiServiceUrl + "/chat",
                    Map.of("message", userMessage),
                    Map.class
            );

            if (response != null && response.get("reply") != null) {
                return response.get("reply");
            }
        } catch (Exception ex) {
            log.warn("AI service unavailable, using fallback response: {}", ex.getMessage());
        }

        return "Эхо-заглушка: бэкенд принял ваше сообщение: " + userMessage;
    }
}

