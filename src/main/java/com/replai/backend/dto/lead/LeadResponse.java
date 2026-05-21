package com.replai.backend.dto.lead;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadResponse {
    private Long id;
    private String name;
    private String phone;
    private String externalChatId;
    private Instant createdAt;
    private Long chatId;
    private String status;
    private String leadSummary;
}
