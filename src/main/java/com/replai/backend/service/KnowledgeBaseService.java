package com.replai.backend.service;

import com.replai.backend.dto.knowledgebase.KnowledgeBaseUploadResponse;
import com.replai.backend.entity.Bot;
import com.replai.backend.entity.KnowledgeBase;
import com.replai.backend.repository.BotRepository;
import com.replai.backend.repository.KnowledgeBaseRepository;
import com.replai.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final BotRepository botRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public KnowledgeBaseUploadResponse upload(MultipartFile file) {
        Bot bot = botRepository.findByOwner_Email(securityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new IllegalStateException("Bot not found for current user"));

        KnowledgeBase knowledgeBase = KnowledgeBase.builder()
                .bot(bot)
                .fileName(file.getOriginalFilename())
                .fileUrl("mock_gcp_url")
                .build();

        KnowledgeBase saved = knowledgeBaseRepository.save(knowledgeBase);
        return KnowledgeBaseUploadResponse.builder()
                .id(saved.getId())
                .fileName(saved.getFileName())
                .fileUrl(saved.getFileUrl())
                .build();
    }
}

