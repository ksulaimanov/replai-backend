package com.replai.backend.service;

import com.replai.backend.dto.auth.AuthResponse;
import com.replai.backend.dto.auth.LoginRequest;
import com.replai.backend.dto.auth.RegisterRequest;
import com.replai.backend.entity.Bot;
import com.replai.backend.entity.User;
import com.replai.backend.entity.VerificationCode;
import com.replai.backend.dto.auth.ResendCodeResponseDTO;
import com.replai.backend.dto.auth.VerifyRequest;
import com.replai.backend.repository.UserRepository;
import com.replai.backend.repository.VerificationCodeRepository;
import com.replai.backend.security.JwtUtils;
import com.replai.backend.exception.EmailNotVerifiedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final VerificationCodeRepository codeRepository;
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

        String code = String.format("%06d", new Random().nextInt(1000000));
        VerificationCode verificationCode = VerificationCode.builder()
                .code(code)
                .user(savedUser)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .build();
        codeRepository.save(verificationCode);

        emailService.sendVerificationEmail(savedUser, code);

        log.info("Registered company {} with email {}. Pending verification.", savedUser.getCompanyName(), savedUser.getEmail());
        return AuthResponse.builder()
                .token("")
                .email(savedUser.getEmail())
                .companyName(savedUser.getCompanyName())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new EmailNotVerifiedException("Email is not verified");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
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
    public AuthResponse verifyEmail(VerifyRequest request) {
        VerificationCode verificationCode = codeRepository.findByCode(request.getCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification code"));

        if (verificationCode.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code has expired");
        }

        if (!verificationCode.getUser().getEmail().equals(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verfication code for this email");
        }

        User user = verificationCode.getUser();
        user.setEnabled(true);
        userRepository.save(user);
        codeRepository.delete(verificationCode);

        String token = jwtUtils.generateToken(user.getEmail());
        log.info("Email verified for user {}", user.getEmail());
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .companyName(user.getCompanyName())
                .build();
    }

    @Transactional
    public ResendCodeResponseDTO resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));

        if (user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already verified");
        }

        codeRepository.findByUser_Email(email).ifPresent(codeRepository::delete);

        String code = String.format("%06d", new Random().nextInt(1000000));
        VerificationCode verificationCode = VerificationCode.builder()
                .code(code)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .build();
        codeRepository.save(verificationCode);

        emailService.sendVerificationEmail(user, code);

        log.info("Resent verification code for user {}", user.getEmail());
        return ResendCodeResponseDTO.builder()
                .success(true)
                .email(user.getEmail())
                .build();
    }
}
