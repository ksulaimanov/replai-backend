package com.replai.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponseDTO {

    private String reply;

    @JsonProperty("is_lead")
    private boolean isLead;

    @JsonProperty("lead_summary")
    private String leadSummary;
}
