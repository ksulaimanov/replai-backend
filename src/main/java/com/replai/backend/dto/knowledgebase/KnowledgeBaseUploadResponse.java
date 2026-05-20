package com.replai.backend.dto.knowledgebase;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class KnowledgeBaseUploadResponse {
    private Long id;
    
    @JsonProperty("name")
    private String fileName;
    
    private String fileUrl;
    
    private Long size;
    
    private Instant uploadDate;
}

