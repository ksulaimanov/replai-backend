package com.replai.backend.service;

import com.replai.backend.entity.User;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmailService {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    @Value("${MAIL_FROM:no-reply@replai.app}")
    private String mailFrom;

    @Value("${MAIL_PASSWORD}")
    private String brevoApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    void init() {
        log.info("Initializing Brevo with key length: {}",
                brevoApiKey != null ? brevoApiKey.length() : 0);
    }

    public void sendVerificationEmail(User user, String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("api-key", brevoApiKey);

        String htmlContent = """
            <div style="font-family: 'Inter', Arial, sans-serif; max-width: 480px; margin: 0 auto; padding: 32px 24px; background: #ffffff;">
              <div style="text-align: center; margin-bottom: 32px;">
                <span style="font-size: 24px; font-weight: 700; color: #42008A; letter-spacing: -0.5px;">repl<span style="text-transform: uppercase;">AI</span></span>
              </div>
              <h2 style="font-size: 20px; font-weight: 600; color: #191B23; margin-bottom: 12px;">Подтверждение email</h2>
              <p style="font-size: 15px; color: #424754; line-height: 1.6; margin-bottom: 28px;">
                Здравствуйте! Введите код ниже, чтобы завершить регистрацию в replAI.
              </p>
              <div style="background: #F4EFFF; border-radius: 14px; padding: 24px; text-align: center; margin-bottom: 28px;">
                <span style="font-size: 40px; font-weight: 700; letter-spacing: 10px; color: #42008A;">""" + code + """
              </span>
              </div>
              <p style="font-size: 13px; color: #9CA3AF; text-align: center; margin-bottom: 0;">
                Код действителен 15 минут. Если вы не регистрировались — просто проигнорируйте это письмо.
              </p>
            </div>
            """;

        Map<String, Object> payload = Map.of(
            "sender", Map.of("name", "replAI", "email", mailFrom),
            "to", List.of(Map.of("email", user.getEmail())),
            "subject", "Код подтверждения регистрации в replAI",
            "htmlContent", htmlContent
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(BREVO_API_URL, request, String.class);
            log.info("Email successfully sent via Brevo API. Status: {}", response.getStatusCode());
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("Brevo API error! Status: {}, Body: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error while sending email to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }
}
