package com.replai.backend.service;

import com.replai.backend.dto.knowledgebase.KnowledgeBaseUploadResponse;
import com.replai.backend.entity.Bot;
import com.replai.backend.entity.KnowledgeBase;
import com.replai.backend.repository.BotRepository;
import com.replai.backend.repository.KnowledgeBaseRepository;
import com.replai.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final BotRepository botRepository;
    private final SecurityUtils securityUtils;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${ai.service.internal-key:}")
    private String aiInternalKey;

    @Transactional(readOnly = true)
    public List<KnowledgeBaseUploadResponse> getFiles() {
        Bot bot = getBotForCurrentUser();
        return knowledgeBaseRepository.findByBotId(bot.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public KnowledgeBaseUploadResponse upload(MultipartFile file) {
        validateFile(file);

        Bot bot = getBotForCurrentUser();
        byte[] fileBytes = readBytes(file);

        try {
            forwardToAiService(fileBytes, file.getOriginalFilename(), bot.getId());
        } catch (RestClientException e) {
            log.error("AI service unavailable for bot {}, file '{}': {}", bot.getId(), file.getOriginalFilename(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Knowledge base upload failed: AI service is unavailable");
        }

        KnowledgeBase saved = knowledgeBaseRepository.save(
                KnowledgeBase.builder()
                        .bot(bot)
                        .fileName(file.getOriginalFilename())
                        .fileSize(file.getSize())
                        .fileUrl("")
                        .build()
        );

        return toResponse(saved);
    }

    @Transactional
    public void delete(Long fileId) {
        Bot bot = getBotForCurrentUser();
        KnowledgeBase kb = knowledgeBaseRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        if (!kb.getBot().getId().equals(bot.getId())) {
            throw new IllegalStateException("Unauthorized to delete this file");
        }
        knowledgeBaseRepository.delete(kb);
    }

    private void validateFile(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File exceeds 5 MB limit");
        }
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (!name.endsWith(".txt") && !name.endsWith(".md") && !name.endsWith(".pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only .txt, .md, and .pdf files are allowed");
        }
    }

    private Bot getBotForCurrentUser() {
        return botRepository.findByOwner_Email(securityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new IllegalStateException("Bot not found for current user"));
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file", e);
        }
    }

    private void forwardToAiService(byte[] fileBytes, String originalFilename, Long botId) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("bot_id", String.valueOf(botId));
        body.add("file", new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return originalFilename;
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-Internal-Key", aiInternalKey);

        restTemplate.postForObject(
                aiServiceUrl + "/knowledge/upload",
                new HttpEntity<>(body, headers),
                String.class
        );
        log.info("File '{}' forwarded to AI knowledge service for bot {}", originalFilename, botId);
    }

    private KnowledgeBaseUploadResponse toResponse(KnowledgeBase kb) {
        return KnowledgeBaseUploadResponse.builder()
                .id(kb.getId())
                .fileName(kb.getFileName())
                .fileUrl(kb.getFileUrl())
                .size(kb.getFileSize())
                .uploadDate(kb.getCreatedAt())
                .build();
    }
}
