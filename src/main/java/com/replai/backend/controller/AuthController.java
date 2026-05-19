package com.replai.backend.controller;

import com.replai.backend.dto.auth.AuthResponse;
import com.replai.backend.dto.auth.LoginRequest;
import com.replai.backend.dto.auth.RegisterRequest;
import com.replai.backend.dto.auth.ResendCodeRequestDTO;
import com.replai.backend.dto.auth.ResendCodeResponseDTO;
import com.replai.backend.dto.auth.VerifyRequest;
import com.replai.backend.dto.auth.VerifyResponseDTO;
import com.replai.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<VerifyResponseDTO> verify(@Valid @RequestBody VerifyRequest request) {
        return ResponseEntity.ok(authService.verifyEmail(request));
    }

    @PostMapping("/resend-code")
    public ResponseEntity<ResendCodeResponseDTO> resendCode(@Valid @RequestBody ResendCodeRequestDTO request) {
        return ResponseEntity.ok(authService.resendVerificationCode(request.getEmail()));
    }
}
