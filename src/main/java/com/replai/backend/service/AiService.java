package com.replai.backend.service;

import com.replai.backend.dto.ai.AiChatRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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

    @Value("${ai.service.mock:false}")
    private boolean mockAi;

    @Value("${ai.service.internal-key}")
    private String internalApiKey;

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

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Key", internalApiKey);
            HttpEntity<AiChatRequestDTO> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    aiServiceUrl + "/chat/",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, String> body = response.getBody();
            if (body != null && body.get("reply") != null) {
                return body.get("reply");
            }
        } catch (Exception ex) {
            log.warn("AI service unavailable, using fallback response: {}", ex.getMessage());
        }

        return "Эхо-заглушка: бэкенд принял ваше сообщение: " + userMessage;
    }
}
