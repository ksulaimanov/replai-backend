package com.replai.backend.dto.telegram;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class TelegramWebhookUpdate {

    @JsonProperty("update_id")
    private Long updateId;

    private TelegramMessage message;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TelegramMessage {
        @JsonProperty("message_id")
        private Long messageId;

        private TelegramChat chat;

        private String text;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TelegramChat {
        private Long id;
    }
}

