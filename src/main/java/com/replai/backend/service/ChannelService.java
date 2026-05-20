package com.replai.backend.service;

import com.replai.backend.dto.channel.TelegramChannelRequest;
import com.replai.backend.dto.channel.WhatsAppChannelRequest;
import com.replai.backend.entity.Bot;
import com.replai.backend.entity.Channel;
import com.replai.backend.entity.ChannelType;
import com.replai.backend.repository.BotRepository;
import com.replai.backend.repository.ChannelRepository;
import com.replai.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final BotRepository botRepository;
    private final SecurityUtils securityUtils;
    private final TelegramService telegramService;

    @Transactional
    public void upsertTelegramChannel(TelegramChannelRequest request) {
        Bot bot = getBotForCurrentUser();
        Channel channel = channelRepository.findByBot_IdAndType(bot.getId(), ChannelType.TELEGRAM)
                .orElseGet(Channel::new);
        channel.setBot(bot);
        channel.setType(ChannelType.TELEGRAM);
        channel.setToken(request.getToken());
        channelRepository.save(channel);
        
        // Register webhook with Telegram
        telegramService.setWebhook(request.getToken());
        
        log.info("Telegram channel updated and webhook registered for user {}", securityUtils.getCurrentUserEmail());
    }

    @Transactional
    public void upsertWhatsAppChannel(WhatsAppChannelRequest request) {
        Bot bot = getBotForCurrentUser();
        Channel channel = channelRepository.findByBot_IdAndType(bot.getId(), ChannelType.WHATSAPP)
                .orElseGet(Channel::new);
        channel.setBot(bot);
        channel.setType(ChannelType.WHATSAPP);
        channel.setToken(request.getCredential());
        channelRepository.save(channel);
        log.info("WhatsApp channel mock-saved for user {}", securityUtils.getCurrentUserEmail());
    }

    private Bot getBotForCurrentUser() {
        return botRepository.findByOwner_Email(securityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new IllegalStateException("Bot not found for current user"));
    }
}

