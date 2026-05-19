package com.replai.backend.service;

import com.replai.backend.dto.auth.AuthResponse;
import com.replai.backend.dto.auth.LoginRequest;
import com.replai.backend.dto.auth.RegisterRequest;
import com.replai.backend.entity.Bot;
import com.replai.backend.entity.User;
import com.replai.backend.repository.UserRepository;
import com.replai.backend.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

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
        String token = jwtUtils.generateToken(savedUser.getEmail());

        log.info("Registered company {} with email {}", savedUser.getCompanyName(), savedUser.getEmail());
        return AuthResponse.builder()
                .token(token)
                .email(savedUser.getEmail())
                .companyName(savedUser.getCompanyName())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("Invalid email or password"));

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
}

