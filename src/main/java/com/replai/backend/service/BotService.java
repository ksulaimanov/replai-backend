package com.replai.backend.service;

import com.replai.backend.dto.bot.BotResponse;
import com.replai.backend.dto.bot.UpdateBotSettingsRequest;
import com.replai.backend.entity.Bot;
import com.replai.backend.repository.BotRepository;
import com.replai.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotService {

    private final BotRepository botRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public BotResponse getCurrentBot() {
        Bot bot = getBotForCurrentUser();
        return BotResponse.builder()
                .id(bot.getId())
                .name(bot.getName())
                .systemPrompt(bot.getSystemPrompt())
                .build();
    }

    @Transactional
    public BotResponse updateSettings(UpdateBotSettingsRequest request) {
        Bot bot = getBotForCurrentUser();
        bot.setName(request.getName());
        bot.setSystemPrompt(request.getSystemPrompt());
        Bot saved = botRepository.save(bot);

        log.info("Updated bot settings for user {}", securityUtils.getCurrentUserEmail());
        return BotResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .systemPrompt(saved.getSystemPrompt())
                .build();
    }

    private Bot getBotForCurrentUser() {
        return botRepository.findByOwner_Email(securityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new IllegalStateException("Bot not found for current user"));
    }
}

