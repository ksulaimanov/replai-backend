package com.replai.backend.service;

import com.replai.backend.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${MAIL_FROM:no-reply@botflow.com}")
    private String mailFrom;

    private final JavaMailSender javaMailSender;

    public void sendVerificationEmail(User user, String code) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(user.getEmail());
            helper.setSubject("Код подтверждения регистрации в BotFlow");

            String htmlContent = "<h3>Здравствуйте!</h3>"
                    + "<p>Ваш код подтверждения регистрации:</p>"
                    + "<h1 style='color: #4CAF50;'>" + code + "</h1>"
                    + "<p>Код действителен в течение 15 минут.</p>";

            helper.setText(htmlContent, true);
            javaMailSender.send(message);
            log.info("Verification email sent to {}", user.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }
}
