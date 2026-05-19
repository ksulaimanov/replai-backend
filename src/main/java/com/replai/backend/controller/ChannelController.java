package com.replai.backend.controller;

import com.replai.backend.dto.channel.TelegramChannelRequest;
import com.replai.backend.dto.channel.WhatsAppChannelRequest;
import com.replai.backend.service.ChannelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @PostMapping("/telegram")
    public ResponseEntity<Void> connectTelegram(@Valid @RequestBody TelegramChannelRequest request) {
        channelService.upsertTelegramChannel(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/whatsapp")
    public ResponseEntity<Void> connectWhatsApp(@Valid @RequestBody WhatsAppChannelRequest request) {
        channelService.upsertWhatsAppChannel(request);
        return ResponseEntity.ok().build();
    }
}

