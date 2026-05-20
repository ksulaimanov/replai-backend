package com.replai.backend.controller;

import com.replai.backend.dto.analytics.AnalyticsResponse;
import com.replai.backend.dto.bot.BotResponse;
import com.replai.backend.dto.bot.UpdateBotSettingsRequest;
import com.replai.backend.dto.knowledgebase.KnowledgeBaseUploadResponse;
import com.replai.backend.dto.lead.LeadResponse;
import com.replai.backend.service.BotService;
import com.replai.backend.service.KnowledgeBaseService;
import com.replai.backend.service.LeadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/bot")
@RequiredArgsConstructor
public class BotController {

    private final BotService botService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final LeadService leadService;

    @GetMapping("/config")
    public ResponseEntity<BotResponse> getBot() {
        return ResponseEntity.ok(botService.getCurrentBot());
    }

    @PutMapping("/config")
    public ResponseEntity<BotResponse> updateSettings(@Valid @RequestBody UpdateBotSettingsRequest request) {
        return ResponseEntity.ok(botService.updateSettings(request));
    }

    @GetMapping("/files")
    public ResponseEntity<List<KnowledgeBaseUploadResponse>> getFiles() {
        return ResponseEntity.ok(knowledgeBaseService.getFiles());
    }

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<KnowledgeBaseUploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(knowledgeBaseService.upload(file));
    }

    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long fileId) {
        knowledgeBaseService.delete(fileId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/leads")
    public ResponseEntity<List<LeadResponse>> getLeads() {
        return ResponseEntity.ok(leadService.getLeads());
    }

    @GetMapping("/analytics")
    public ResponseEntity<AnalyticsResponse> getAnalytics() {
        return ResponseEntity.ok(leadService.getAnalytics());
    }
}
