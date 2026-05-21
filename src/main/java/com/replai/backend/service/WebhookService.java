package com.replai.backend.service;

import com.replai.backend.dto.ai.AiChatResponseDTO;
import com.replai.backend.dto.telegram.TelegramWebhookUpdate;
import com.replai.backend.entity.Bot;
import com.replai.backend.entity.Channel;
import com.replai.backend.entity.Chat;
import com.replai.backend.entity.ChatStatus;
import com.replai.backend.entity.Lead;
import com.replai.backend.entity.Message;
import com.replai.backend.entity.SourceChannel;
import com.replai.backend.repository.ChannelRepository;
import com.replai.backend.repository.ChatRepository;
import com.replai.backend.repository.LeadRepository;
import com.replai.backend.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    // Matches: +996 555 123 456 (KG), +998 90 123 45 67 (UZ), +7 (999) 123-45-67 (RU), 89991234567
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+\\d{1,4}|8)[\\s\\-]?\\(?\\d{2,5}\\)?[\\s\\-]?\\d{3,5}(?:[\\s\\-]?\\d{2,3}){1,3}"
    );

    private final ChannelRepository channelRepository;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final LeadRepository leadRepository;
    private final AiService aiService;
    private final TelegramService telegramService;

    @Transactional
    public void processTelegramUpdate(String botToken, TelegramWebhookUpdate update) {
        if (update == null || update.getMessage() == null || update.getMessage().getChat() == null) {
            log.warn("Telegram update ignored because message/chat is missing");
            return;
        }

        Channel channel = channelRepository.findByToken(botToken)
                .orElseThrow(() -> new IllegalStateException("No channel registered for token"));
        Bot bot = channel.getBot();

        Long telegramChatId = update.getMessage().getChat().getId();
        String externalChatId = String.valueOf(telegramChatId);
        String incomingText = update.getMessage().getText() == null ? "" : update.getMessage().getText();

        Chat chat = chatRepository.findByExternalChatId(externalChatId)
                .orElseGet(() -> chatRepository.save(Chat.builder()
                        .externalChatId(externalChatId)
                        .sourceChannel(SourceChannel.TG)
                        .bot(bot)
                        .createdAt(Instant.now())
                        .build()));

        messageRepository.save(Message.builder()
                .chat(chat)
                .content(incomingText)
                .isFromClient(true)
                .timestamp(Instant.now())
                .build());

        extractLead(bot, externalChatId, incomingText, update.getMessage().getFrom());

        AiChatResponseDTO aiResponse = aiService.generateReply(
                bot.getId(), externalChatId, incomingText, bot.getSystemPrompt());

        messageRepository.save(Message.builder()
                .chat(chat)
                .content(aiResponse.getReply())
                .isFromClient(false)
                .timestamp(Instant.now())
                .build());

        if (aiResponse.isLead() && chat.getStatus() != ChatStatus.HOT_LEAD) {
            chat.setStatus(ChatStatus.HOT_LEAD);
            chatRepository.save(chat);
            log.info("HOT_LEAD detected for chatId={} botId={}: {}",
                    externalChatId, bot.getId(), aiResponse.getLeadSummary());
        }

        String[] parts = aiResponse.getReply().split("\\|\\|\\|");
        for (String part : parts) {
            String text = part.strip();
            if (!text.isEmpty()) {
                telegramService.sendMessage(botToken, telegramChatId, text);
            }
        }
    }

    private void extractLead(Bot bot, String externalChatId, String text,
                              TelegramWebhookUpdate.TelegramUser from) {
        Matcher matcher = PHONE_PATTERN.matcher(text);
        if (!matcher.find()) return;
        if (leadRepository.existsByBot_IdAndExternalChatId(bot.getId(), externalChatId)) return;

        String raw = matcher.group().trim();
        boolean hasPlus = raw.startsWith("+");
        String digits = raw.replaceAll("[^\\d]", "");
        if (digits.length() < 7 || digits.length() > 15) return;
        String phone = hasPlus ? "+" + digits : digits;
        String name = buildName(from);

        leadRepository.save(Lead.builder()
                .bot(bot)
                .externalChatId(externalChatId)
                .phone(phone)
                .name(name)
                .createdAt(Instant.now())
                .build());
        log.info("Lead captured: chatId={} phone={}", externalChatId, phone);
    }

    private String buildName(TelegramWebhookUpdate.TelegramUser from) {
        if (from == null) return null;
        StringBuilder sb = new StringBuilder();
        if (from.getFirstName() != null) sb.append(from.getFirstName());
        if (from.getLastName() != null) sb.append(" ").append(from.getLastName());
        String full = sb.toString().trim();
        if (!full.isEmpty()) return full;
        return from.getUsername() != null ? "@" + from.getUsername() : null;
    }
}
