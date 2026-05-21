package com.replai.backend.service;

import com.replai.backend.dto.ai.AiChatRequestDTO;
import com.replai.backend.dto.ai.AiChatResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private static final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(30_000);
        return new RestTemplate(factory);
    }

    private static final AiChatResponseDTO FALLBACK =
            new AiChatResponseDTO(
                    "Извините, я сейчас уточняю информацию у менеджера. Он свяжется с вами в ближайшее время 🙏",
                    false, null);

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${ai.service.mock:false}")
    private boolean mockAi;

    @Value("${ai.service.internal-key}")
    private String internalApiKey;

    public AiChatResponseDTO generateReply(Long botId, String chatId, String userMessage, String systemPrompt) {
        if (mockAi) {
            log.info("AI mock mode: returning echo for botId={}", botId);
            return new AiChatResponseDTO("Тест: " + userMessage, false, null);
        }

        try {
            AiChatRequestDTO request = AiChatRequestDTO.builder()
                    .botId(botId)
                    .chatId(chatId)
                    .message(userMessage)
                    .systemPrompt(systemPrompt)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Key", internalApiKey);
            HttpEntity<AiChatRequestDTO> entity = new HttpEntity<>(request, headers);

            ResponseEntity<AiChatResponseDTO> response = restTemplate.exchange(
                    aiServiceUrl + "/chat/",
                    HttpMethod.POST,
                    entity,
                    AiChatResponseDTO.class
            );

            AiChatResponseDTO body = response.getBody();
            if (body != null && body.getReply() != null) {
                return body;
            }
        } catch (Exception ex) {
            log.warn("AI service unavailable (botId={}): {}", botId, ex.getMessage());
        }

        return FALLBACK;
    }
}
