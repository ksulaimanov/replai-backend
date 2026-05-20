package com.replai.backend.dto.analytics;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsResponse {
    private long totalMessages;
    private long uniqueChats;
    private long leads;
}
