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

    @JsonProperty("bot_id")
    private Long botId;

    @JsonProperty("chat_id")
    private String chatId;

    private String message;
}

