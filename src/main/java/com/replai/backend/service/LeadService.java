package com.replai.backend.service;

import com.replai.backend.dto.analytics.AnalyticsResponse;
import com.replai.backend.dto.lead.LeadResponse;
import com.replai.backend.entity.Bot;
import com.replai.backend.entity.Chat;
import com.replai.backend.entity.ChatStatus;
import com.replai.backend.repository.BotRepository;
import com.replai.backend.repository.ChatRepository;
import com.replai.backend.repository.LeadRepository;
import com.replai.backend.repository.MessageRepository;
import com.replai.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final BotRepository botRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<LeadResponse> getLeads() {
        Bot bot = getBotForCurrentUser();
        return leadRepository.findByBot_IdOrderByCreatedAtDesc(bot.getId())
                .stream()
                .map(lead -> {
                    Chat chat = chatRepository
                            .findByBot_IdAndExternalChatId(bot.getId(), lead.getExternalChatId())
                            .orElse(null);
                    return LeadResponse.builder()
                            .id(lead.getId())
                            .name(lead.getName())
                            .phone(lead.getPhone())
                            .externalChatId(lead.getExternalChatId())
                            .createdAt(lead.getCreatedAt())
                            .chatId(chat != null ? chat.getId() : null)
                            .status(chat != null ? chat.getStatus().name() : ChatStatus.ACTIVE.name())
                            .leadSummary(chat != null ? chat.getLeadSummary() : null)
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics() {
        Bot bot = getBotForCurrentUser();
        long messages = messageRepository.countByBotId(bot.getId());
        long chats = chatRepository.countByBot_Id(bot.getId());
        long leads = leadRepository.countByBot_Id(bot.getId());
        return AnalyticsResponse.builder()
                .totalMessages(messages)
                .uniqueChats(chats)
                .leads(leads)
                .build();
    }

    private Bot getBotForCurrentUser() {
        return botRepository.findByOwner_Email(securityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new IllegalStateException("Bot not found for current user"));
    }
}
