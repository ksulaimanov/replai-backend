package com.replai.backend.dto.bot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBotSettingsRequest {

    @NotBlank(message = "Bot name is required")
    @Size(min = 2, max = 150, message = "Bot name must be between 2 and 150 characters")
    private String name;

    @NotBlank(message = "System prompt is required")
    @Size(min = 1, max = 4000, message = "System prompt must be between 1 and 4000 characters")
    private String systemPrompt;
}

