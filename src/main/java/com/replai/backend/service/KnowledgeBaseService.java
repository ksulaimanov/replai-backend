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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final BotRepository botRepository;
    private final SecurityUtils securityUtils;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${ai.service.internal-key:}")
    private String aiInternalKey;

    @Value("${app.storage.local-path:}")
    private String localStoragePath;

    @Transactional(readOnly = true)
    public List<KnowledgeBaseUploadResponse> getFiles() {
        Bot bot = getBotForCurrentUser();
        return knowledgeBaseRepository.findByBotId(bot.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public KnowledgeBaseUploadResponse upload(MultipartFile file) {
        Bot bot = getBotForCurrentUser();

        byte[] fileBytes = readBytes(file);
        // Production: swap storeLocally() for a GCS upload:
        //   Storage gcs = StorageOptions.getDefaultInstance().getService();
        //   BlobId blobId = BlobId.of(GCS_BUCKET, "kb/" + botId + "/" + uniqueName);
        //   gcs.create(BlobInfo.newBuilder(blobId).build(), fileBytes);
        //   String fileUrl = "gs://" + GCS_BUCKET + "/" + blobId.getName();
        String fileUrl = storeLocally(file.getOriginalFilename(), fileBytes);

        KnowledgeBase saved = knowledgeBaseRepository.save(
                KnowledgeBase.builder()
                        .bot(bot)
                        .fileName(file.getOriginalFilename())
                        .fileSize(file.getSize())
                        .fileUrl(fileUrl)
                        .build()
        );

        forwardToAiService(fileBytes, file.getOriginalFilename(), bot.getId());

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

    private String storeLocally(String originalFilename, byte[] bytes) {
        try {
            Path dir = localStoragePath == null || localStoragePath.isBlank()
                    ? Paths.get(System.getProperty("java.io.tmpdir"), "replai-uploads")
                    : Paths.get(localStoragePath);
            Files.createDirectories(dir);
            Path target = dir.resolve(UUID.randomUUID() + "_" + originalFilename);
            Files.write(target, bytes);
            log.info("File saved locally: {}", target);
            return target.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException("Local file storage failed", e);
        }
    }

    private void forwardToAiService(byte[] fileBytes, String originalFilename, Long botId) {
        try {
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
        } catch (Exception e) {
            log.warn("AI knowledge upload failed for bot {}, file '{}': {}", botId, originalFilename, e.getMessage());
        }
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
