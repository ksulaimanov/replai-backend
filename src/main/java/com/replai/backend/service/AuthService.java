package com.replai.backend.service;

import com.replai.backend.dto.auth.AuthResponse;
import com.replai.backend.dto.auth.LoginRequest;
import com.replai.backend.dto.auth.RegisterRequest;
import com.replai.backend.entity.Bot;
import com.replai.backend.entity.User;
import com.replai.backend.entity.VerificationToken;
import com.replai.backend.repository.UserRepository;
import com.replai.backend.repository.VerificationTokenRepository;
import com.replai.backend.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            throw new IllegalStateException("User with this email already exists");
        });

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .companyName(request.getCompanyName())
                .build();

        Bot bot = Bot.builder()
                .name(request.getCompanyName() + " Bot")
                .systemPrompt("You are an AI sales assistant for " + request.getCompanyName())
                .owner(user)
                .build();
        user.setBot(bot);

        User savedUser = userRepository.save(user);

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .user(savedUser)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .build();
        tokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(savedUser, token);

        log.info("Registered company {} with email {}. Pending verification.", savedUser.getCompanyName(), savedUser.getEmail());
        return AuthResponse.builder()
                .token("")
                .email(savedUser.getEmail())
                .companyName(savedUser.getCompanyName())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new IllegalStateException("Email не подтвержден");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalStateException("Invalid email or password");
        }

        String token = jwtUtils.generateToken(user.getEmail());
        log.info("User {} logged in", user.getEmail());
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .companyName(user.getCompanyName())
                .build();
    }

    @Transactional
    public void verifyEmail(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalStateException("Invalid verification token"));

        if (verificationToken.isExpired()) {
            throw new IllegalStateException("Verification token has expired");
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);
        tokenRepository.delete(verificationToken);

        log.info("Email verified for user {}", user.getEmail());
    }
}
