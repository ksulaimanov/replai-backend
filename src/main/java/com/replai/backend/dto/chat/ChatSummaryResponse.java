package com.replai.backend.dto.chat;

import com.replai.backend.entity.SourceChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSummaryResponse {
    private Long id;
    private String externalChatId;
    private SourceChannel sourceChannel;
    private String lastMessage;
    private Instant lastMessageAt;
}

