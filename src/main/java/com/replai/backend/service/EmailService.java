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

    @Value("${MAIL_FROM:no-reply@replai.app}")
    private String mailFrom;

    private final JavaMailSender javaMailSender;

    public void sendVerificationEmail(User user, String code) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(user.getEmail());
            helper.setSubject("Код подтверждения регистрации в replAI");

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

            helper.setText(htmlContent, true);
            javaMailSender.send(message);
            log.info("Verification email sent to {}", user.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }
}
