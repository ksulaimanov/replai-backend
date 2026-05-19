package com.replai.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResendCodeRequestDTO {

    @NotBlank
    private String email;
}

