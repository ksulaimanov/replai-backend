package com.replai.backend.service;

import com.replai.backend.dto.ai.AiChatRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${ai.service.mock:true}")
    private boolean mockAi;

    public String generateReply(Long botId, String chatId, String userMessage) {
        if (mockAi) {
            log.info("AI service is mocked, returning echo response");
            return "Эхо-заглушка: бэкенд принял ваше сообщение: " + userMessage;
        }

        try {
            AiChatRequestDTO request = AiChatRequestDTO.builder()
                    .botId(botId)
                    .chatId(chatId)
                    .message(userMessage)
                    .build();
            Map<String, String> response = restTemplate.postForObject(
                    aiServiceUrl + "/chat",
                    request,
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
