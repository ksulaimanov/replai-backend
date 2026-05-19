package com.replai.backend.service;

import com.replai.backend.dto.telegram.TelegramWebhookUpdate;
import com.replai.backend.entity.Bot;
import com.replai.backend.entity.Chat;
import com.replai.backend.entity.Message;
import com.replai.backend.entity.SourceChannel;
import com.replai.backend.repository.BotRepository;
import com.replai.backend.repository.ChatRepository;
import com.replai.backend.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final BotRepository botRepository;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final AiService aiService;
    private final TelegramService telegramService;

    @Transactional
    public void processTelegramUpdate(String botToken, TelegramWebhookUpdate update) {
        if (update == null || update.getMessage() == null || update.getMessage().getChat() == null) {
            log.warn("Telegram update ignored because message/chat is missing");
            return;
        }

        Bot bot = botRepository.findAll().stream()
                .filter(candidate -> candidate.getChannels().stream().anyMatch(channel -> botToken.equals(channel.getToken())))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Bot not found for provided token"));

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

        Message incoming = Message.builder()
                .chat(chat)
                .content(incomingText)
                .isFromClient(true)
                .timestamp(Instant.now())
                .build();
        messageRepository.save(incoming);

        String aiReply = aiService.generateReply(bot.getId(), chat.getExternalChatId(), incomingText);

        Message outgoing = Message.builder()
                .chat(chat)
                .content(aiReply)
                .isFromClient(false)
                .timestamp(Instant.now())
                .build();
        messageRepository.save(outgoing);

        telegramService.sendMessage(botToken, telegramChatId, aiReply);
    }
}
