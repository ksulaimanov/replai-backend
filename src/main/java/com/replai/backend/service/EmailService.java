package com.replai.backend.service;

import com.replai.backend.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    @Value("${APP_BASE_URL:http://localhost:8080}")
    private String appBaseUrl;

    public void sendVerificationEmail(User user, String token) {
        String verificationUrl = appBaseUrl + "/api/auth/verify?token=" + token;
        log.info("--- [EMAIL MOCK] Ссылка для верификации пользователя {}: {} ---", user.getEmail(), verificationUrl);
    }
}

