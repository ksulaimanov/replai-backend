package com.replai.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequestDTO {

    @JsonProperty("botId")
    private Long botId;

    @JsonProperty("chatId")
    private String chatId;

    private String message;
}

