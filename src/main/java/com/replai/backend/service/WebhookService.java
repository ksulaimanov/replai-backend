package com.replai.backend.service;

import com.replai.backend.dto.ai.AiChatResponseDTO;
import com.replai.backend.dto.telegram.TelegramWebhookUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookPersistenceService webhookPersistenceService;
    private final AiService aiService;
    private final TelegramService telegramService;

    public void processTelegramUpdate(String botToken, String secretToken,
                                      TelegramWebhookUpdate update) {
        if (update == null || update.getMessage() == null || update.getMessage().getChat() == null) {
            log.warn("Telegram update ignored because message/chat is missing");
            return;
        }

        // Phase 1: persist incoming (short transaction)
        WebhookContext ctx = webhookPersistenceService.persistIncoming(botToken, secretToken, update);

        // Phase 2: AI call — no DB transaction held
        AiChatResponseDTO aiResponse = aiService.generateReply(
                ctx.botId(), ctx.externalChatId(), ctx.incomingText(), ctx.systemPrompt());

        // Phase 3: persist reply + HOT_LEAD status (short transaction)
        webhookPersistenceService.persistReply(
                ctx.chatId(), ctx.botId(), ctx.externalChatId(),
                aiResponse.getReply(), aiResponse.isLead(), aiResponse.getLeadSummary());

        // Phase 4: send to Telegram — no DB transaction
        for (String part : aiResponse.getReply().split("\\|\\|\\|")) {
            String text = part.strip();
            if (!text.isEmpty()) {
                telegramService.sendMessage(botToken, ctx.telegramChatId(), text);
            }
        }
    }
}
