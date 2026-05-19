package com.replai.backend.service;

import com.replai.backend.dto.chat.ChatSummaryResponse;
import com.replai.backend.dto.chat.MessageResponse;
import com.replai.backend.entity.Bot;
import com.replai.backend.entity.Chat;
import com.replai.backend.entity.Message;
import com.replai.backend.repository.BotRepository;
import com.replai.backend.repository.ChatRepository;
import com.replai.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final BotRepository botRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<ChatSummaryResponse> getChats() {
        Bot bot = getBotForCurrentUser();
        return bot.getChats().stream()
                .map(chat -> {
                    Message last = chat.getMessages().stream()
                            .max(Comparator.comparing(Message::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())))
                            .orElse(null);
                    return ChatSummaryResponse.builder()
                            .id(chat.getId())
                            .externalChatId(chat.getExternalChatId())
                            .sourceChannel(chat.getSourceChannel())
                            .lastMessage(last != null ? last.getContent() : null)
                            .lastMessageAt(last != null ? last.getTimestamp() : null)
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long chatId) {
        Bot bot = getBotForCurrentUser();
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalStateException("Chat not found"));
        if (!chat.getBot().getId().equals(bot.getId())) {
            throw new IllegalStateException("Chat does not belong to current user");
        }
        return chat.getMessages().stream()
                .sorted(Comparator.comparing(Message::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(message -> MessageResponse.builder()
                        .id(message.getId())
                        .content(message.getContent())
                        .isFromClient(message.getIsFromClient())
                        .timestamp(message.getTimestamp())
                        .build())
                .toList();
    }

    private Bot getBotForCurrentUser() {
        return botRepository.findByOwner_Email(securityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new IllegalStateException("Bot not found for current user"));
    }
}

